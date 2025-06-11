package com.example.myapplication3.priceon.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.myapplication3.priceon.R;
import com.example.myapplication3.priceon.data.model.Brands;
import com.example.myapplication3.priceon.data.model.ProductTypes;
import com.example.myapplication3.priceon.data.model.Supermarkets;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddProductActivity extends AppCompatActivity {

    private TextInputLayout barCodeInputLayout, productNameInputLayout, packQuantityInputLayout, unityQuantityInputLayout, unitInputLayout, initialPriceInputLayout, photoUrlInputLayout;
    private TextInputEditText barCodeEditText, productNameEditText, packQuantityEditText, unityQuantityEditText, unitEditText, initialPriceEditText, photoUrlEditText;
    private Spinner spinnerBrand, spinnerType;
    private TextView tvSelectSupermarkets;
    private Button saveButton;

    private List<Brands> brandsList;
    private List<ProductTypes> typesList;
    private List<Supermarkets> supermarketList;
    private String[] allSupermarketNames;
    private boolean[] checkedSupermarkets;
    private List<Integer> selectedSupermarketIndices;
    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        Toolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setTitle("Nuevo producto");
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();

        barCodeInputLayout = findViewById(R.id.tilBarCode);
        barCodeEditText = findViewById(R.id.etBarCode);
        productNameInputLayout = findViewById(R.id.tilProductName);
        productNameEditText = findViewById(R.id.etProductName);
        photoUrlInputLayout = findViewById(R.id.tilPhotoUrl);
        photoUrlEditText = findViewById(R.id.etPhotoUrl);
        spinnerBrand = findViewById(R.id.spinnerBrand);
        spinnerType = findViewById(R.id.spinnerType);
        packQuantityInputLayout = findViewById(R.id.tilQtyPack);
        packQuantityEditText = findViewById(R.id.etQtyPack);
        unityQuantityInputLayout = findViewById(R.id.tilQtyUnity);
        unityQuantityEditText = findViewById(R.id.etQtyUnity);
        unitInputLayout = findViewById(R.id.tilUnit);
        unitEditText = findViewById(R.id.etUnit);
        tvSelectSupermarkets = findViewById(R.id.tvSelectSupermarkets);
        initialPriceInputLayout = findViewById(R.id.tilInitPrice);
        initialPriceEditText = findViewById(R.id.etInitialPrice);
        saveButton = findViewById(R.id.btnSaveProduct);
        bottomNavigationView = findViewById(R.id.bottomNavigationBar);

        loadBrands();
        loadProductTypes();
        loadSupermarkets();

        saveButton.setOnClickListener(v -> saveProduct());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                startActivity(new Intent(this, HomeActivity.class));
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

    private void loadBrands() {
        db.collection("brands")
                .get()
                .addOnSuccessListener(snap -> {
                    brandsList = snap.toObjects(Brands.class);
                    List<String> labels = new ArrayList<>();
                    for (Brands b : brandsList) {
                        labels.add(b.getName());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_item, labels
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerBrand.setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al cargar marcas", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadProductTypes() {
        db.collection("productTypes")
                .get()
                .addOnSuccessListener(snap -> {
                    typesList = snap.toObjects(ProductTypes.class);
                    List<String> labels = new ArrayList<>();
                    for (ProductTypes t : typesList) {
                        labels.add(t.getName());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_item, labels
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerType.setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al cargar tipos de producto", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadSupermarkets() {
        db.collection("supermarkets")
                .get()
                .addOnSuccessListener(snap -> {
                    supermarketList = snap.toObjects(Supermarkets.class);

                    int n = supermarketList.size();
                    allSupermarketNames = new String[n];
                    checkedSupermarkets  = new boolean[n];
                    for (int i = 0; i < n; i++) {
                        allSupermarketNames[i] = supermarketList.get(i).getName();
                        checkedSupermarkets[i] = false;
                    }

                    tvSelectSupermarkets.setOnClickListener(v -> showMultiSelectDialog());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al cargar lista de supermercados", Toast.LENGTH_SHORT).show()
                );
    }
    private void showMultiSelectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Elige supermercados");
        builder.setMultiChoiceItems(
                allSupermarketNames,
                checkedSupermarkets,
                (dialog, which, isChecked) -> {
                    checkedSupermarkets[which] = isChecked;
                }
        );

        builder.setPositiveButton("OK", (dialog, which) -> {
            selectedSupermarketIndices = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < checkedSupermarkets.length; i++) {
                if (checkedSupermarkets[i]) {
                    selectedSupermarketIndices.add(i);
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(allSupermarketNames[i]);
                }
            }
            if (selectedSupermarketIndices.isEmpty()) {
                tvSelectSupermarkets.setText("");
                tvSelectSupermarkets.setHint("Selecciona supermercados");
            } else {
                tvSelectSupermarkets.setText(sb.toString());
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void saveProduct() {

        String barCodeValue = barCodeEditText.getText().toString().trim();
        if (barCodeValue.isEmpty()) {
            barCodeInputLayout.setError("Introduce un código de barras");
            return;
        } else {
            barCodeInputLayout.setError(null);
        }

        String nameValue = productNameEditText.getText().toString().trim();
        if (nameValue.isEmpty()) {
            productNameInputLayout.setError("Introduce el nombre del producto");
            return;
        } else {
            productNameInputLayout.setError(null);
        }

        String qtyPackStr = packQuantityEditText.getText().toString().trim();
        double qtyPackValue;
        if (qtyPackStr.isEmpty()) {
            packQuantityInputLayout.setError("Introduce la cantidad en pack");
            return;
        }
        try {
            qtyPackValue = Double.parseDouble(qtyPackStr);
            if (qtyPackValue <= 0) {
                packQuantityInputLayout.setError("Debe ser un número mayor que 0");
                return;
            } else {
                packQuantityInputLayout.setError(null);
            }
        } catch (NumberFormatException e) {
            packQuantityInputLayout.setError("Formato inválido");
            return;
        }

        String qtyUnityStr = unityQuantityEditText.getText().toString().trim();
        double qtyUnityValue;
        if (qtyUnityStr.isEmpty()) {
            unityQuantityInputLayout.setError("Introduce la cantidad en unidad");
            return;
        }
        try {
            qtyUnityValue = Double.parseDouble(qtyUnityStr);
            if (qtyUnityValue < 0) {
                unityQuantityInputLayout.setError("Debe ser un número igual o mayor que 0");
                return;
            } else {
                unityQuantityInputLayout.setError(null);
            }
        } catch (NumberFormatException e) {
            unityQuantityInputLayout.setError("Formato inválido");
            return;
        }

        String unitValue = unitEditText.getText().toString().trim();
        if (unitValue.isEmpty()) {
            unitInputLayout.setError("Introduce la unidad (e.g. kg, L, uds)");
            return;
        } else {
            unitInputLayout.setError(null);
        }

        String initPriceStr = initialPriceEditText.getText().toString().trim();
        double initPriceValue;
        if (initPriceStr.isEmpty()) {
            initialPriceInputLayout.setError("Introduce el precio inicial");
            return;
        }
        try {
            initPriceValue = Double.parseDouble(initPriceStr);
            if (initPriceValue <= 0) {
                initialPriceInputLayout.setError("Debe ser un número mayor que 0");
                return;
            } else {
                initialPriceInputLayout.setError(null);
            }
        } catch (NumberFormatException e) {
            initialPriceInputLayout.setError("Formato inválido");
            return;
        }

        String photoUrlValue = photoUrlEditText.getText().toString().trim();
        if (photoUrlValue.isEmpty()) {
            photoUrlInputLayout.setError("Introduce la URL de la foto");
            return;
        } else {
            photoUrlInputLayout.setError(null);
        }

        if (selectedSupermarketIndices == null || selectedSupermarketIndices.isEmpty()) {
            Toast.makeText(this,
                    "Debes elegir al menos un supermercado",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("products")
                .whereEqualTo("barCode", barCodeValue)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        barCodeInputLayout.setError("Ya existe un producto con este código de barras");
                        return;
                    }

                    String brandId = brandsList.get(spinnerBrand.getSelectedItemPosition()).getId();
                    String typeId  = typesList .get(spinnerType .getSelectedItemPosition()).getId();

                    Map<String,Object> prod = new HashMap<>();
                    prod.put("barCode", barCodeValue);
                    prod.put("name", nameValue);
                    prod.put("brandId", brandId);
                    prod.put("productType", typeId);
                    prod.put("quantityPack", qtyPackValue);
                    prod.put("quantityUnity", qtyUnityValue);
                    prod.put("unit", unitValue);
                    prod.put("photoUrl", photoUrlValue);

                    db.collection("products")
                            .add(prod)
                            .addOnSuccessListener(docRef -> {
                                String newProdId = docRef.getId();

                                for (int idx : selectedSupermarketIndices) {
                                    String supId = supermarketList.get(idx).getId();

                                    Map<String,Object> rel = new HashMap<>();
                                    rel.put("productId", newProdId);
                                    rel.put("supermarketId", supId);

                                    db.collection("productSupermarket")
                                            .add(rel)
                                            .addOnSuccessListener(psRef -> {
                                                Map<String,Object> upd = new HashMap<>();
                                                upd.put("price", initPriceValue);
                                                upd.put("lastPriceUpdate", Timestamp.now());

                                                psRef.collection("priceUpdate")
                                                        .add(upd)
                                                        .addOnFailureListener(e ->
                                                                Toast.makeText(this, "Error al guardar precio inicial", Toast.LENGTH_SHORT).show()
                                                        );
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this, "Error al crear relación supermercado", Toast.LENGTH_SHORT).show()
                                            );
                                }

                                Toast.makeText(this, "Producto creado con éxito", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error al guardar producto", Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al comprobar código de barras", Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_profile) {
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
}

