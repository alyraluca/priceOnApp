package com.example.myapplication3.priceon.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.myapplication3.priceon.R;
import com.example.myapplication3.priceon.data.model.Product;
import com.example.myapplication3.priceon.adapter.ProductAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeActivity extends AppCompatActivity {
    private EditText searchEditText;
    private TextView labelSearchHistory, labelNearby;
    private RecyclerView recyclerView, historyRecyclerView;
    private ProductAdapter adapter, historyAdapter;
    List<Product> productList = new ArrayList<>();
    FirebaseFirestore db;
    private BottomNavigationView bottomNavigationView;
    String uid;
    private List<Product> historyList = new ArrayList<>();
    private LinearLayout nearbyContainer;
    private static final int REQ_LOC = 42;

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
        historyRecyclerView = findViewById(R.id.historyRecyclerView);
        labelNearby        = findViewById(R.id.labelNearby);
        nearbyContainer    = findViewById(R.id.nearbyContainer);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = user != null ? user.getUid() : null;

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (uid == null) {
            labelSearchHistory.setVisibility(View.GONE);
            historyRecyclerView.setVisibility(View.GONE);
        } else {
            labelSearchHistory.setText("Últimas búsquedas");
            historyRecyclerView.setVisibility(View.VISIBLE);
            historyRecyclerView.setLayoutManager(
                    new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            );
            historyAdapter = new ProductAdapter(historyList, p -> {
                startDetail(p);
            });
            historyRecyclerView.setAdapter(historyAdapter);
            loadSearchHistory();
        }

        adapter = new ProductAdapter(
                productList,
                product -> {
                    if (uid != null) {
                        Map<String,Object> entry = new HashMap<>();
                        entry.put("productId", product.getId());
                        entry.put("timestamp", com.google.firebase.Timestamp.now());
                        db.collection("users")
                                .document(uid)
                                .collection("searchHistory")
                                .add(entry);
                    }
                    Intent it = new Intent(this, ProductDetailActivity.class);
                    it.putExtra("product", product);
                    startActivity(it);
                }
        );

        searchEditText.setOnEditorActionListener((v, a, e) -> {
            String txt = v.getText().toString().trim();
            if (txt.isEmpty()) {
                if (uid != null) {
                    labelSearchHistory.setVisibility(View.VISIBLE);
                    historyRecyclerView.setVisibility(View.VISIBLE);
                }
                labelNearby.setVisibility(View.VISIBLE);
                findViewById(R.id.nearbyCard).setVisibility(View.VISIBLE);
                productList.clear();
                adapter.notifyDataSetChanged();
            } else {
                labelSearchHistory.setVisibility(View.GONE);
                historyRecyclerView.setVisibility(View.GONE);
                labelNearby.setVisibility(View.GONE);
                findViewById(R.id.nearbyCard).setVisibility(View.GONE);
                searchProducts(txt);
            }
            return true;
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, REQ_LOC);
        } else {
            setupNearbySupermarkets();
        }

        adapter = new ProductAdapter(productList, p-> {
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
    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms, @NonNull int[] grants) {
        super.onRequestPermissionsResult(req, perms, grants);
        if (req == REQ_LOC) {
            if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) {
                setupNearbySupermarkets();
            } else {
                findViewById(R.id.nearbyCard).setVisibility(View.GONE);
            }
        }
    }

    public void setupNearbySupermarkets() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            findViewById(R.id.nearbyCard).setVisibility(View.GONE);
            return;
        }
        Location last = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (last == null) {
            last = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        if (last == null) {
            findViewById(R.id.nearbyCard).setVisibility(View.GONE);
        } else {
            loadNearbySupermarkets(last, nearbyContainer);
        }
    }
    public void loadNearbySupermarkets(Location userLoc, LinearLayout container) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("supermarkets")
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot supDoc: snap.getDocuments()) {
                        String supId   = supDoc.getId();
                        String logoUrl = supDoc.getString("logoUrl");

                        db.collection("supermarkets")
                                .document(supId)
                                .collection("locations")
                                .get()
                                .addOnSuccessListener(locSnap -> {
                                    double bestDist = Double.MAX_VALUE;
                                    DocumentSnapshot bestLocDoc = null;
                                    for (DocumentSnapshot locDoc: locSnap.getDocuments()) {
                                        GeoPoint gp = locDoc.getGeoPoint("location");
                                        if (gp == null) continue;
                                        float[] results = new float[1];
                                        Location.distanceBetween(
                                                userLoc.getLatitude(), userLoc.getLongitude(),
                                                gp.getLatitude(), gp.getLongitude(),
                                                results
                                        );
                                        if (results[0] < bestDist) {
                                            bestDist = results[0];
                                            bestLocDoc = locDoc;
                                        }
                                    }
                                    if (bestLocDoc != null) {
                                        View item = LayoutInflater.from(this)
                                                .inflate(R.layout.item_nearby_supermarket, container, false);
                                        ImageView ivLogo = item.findViewById(R.id.nearbyLogo);
                                        TextView  tvName = item.findViewById(R.id.nearbyName);
                                        TextView  tvAddr = item.findViewById(R.id.nearbyAddress);
                                        TextView  tvDist = item.findViewById(R.id.nearbyDistance);

                                        Glide.with(this).load(logoUrl).into(ivLogo);
                                        tvName.setText(bestLocDoc.getString("name"));
                                        tvAddr.setText(bestLocDoc.getString("address"));
                                        tvDist.setText(String.format(Locale.getDefault(),"%.1f km", bestDist/1000));

                                        container.addView(item);
                                    }
                                });
                    }
                });
    }

    public void startDetail(Product p) {
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

    public void loadSearchHistory() {
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
                        db.collection("products").document(pid)
                                .get()
                                .addOnSuccessListener(pd -> {
                                    Product p = pd.toObject(Product.class);
                                    if (p == null) return;
                                    p.setId(pd.getId());
                                    loadProductBrand(p, () -> {
                                        loadPricesAndSupermarkets(p, () -> {
                                            historyList.add(p);
                                            historyAdapter.notifyDataSetChanged();
                                        });
                                    });
                                });
                    }
                });
    }

    public void loadProductBrand(Product product, Runnable onDone) {
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


    public void searchProducts(String searchText) {
        if (searchText.isEmpty()) {
            labelSearchHistory.setVisibility(View.VISIBLE);
            historyRecyclerView.setVisibility(View.VISIBLE);

            productList.clear();
            adapter.notifyDataSetChanged();
            return;
        }
        labelSearchHistory.setVisibility(View.GONE);
        historyRecyclerView.setVisibility(View.GONE);

        db.collection("products")
                .orderBy("name")
                .startAt(searchText)
                .endAt(searchText + "\uf8ff")
                .get()
                .addOnSuccessListener(productSnapshots -> {
                    productList.clear();
                    adapter.notifyDataSetChanged();
                    for (DocumentSnapshot doc : productSnapshots.getDocuments()) {
                        Product p = doc.toObject(Product.class);
                        if (p == null) continue;
                        p.setId(doc.getId());
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


    public void loadProductBrand(Product product) {
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

    public void loadPricesAndSupermarkets(Product product, Runnable onDone) {
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