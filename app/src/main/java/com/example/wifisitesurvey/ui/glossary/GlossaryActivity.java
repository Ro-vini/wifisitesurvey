package com.example.wifisitesurvey.ui.glossary;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wifisitesurvey.R;
import com.example.wifisitesurvey.utils.EdgeToEdgeUtils;

import java.util.ArrayList;
import java.util.List;

public class GlossaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glossary);
        setTitle("Glossário");

        View mainLayout = findViewById(R.id.glossary_container);
        EdgeToEdgeUtils.setupEdgeToEdge(mainLayout);

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

        items.add(new GlossaryItem("Qualidade do Sinal (Heatmap)",
                "Representa a força do sinal Wi-Fi em diferentes cores no mapa de calor, permitindo uma fácil visualização da cobertura da rede.<br>" +
                        "\t\t<b>Excelente (Verde-Amarelo):</b> RSSI de -55 dBm ou superior. Ideal para todas as atividades online, incluindo streaming em 4K, jogos e videochamadas.<br>" +
                        "\t\t<b>Bom (Laranja):</b> RSSI entre -56 dBm e -70 dBm. Navegação na web e streaming em HD devem funcionar bem.<br>" +
                        "\t\t<b>Fraco (Vermelho):</b> RSSI entre -71 dBm e -80 dBm. A conexão pode ser instável, adequada apenas para tarefas básicas como e-mails e mensagens.<br>" +
                        "\t\t<b>Muito Fraco (Vermelho Escuro):</b> RSSI entre -81 dBm e -90 dBm. A conexão é unreliable e pode cair frequentemente.<br>" +
                        "\t\t<b>Sem Sinal (Transparente):</b> RSSI inferior a -90 dBm. O dispositivo provavelmente não conseguirá se conectar à rede."));

        items.add(new GlossaryItem("SSID",
                "Service Set Identifier. É o nome público de uma rede Wi-Fi, aquele que você seleciona na lista de redes disponíveis no seu dispositivo para se conectar (ex: \"Casa-WiFi\")."));

        items.add(new GlossaryItem("BSSID",
                "Basic Service Set Identifier. É o endereço MAC (um identificador único de hardware) do ponto de acesso (roteador) que está transmitindo o sinal Wi-Fi. Uma única rede (SSID) pode ter múltiplos BSSIDs se houver mais de um roteador ou repetidor estendendo a cobertura."));

        items.add(new GlossaryItem("Colisão de Canal",
                "Ocorre quando duas ou mais redes Wi-Fi próximas operam no mesmo canal ou em canais sobrepostos. Isso causa interferência, degrada a performance da rede e pode levar a conexões instáveis e velocidades mais baixas para todos os dispositivos na área."));

        items.add(new GlossaryItem("Mbps", "Megabits por segundo. Unidade de medida da velocidade da conexão de rede. Quanto maior o número de Mbps, mais rápida é a transferência de dados."));

        // Adicione mais termos aqui...

        return items;
    }
}