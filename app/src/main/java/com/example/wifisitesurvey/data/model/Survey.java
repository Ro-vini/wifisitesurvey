package com.example.wifisitesurvey.data.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Representa um único "survey" de Wi-Fi.
 */
@Entity(tableName = "surveys",
        indices = {@Index(value = {"name", "ssid"}, unique = true)})
public class Survey {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;
    public String ssid;
    public long creationTimestamp;

    // Construtor vazio necessário para o Room e para criar cópias do objeto.
    public Survey() {}

    // Construtor
    public Survey(String name, String ssid) {
        this.name = name;
        this.ssid = ssid;
        this.creationTimestamp = System.currentTimeMillis();
    }
}