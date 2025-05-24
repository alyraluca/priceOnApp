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
        historyRv = findViewById(R.id.historyRecyclerView);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = user != null ? user.getUid() : null;

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (uid == null) {
            // Sin usuario: mostramos mensaje y ocultamos la lista
            labelSearchHistory.setText("");
            historyRv.setVisibility(View.GONE);
        } else {
            // Usuario logueado: inicializamos el RecyclerView y cargamos el historial
            labelSearchHistory.setText("Últimas búsquedas");
            historyRv.setVisibility(View.VISIBLE);

            historyRv.setLayoutManager(
                    new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            );
            historyAdapter = new ProductAdapter(historyList, p -> {
                // para el historial sólo abrimos detalle
                startDetail(p);
            });
            historyRv.setAdapter(historyAdapter);
            loadSearchHistory();
        }

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


//        // Historial horizontal
//        historyRv = findViewById(R.id.historyRecyclerView);
//        historyRv.setLayoutManager(
//                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
//        );
//        historyAdapter = new ProductAdapter(historyList, p->{
//            // para el historial sólo abrimos detalle
//            startDetail(p);
//        });
//        historyRv.setAdapter(historyAdapter);
//        loadSearchHistory();

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
        historyList.clear();
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("users")
                .document(uid)
                .collection("searchHistory")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot hs : snap) {
                        String pid = hs.getString("productId");
                        // 1) traigo el doc product
                        db.collection("products").document(pid)
                                .get()
                                .addOnSuccessListener(pd -> {
                                    Product p = pd.toObject(Product.class);
                                    if (p == null) return;
                                    p.setId(pd.getId());
                                    // 2) cargo la marca
                                    loadProductBrand(p, () -> {
                                        // 3) cargo precios y logos, y sólo al final añado al historial
                                        loadPricesAndSupermarkets(p, () -> {
                                            historyList.add(p);
                                            historyAdapter.notifyDataSetChanged();
                                        });
                                    });
                                });
                    }
                });
    }


    private void loadProductBrand(Product product, Runnable onDone) {
        if (product.getBrandId() == null) {
            onDone.run();
            return;
        }
        db.collection("brands")
                .document(product.getBrandId())
                .get()
                .addOnSuccessListener(brandDoc -> {
                    product.setBrandName(brandDoc.getString("name"));
                    onDone.run();
                })
                .addOnFailureListener(e -> onDone.run());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar_menu, menu);
        return true;
    }


    private void searchProducts(String searchText) {
        if (searchText.isEmpty()) {
            // Si borras el texto, restauramos la vista de historial
            labelSearchHistory.setVisibility(View.VISIBLE);
            historyRv.setVisibility(View.VISIBLE);

            productList.clear();
            adapter.notifyDataSetChanged();
            return;
        }

        // Ya sabemos que queremos resultados de búsqueda: ocultamos el historial
        labelSearchHistory.setVisibility(View.GONE);
        historyRv.setVisibility(View.GONE);

        db.collection("products")
                .orderBy("name")
                .startAt(searchText)
                .endAt(searchText + "\uf8ff")
                .get()
                .addOnSuccessListener(productSnapshots -> {
                    // Vaciamos la lista para los nuevos resultados
                    productList.clear();
                    adapter.notifyDataSetChanged();  // limpia la RecyclerView

                    for (DocumentSnapshot doc : productSnapshots.getDocuments()) {
                        Product p = doc.toObject(Product.class);
                        if (p == null) continue;
                        p.setId(doc.getId());

                        // Cadena de callbacks: marca → precios/logos → añadir a lista
                        loadProductBrand(p, () -> {
                            loadPricesAndSupermarkets(p, () -> {
                                productList.add(p);
                                adapter.notifyDataSetChanged();
                            });
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error buscando productos", Toast.LENGTH_SHORT).show();
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

    private void loadPricesAndSupermarkets(Product product, Runnable onDone) {
        db.collection("productSupermarket")
                .whereEqualTo("productId", product.getId())
                .get()
                .addOnSuccessListener(productSuperDocs -> {
                    if (productSuperDocs.isEmpty()) {
                        onDone.run();
                        return;
                    }

                    AtomicInteger processed = new AtomicInteger(0);
                    double[] min = { Double.MAX_VALUE };
                    String[] cheapestSuperId = { null };

                    for (DocumentSnapshot superDoc : productSuperDocs) {
                        String psId = superDoc.getId();
                        String supermarketId = superDoc.getString("supermarketId");

                        db.collection("productSupermarket")
                                .document(psId)
                                .collection("priceUpdate")
                                .orderBy("lastPriceUpdate", Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(priceDocs -> {
                                    for (DocumentSnapshot pd : priceDocs) {
                                        Double price = pd.getDouble("price");
                                        if (price != null && price < min[0]) {
                                            min[0] = price;
                                            cheapestSuperId[0] = supermarketId;
                                        }
                                    }
                                    db.collection("supermarkets")
                                            .document(supermarketId)
                                            .get()
                                            .addOnSuccessListener(sDoc -> {
                                                String logoUrl = sDoc.getString("logoUrl");
                                                if (logoUrl != null) product.addSupermarketLogoUrl(logoUrl);

                                                if (processed.incrementAndGet() == productSuperDocs.size()) {
                                                    if (min[0] < Double.MAX_VALUE) {
                                                        product.setMinPrice(min[0]);
                                                        product.setCheapestSupermarketId(cheapestSuperId[0]);
                                                    }
                                                    onDone.run();
                                                }
                                            });
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    onDone.run();
                });
    }


}