package com.example.myapplication3.priceon.data.model;

import com.google.firebase.firestore.DocumentId;

public class Supermarkets {

    @DocumentId
    private String id;
    private String name;
    private String logoUrl;

    public Supermarkets(String id, String name, String logoUrl) {
        this.id = id;
        this.name = name;
        this.logoUrl = logoUrl;
    }

    public Supermarkets() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }
}
