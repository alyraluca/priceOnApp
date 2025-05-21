package com.example.myapplication3.priceon;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication3.priceon.data.model.Product;
import com.example.myapplication3.priceon.ui.HomeActivity;
import com.example.myapplication3.priceon.ui.adapter.FavoritesAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView favoritesRecycler;
    private FavoritesAdapter adapter;
    private FirebaseFirestore db;
    private String uid;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        bottomNavigationView = findViewById(R.id.bottomNavigationBar);
        favoritesRecycler = findViewById(R.id.favoritesRecycler);
        favoritesRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FavoritesAdapter(this, new ArrayList<>());
        favoritesRecycler.setAdapter(adapter);


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

        loadFavorites();
    }

    private void loadFavorites() {
        db.collection("users")
                .document(uid)
                .collection("favouriteProducts")
                .get()
                .addOnSuccessListener(query -> {
                    List<String> ids = new ArrayList<>();
                    for (DocumentSnapshot doc : query) {
                        ids.add(doc.getString("productId"));
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
                    List<Product> favorites = new ArrayList<>();
                    for (DocumentSnapshot doc : query) {
                        Product p = doc.toObject(Product.class);
                        p.setId(doc.getId());
                        favorites.add(p);
                    }
                    // Ahora calculamos el precio mínimo de cada uno:
                    calculateMinPrices(favorites);
                });
    }

    private void calculateMinPrices(List<Product> list) {
        AtomicInteger processed = new AtomicInteger(0);
        for (Product p : list) {
            db.collection("productSupermarket")
                    .whereEqualTo("productId", p.getId())
                    .get()
                    .addOnSuccessListener(snaps -> {
                        final double[] min = {Double.MAX_VALUE};
                        for (DocumentSnapshot superDoc : snaps) {
                            String psId = superDoc.getId();
                            // obtenemos el último precio
                            db.collection("productSupermarket")
                                    .document(psId)
                                    .collection("priceUpdate")
                                    .orderBy("lastPriceUpdate", Query.Direction.DESCENDING)
                                    .limit(1)
                                    .get()
                                    .addOnSuccessListener(priceSnaps -> {
                                        for (DocumentSnapshot priceDoc : priceSnaps) {
                                            Double price = priceDoc.getDouble("price");
                                            if (price != null && price < min[0]) min[0] = price;
                                        }
                                        // si hemos encontrado uno
                                        if (min[0] < Double.MAX_VALUE) {
                                            p.setMinPrice(min[0]);
                                        } else {
                                            p.setMinPrice(0.0); // o marca N/A
                                        }
                                        if (processed.incrementAndGet() == list.size()) {
                                            // todos procesados
                                            adapter.setItems(list);
                                        }
                                    });
                        }
                        // si no había supermercados:
                        if (snaps.isEmpty() && processed.incrementAndGet() == list.size()) {
                            adapter.setItems(list);
                        }
                    });
        }
    }

}