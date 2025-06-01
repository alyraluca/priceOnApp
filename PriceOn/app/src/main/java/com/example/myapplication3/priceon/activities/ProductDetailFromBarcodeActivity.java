package com.example.myapplication3.priceon.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myapplication3.priceon.R;
import com.example.myapplication3.priceon.data.model.Product;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProductDetailFromBarcodeActivity extends AppCompatActivity {
    private ImageView productImage, favoriteIcon;
    private TextView productName, productBrand, minPriceLabel;
    private LinearLayout supermarketListContainer;
    private LineChart priceEvolutionChart;
    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Product product;
    private String uid, productId, role;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail_from_barcode);

        favoriteIcon = findViewById(R.id.favoriteIcon);
        productImage = findViewById(R.id.productImage);
        productName = findViewById(R.id.productName);
        productBrand = findViewById(R.id.productBrand);
        minPriceLabel = findViewById(R.id.minPriceLabel);
        supermarketListContainer = findViewById(R.id.supermarketListContainer);
        priceEvolutionChart = findViewById(R.id.priceEvolutionChart);
        bottomNavigationView = findViewById(R.id.bottomNavigationBar);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            uid = user.getUid();
        } else {
            uid = null;
            favoriteIcon.setVisibility(View.GONE);
        }

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
        setSupportActionBar(topAppBar);
        topAppBar.inflateMenu(R.menu.top_app_bar_menu);
        topAppBar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_add) {
                if (uid == null) {
                    Toast.makeText(this, "Necesitas estar logueado", Toast.LENGTH_SHORT).show();
                } else {
                    db.collection("users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(doc -> {
                                String role = doc.getString("role");
                                if ("admin".equals(role)) {
                                    Toast.makeText(this, "Aquí podrías abrir AddProduct", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this,
                                            "Necesitas ser administrador para añadir productos",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                }
                return true;
            }
            return false;
        });


        String barcode = getIntent().getStringExtra("barcode");
        if (barcode != null) {
            if (uid != null) {
                db.collection("users").document(uid)
                        .get()
                        .addOnSuccessListener(doc -> {
                            role = doc.getString("role");
                            searchProductByBarcode(barcode);
                        })
                        .addOnFailureListener(e -> {
                            role = null; // o default
                            searchProductByBarcode(barcode);
                        });
            } else {
                role = null;
                searchProductByBarcode(barcode);
            }
        } else {
            Toast.makeText(this, "No se recibió código de barras", Toast.LENGTH_LONG).show();
            finish();
        }

    }
    private void checkFavoriteState() {
        db.collection("users")
                .document(uid)
                .collection("favouriteProducts")
                .document(productId)
                .get()
                .addOnSuccessListener(doc -> {
                    isFavorite = doc.exists();
                    updateHeartIcon();
                });
    }
    private void addToFavorites() {
        Map<String,Object> data = new HashMap<>();
        data.put("productId", productId);
        db.collection("users")
                .document(uid)
                .collection("favouriteProducts")
                .document(productId)
                .set(data)
                .addOnSuccessListener(v -> {
                    isFavorite = true;
                    updateHeartIcon();
                });
    }
    private void removeFromFavorites() {
        db.collection("users")
                .document(uid)
                .collection("favouriteProducts")
                .document(productId)
                .delete()
                .addOnSuccessListener(v -> {
                    isFavorite = false;
                    updateHeartIcon();
                });
    }
    private void updateHeartIcon() {
        int res = isFavorite
                ? R.drawable.corazon_relleno
                : R.drawable.heart;
        favoriteIcon.setImageResource(res);
    }

    private void searchProductByBarcode(String barcode) {
        db.collection("products")
                .whereEqualTo("barCode", barcode)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        product = doc.toObject(Product.class);
                        if (product != null) {
                            updateUI(product);
                            productId = product.getId();

                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                uid = user.getUid();

                                CollectionReference hist = db.collection("users")
                                        .document(uid)
                                        .collection("searchHistory");
                                hist.whereEqualTo("productId", productId)
                                        .limit(1)
                                        .get()
                                        .addOnSuccessListener(q -> {
                                            if (!q.isEmpty()) {
                                                q.getDocuments().get(0)
                                                        .getReference()
                                                        .update("timestamp", Timestamp.now());
                                            } else {
                                                Map<String,Object> entry = new HashMap<>();
                                                entry.put("productId", productId);
                                                entry.put("timestamp", Timestamp.now());
                                                hist.add(entry);
                                            }
                                        });

                                favoriteIcon.setVisibility(View.VISIBLE);
                                checkFavoriteState();
                                favoriteIcon.setOnClickListener(v -> {
                                    if (isFavorite) removeFromFavorites();
                                    else addToFavorites();
                                });
                            } else {
                                favoriteIcon.setVisibility(View.GONE);
                            }
                            loadSupermarkets();
                            loadPriceEvolution();
                        } else {
                            showNotFound();
                        }
                    } else {
                        showNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    showError();
                });
    }

    private void updateUI(Product product) {
        productName.setText(product.getName());
        //productBrand.setText(product.getBrandName());

        Glide.with(this)
                .load(product.getPhotoUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .into(productImage);
        // Obtener marca desde la colección brands usando brandId
        String brandId = product.getBrandId();
        if (brandId != null && !brandId.isEmpty()) {
            db.collection("brands").document(brandId)
                    .get()
                    .addOnSuccessListener(brandDoc -> {
                        if (brandDoc.exists()) {
                            String brandName = brandDoc.getString("name");
                            productBrand.setText(brandName != null ? brandName : "Marca desconocida");
                        } else {
                            productBrand.setText("Marca desconocida");
                        }
                    })
                    .addOnFailureListener(e -> {
                        productBrand.setText("Error al cargar marca");
                        e.printStackTrace();
                    });
        } else {
            productBrand.setText("Marca no disponible");
        }
    }

    private void loadSupermarkets() {
        supermarketListContainer.removeAllViews();

        db.collection("productSupermarket")
                .whereEqualTo("productId", productId)
                .get()
                .addOnSuccessListener(superDocs -> {
                    if (superDocs.isEmpty()) {
                        minPriceLabel.setText("Precio no disponible");
                        return;
                    }

                    final double[] minPrice = { Double.MAX_VALUE };
                    final int total = superDocs.size();
                    final int[] processed = { 0 };

                    for (DocumentSnapshot doc : superDocs) {
                        String psDocId       = doc.getId();
                        String supermarketId = doc.getString("supermarketId");

                        db.collection("supermarkets").document(supermarketId).get()
                                .addOnSuccessListener(supermarketDoc -> {
                                    if (!supermarketDoc.exists()) {
                                        processed[0]++;
                                        return;
                                    }

                                    String supermarketName = supermarketDoc.getString("name");

                                    db.collection("productSupermarket")
                                            .document(psDocId)
                                            .collection("priceUpdate")
                                            .orderBy("lastPriceUpdate", Query.Direction.DESCENDING)
                                            .limit(1)
                                            .get()
                                            .addOnSuccessListener(priceDocs -> {
                                                processed[0]++;

                                                double price = priceDocs.isEmpty()
                                                        ? Double.MAX_VALUE
                                                        : priceDocs.getDocuments().get(0).getDouble("price");

                                                if (price < minPrice[0]) {
                                                    minPrice[0] = price;
                                                }

                                                View item = LayoutInflater.from(this)
                                                        .inflate(R.layout.item_supermarket_info, supermarketListContainer, false);

                                                ImageView logo       = item.findViewById(R.id.supermarketLogo);
                                                TextView  nameTv     = item.findViewById(R.id.supermarketName);
                                                TextView  priceTv    = item.findViewById(R.id.supermarketPrice);
                                                TextView  unitPriceTv= item.findViewById(R.id.supermarketUnitPrice);
                                                ImageView btnEdit    = item.findViewById(R.id.btnUpdateSuperPrice);

                                                nameTv.setText(supermarketName);
                                                priceTv.setText(price < Double.MAX_VALUE
                                                        ? String.format(Locale.getDefault(), "%.2f €", price)
                                                        : "N/A");

                                                double qty = product.getQuantityUnity();
                                                String unit = product.getUnit() != null ? product.getUnit() : "";
                                                unitPriceTv.setText(qty > 0 && price < Double.MAX_VALUE
                                                        ? String.format(Locale.getDefault(),"%.2f € / %s", price/qty, unit)
                                                        : "N/A");

                                                if ("Mercadona".equalsIgnoreCase(supermarketName))      logo.setImageResource(R.drawable.mercadona);
                                                else if ("Dia".equalsIgnoreCase(supermarketName))      logo.setImageResource(R.drawable.dia_logo);
                                                else                                                    logo.setImageResource(R.drawable.alcampo);

                                                btnEdit.setVisibility(View.VISIBLE);
                                                btnEdit.setEnabled(false);
                                                btnEdit.setAlpha(0.4f);

                                                if ("admin".equals(role) || "mod".equals(role)) {
                                                    btnEdit.setEnabled(true);
                                                    btnEdit.setAlpha(1f);
                                                    btnEdit.setOnClickListener(v ->
                                                            showCustomUpdateDialog(supermarketName, psDocId, price)
                                                    );
                                                } else {
                                                    btnEdit.setOnClickListener(v ->
                                                            Toast.makeText(this,
                                                                    "Necesitas permisos para actualizar precio",
                                                                    Toast.LENGTH_SHORT
                                                            ).show()
                                                    );
                                                }

                                                supermarketListContainer.addView(item);

                                                if (processed[0] == total) {
                                                    if (minPrice[0] != Double.MAX_VALUE) {
                                                        minPriceLabel.setText(
                                                                String.format(Locale.getDefault(), "%.2f €", minPrice[0])
                                                        );
                                                    } else {
                                                        minPriceLabel.setText("Precio no disponible");
                                                    }
                                                }
                                            });
                                });
                    }
                });
    }

    private void loadPriceEvolution() {
        Map<Long, List<Double>> pricesByDate = new HashMap<>();

        db.collection("productSupermarket")
                .whereEqualTo("productId", product.getId())
                .get()
                .addOnSuccessListener(supermarketDocs -> {
                    int total = supermarketDocs.size();
                    if (total == 0) return;

                    final int[] done = {0};
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
                                            pricesByDate.computeIfAbsent(timestamp, k -> new ArrayList<>()).add(price);
                                        }
                                    }

                                    done[0]++;
                                    if (done[0] == total) {
                                        drawPriceChart(pricesByDate);
                                    }
                                });
                    }
                });
    }

    private void drawPriceChart(Map<Long, List<Double>> pricesByDate) {
        List<Long> sortedDates = new ArrayList<>(pricesByDate.keySet());
        Collections.sort(sortedDates);

        if (sortedDates.size() > 4) {
            sortedDates = sortedDates.subList(sortedDates.size() - 4, sortedDates.size());
        }

        List<Entry> entries = new ArrayList<>();
        int index = 0;
        for (Long ts : sortedDates) {
            List<Double> prices = pricesByDate.get(ts);
            double avg = prices.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            entries.add(new Entry(index++, (float) avg));
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
            minPrice = Math.min(minPrice, y);
            maxPrice = Math.max(maxPrice, y);
        }

        float margin = (maxPrice - minPrice) * 0.1f;
        YAxis yAxis = priceEvolutionChart.getAxisLeft();
        yAxis.setAxisMinimum(minPrice - margin);
        yAxis.setAxisMaximum(maxPrice + margin);
        yAxis.setLabelCount(2, true);

        priceEvolutionChart.getAxisRight().setEnabled(false);

        XAxis xAxis = priceEvolutionChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(2, true);
        List<Long> finalSortedDates = sortedDates;
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int i = Math.round(value);
                if (i >= 0 && i < finalSortedDates.size()) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM");
                    return sdf.format(new java.util.Date(finalSortedDates.get(i)));
                }
                return "";
            }
        });

        priceEvolutionChart.getDescription().setEnabled(false);
        priceEvolutionChart.getLegend().setEnabled(false);
        priceEvolutionChart.invalidate();
    }

    private void showNotFound() {
        Toast.makeText(this, "Producto no encontrado.", Toast.LENGTH_LONG).show();
        finish();
    }

    private void showError() {
        Toast.makeText(this, "Error al buscar producto.", Toast.LENGTH_LONG).show();
        finish();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar_menu, menu);
        return true;
    }

    private void showCustomUpdateDialog(
            String supermarketName,
            String psDocId,
            double oldPrice
    ) {
        View custom = LayoutInflater.from(this)
                .inflate(R.layout.dialog_update_price, null, false);

        TextView title         = custom.findViewById(R.id.dialogTitle);
        TextInputEditText input= custom.findViewById(R.id.etNewPrice);
        Button btnSave         = custom.findViewById(R.id.btnSave);
        Button btnCancel       = custom.findViewById(R.id.btnCancel);

        title.setText("Super: " + supermarketName);
        input.setText(String.format(Locale.getDefault(),"%.2f", oldPrice));

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(custom)
                .create();

        btnCancel.setOnClickListener(v -> dlg.dismiss());
        btnSave  .setOnClickListener(v -> {
            String txt = input.getText().toString().trim();
            if (txt.isEmpty()) {
                input.setError("Introduce un precio"); return;
            }
            double newPrice;
            try { newPrice = Double.parseDouble(txt); }
            catch(NumberFormatException e){
                input.setError("Formato inválido"); return;
            }
            double priceDiference = 0.30;

            if (oldPrice == 0) {
                input.setError("Precio original inválido (0)");
                return;
            }

            double porcentaje = Math.abs(newPrice - oldPrice) / oldPrice;
            if (porcentaje > priceDiference) {
                double porcentajeActual = Math.round(porcentaje * 1000) / 10.0;
                input.setError(
                        "La variación del " + porcentajeActual + "% respecto al precio actual ("
                                + String.format(Locale.getDefault(), "%.2f", oldPrice)
                                + " €) supera el 30%."
                );
                return;
            }
            Map<String,Object> data = new HashMap<>();
            data.put("price", newPrice);
            data.put("lastPriceUpdate", Timestamp.now());

            db.collection("productSupermarket")
                    .document(psDocId)
                    .collection("priceUpdate")
                    .add(data)
                    .addOnSuccessListener(r -> {
                        Toast.makeText(this,
                                "Precio actualizado", Toast.LENGTH_SHORT
                        ).show();
                        dlg.dismiss();
                        loadSupermarkets();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "Error al actualizar", Toast.LENGTH_SHORT
                            ).show()
                    );
        });

        dlg.show();
    }

}