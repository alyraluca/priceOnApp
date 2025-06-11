// Product.java
package com.example.myapplication3.priceon.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;
import java.security.SecureRandomParameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Product implements Serializable {

    @DocumentId
    private String id;
    private String name;
    private String brandId;
    private String brandName;

    @PropertyName("photoUrl")
    private String photoUrl;

    private Double quantityUnity;
    private String unit;
    private String barCode;
    private Integer quantityPack;

    @Exclude
    private String productTypeId;
    @Exclude
    private double minPrice;
    @Exclude
    private String cheapestSupermarketId;
    @Exclude
    private String cheapestSupermarketName;
    @Exclude
    private List<String> supermarketLogoUrls = new ArrayList<>();

    public Product() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrandId() { return brandId; }
    public void setBrandId(String brandId) { this.brandId = brandId; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public Double getQuantityUnity() { return quantityUnity; }
    public void setQuantityUnity(Double quantityUnity) { this.quantityUnity = quantityUnity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getBarCode() { return barCode; }
    public void setBarCode(String barCode) { this.barCode = barCode; }

    public Integer getQuantityPack() { return quantityPack; }
    public void setQuantityPack(Integer quantityPack) { this.quantityPack = quantityPack; }

    public String getProductTypeId() { return productTypeId; }
    public void setProductTypeId(String productTypeId) { this.productTypeId = productTypeId; }

    public double getMinPrice() { return minPrice; }
    public void setMinPrice(double minPrice) { this.minPrice = minPrice; }

    public String getCheapestSupermarketId() { return cheapestSupermarketId; }
    public void setCheapestSupermarketId(String id) { this.cheapestSupermarketId = id; }

    public String getCheapestSupermarketName() { return cheapestSupermarketName; }
    public void setCheapestSupermarketName(String name) { this.cheapestSupermarketName = name; }

    public List<String> getSupermarketLogoUrls() { return supermarketLogoUrls; }
    public void setSupermarketLogoUrls(List<String> urls) { this.supermarketLogoUrls = urls; }
    public void addSupermarketLogoUrl(String url) { this.supermarketLogoUrls.add(url); }

    @Exclude
    public String getPricePerUnit() {
        if (minPrice <= 0 || quantityUnity == null || quantityUnity <= 0 || quantityPack == null || quantityPack <= 0) return "-";
        double totalQuantity = quantityPack * quantityUnity;
        double pricePerUnit = minPrice / totalQuantity;
        return String.format(Locale.getDefault(), "%.2f", pricePerUnit);
    }
}
