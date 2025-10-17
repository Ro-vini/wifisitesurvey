package com.example.wifisitesurvey.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Representa um único "survey" de Wi-Fi.
 */
@Entity(tableName = "surveys")
public class Survey {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;

    public long creationTimestamp;

    // Construtor
    public Survey(String name) {
        this.name = name;
        this.creationTimestamp = System.currentTimeMillis();
    }
}