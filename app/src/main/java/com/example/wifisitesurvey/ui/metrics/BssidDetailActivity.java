package com.example.wifisitesurvey.ui.metrics;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wifisitesurvey.R;
import com.example.wifisitesurvey.ui.survey.SurveyActivity;
import com.example.wifisitesurvey.utils.EdgeToEdgeUtils;

public class BssidDetailActivity extends AppCompatActivity {

    public static final String EXTRA_SSID_GROUP = "SSID_GROUP_DATA";

    private TextView tvSsidTitle;
    private Button btnDeepAnalysis;
    private RecyclerView rvBssids;
    private BssidDetailAdapter adapter;
    private SsidGroupItem ssidGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bssid_detail);

        View constraintLayout = findViewById(R.id.activity_bssid_root);
        EdgeToEdgeUtils.setupEdgeToEdge(constraintLayout);

        // Resgatar os dados passados pela Intent
        ssidGroup = (SsidGroupItem) getIntent().getSerializableExtra(EXTRA_SSID_GROUP);
        if (ssidGroup == null) {
            Toast.makeText(this, "Erro: Não foi possível carregar os dados do SSID.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvSsidTitle = findViewById(R.id.tvSsidTitle);
        btnDeepAnalysis = findViewById(R.id.btnDeepAnalysis);
        rvBssids = findViewById(R.id.rvBssids);

        // Configurar Título
        // 1. Define apenas o nome do SSID como texto
        tvSsidTitle.setText(ssidGroup.getSsidName());

        // 2. Decide qual ícone usar (o de "check" se for a rede atual)
                int iconResource = ssidGroup.isCurrentNetwork() ?
                        R.drawable.ic_network_check :
                        R.drawable.ic_wifi;

        // 3. Define o ícone à esquerda (DrawableStart) do TextView
                tvSsidTitle.setCompoundDrawablesWithIntrinsicBounds(iconResource, 0, 0, 0);

        // Configurar RecyclerView
        setupRecyclerView();
        adapter.setBssidList(ssidGroup.getBssids());

        // Configurar o botão "Análise Profunda"
        setupDeepAnalysisButton();
    }

    private void setupRecyclerView() {
        adapter = new BssidDetailAdapter(this);
        rvBssids.setLayoutManager(new LinearLayoutManager(this));
        rvBssids.setAdapter(adapter);
    }

    private void setupDeepAnalysisButton() {
        // O botão SÓ aparece se for a rede atual E o usuário estiver conectado
        if (ssidGroup.isCurrentNetwork() && isConnectedToWifi()) {
            btnDeepAnalysis.setVisibility(View.VISIBLE);
            btnDeepAnalysis.setEnabled(true);
            btnDeepAnalysis.setAlpha(1.0f);

            btnDeepAnalysis.setOnClickListener(v -> {
                // Coloque a lógica para abrir sua MainActivity aqui
                // Exemplo:
                Intent intent = new Intent(BssidDetailActivity.this, SurveyActivity.class);
                startActivity(intent);
            });
        } else {
            // Se não for a rede atual, o botão fica escondido
            btnDeepAnalysis.setVisibility(View.GONE);
        }
    }

    /**
     * Verifica se o dispositivo está atualmente conectado a uma rede Wi-Fi.
     */
    private boolean isConnectedToWifi() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (capabilities == null) return false;
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }
}