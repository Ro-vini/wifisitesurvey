package com.example.wifisitesurvey.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.wifisitesurvey.data.model.DataPoint;
import com.example.wifisitesurvey.data.model.Survey;

import java.util.List;

/**
 * DAO (Data Access Object) para interagir com as tabelas do banco de dados.
 * Define os métodos para realizar operações de CRUD (Create, Read, Update, Delete).
 */
@Dao
public interface SurveyDao {

    /**
     * Insere um novo Survey na tabela. Se já existir um com o mesmo ID, ele será substituído.
     * @param survey O objeto Survey a ser inserido.
     * @return O ID (long) do novo survey inserido.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertSurvey(Survey survey);

    /**
     * Insere um novo ponto de medição (DataPoint) no banco de dados.
     * @param dataPoint O objeto DataPoint a ser inserido.
     */
    @Insert
    void insertDataPoint(DataPoint dataPoint);

    /**
     * Atualiza um DataPoint existente no banco de dados.
     * @param dataPoint O ponto de dados a ser atualizado.
     */
    @Update
    void updateDataPoint(DataPoint dataPoint);

    /**
     * Atualiza um Survey existente no banco de dados.
     * @param survey O survey a ser atualizado.
     */
    @Update
    void updateSurvey(Survey survey);

    /**
     * Deleta um Survey existente no banco de dados.
     * @param survey O survey a ser deletado.
     */
    @Delete
    void deleteSurvey(Survey survey);

    /**
     * Retorna um LiveData contendo a lista de todos os surveys salvos,
     * ordenados do mais recente para o mais antigo.
     * O LiveData notificará automaticamente a UI sobre quaisquer mudanças.
     * @return LiveData com a lista de todos os surveys.
     */
    @Query("SELECT * FROM surveys ORDER BY creationTimestamp DESC")
    LiveData<List<Survey>> getAllSurveys();

    /**
     * Retorna um LiveData contendo a lista de surveys para um SSID específico.
     * @param ssid O SSID para filtrar os surveys.
     * @return LiveData com a lista de surveys filtrada.
     */
    @Query("SELECT * FROM surveys WHERE ssid = :ssid ORDER BY creationTimestamp DESC")
    LiveData<List<Survey>> getSurveysBySsid(String ssid);

    /**
     * Retorna um LiveData contendo a lista de todos os DataPoints associados
     * a um surveyId específico.
     * @param surveyId O ID do survey para o qual os pontos de dados são solicitados.
     * @return LiveData com a lista de DataPoints.
     */
    @Query("SELECT * FROM data_points WHERE surveyId = :surveyId")
    LiveData<List<DataPoint>> getDataPointsForSurvey(long surveyId);

    /**
     * Encontra um DataPoint existente com base no ID do survey e na localização.
     * @param surveyId O ID do survey.
     * @param latitude A latitude do ponto.
     * @param longitude A longitude do ponto.
     * @return O DataPoint encontrado, ou null se não existir.
     */
    @Query("SELECT * FROM data_points WHERE surveyId = :surveyId AND latitude = :latitude AND longitude = :longitude LIMIT 1")
    DataPoint findDataPointByLocation(long surveyId, double latitude, double longitude);
}