package com.example.wifisitesurvey.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "floorplans",
        // Garante que cada Survey tenha apenas um Floorplan
        indices = {@Index(value = "surveyId", unique = true)},
        foreignKeys = @ForeignKey(entity = Survey.class,
                parentColumns = "id",
                childColumns = "surveyId",
                onDelete = ForeignKey.CASCADE))
public class Floorplan {
    @PrimaryKey(autoGenerate = true)
    public int id;

    // ID do Survey ao qual esta planta pertence
    public long surveyId;
    public String imageUri;
    public double latitude;
    public double longitude;
    public float width;
    public float bearing;
}