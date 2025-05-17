package com.example.myapplication3.priceon;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.myapplication3.priceon.ui.HomeActivity;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductDetailActivity extends AppCompatActivity {

    private ImageView productImage;
    private TextView productName, productBrand, fromLabel, minPriceLabel;
    private LinearLayout supermarketListContainer;
    private LineChart priceEvolutionChart;
    private BottomNavigationView bottomNavigationView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

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
                Intent homeIntent = new Intent(ProductDetailActivity.this, HomeActivity.class);
                startActivity(homeIntent);
                finish();
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

        Product product = (Product) getIntent().getSerializableExtra("product");

        if (product != null) {
            productName.setText(product.getName());
            productBrand.setText(product.getBrandName());
            minPriceLabel.setText(product.getMinPrice() + " €");

            Glide.with(this)
                    .load(product.getPhotoUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(productImage);

            loadSupermarkets(product);
            loadPriceEvolution(product);

        }
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
                                    if (supermarketDoc.exists()) {
                                        String supermarketName = supermarketDoc.getString("name");

                                        if (supermarketName != null) {
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
                                        }
                                    }
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

        float margin = (maxPrice - minPrice) * 0.05f;
        yAxis.setAxisMinimum(minPrice);
        yAxis.setAxisMaximum(maxPrice);
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