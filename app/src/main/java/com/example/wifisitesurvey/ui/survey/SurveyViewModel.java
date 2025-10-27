package com.example.wifisitesurvey.ui.survey;

import android.app.Application;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.wifisitesurvey.data.model.DataPoint;
import com.example.wifisitesurvey.data.model.Floorplan;
import com.example.wifisitesurvey.data.repository.SurveyRepository;
import com.example.wifisitesurvey.services.LocationProvider;
import com.example.wifisitesurvey.services.WifiService;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import android.widget.Toast;

public class SurveyViewModel extends AndroidViewModel {
    private final SurveyRepository repository;
    private final LocationProvider locationProvider;
    private final WifiService wifiService;
    private final MutableLiveData<Boolean> isTracking = new MutableLiveData<>(false);
    private final MutableLiveData<Long> currentSurveyId = new MutableLiveData<>(-1L);

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
        return Transformations.switchMap(currentSurveyId, id -> {
            if (id == null || id == -1L) {
                return new MutableLiveData<>(); // Retorna um LiveData vazio se o ID não for válido
            }
            return repository.getDataPointsForSurvey(id);
        });
    }

    // AÇÕES INICIADAS PELA UI

    public void setCurrentSurveyId(long id) {
        this.currentSurveyId.setValue(id);
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
        Long surveyId = currentSurveyId.getValue();
        if (location != null && surveyId != null && surveyId != -1) {
            int rssi = wifiService.getCurrentRssi();

            Toast.makeText(getApplication(), "RSSI Coletado: " + rssi + " dBm", Toast.LENGTH_SHORT).show();

            DataPoint dataPoint = new DataPoint();
            dataPoint.surveyId = surveyId;
            dataPoint.latitude = location.getLatitude();
            dataPoint.longitude = location.getLongitude(); // CORREÇÃO: data.longitude -> dataPoint.longitude
            dataPoint.rssi = rssi;
            repository.upsertDataPoint(dataPoint);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // evitar vazamentos de memória e consumo de bateria.
        locationProvider.stopLocationUpdates();
    }

    // Métodos do Floorplan

    public void saveFloorplanData(String uriString, LatLng center, float width, float bearing) {
        Long surveyId = currentSurveyId.getValue();
        if (surveyId == null || surveyId == -1L) return;

        Floorplan floorplan = new Floorplan();
        floorplan.surveyId = surveyId;
        floorplan.imageUri = uriString;
        floorplan.latitude = center.latitude;
        floorplan.longitude = center.longitude;
        floorplan.width = width;
        floorplan.bearing = bearing;
        repository.saveFloorplan(floorplan);
    }

    public LiveData<Floorplan> getFloorplanForSurvey() {
        return Transformations.switchMap(currentSurveyId, id -> {
            if (id == null || id == -1L) {
                return new MutableLiveData<>(null); // Retorna nulo se não houver ID
            }
            return repository.getFloorplanForSurvey(id);
        });
    }

    public void clearFloorplanData() {
        Long surveyId = currentSurveyId.getValue();
        if (surveyId != null && surveyId != -1L) {
            repository.clearFloorplanData(surveyId);
        }
    }
}