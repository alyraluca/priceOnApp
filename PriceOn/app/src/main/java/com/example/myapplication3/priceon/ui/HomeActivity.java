package com.example.myapplication3.priceon.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication3.priceon.BarcodeScannerActivity;
import com.example.myapplication3.priceon.R;
import com.example.myapplication3.priceon.data.model.Product;
import com.example.myapplication3.priceon.ui.adapter.ProductAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeActivity extends AppCompatActivity {
    private EditText searchEditText;
    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private List<Product> productList = new ArrayList<>();
    private FirebaseFirestore db;
    private BottomNavigationView bottomNavigationView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        bottomNavigationView = findViewById(R.id.bottomNavigationBar);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {

                return true;
            } else if (id == R.id.navigation_scan) {
                Intent intent = new Intent(this, BarcodeScannerActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.navigation_favorites) {
                // Acción para favoritos
                return true;
            }
            return false;
        });


        searchEditText = findViewById(R.id.searchEditText);
        recyclerView = findViewById(R.id.contentRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ProductAdapter(productList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            String searchText = searchEditText.getText().toString().trim();
            if (!searchText.isEmpty()) {
                searchProducts(searchText);
            }
            return true;
        });

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