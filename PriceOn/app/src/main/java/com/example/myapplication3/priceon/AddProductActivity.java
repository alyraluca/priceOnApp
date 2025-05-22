package com.example.myapplication3.priceon;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
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

    private TextInputLayout tilBarCode, tilName, tilQtyPack, tilQtyUnity, tilUnit, tilInitialPrice;
    private TextInputEditText etBarCode, etName, etQtyPack, etQtyUnity, etUnit, etInitialPrice;
    private Spinner spinnerBrand, spinnerType, spinnerSupermarket;
    private Button btnSave;

    private List<Brands>    brandsList;
    private List<ProductTypes> typesList;
    private List<Supermarkets> supsList;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        // Toolbar con icono “+”
        Toolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setTitle("Nuevo producto");
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();

        // Referencias de vistas
        tilBarCode        = findViewById(R.id.tilBarCode);
        etBarCode         = findViewById(R.id.etBarCode);
        tilName           = findViewById(R.id.tilProductName);
        etName            = findViewById(R.id.etProductName);
        spinnerBrand      = findViewById(R.id.spinnerBrand);
        spinnerType       = findViewById(R.id.spinnerType);
        tilQtyPack        = findViewById(R.id.tilQtyPack);
        etQtyPack         = findViewById(R.id.etQtyPack);
        tilQtyUnity       = findViewById(R.id.tilQtyUnity);
        etQtyUnity        = findViewById(R.id.etQtyUnity);
        tilUnit           = findViewById(R.id.tilUnit);
        etUnit            = findViewById(R.id.etUnit);
        spinnerSupermarket= findViewById(R.id.spinnerSupermarket);
        tilInitialPrice   = findViewById(R.id.tilInitPrice);
        etInitialPrice    = findViewById(R.id.etInitialPrice);
        btnSave           = findViewById(R.id.btnSaveProduct);

        loadBrands();
        loadProductTypes();
        loadSupermarkets();

        btnSave.setOnClickListener(v -> saveProduct());
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
                    supsList = snap.toObjects(Supermarkets.class);
                    List<String> labels = new ArrayList<>();
                    for (Supermarkets s : supsList) labels.add(s.getName());
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_item, labels
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerSupermarket.setAdapter(adapter);
                });
    }

    private void saveProduct() {
        // Validaciones…
        // Obtenemos el ID de la posición seleccionada:
        String brandId = brandsList.get(spinnerBrand.getSelectedItemPosition()).getId();
        String typeId  = typesList .get(spinnerType .getSelectedItemPosition()).getId();
        String supId   = supsList  .get(spinnerSupermarket.getSelectedItemPosition()).getId();

        Map<String,Object> prod = new HashMap<>();
        prod.put("barCode",       etBarCode.getText().toString().trim());
        prod.put("name",          etName   .getText().toString().trim());
        prod.put("brandId",       brandId);
        prod.put("productType",   typeId);
        prod.put("quantityPack",  Double.parseDouble(etQtyPack.getText().toString().trim()));
        prod.put("quantityUnity", Double.parseDouble(etQtyUnity.getText().toString().trim()));
        prod.put("unit",          etUnit   .getText().toString().trim());
        prod.put("photoUrl",      "");
        // 1) guardo en products
        db.collection("products")
                .add(prod)
                .addOnSuccessListener(docRef -> {
                    String newProdId = docRef.getId();
                    // 2) creo relación en productSupermarket
                    Map<String,Object> rel = new HashMap<>();
                    rel.put("productId",     newProdId);
                    rel.put("supermarketId", supId);
                    db.collection("productSupermarket")
                            .add(rel)
                            .addOnSuccessListener(psRef -> {
                                // 3) subcolección priceUpdate
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
    }
}
