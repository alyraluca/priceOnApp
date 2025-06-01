package com.example.myapplication3.priceon.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.myapplication3.priceon.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.widget.Spinner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.myapplication3.priceon.data.model.Brands;
import com.example.myapplication3.priceon.data.model.ProductTypes;
import com.example.myapplication3.priceon.data.model.Supermarkets;

public class AddProductActivity extends AppCompatActivity {

    private TextInputLayout barCodeInputLayout, productNameInputLayout, packQuantityInputLayout, unityQuantityInputLayout, unitInputLayout, initialPriceInputLayout;
    private TextInputEditText etBarCode, etName, etQtyPack, etQtyUnity, etUnit, etInitialPrice;
    private Spinner spinnerBrand, spinnerType, spinnerSupermarket;
    private Button saveButton;
    private List<Brands>    brandsList;
    private List<ProductTypes> typesList;
    private List<Supermarkets> supermarketList;
    private TextInputLayout photoUrlInputLayout;
    private TextInputEditText photoUrlEditText;
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
        etBarCode         = findViewById(R.id.etBarCode);
        productNameInputLayout = findViewById(R.id.tilProductName);
        etName            = findViewById(R.id.etProductName);
        spinnerBrand      = findViewById(R.id.spinnerBrand);
        spinnerType       = findViewById(R.id.spinnerType);
        packQuantityInputLayout = findViewById(R.id.tilQtyPack);
        etQtyPack         = findViewById(R.id.etQtyPack);
        unityQuantityInputLayout = findViewById(R.id.tilQtyUnity);
        etQtyUnity        = findViewById(R.id.etQtyUnity);
        unitInputLayout = findViewById(R.id.tilUnit);
        etUnit            = findViewById(R.id.etUnit);
        spinnerSupermarket= findViewById(R.id.spinnerSupermarket);
        initialPriceInputLayout = findViewById(R.id.tilInitPrice);
        etInitialPrice    = findViewById(R.id.etInitialPrice);
        saveButton = findViewById(R.id.btnSaveProduct);
        photoUrlInputLayout = findViewById(R.id.tilPhotoUrl);
        photoUrlEditText = findViewById(R.id.etPhotoUrl);
        bottomNavigationView = findViewById(R.id.bottomNavigationBar);

        loadBrands();
        loadProductTypes();
        loadSupermarkets();

        saveButton.setOnClickListener(v -> saveProduct());

        bottomNavigationView = findViewById(R.id.bottomNavigationBar);
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
                    for (Brands b : brandsList) labels.add(b.getName());
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_item, labels
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerBrand.setAdapter(adapter);
                });
    }

    private void loadProductTypes() {
        db.collection("productTypes")
                .get()
                .addOnSuccessListener(snap -> {
                    typesList = snap.toObjects(ProductTypes.class);
                    List<String> labels = new ArrayList<>();
                    for (ProductTypes t : typesList) labels.add(t.getName());
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_item, labels
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerType.setAdapter(adapter);
                });
    }

    private void loadSupermarkets() {
        db.collection("supermarkets")
                .get()
                .addOnSuccessListener(snap -> {
                    supermarketList = snap.toObjects(Supermarkets.class);
                    List<String> labels = new ArrayList<>();
                    for (Supermarkets s : supermarketList) labels.add(s.getName());
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_item, labels
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerSupermarket.setAdapter(adapter);
                });
    }

    private void saveProduct() {
        String barCodeValue = etBarCode.getText().toString().trim();
        if (barCodeValue.isEmpty()) {
            barCodeInputLayout.setError("Introduce un código de barras");
            return;
        } else {
            barCodeInputLayout.setError(null);
        }

        String nameValue = etName.getText().toString().trim();
        if (nameValue.isEmpty()) {
            productNameInputLayout.setError("Introduce el nombre del producto");
            return;
        } else {
            productNameInputLayout.setError(null);
        }

        String qtyPackStr = etQtyPack.getText().toString().trim();
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

        String qtyUnityStr = etQtyUnity.getText().toString().trim();
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

        String unitValue = etUnit.getText().toString().trim();
        if (unitValue.isEmpty()) {
            unitInputLayout.setError("Introduce la unidad (ej. kg, L, uds)");
            return;
        } else {
            unitInputLayout.setError(null);
        }

        String initPriceStr = etInitialPrice.getText().toString().trim();
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

        barCodeInputLayout.setError(null);
        productNameInputLayout.setError(null);
        packQuantityInputLayout.setError(null);
        unityQuantityInputLayout.setError(null);
        unitInputLayout.setError(null);
        initialPriceInputLayout.setError(null);
        photoUrlInputLayout.setError(null);

        if (barCodeValue.isEmpty()) {
            barCodeInputLayout.setError("Introduce un código de barras");
            return;
        }
        barCodeInputLayout.setError(null);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

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
                    String supermarketId   = supermarketList.get(spinnerSupermarket.getSelectedItemPosition()).getId();

                    Map<String,Object> prod = new HashMap<>();
                    prod.put("barCode",       barCodeValue);
                    prod.put("name",          etName   .getText().toString().trim());
                    prod.put("brandId",       brandId);
                    prod.put("productType",   typeId);
                    prod.put("quantityPack",  Double.parseDouble(etQtyPack.getText().toString().trim()));
                    prod.put("quantityUnity", Double.parseDouble(etQtyUnity.getText().toString().trim()));
                    prod.put("unit",          etUnit   .getText().toString().trim());
                    String url = photoUrlEditText.getText().toString().trim();
                    prod.put("photoUrl", url);

                    db.collection("products")
                            .add(prod)
                            .addOnSuccessListener(docRef -> {
                                String newProdId = docRef.getId();
                                Map<String,Object> rel = new HashMap<>();
                                rel.put("productId",     newProdId);
                                rel.put("supermarketId", supermarketId);
                                db.collection("productSupermarket")
                                        .add(rel)
                                        .addOnSuccessListener(psRef -> {
                                            Map<String,Object> upd = new HashMap<>();
                                            upd.put("price",          Double.parseDouble(etInitialPrice.getText().toString().trim()));
                                            upd.put("lastPriceUpdate", Timestamp.now());
                                            psRef.collection("priceUpdate")
                                                    .add(upd)
                                                    .addOnSuccessListener(r -> {
                                                        Toast.makeText(this, "Producto creado con éxito", Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    })
                                                    .addOnFailureListener(e ->
                                                            Toast.makeText(this, "Error al guardar precio inicial", Toast.LENGTH_SHORT).show()
                                                    );
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Error al crear relación supermercado", Toast.LENGTH_SHORT).show()
                                        );
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error al guardar producto", Toast.LENGTH_SHORT).show()
                            );
                });
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar_menu, menu);
        return true;
    }
}
