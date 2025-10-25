package com.example.wifisitesurvey.ui.metrics;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wifisitesurvey.R;
import com.example.wifisitesurvey.ui.main.MainActivity;
import com.example.wifisitesurvey.utils.EdgeToEdgeUtils;

import java.util.ArrayList;
import java.util.List;

public class MetricsActivity extends AppCompatActivity {

    private Button btnScan;
    private RecyclerView rvNetworks;
    private WifiFacade wifiFacade;
    private SsidGroupAdapter ssidGroupAdapter;
    private ConstraintLayout constraintLayout;
    private boolean isInitialState = true;

    private static final int PERMISSIONS_REQUEST_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metrics);

        constraintLayout = findViewById(R.id.activity_metrics_root);
        EdgeToEdgeUtils.setupEdgeToEdge(constraintLayout);

        // Encontre o layout raiz (que você deu o ID no XML)
        btnScan = findViewById(R.id.btnScan);
        rvNetworks = findViewById(R.id.rvNetworks);

        wifiFacade = new WifiFacade(this);

        // Configurar RecyclerView
        setupRecyclerView();

        // Configurar Listeners
        btnScan.setOnClickListener(v -> checkPermissionsAndScan());
    }

    private void setupRecyclerView() {
        ssidGroupAdapter = new SsidGroupAdapter(this);
        rvNetworks.setLayoutManager(new LinearLayoutManager(this));
        rvNetworks.setAdapter(ssidGroupAdapter);
    }

    private void checkPermissionsAndScan() {
        // Checar permissão de Localização
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_CODE);
        } else {
            // serviços de localização (GPS)
            checkLocationServicesAndScan();
        }
    }

    private void checkLocationServicesAndScan() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) && !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            new AlertDialog.Builder(this)
                    .setMessage("Para escanear redes Wi-Fi, por favor, ative os Serviços de Localização (GPS).")
                    .setPositiveButton("Ligar", (paramDialogInterface, paramInt) ->
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("Cancelar", (dialog, which) ->
                            Toast.makeText(this, "A varredura de Wi-Fi pode falhar.", Toast.LENGTH_LONG).show())
                    .show();
        } else {
            scanAndDisplay();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationServicesAndScan();
            } else {
                Toast.makeText(this, "Permissão de localização negada. Não é possível escanear redes.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateUiToScanningState() {
        if (isInitialState) {
            isInitialState = false;

            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);

            constraintSet.clear(R.id.btnScan, ConstraintSet.TOP);
            constraintSet.clear(R.id.btnScan, ConstraintSet.BOTTOM);

            constraintSet.connect(R.id.btnScan, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);

            constraintSet.connect(R.id.rvNetworks, ConstraintSet.TOP, R.id.btnScan, ConstraintSet.BOTTOM, 16);

            constraintSet.applyTo(constraintLayout);

            rvNetworks.setVisibility(View.VISIBLE);
        }

        btnScan.setEnabled(false);
        btnScan.setText("Verificando...");
        ssidGroupAdapter.setSsidGroups(new ArrayList<>());
    }

    private void scanAndDisplay() {
        updateUiToScanningState();

        new Thread(() -> {
            wifiFacade.performScan();
            final List<SsidGroupItem> groupItems = wifiFacade.buildSsidGroups();

            // Atualiza a UI
            runOnUiThread(() -> {
                ssidGroupAdapter.setSsidGroups(groupItems);

                btnScan.setEnabled(true);
                btnScan.setText("Verificar Novamente");

                if (groupItems.isEmpty()) {
                    Toast.makeText(this, "Nenhuma rede Wi-Fi encontrada.", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}