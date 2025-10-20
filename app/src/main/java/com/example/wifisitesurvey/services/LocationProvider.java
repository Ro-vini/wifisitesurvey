package com.example.wifisitesurvey.services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Gerencia o acesso à localização do dispositivo usando a FusedLocationProviderClient.
 * Esta classe abstrai a complexidade de solicitar, receber e parar atualizações de localização,
 * expondo os dados de forma reativa através de LiveData.
 * Filtra posições inconsistentes com o filtro Kalman.
 */
public class LocationProvider {

    private static final String TAG = "LocationProvider";
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static final float MIN_ACCURACY_METERS = 32.8f; // 10 pés

    private final FusedLocationProviderClient fusedLocationClient;
    private final MutableLiveData<Location> locationLiveData = new MutableLiveData<>();
    private final LocationRequest locationRequest;
    private final LocationCallback locationCallback;

    private final KalmanFilter kalmanFilter = new KalmanFilter();

    public LocationProvider(Context context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context.getApplicationContext());

        // Configura os parâmetros para as solicitações de localização
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_IN_MILLISECONDS)
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
                .build();

        // Define o callback que será executado quando uma nova localização for recebida
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location raw = locationResult.getLastLocation();
                if (raw == null || !raw.hasAccuracy() || raw.getAccuracy() > MIN_ACCURACY_METERS) {
                    Log.w(TAG, "Location discarded (low accuracy)");
                    return;
                }

                Location filtered = kalmanFilter.update(raw);
                if (filtered != null) {
                    locationLiveData.postValue(filtered);
                    Log.d(TAG, "Accepted filtered location (" + filtered.getAccuracy() + "m)");
                }
            }
        };
    }

    /**
     * Retorna um LiveData que emite atualizações de localização.
     * A UI pode observar este LiveData para receber as localizações.
     * @return um objeto LiveData<Location>
     */
    public LiveData<Location> getLiveLocation() {
        return locationLiveData;
    }

    /**
     * Inicia o processo de solicitação de atualizações de localização.
     * <p>
     * <b>Atenção:</b> A verificação da permissão {@link android.Manifest.permission#ACCESS_FINE_LOCATION}
     * deve ser feita na camada de UI (Activity/Fragment) antes de chamar este método.
     * </p>
     */
    @SuppressLint("MissingPermission")
    public void startLocationUpdates() {
        Log.i(TAG, "Starting location updates.");
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted.", e);
        }
    }

    /**
     * Para as atualizações de localização para economizar bateria.
     */
    public void stopLocationUpdates() {
        Log.i(TAG, "Stopping location updates.");
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    /**
     * Implementação leve do filtro de Kalman para suavizar o ruído do GPS.
     */
    private static class KalmanFilter {
        private static final double Q = 0.0001;  // variância do processo
        private static final double R = 10;      // variância da medição

        private boolean initialized = false;
        private double lat, lon;
        private double pLat = 1, pLon = 1; // covariâncias

        public Location update(Location measurement) {
            if (!initialized) {
                lat = measurement.getLatitude();
                lon = measurement.getLongitude();
                initialized = true;
                return measurement;
            }

            // Kalman 1D para latitude
            pLat += Q;
            double kLat = pLat / (pLat + R);
            lat += kLat * (measurement.getLatitude() - lat);
            pLat = (1 - kLat) * pLat;

            // Kalman 1D para longitude
            pLon += Q;
            double kLon = pLon / (pLon + R);
            lon += kLon * (measurement.getLongitude() - lon);
            pLon = (1 - kLon) * pLon;

            Location filtered = new Location(measurement);
            filtered.setLatitude(lat);
            filtered.setLongitude(lon);
            filtered.setAccuracy((float) Math.min(measurement.getAccuracy(), 5.0f));

            return filtered;
        }
    }
}