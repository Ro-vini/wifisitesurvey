package com.example.wifisitesurvey.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Representa um único ponto de dados coletado durante um survey.
 * Contém a localização geográfica (latitude/longitude) e a intensidade do sinal Wi-Fi (RSSI)
 * naquele ponto específico.
 */
@Entity(tableName = "data_points",
        // Define a chave estrangeira para garantir a integridade referencial com a tabela 'surveys'.
        // Se um 'Survey' for deletado, todos os 'DataPoints' associados também serão.
        foreignKeys = @ForeignKey(entity = Survey.class,
                parentColumns = "id",
                childColumns = "surveyId",
                onDelete = ForeignKey.CASCADE),
        // Cria um índice na coluna surveyId para otimizar as consultas que filtram por survey.
        indices = {@Index(value = "surveyId")})

public class DataPoint {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public long surveyId;
    public double latitude;
    public double longitude;
    public int rssi; // Received Signal Strength Indicator
}
