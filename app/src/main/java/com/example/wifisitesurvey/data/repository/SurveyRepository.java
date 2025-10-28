package com.example.wifisitesurvey.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.wifisitesurvey.data.database.AppDatabase;
import com.example.wifisitesurvey.data.database.SurveyDao;
import com.example.wifisitesurvey.data.database.FloorplanDao;
import com.example.wifisitesurvey.data.model.DataPoint;
import com.example.wifisitesurvey.data.model.Floorplan;
import com.example.wifisitesurvey.data.model.Survey;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Mediador entre as fontes de dados (Room) e o resto do aplicativo (os ViewModels).
 * Fornece uma API limpa para o acesso aos dados.
 */
public class SurveyRepository {

    private final SurveyDao surveyDao;
    private final FloorplanDao floorplanDao;
    private final ExecutorService databaseExecutor;

    public SurveyRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        surveyDao = db.surveyDao();
        floorplanDao = db.floorplanDao();
        databaseExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * Insere um novo DataPoint no banco de dados em uma thread de background.
     * @param dataPoint O ponto de dados a ser salvo.
     */
    public void insertDataPoint(DataPoint dataPoint) {
        databaseExecutor.execute(() -> surveyDao.insertDataPoint(dataPoint));
    }

    /**
     * Insere um novo Survey e retorna seu ID. A operação é síncrona dentro do executor
     * para que possamos obter o ID de volta para uso imediato.
     * @param survey O survey a ser criado.
     * @return O ID do survey recém-criado.
     */
    public Future<Long> insertSurvey(Survey survey) {
        return databaseExecutor.submit(() -> surveyDao.insertSurvey(survey));
    }

    /**
     * Retorna um LiveData com a lista de todos os surveys.
     * O Room já executa esta consulta em uma thread de background por padrão.
     * @return LiveData contendo a lista de surveys.
     */
    public LiveData<List<Survey>> getAllSurveys() {
        return surveyDao.getAllSurveys();
    }

    public LiveData<List<Survey>> getSurveysBySsid(String ssid) {
        return surveyDao.getSurveysBySsid(ssid);
    }

    /**
     * Retorna um LiveData com a lista de DataPoints para um survey específico.
     * @param surveyId O ID do survey.
     * @return LiveData contendo a lista de DataPoints.
     */
    public LiveData<List<DataPoint>> getDataPointsForSurvey(long surveyId) {
        return surveyDao.getDataPointsForSurvey(surveyId);
    }

    /**
     * Insere um novo DataPoint ou atualiza um existente com a mesma localização (latitude/longitude).
     * Esta operação é conhecida como "upsert".
     * @param dataPoint O ponto de dados a ser salvo.
     */
    public void upsertDataPoint(DataPoint dataPoint) {
        databaseExecutor.execute(() -> {
            DataPoint existingPoint = surveyDao.findDataPointByLocation(
                    dataPoint.surveyId,
                    dataPoint.latitude,
                    dataPoint.longitude
            );

            if (existingPoint != null) {
                // Se o ponto já existe, atualiza o RSSI e salva.
                existingPoint.rssi = dataPoint.rssi;
                surveyDao.updateDataPoint(existingPoint);
            } else {
                // Se o ponto é novo, insere-o.
                surveyDao.insertDataPoint(dataPoint);
            }
        });
    }

    public void updateSurvey(Survey survey) {
        databaseExecutor.execute(() -> surveyDao.updateSurvey(survey));
    }

    public void deleteSurvey(Survey survey) {
        databaseExecutor.execute(() -> surveyDao.deleteSurvey(survey));
    }

    public LiveData<Floorplan> getFloorplanForSurvey(long surveyId) {
        return floorplanDao.getFloorplanForSurvey(surveyId);
    }

    public void saveFloorplan(Floorplan floorplan) {
        // Execute a inserção em uma thread de fundo para não bloquear a UI
        AppDatabase.databaseWriteExecutor.execute(() -> {
            floorplanDao.insertOrUpdate(floorplan);
        });
    }

    public void clearFloorplanData(long surveyId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            floorplanDao.deleteBySurveyId(surveyId);
        });
    }
}