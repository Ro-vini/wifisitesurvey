package com.example.wifisitesurvey.ui.bssidDetail;

import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wifisitesurvey.R;
import com.example.wifisitesurvey.services.WifiService;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

public class BssidDetailAdapter extends RecyclerView.Adapter<BssidDetailAdapter.BssidViewHolder> {

    private List<BssidInfo> bssids = new ArrayList<>();
    private String currentBssid = null;
    private final WifiService wifiService;

    // Guarda a posição do item que está expandido. -1 significa nenhum.
    private int expandedPosition = -1;

    public BssidDetailAdapter(WifiService wifiService) {
        this.wifiService = wifiService;
    }

    public void setBssidList(List<BssidInfo> bssids) {
        this.bssids = (bssids != null) ? bssids : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setCurrentBssid(String currentBssid) {
        this.currentBssid = currentBssid;
        notifyDataSetChanged(); // Notificar para reavaliar a visibilidade do botão
    }

    @NonNull
    @Override
    public BssidViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_bssid_detail, parent, false);
        return new BssidViewHolder(view, wifiService);
    }

    @Override
    public void onBindViewHolder(@NonNull BssidViewHolder holder, int position) {
        BssidInfo bssidInfo = bssids.get(position);
        boolean isCurrentlyConnectedBssid = bssidInfo.getBssid().equals(this.currentBssid);
        final boolean isExpanded = position == expandedPosition;

        holder.bind(bssidInfo, isCurrentlyConnectedBssid, isExpanded);

        holder.itemView.setOnClickListener(v -> {
            // Se o item clicado já estava expandido, apenas o recolhe.
            if (isExpanded) {
                expandedPosition = -1;
                notifyItemChanged(position);
            } else {
                // Se outro item estava expandido, o recolhe primeiro.
                if (expandedPosition >= 0) {
                    int prevExpandedPosition = expandedPosition;
                    expandedPosition = -1;
                    notifyItemChanged(prevExpandedPosition);
                }
                // Expande o novo item.
                expandedPosition = position;
                notifyItemChanged(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bssids.size();
    }

    /**
     * Quando o item sai da tela, paramos a análise para economizar bateria.
     */
    @Override
    public void onViewRecycled(@NonNull BssidViewHolder holder) {
        super.onViewRecycled(holder);
        // Garante que a análise pare se a view for reciclada (sair da tela)
        holder.stopLiveAnalysis();
    }

    static class BssidViewHolder extends RecyclerView.ViewHolder {
        // Views
        private final TextView tvBssidHeader, tvBssidDetails, tvBssidCollision;
        private final ImageView ivBssidExpandArrow;
        private final LinearLayout llBssidDetailsContent;
        private final LineChart liveChart;

        // Controle da Análise
        private final WifiService wifiService;
        private final Handler analysisHandler = new Handler(Looper.getMainLooper());
        private boolean isAnalyzing = false;
        private long analysisStartTime = 0;
        private static final int ANALYSIS_INTERVAL_MS = 1000;

        BssidViewHolder(@NonNull View itemView, WifiService wifiService) {
            super(itemView);
            this.wifiService = wifiService;

            tvBssidHeader = itemView.findViewById(R.id.tvBssidHeader);
            tvBssidDetails = itemView.findViewById(R.id.tvBssidDetails);
            tvBssidCollision = itemView.findViewById(R.id.tvBssidCollision);

            ivBssidExpandArrow = itemView.findViewById(R.id.ivBssidExpandArrow);
            llBssidDetailsContent = itemView.findViewById(R.id.llBssidDetailsContent);
            liveChart = itemView.findViewById(R.id.liveAnalysisChart);
        }

        void bind(BssidInfo bssidInfo, boolean isConnected, boolean isExpanded) {
            tvBssidHeader.setText("BSSID: " + bssidInfo.getBssid());
            tvBssidDetails.setText(bssidInfo.getDetails());
            llBssidDetailsContent.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            ivBssidExpandArrow.setRotation(isExpanded ? 180 : 0);

            // Conteúdo de colisão
            String collisionReport = bssidInfo.getCollisionReport();
            if (collisionReport != null && !collisionReport.isEmpty()) {
                tvBssidCollision.setText(collisionReport);
                tvBssidCollision.setVisibility(View.VISIBLE);
            } else {
                tvBssidCollision.setVisibility(View.GONE);
            }

            // A análise só é possível no BSSID conectado E se o item estiver expandido.
            if (isConnected && isExpanded) {
                if (!isAnalyzing) { // Evita iniciar a análise múltiplas vezes
                    startLiveAnalysis();
                }
            } else {
                if (isAnalyzing) {
                    stopLiveAnalysis();
                }
            }
        }

        void startLiveAnalysis() {
            isAnalyzing = true;
            liveChart.setVisibility(View.VISIBLE);
            setupChart();
            analysisStartTime = System.currentTimeMillis();
            analysisHandler.post(analysisRunnable);
            Toast.makeText(itemView.getContext(), "Iniciando análise...", Toast.LENGTH_SHORT).show();
        }

        void stopLiveAnalysis() {
            if (!isAnalyzing) return;
            isAnalyzing = false;
            liveChart.setVisibility(View.GONE);
            analysisHandler.removeCallbacks(analysisRunnable);
        }

        private final Runnable analysisRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAnalyzing) return;

                WifiInfo currentConnection = wifiService.getCurrentConnection();
                if (currentConnection == null) {
                    Toast.makeText(itemView.getContext(), "Conexão Wi-Fi perdida.", Toast.LENGTH_SHORT).show();
                    stopLiveAnalysis();
                    return;
                }

                addChartEntry(currentConnection.getRssi(), currentConnection.getLinkSpeed());
                analysisHandler.postDelayed(this, ANALYSIS_INTERVAL_MS);
            }
        };

        private void setupChart() {
            // --- Início da Modificação: Cores Reativas e Scroll ---
            liveChart.getDescription().setEnabled(false);
            liveChart.setDrawGridBackground(false);

            // Habilita o toque e o arrastar, essenciais para o scroll horizontal
            liveChart.setTouchEnabled(true);
            liveChart.setDragEnabled(true);
            liveChart.setScaleEnabled(true); // Permite zoom
            liveChart.setPinchZoom(true);

            // Obter a cor do texto principal do tema atual (funciona para light/dark)
            int textColor = MaterialColors.getColor(
                    itemView.getContext(),
                    com.google.android.material.R.attr.colorOnSurface,
                    Color.BLACK // Cor de fallback
            );

            // Configurar Eixo X (inferior)
            XAxis xAxis = liveChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setTextColor(textColor);
            xAxis.setDrawGridLines(false);

            // Configurar Eixo Y (esquerdo)
            YAxis leftAxis = liveChart.getAxisLeft();
            leftAxis.setTextColor(textColor);
            leftAxis.setDrawGridLines(true); // Mantém as linhas de grade para referência

            // Desabilitar Eixo Y (direito)
            liveChart.getAxisRight().setEnabled(false);

            // Configurar Legenda
            liveChart.getLegend().setTextColor(textColor);

            // Limpar dados antigos e preparar o gráfico
            liveChart.setData(new LineData());
            // --- Fim da Modificação ---
        }

        private void addChartEntry(int rssi, int linkSpeed) {
            LineData data = liveChart.getData();
            if (data == null) return;

            ILineDataSet rssiSet = data.getDataSetByIndex(0);
            if (rssiSet == null) {
                rssiSet = createDataSet("RSSI (dBm)", Color.CYAN);
                data.addDataSet(rssiSet);
            }

            ILineDataSet speedSet = data.getDataSetByIndex(1);
            if (speedSet == null) {
                speedSet = createDataSet("Velocidade (Mbps)", Color.GREEN);
                data.addDataSet(speedSet);
            }

            float timeInSeconds = (System.currentTimeMillis() - analysisStartTime) / 1000f;
            data.addEntry(new Entry(timeInSeconds, rssi), 0);
            data.addEntry(new Entry(timeInSeconds, linkSpeed), 1);

            data.notifyDataChanged();
            liveChart.notifyDataSetChanged();

            // Limita a janela visível a 15 segundos, o que força o scroll horizontal
            liveChart.setVisibleXRangeMaximum(15);
            // Move a visão para a última entrada, fazendo o gráfico "rolar" automaticamente
            liveChart.moveViewToX(data.getEntryCount());
        }

        private LineDataSet createDataSet(String label, int color) {
            LineDataSet set = new LineDataSet(new ArrayList<>(), label);
            set.setAxisDependency(YAxis.AxisDependency.LEFT);
            set.setColor(color);
            set.setCircleColor(color);
            set.setLineWidth(2f);
            set.setCircleRadius(2f);
            set.setDrawValues(false);
            return set;
        }
    }
}