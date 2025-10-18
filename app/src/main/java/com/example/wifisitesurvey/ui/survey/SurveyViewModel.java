package com.example.wifisitesurvey.ui.survey;

import android.app.Application;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.wifisitesurvey.data.model.DataPoint;
import com.example.wifisitesurvey.data.repository.SurveyRepository;
import com.example.wifisitesurvey.services.LocationProvider;
import com.example.wifisitesurvey.services.WifiService;
import java.util.List;

import android.widget.Toast;

public class SurveyViewModel extends AndroidViewModel {
    private final SurveyRepository repository;
    private final LocationProvider locationProvider;
    private final WifiService wifiService;

    private final MutableLiveData<Boolean> isTracking = new MutableLiveData<>(false);
    private long currentSurveyId = -1L;

    public SurveyViewModel(@NonNull Application application) {
        super(application);
        repository = new SurveyRepository(application);
        locationProvider = new LocationProvider(application);
        wifiService = new WifiService(application);
    }

    // GETTERS PARA A UI OBSERVAR

    public LiveData<Location> getLiveLocation() {
        return locationProvider.getLiveLocation();
    }

    public LiveData<Boolean> getIsTracking() {
        return isTracking;
    }

    public LiveData<List<DataPoint>> getDataPointsForSurvey() {
        // Retorna um LiveData vazio se o ID ainda não foi definido
        if (currentSurveyId == -1L) return new MutableLiveData<>();
        return repository.getDataPointsForSurvey(currentSurveyId);
    }

    // AÇÕES INICIADAS PELA UI

    public void setCurrentSurveyId(long id) {
        this.currentSurveyId = id;
    }

    public void toggleTracking() {
        boolean tracking = isTracking.getValue() != null && isTracking.getValue();
        if (!tracking) {
            locationProvider.startLocationUpdates();
            isTracking.setValue(true);
        } else {
            locationProvider.stopLocationUpdates();
            isTracking.setValue(false);
        }
    }

    public void recordDataPoint(Location location) {
        if (location != null && currentSurveyId != -1) {
            int rssi = wifiService.getCurrentRssi();

            // --- LINHA DE DEPURAÇÃO ---
            // Mostra o RSSI real que está sendo coletado
            Toast.makeText(getApplication(), "RSSI Coletado: " + rssi + " dBm", Toast.LENGTH_SHORT).show();
            // --- FIM DA LINHA DE DEPURAÇÃO ---

            DataPoint dataPoint = new DataPoint();
            dataPoint.surveyId = currentSurveyId;
            dataPoint.latitude = location.getLatitude();
            dataPoint.longitude = location.getLongitude();
            dataPoint.rssi = rssi;
            repository.insertDataPoint(dataPoint);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // evitar vazamentos de memória e consumo de bateria.
        locationProvider.stopLocationUpdates();
    }
}