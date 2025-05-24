package com.example.myapplication3.priceon.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication3.priceon.AddProductActivity;
import com.example.myapplication3.priceon.BarcodeScannerActivity;
import com.example.myapplication3.priceon.FavoritesActivity;
import com.example.myapplication3.priceon.ProductDetailActivity;
import com.example.myapplication3.priceon.ProfileActivity;
import com.example.myapplication3.priceon.R;
import com.example.myapplication3.priceon.data.model.Product;
import com.example.myapplication3.priceon.ui.adapter.ProductAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeActivity extends AppCompatActivity {
    private EditText searchEditText;
    private TextView labelSearchHistory;
    private RecyclerView recyclerView, historyRv;
    private ProductAdapter adapter, historyAdapter;
    private List<Product> productList = new ArrayList<>();
    private FirebaseFirestore db;
    private BottomNavigationView bottomNavigationView;
    private String uid;
    private List<Product> historyList = new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        bottomNavigationView = findViewById(R.id.bottomNavigationBar);

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);

        searchEditText = findViewById(R.id.searchEditText);
        recyclerView = findViewById(R.id.contentRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        labelSearchHistory = findViewById(R.id.labelSearchHistory);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = user != null ? user.getUid() : null;

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ProductAdapter(
                productList,
                product -> {
                    // 1) Guarda en el historial
                    if (uid != null) {
                        Map<String,Object> entry = new HashMap<>();
                        entry.put("productId", product.getId());
                        entry.put("timestamp", com.google.firebase.Timestamp.now());
                        db.collection("users")
                                .document(uid)
                                .collection("searchHistory")
                                .add(entry);
                    }
                    // 2) Abre la pantalla de detalle
                    Intent it = new Intent(this, ProductDetailActivity.class);
                    it.putExtra("product", product);
                    startActivity(it);
                }
        );

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            String searchText = searchEditText.getText().toString().trim();
            if (!searchText.isEmpty()) {
                // ocultamos histórico
                labelSearchHistory.setVisibility(View.GONE);
                historyRv.setVisibility(View.GONE);

                searchProducts(searchText);
            } else {
                // volvemos a mostrar histórico si el campo está vacío
                labelSearchHistory.setVisibility(View.VISIBLE);
                historyRv.setVisibility(View.VISIBLE);

                productList.clear();
                adapter.notifyDataSetChanged();
            }
            return true;
        });


        // Historial horizontal
        historyRv = findViewById(R.id.historyRecyclerView);
        historyRv.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        historyAdapter = new ProductAdapter(historyList, p->{
            // para el historial sólo abrimos detalle
            startDetail(p);
        });
        historyRv.setAdapter(historyAdapter);
        loadSearchHistory();

        adapter = new ProductAdapter(productList, p-> {
            // sólo cuando hay texto en el buscador guardamos en searchHistory
            String txt = searchEditText.getText().toString().trim();
            if (!txt.isEmpty() && uid!=null) {
                Map<String,Object> entry = new HashMap<>();
                entry.put("productId", p.getId());
                entry.put("timestamp", com.google.firebase.Timestamp.now());
                db.collection("users")
                        .document(uid)
                        .collection("searchHistory")
                        .add(entry);
            }
            startDetail(p);
        });
        recyclerView.setAdapter(adapter);

        bottomNavigationView = findViewById(R.id.bottomNavigationBar);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                return true;
            } else if (id == R.id.navigation_scan) {
                startActivity(new Intent(this, BarcodeScannerActivity.class));
                return true;
            } else if (id == R.id.navigation_favorites) {
                startActivity(new Intent(this, FavoritesActivity.class));
                return true;
            }
            return false;
        });


    }
    private void startDetail(Product p) {
        Intent it = new Intent(this, ProductDetailActivity.class);
        it.putExtra("product", p);
        startActivity(it);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add) {
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            if (u == null) {
                Toast.makeText(this, "Necesitas estar logueado", Toast.LENGTH_SHORT).show();
            } else {
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(u.getUid())
                        .get()
                        .addOnSuccessListener(doc -> {
                            String role = doc.getString("role");
                            if ("admin".equals(role)) {
                                startActivity(new Intent(this, AddProductActivity.class));
                            } else {
                                Toast.makeText(this,
                                        "Necesitas ser administrador para añadir productos",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
            return true;
        }
        else if (id == R.id.action_profile) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            Intent intent;
            if (currentUser != null) {
                intent = new Intent(this, ProfileActivity.class);
            } else {
                intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            }
            startActivity(intent);
            if (currentUser == null) finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadSearchHistory() {
        if (uid==null) return;
        historyList.clear();
        db.collection("users")
                .document(uid)
                .collection("searchHistory")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get().addOnSuccessListener(snap -> {
                    for (var hs: snap) {
                        String pid = hs.getString("productId");
                        db.collection("products").document(pid)
                                .get().addOnSuccessListener(pd -> {
                                    Product p = pd.toObject(Product.class);
                                    if (p!=null) {
                                        p.setId(pd.getId());
                                        historyList.add(p);
                                        historyAdapter.notifyDataSetChanged();
                                    }
                                });
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar_menu, menu);
        return true;
    }


    private void searchProducts(String searchText) {
        if (searchText.isEmpty()) {
            productList.clear();
            adapter.notifyDataSetChanged();
            return;
        }
        db.collection("products")
                .orderBy("name")
                .startAt(searchText)
                .endAt(searchText + "\uf8ff")
                .get()
                .addOnSuccessListener(productSnapshots -> {
                    productList.clear();

                    for (DocumentSnapshot doc : productSnapshots.getDocuments()) {
                        Product product = doc.toObject(Product.class);
                        if (product == null) continue;
                        product.setId(doc.getId());

                        loadProductBrand(product);
                        loadPricesAndSupermarkets(product);

                        productList.add(product);
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    private void loadProductBrand(Product product) {
        if (product.getBrandId() == null) return;

        db.collection("brands")
                .document(product.getBrandId())
                .get()
                .addOnSuccessListener(brandDoc -> {
                    if (brandDoc.exists()) {
                        product.setBrandName(brandDoc.getString("name"));
                        adapter.notifyDataSetChanged();
                    }
                });
    }
    private void loadPricesAndSupermarkets(Product product) {
        db.collection("productSupermarket")
                .whereEqualTo("productId", product.getId())
                .get()
                .addOnSuccessListener(productSuperDocs -> {
                    if (productSuperDocs.isEmpty()) return;

                    AtomicInteger docsProcessed = new AtomicInteger(0);
                    int totalDocs = productSuperDocs.size();
                    double[] min = {Double.MAX_VALUE};
                    String[] cheapestSuperId = {null};

                    for (DocumentSnapshot superDoc : productSuperDocs) {
                        String superDocId = superDoc.getId();
                        String supermarketId = superDoc.getString("supermarketId");

                        db.collection("productSupermarket")
                                .document(superDocId)
                                .collection("priceUpdate")
                                .orderBy("lastPriceUpdate", Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(priceUpdates -> {
                                    for (DocumentSnapshot priceDoc : priceUpdates) {
                                        Double price = priceDoc.getDouble("price");
                                        if (price != null && price < min[0]) {
                                            min[0] = price;
                                            cheapestSuperId[0] = supermarketId;
                                        }
                                    }

                                    db.collection("supermarkets")
                                            .document(supermarketId)
                                            .get()
                                            .addOnSuccessListener(superDocFull -> {
                                                if (superDocFull.exists()) {
                                                    String logoUrl = superDocFull.getString("logoUrl");
                                                    if (logoUrl != null) {
                                                        product.addSupermarketLogoUrl(logoUrl);
                                                    }
                                                }

                                                if (docsProcessed.incrementAndGet() == totalDocs) {
                                                    if (min[0] < Double.MAX_VALUE) {
                                                        product.setMinPrice(min[0]);
                                                        product.setCheapestSupermarketId(cheapestSuperId[0]);
                                                    }
                                                    adapter.notifyDataSetChanged();
                                                }
                                            });
                                });
                    }
                });
    }

}