package com.example.wifisitesurvey.ui.metrics;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.wifisitesurvey.R;

public class MetricsActivity extends AppCompatActivity {
    private TextView tvMetrics;
    private Button btnScan;
    private WifiFacade wifiFacade;
    private static final int PERMISSIONS_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metrics);

        tvMetrics = findViewById(R.id.tvMetrics);
        btnScan = findViewById(R.id.btnScan);

        //tvMetrics.setText("Aguardando permissões...");
        //checkPermissions();

        wifiFacade = new WifiFacade(this);
        btnScan.setOnClickListener(v -> scanAndDisplay());
    }

    private void scanAndDisplay() {
        new Thread(() -> {
            final String report = wifiFacade.generateFullReport();
            runOnUiThread(() -> tvMetrics.setText(report));
        }).start();
    }
}