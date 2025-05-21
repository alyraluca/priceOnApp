package com.example.myapplication3.priceon;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication3.priceon.ui.HomeActivity;
import com.example.myapplication3.priceon.ui.MainActivity;
import com.github.mikephil.charting.charts.LineChart;

import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.myapplication3.priceon.data.model.Product;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProductDetailActivity extends AppCompatActivity {

    private ImageView productImage, favoriteIcon;
    private TextView productName, productBrand, fromLabel, minPriceLabel;
    private LinearLayout supermarketListContainer;
    private LineChart priceEvolutionChart;
    private BottomNavigationView bottomNavigationView;

    //Changes
    private boolean isFavorite = false;
    private String favDocId;
    private FirebaseFirestore db;
    private String uid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_product_detail);

            // inicializaciones
            db = FirebaseFirestore.getInstance();
            uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
            favoriteIcon             = findViewById(R.id.favoriteIcon);

            productImage = findViewById(R.id.productImage);
            productName = findViewById(R.id.productName);
            productBrand = findViewById(R.id.productBrand);
            fromLabel = findViewById(R.id.fromLabel);
            minPriceLabel = findViewById(R.id.minPriceLabel);
            supermarketListContainer = findViewById(R.id.supermarketListContainer);
            priceEvolutionChart = findViewById(R.id.priceEvolutionChart);
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
                    startActivity(new Intent(this, FavoritesActivity.class));
                    return true;
                }
                return false;
            });

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_profile) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                if (currentUser != null) {
                    startActivity(new Intent(this, ProfileActivity.class));
                } else {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish(); // Cierra HomeActivity para que no pueda volver con el botón atrás
                }

                return true;
            }
            return false;
        });

            Product product = (Product) getIntent().getSerializableExtra("product");

            if (product == null) return; //

            productName.setText(product.getName());
            productBrand.setText(product.getBrandName());
            minPriceLabel.setText(product.getMinPrice() + " €");

            Glide.with(this)
                    .load(product.getPhotoUrl())
                    .into(productImage);

            checkFavoriteState(product.getId());
            favoriteIcon.setOnClickListener(v -> {
                if (isFavorite) {
                    removeFromFavorites();
                } else {
                    addToFavorites(product.getId());
                }
            });


            loadSupermarkets(product);
            loadPriceEvolution(product);

    //        if (product != null) {
    //            productName.setText(product.getName());
    //            productBrand.setText(product.getBrandName());
    //            minPriceLabel.setText(product.getMinPrice() + " €");
    //
    //            Glide.with(this)
    //                    .load(product.getPhotoUrl())
    //                    .into(productImage);
    //
    //            loadSupermarkets(product);
    //            loadPriceEvolution(product);
    //
    //        }
    }

    private void checkFavoriteState(String productId) {
        db.collection("users")
                .document(uid)
                .collection("favouriteProducts")
                .whereEqualTo("productId", productId)
                .limit(1)
                .get()
                .addOnSuccessListener(q -> {
                    if (!q.isEmpty()) {
                        isFavorite = true;
                        DocumentSnapshot doc = q.getDocuments().get(0);
                        favDocId = doc.getId();
                        favoriteIcon.setColorFilter(
                                getResources().getColor(android.R.color.holo_red_dark)
                        );
                    } else {
                        isFavorite = false;
                        favoriteIcon.setColorFilter(
                                getResources().getColor(android.R.color.darker_gray)
                        );
                    }
                });
    }

    private void addToFavorites(String productId) {
        Map<String, Object> fav = new HashMap<>();
        fav.put("productId", productId);
        db.collection("users")
                .document(uid)
                .collection("favouriteProducts")
                .add(fav)
                .addOnSuccessListener(docRef -> {
                    isFavorite = true;
                    favDocId = docRef.getId();
                    favoriteIcon.setColorFilter(
                            getResources().getColor(android.R.color.holo_red_dark)
                    );
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al guardar favorito", Toast.LENGTH_SHORT).show();
                });
    }

    private void removeFromFavorites() {
        if (favDocId == null) return;
        db.collection("users")
                .document(uid)
                .collection("favouriteProducts")
                .document(favDocId)
                .delete()
                .addOnSuccessListener(v -> {
                    isFavorite = false;
                    favoriteIcon.setColorFilter(
                            getResources().getColor(android.R.color.darker_gray)
                    );
                    favDocId = null;
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al eliminar favorito", Toast.LENGTH_SHORT).show();
                });
    }
    private void loadSupermarkets(Product product) {
        LinearLayout container = findViewById(R.id.supermarketListContainer);
        container.removeAllViews();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("productSupermarket")
                .whereEqualTo("productId", product.getId())
                .get()
                .addOnSuccessListener(superDocs -> {
                    for (DocumentSnapshot doc : superDocs) {
                        String supermarketId = doc.getString("supermarketId");
                        double quantityUnity = product.getQuantityUnity();
                        String unit = product.getUnit() != null ? product.getUnit() : "";

                        Log.d("DEBUG", "quantityUnity: " + quantityUnity + ", unit: " + unit);

                        db.collection("supermarkets").document(supermarketId).get()
                                .addOnSuccessListener(supermarketDoc -> {

                                    String supermarketName = supermarketDoc.getString("name");

                                    db.collection("productSupermarket").document(doc.getId())
                                            .collection("priceUpdate")
                                            .orderBy("lastPriceUpdate", Query.Direction.DESCENDING)
                                            .limit(1)
                                            .get()
                                            .addOnSuccessListener(priceDocs -> {
                                                if (!priceDocs.isEmpty()) {
                                                    DocumentSnapshot priceDoc = priceDocs.getDocuments().get(0);
                                                    Double price = priceDoc.getDouble("price");

                                                    View item = LayoutInflater.from(this).inflate(R.layout.item_supermarket_info, container, false);

                                                    ImageView logo = item.findViewById(R.id.supermarketLogo);
                                                    TextView name = item.findViewById(R.id.supermarketName);
                                                    TextView priceText = item.findViewById(R.id.supermarketPrice);
                                                    TextView unitPriceText = item.findViewById(R.id.supermarketUnitPrice);

                                                    name.setText(supermarketName);
                                                    priceText.setText(String.format("%.2f €", price));

                                                    if (quantityUnity > 0 && price != null) {
                                                        double unitPrice = price / quantityUnity;
                                                        unitPriceText.setText(String.format("%.2f € / %s", unitPrice, unit));
                                                        unitPriceText.setVisibility(View.VISIBLE);
                                                    } else {
                                                        unitPriceText.setText("N/A");
                                                    }
                                                    Log.d("DEBUG", "price: " + price);

                                                    if ("Mercadona".equalsIgnoreCase(supermarketName)) {
                                                        logo.setImageResource(R.drawable.mercadona);
                                                    } else if ("Dia".equalsIgnoreCase(supermarketName)) {
                                                        logo.setImageResource(R.drawable.dia_logo);
                                                    } else {
                                                        logo.setImageResource(R.drawable.alcampo);
                                                    }

                                                    container.addView(item);
                                                }
                                            });
                                });
                    }
                });
    }

    private void loadPriceEvolution(Product product) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String productId = product.getId(); // Asegúrate de que getId() devuelve el ID de Firestore

        Map<Long, List<Double>> pricesByDate = new HashMap<>();

        db.collection("productSupermarket")
                .whereEqualTo("productId", productId)
                .get()
                .addOnSuccessListener(supermarketDocs -> {
                    int supermarketsCount = supermarketDocs.size();
                    if (supermarketsCount == 0) return;

                    final int[] processed = {0};

                    for (DocumentSnapshot superDoc : supermarketDocs) {
                        db.collection("productSupermarket")
                                .document(superDoc.getId())
                                .collection("priceUpdate")
                                .get()
                                .addOnSuccessListener(priceDocs -> {
                                    for (DocumentSnapshot priceDoc : priceDocs) {
                                        Double price = priceDoc.getDouble("price");

                                        Timestamp ts = priceDoc.getTimestamp("lastPriceUpdate");
                                        Long timestamp = (ts != null) ? ts.toDate().getTime() : null;


                                        if (price != null && timestamp != null) {
                                            if (!pricesByDate.containsKey(timestamp)) {
                                                pricesByDate.put(timestamp, new ArrayList<>());
                                            }
                                            pricesByDate.get(timestamp).add(price);
                                        }
                                    }

                                    processed[0]++;
                                    if (processed[0] == supermarketsCount) {
                                        drawPriceEvolutionChart(pricesByDate);
                                    }
                                });
                    }
                });
    }

    private void drawPriceEvolutionChart(Map<Long, List<Double>> pricesByDate) {
        List<Long> sortedDates = new ArrayList<>(pricesByDate.keySet());
        Collections.sort(sortedDates);

        // Quedarse solo con las últimas 4 fechas
        if (sortedDates.size() > 4) {
            sortedDates = sortedDates.subList(sortedDates.size() - 4, sortedDates.size());
        }

        List<Entry> entries = new ArrayList<>();
        int index = 0;

        for (Long timestamp : sortedDates) {
            List<Double> prices = pricesByDate.get(timestamp);
            double sum = 0;
            for (Double p : prices) sum += p;
            double avg = sum / prices.size();

            entries.add(new Entry(index, (float) avg));
            index++;
        }

        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(true);
        dataSet.setCircleRadius(5f);
        dataSet.setCircleColor(getResources().getColor(android.R.color.holo_blue_dark));
        dataSet.setColor(getResources().getColor(android.R.color.holo_blue_dark));
        dataSet.setLineWidth(2f);

        LineData lineData = new LineData(dataSet);
        priceEvolutionChart.setData(lineData);

        float minPrice = Float.MAX_VALUE;
        float maxPrice = Float.MIN_VALUE;
        for (Entry e : entries) {
            float y = e.getY();
            if (y < minPrice) minPrice = y;
            if (y > maxPrice) maxPrice = y;
        }

        YAxis yAxis = priceEvolutionChart.getAxisLeft();

        float margin = (maxPrice - minPrice) * 0.1f;
        yAxis.setAxisMinimum(minPrice-margin);
        yAxis.setAxisMaximum(maxPrice+margin);
        yAxis.setLabelCount(2, true);

        priceEvolutionChart.getAxisRight().setEnabled(false);

        priceEvolutionChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);

        priceEvolutionChart.getXAxis().setGranularity(1f);
        priceEvolutionChart.getXAxis().setLabelCount(2, true);
        List<Long> finalSortedDates = sortedDates;
        priceEvolutionChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int i = Math.round(value);
                if (i >= 0 && i < finalSortedDates.size()) {
                    if (i == 0 || i == finalSortedDates.size() - 1) {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM");
                        return sdf.format(new java.util.Date(finalSortedDates.get(i)));
                    }
                }
                return "";
            }
        });

        priceEvolutionChart.getDescription().setEnabled(false);
        priceEvolutionChart.getLegend().setEnabled(false);
        priceEvolutionChart.invalidate();
    }





}