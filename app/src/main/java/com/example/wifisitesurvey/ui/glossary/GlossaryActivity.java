package com.example.wifisitesurvey.ui.glossary;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wifisitesurvey.R;

import java.util.ArrayList;
import java.util.List;

public class GlossaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glossary);
        setTitle("Glossário");

        RecyclerView rvGlossary = findViewById(R.id.rvGlossary);
        rvGlossary.setLayoutManager(new LinearLayoutManager(this));

        GlossaryAdapter adapter = new GlossaryAdapter();
        rvGlossary.setAdapter(adapter);

        // Você pode carregar esta lista de um arquivo de strings, banco de dados, etc.
        adapter.setGlossaryItems(createGlossaryData());
    }

    private List<GlossaryItem> createGlossaryData() {
        List<GlossaryItem> items = new ArrayList<>();

        items.add(new GlossaryItem("RSSI",
                "Received Signal Strength Indicator (Indicador de Força do Sinal Recebido). Mede a potência de um sinal de rádio captado por um dispositivo. É expresso em decibéis-miliwatts (dBm) e seus valores são negativos. Valores mais próximos de 0 indicam um sinal mais forte (ex: -55 dBm é melhor que -75 dBm)."));

        items.add(new GlossaryItem("SSID",
                "Service Set Identifier. É o nome público de uma rede Wi-Fi, aquele que você seleciona na lista de redes disponíveis no seu dispositivo para se conectar (ex: \"Casa-WiFi\")."));

        items.add(new GlossaryItem("BSSID",
                "Basic Service Set Identifier. É o endereço MAC (um identificador único de hardware) do ponto de acesso (roteador) que está transmitindo o sinal Wi-Fi. Uma única rede (SSID) pode ter múltiplos BSSIDs se houver mais de um roteador ou repetidor estendendo a cobertura."));

        items.add(new GlossaryItem("Colisão de Canal",
                "Ocorre quando duas ou mais redes Wi-Fi próximas operam no mesmo canal ou em canais sobrepostos. Isso causa interferência, degrada a performance da rede e pode levar a conexões instáveis e velocidades mais baixas para todos os dispositivos na área."));

        // Adicione mais termos aqui...

        return items;
    }
}
