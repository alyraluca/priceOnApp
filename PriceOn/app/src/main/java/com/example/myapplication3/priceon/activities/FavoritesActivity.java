package com.example.myapplication3.priceon.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication3.priceon.R;
import com.example.myapplication3.priceon.data.model.Product;
import com.example.myapplication3.priceon.adapter.ProductAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView favoritesRecycler;
    private ProductAdapter adapter;
    private List<Product> favoritesList = new ArrayList<>();
    private FirebaseFirestore db;
    private String uid;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = user != null ? user.getUid() : null;

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);
        topAppBar.setTitle("Favoritos");

        favoritesRecycler = findViewById(R.id.favoritesRecycler);
        favoritesRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter(
                favoritesList,
                product -> {
                    Intent it = new Intent(this, ProductDetailActivity.class);
                    it.putExtra("product", product);
                    startActivity(it);
                }
        );
        favoritesRecycler.setAdapter(adapter);

        bottomNavigationView = findViewById(R.id.bottomNavigationBar);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
                return true;
            } else if (id == R.id.navigation_scan) {
                startActivity(new Intent(this, BarcodeScannerActivity.class));
                return true;
            } else if (id == R.id.navigation_favorites) {
                return true;
            }
            return false;
        });

        if (uid != null) {
            loadFavorites();
        }
    }

    private void loadFavorites() {
        db.collection("users")
                .document(uid)
                .collection("favouriteProducts")
                .get()
                .addOnSuccessListener(query -> {
                    List<String> ids = new ArrayList<>();
                    for (DocumentSnapshot doc : query) {
                        String pid = doc.getString("productId");
                        if (pid != null) ids.add(pid);
                    }
                    fetchProducts(ids);
                });
    }

    private void fetchProducts(List<String> ids) {
        if (ids.isEmpty()) return;

        db.collection("products")
                .whereIn(FieldPath.documentId(), ids)
                .get()
                .addOnSuccessListener(query -> {
                    List<Product> list = new ArrayList<>();
                    for (DocumentSnapshot doc : query) {
                        Product p = doc.toObject(Product.class);
                        if (p != null) {
                            p.setId(doc.getId());
                            list.add(p);
                        }
                    }
                    calculateMinPrices(list);
                });
    }

    private void calculateMinPrices(List<Product> list) {
        AtomicInteger processed = new AtomicInteger(0);
        for (Product p : list) {
            db.collection("productSupermarket")
                    .whereEqualTo("productId", p.getId())
                    .get()
                    .addOnSuccessListener(snaps -> {
                        if (snaps.isEmpty()) {
                            p.setMinPrice(0.0);
                            afterPriceLoaded(p, list, processed);
                        } else {
                            final double[] min = { Double.MAX_VALUE };
                            AtomicInteger innerProcessed = new AtomicInteger(0);
                            for (DocumentSnapshot superDoc : snaps) {
                                String psId = superDoc.getId();
                                String supId = superDoc.getString("supermarketId");
                                db.collection("productSupermarket")
                                        .document(psId)
                                        .collection("priceUpdate")
                                        .orderBy("lastPriceUpdate", Query.Direction.DESCENDING)
                                        .limit(1)
                                        .get()
                                        .addOnSuccessListener(priceSnaps -> {
                                            for (DocumentSnapshot priceDoc : priceSnaps) {
                                                Double price = priceDoc.getDouble("price");
                                                if (price != null && price < min[0]) {
                                                    min[0] = price;
                                                }
                                            }
                                            if (innerProcessed.incrementAndGet() == snaps.size()) {
                                                p.setMinPrice(min[0] < Double.MAX_VALUE ? min[0] : 0.0);
                                                afterPriceLoaded(p, list, processed);
                                            }
                                        });
                            }
                        }
                    });
        }
    }

    private void afterPriceLoaded(Product p, List<Product> list, AtomicInteger processed) {
        loadProductBrand(p, () ->
                loadPricesAndSupermarkets(p, () -> {
                    favoritesList.add(p);
                    if (processed.incrementAndGet() == list.size()) {
                        adapter.notifyDataSetChanged();
                    }
                })
        );
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

    private void loadPricesAndSupermarkets(Product product, Runnable onDone) {
        db.collection("productSupermarket")
                .whereEqualTo("productId", product.getId())
                .get()
                .addOnSuccessListener(productSuperDocs -> {
                    if (productSuperDocs.isEmpty()) {
                        onDone.run();
                        return;
                    }
                    AtomicInteger done = new AtomicInteger(0);
                    for (DocumentSnapshot superDoc : productSuperDocs) {
                        String psId = superDoc.getId();
                        String supId = superDoc.getString("supermarketId");

                        db.collection("productSupermarket")
                                .document(psId)
                                .collection("priceUpdate")
                                .orderBy("lastPriceUpdate", Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(priceDocs -> {
                                    for (DocumentSnapshot pd : priceDocs) {
                                        Double price = pd.getDouble("price");
                                        if (price != null && price < product.getMinPrice()) {
                                            product.setMinPrice(price);
                                        }
                                    }
                                    db.collection("supermarkets")
                                            .document(supId)
                                            .get()
                                            .addOnSuccessListener(sDoc -> {
                                                String logoUrl = sDoc.getString("logoUrl");
                                                if (logoUrl != null) {
                                                    product.addSupermarketLogoUrl(logoUrl);
                                                }
                                                if (done.incrementAndGet() == productSuperDocs.size()) {
                                                    onDone.run();
                                                }
                                            });
                                });
                    }
                })
                .addOnFailureListener(e -> onDone.run());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar_menu, menu);
        return true;
    }
}
