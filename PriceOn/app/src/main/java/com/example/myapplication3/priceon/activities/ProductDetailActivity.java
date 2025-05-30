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

import com.example.myapplication3.priceon.R;
import com.github.mikephil.charting.charts.LineChart;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProductDetailActivity extends AppCompatActivity {

    private ImageView productImage, favoriteIcon;
    private TextView productName, productBrand, fromLabel, minPriceLabel;
    private LinearLayout supermarketListContainer;
    private LineChart priceEvolutionChart;
    private BottomNavigationView bottomNavigationView;
    private boolean isFavorite = false;
    private String uid;
    private String productId;
    private FirebaseFirestore db;
    private String role;
    private Product product;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        db = FirebaseFirestore.getInstance();

        MaterialToolbar topAppBar             = findViewById(R.id.topAppBar);
        productImage                         = findViewById(R.id.productImage);
        productName                          = findViewById(R.id.productName);
        productBrand                         = findViewById(R.id.productBrand);
        fromLabel                            = findViewById(R.id.fromLabel);
        minPriceLabel                        = findViewById(R.id.minPriceLabel);
        favoriteIcon                         = findViewById(R.id.favoriteIcon);
        supermarketListContainer             = findViewById(R.id.supermarketListContainer);
        priceEvolutionChart                  = findViewById(R.id.priceEvolutionChart);
        bottomNavigationView                 = findViewById(R.id.bottomNavigationBar);

        setSupportActionBar(topAppBar);
        topAppBar.setTitle("PriceOn");

        Product product = (Product) getIntent().getSerializableExtra("product");
        if (product == null) {
            finish();
            return;
        }
        this.product = product;
        productId = product.getId();

        productName.setText(product.getName());
        productBrand.setText(product.getBrandName());
        minPriceLabel.setText(product.getMinPrice() + " €");
        Glide.with(this)
                .load(product.getPhotoUrl())
                .into(productImage);

        if (product.getBrandName() == null && product.getBrandId() != null) {
            FirebaseFirestore.getInstance()
                    .collection("brands")
                    .document(product.getBrandId())
                    .get()
                    .addOnSuccessListener(brandDoc -> {
                        String brand = brandDoc.getString("name");
                        productBrand.setText(brand != null ? brand : "Marca desconocida");
                    })
                    .addOnFailureListener(e -> productBrand.setText("Marca no disponible"));
        } else {
            productBrand.setText(product.getBrandName());
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
                return true;
            }
            return false;
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            uid = user.getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        role = doc.getString("role");
                        loadSupermarkets();
                    });
        } else {
            role = null;
            loadSupermarkets();
        }

        if (uid != null) {
            checkFavoriteState();
            favoriteIcon.setOnClickListener(v -> {
                if (isFavorite) removeFromFavorites();
                else addToFavorites();
            });
        }
        loadPriceEvolution(product);
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
            double np;
            try { np = Double.parseDouble(txt); }
            catch(NumberFormatException e){
                input.setError("Formato inválido"); return;
            }
            Map<String,Object> data = new HashMap<>();
            data.put("price", np);
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

    private void checkFavoriteState() {
        DocumentReference favRef = db
                .collection("users")
                .document(uid)
                .collection("favouriteProducts")
                .document(productId);
        favRef.get().addOnSuccessListener(doc -> {
            isFavorite = doc.exists();
            updateHeartIcon();
        });
    }

    private void addToFavorites() {
        DocumentReference favRef = db
                .collection("users")
                .document(uid)
                .collection("favouriteProducts")
                .document(productId);

        Map<String,Object> data = new HashMap<>();
        data.put("productId", productId);

        favRef.set(data).addOnSuccessListener(v -> {
            isFavorite = true;
            updateHeartIcon();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error al guardar favorito", Toast.LENGTH_SHORT).show()
        );
    }

    private void removeFromFavorites() {
        DocumentReference favRef = db
                .collection("users")
                .document(uid)
                .collection("favouriteProducts")
                .document(productId);

        favRef.delete().addOnSuccessListener(v -> {
            isFavorite = false;
            updateHeartIcon();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error al quitar favorito", Toast.LENGTH_SHORT).show()
        );
    }
    private void updateHeartIcon() {
        int res = isFavorite
                ? R.drawable.corazon_relleno
                : R.drawable.heart;
        favoriteIcon.setImageResource(res);
    }

    private void loadSupermarkets() {
        supermarketListContainer.removeAllViews();

        db.collection("productSupermarket")
                .whereEqualTo("productId", productId)
                .get()
                .addOnSuccessListener(superDocs -> {
                    int total = superDocs.size();
                    if (total == 0) {
                        minPriceLabel.setText("Precio no disponible");
                        return;
                    }

                    // Variables para el mínimo y el control de concurrencia
                    final double[] minPrice = { Double.MAX_VALUE };
                    final int[] processed = { 0 };

                    for (DocumentSnapshot doc : superDocs) {
                        String psDocId       = doc.getId();
                        String supermarketId = doc.getString("supermarketId");

                        // Traer el nombre del súper
                        db.collection("supermarkets").document(supermarketId)
                                .get()
                                .addOnSuccessListener(supermarketDoc -> {
                                    String name = supermarketDoc.getString("name");

                                    // Traer el último precio
                                    db.collection("productSupermarket").document(psDocId)
                                            .collection("priceUpdate")
                                            .orderBy("lastPriceUpdate", Query.Direction.DESCENDING)
                                            .limit(1)
                                            .get()
                                            .addOnSuccessListener(priceDocs -> {
                                                processed[0]++;

                                                if (!priceDocs.isEmpty()) {
                                                    double price = priceDocs.getDocuments().get(0).getDouble("price");

                                                    // Actualizamos el mínimo global si hace falta
                                                    if (price < minPrice[0]) {
                                                        minPrice[0] = price;
                                                    }

                                                    // Inflamos el ítem de la lista
                                                    View item = LayoutInflater.from(this)
                                                            .inflate(R.layout.item_supermarket_info, supermarketListContainer, false);

                                                    ImageView  logo       = item.findViewById(R.id.supermarketLogo);
                                                    TextView   tvName     = item.findViewById(R.id.supermarketName);
                                                    TextView   tvPrice    = item.findViewById(R.id.supermarketPrice);
                                                    TextView   tvUnitPrice= item.findViewById(R.id.supermarketUnitPrice);
                                                    ImageView btnEdit   = item.findViewById(R.id.btnUpdateSuperPrice);

                                                    //btnEdit.setImageResource(R.drawable.actualizar);

                                                    tvName.setText(name);
                                                    tvPrice.setText(String.format(Locale.getDefault(), "%.2f €", price));
                                                    double qty = product.getQuantityUnity();
                                                    String unit = product.getUnit() == null ? "" : product.getUnit();
                                                    tvUnitPrice.setText(qty > 0
                                                            ? String.format(Locale.getDefault(), "%.2f € / %s", price / qty, unit)
                                                            : "N/A");

                                                    // Logo según súper
                                                    if ("Mercadona".equalsIgnoreCase(name))      logo.setImageResource(R.drawable.mercadona);
                                                    else if ("Dia".equalsIgnoreCase(name))      logo.setImageResource(R.drawable.dia_logo);
                                                    else                                        logo.setImageResource(R.drawable.alcampo);

                                                    btnEdit.setVisibility(View.VISIBLE);
                                                    btnEdit.setEnabled(false);
                                                    btnEdit.setAlpha(0.4f);

                                                    // Botón editar si es admin/mod
                                                    if ("admin".equals(role) || "mod".equals(role)) {
                                                        btnEdit.setEnabled(true);
                                                        btnEdit.setAlpha(1f);
                                                        btnEdit.setOnClickListener(v ->
                                                                showCustomUpdateDialog(name, psDocId, price)
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
                                                }

                                                // Cuando procesamos todos, actualizamos el "Desde"
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




}