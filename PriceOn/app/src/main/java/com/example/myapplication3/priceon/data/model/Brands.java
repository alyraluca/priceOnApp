package com.example.myapplication3.priceon.data.model;

import com.google.firebase.firestore.DocumentId;

public class Brands {
    @DocumentId
    private String id;
    private String name;

    public Brands() {
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
}
