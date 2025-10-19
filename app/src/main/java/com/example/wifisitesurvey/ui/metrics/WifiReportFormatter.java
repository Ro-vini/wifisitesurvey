package com.example.wifisitesurvey.ui.metrics;

import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;

import com.example.wifisitesurvey.services.ShellPingService;
import com.example.wifisitesurvey.utils.WifiAnalyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WifiReportFormatter {
    private final WifiAnalyzer analyzer;

    public WifiReportFormatter(WifiAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public String formatCurrentNetwork(WifiInfo info, ScanResult scan, DhcpInfo dhcp, int health, String mobileIpAddress) {
        if (info == null) {
            return "Nenhuma rede atual conectada.\n";
        }

        StringBuilder sb = new StringBuilder("========== Rede Atual ==========");
        sb.append("\n📡 SSID: ").append(info.getSSID());
        sb.append("\n🔗 BSSID: ").append(info.getBSSID());
        sb.append("\n📶 RSSI: ").append(info.getRssi()).append(" dBm (")
                .append(analyzer.classifySignal(info.getRssi())).append(")");
        sb.append("\nQualidade: ").append(analyzer.calculateSignalQuality(info.getRssi())).append("%");
        sb.append("\n💚 Saúde da rede: ").append(health).append("%");
        sb.append("\n🚀 Velocidade: ").append(info.getLinkSpeed()).append(" Mbps");

        if (scan != null) {
            sb.append("\n📡 Modo: ").append(analyzer.mapWifiStandard(scan));
            sb.append("\n Segurança: ").append(analyzer.getSecurityType(scan.capabilities));
            sb.append("\n📶 Largura de Canal: ").append(analyzer.mapChannelWidth(scan));
            sb.append("\n📡 Frequência: ").append(scan.frequency).append(" MHz / ").append(analyzer.getSignalType(scan.frequency));

            int channel = analyzer.frequencyToChannel(scan.frequency);
            sb.append("\n📺 Canal: ").append(channel >= 0 ? channel : "Desconhecido");
        }

        String wifiIp = "N/A";
        if (dhcp != null && dhcp.ipAddress != 0) {
            wifiIp = analyzer.formatIpAddress(dhcp.ipAddress); // formatIpAddress está em WifiAnalyzer
        }
        else if (info.getIpAddress() != 0) {
            // Fallback para WifiInfo se DhcpInfo não tiver o IP
            wifiIp = analyzer.formatIpAddress(info.getIpAddress());
        }

        sb.append("\n🌐 IP (Wi-Fi): ").append(wifiIp);
        sb.append("\n📱 IP (Dados Móveis): ").append(mobileIpAddress);

        String pingTarget = "8.8.8.8";
        String pingResult = new ShellPingService().pingHost(pingTarget);
        sb.append("\n🏓 Ping (").append(pingTarget).append(" via rede padrão): ").append(pingResult);

        sb.append("\n");

        return sb.toString();
    }

    public String formatNearbyNetwork(List<ScanResult> scans) {
        if (scans == null || scans.isEmpty()) {
            return "Nenhuma rede próxima encontrada.\n";
        }

        StringBuilder sb = new StringBuilder("========== Rede Próximas ==========");

        Map<String, List<ScanResult>> networksBySsid = new HashMap<>();
        for (ScanResult scan : scans) {
            String ssid = (scan.SSID == null || scan.SSID.isEmpty()) ? "<Oculto>" : scan.SSID;
            networksBySsid.computeIfAbsent(ssid, newSsidGroup -> new ArrayList<>()).add(scan);
        }

        List<String> sortedSsids = new ArrayList<>(networksBySsid.keySet());
        Collections.sort(sortedSsids);

        for (String ssid : sortedSsids) {
            List<ScanResult> networkPoints = networksBySsid.get(ssid);
            if (networkPoints == null || networkPoints.isEmpty()) {
                continue;
            }

            sb.append("\n📡 SSID: ").append(ssid);

            Map<String, List<ScanResult>> bssidsBySignalType = new HashMap<>();
            for (ScanResult sr : networkPoints) {
                String signalType = analyzer.getSignalType(sr.frequency);
                bssidsBySignalType.computeIfAbsent(signalType, k -> new ArrayList<>()).add(sr);
            }

            boolean hasCalc = false;
            for (Map.Entry<String, List<ScanResult>> signalEntry : bssidsBySignalType.entrySet()) {
                String signalType = signalEntry.getKey();
                List<ScanResult> pointsInSignalType = signalEntry.getValue();

                if (pointsInSignalType.size() > 1) {
                    hasCalc = true;

                    double sumRssiForAverage = 0;
                    for (ScanResult sr : pointsInSignalType) {
                        sumRssiForAverage += sr.level;
                    }

                    double avgRssi = sumRssiForAverage / pointsInSignalType.size();

                    double accumulatedDbm = analyzer.calculateAccumulatedDbm(pointsInSignalType);

                    sb.append("\n\t\t📈").append(signalType).append(": ~").append(avgRssi).append(" dBm (média de ").append(pointsInSignalType.size()).append("APs)");
                    sb.append("\n\t\t🔋").append(signalType).append(": ~").append(accumulatedDbm).append(" dBm (média de ").append(pointsInSignalType.size()).append("APs)");
                }
            }

            if (hasCalc) {
                sb.append("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
            }

            networkPoints.sort(Comparator.comparing(sr -> sr.BSSID));

            for (ScanResult sr : networkPoints) {
                sb.append("\n\t\t🔗 BSSID: ").append(sr.BSSID);
                sb.append("\n\t\t📶 RSSI: ").append(sr.level).append(" dBm (")
                        .append(analyzer.classifySignal(sr.level)).append(")");
                sb.append("\n\t\tQualidade: ").append(analyzer.calculateSignalQuality(sr.level)).append("%");
                sb.append("\n\t\t📡 Modo: ").append(analyzer.mapWifiStandard(sr));
                sb.append("\n\t\tSegurança: ").append(analyzer.getSecurityType(sr.capabilities));
                sb.append("\n\t\t📶 Largura de Canal: ").append(analyzer.mapChannelWidth(sr));
                sb.append("\n\t\t📡 Frequência: ").append(sr.frequency).append(" MHz / ").append(analyzer.getSignalType(sr.frequency));

                int channel = analyzer.frequencyToChannel(sr.frequency);
                sb.append("\n\t\t📺 Canal: ").append(channel >= 0 ? channel : "Desconhecido");

                if (!networkPoints.getLast().BSSID.equals(sr.BSSID)) {
                    sb.append("\n\t\t+++++++++++++++++++++++++++++++++");
                }
            }

            if (!sortedSsids.get(sortedSsids.size() - 1).equals(ssid)) {
                sb.append("\n\t\t---------------------------------");
            }
        }

        sb.append("\n");

        return sb.toString();
    }

    public String formatCollisionsReport(List<ScanResult> scans) {
        if (scans == null || scans.isEmpty()) {
            return "Nenhuma rede encontrada para analisar colisões.\n";
        }

        // 1. Agrupar por canal
        Map<Integer, List<ScanResult>> byChannel = new HashMap<>();
        for (ScanResult sr : scans) {
            int channel = analyzer.frequencyToChannel(sr.frequency);
            // Considerar apenas canais válidos para o relatório de colisão
            if (channel >= 0) {
                byChannel.computeIfAbsent(channel, channelValue -> new ArrayList<>()).add(sr);
            }
        }

        // Se nenhum canal válido foi encontrado (por exemplo, todas as frequências eram desconhecidas)
        if (byChannel.isEmpty()) {
            return "Nenhuma rede com canal conhecido encontrada para analisar colisões.\n";
        }

        // 2. Ordenar as chaves (canais)
        List<Integer> sortedChannels = new ArrayList<>(byChannel.keySet());
        Collections.sort(sortedChannels);

        // Usar LinkedHashMap para manter a ordem dos canais no relatório final
        Map<Integer, List<ScanResult>> sortedByChannel = new LinkedHashMap<>();

        // 3. Para cada canal, ordenar a lista de ScanResult por SSID e depois por BSSID
        for (Integer channel : sortedChannels) {
            List<ScanResult> scansInChannel = byChannel.get(channel);
            if (scansInChannel != null) {
                Collections.sort(scansInChannel, new Comparator<ScanResult>() {
                    @Override
                    public int compare(ScanResult sr1, ScanResult sr2) {
                        // Tratar SSID nulo ou vazio como vindo antes/depois ou de forma consistente
                        String ssid1 = (sr1.SSID == null || sr1.SSID.isEmpty()) ? "" : sr1.SSID;
                        String ssid2 = (sr2.SSID == null || sr2.SSID.isEmpty()) ? "" : sr2.SSID;

                        int ssidCompare = ssid1.compareToIgnoreCase(ssid2);
                        if (ssidCompare != 0) {
                            return ssidCompare;
                        }
                        // Se SSIDs são iguais (ou ambos vazios), comparar por BSSID
                        return sr1.BSSID.compareToIgnoreCase(sr2.BSSID);
                    }
                });
                sortedByChannel.put(channel, scansInChannel);
            }
        }

        StringBuilder sb = new StringBuilder("========== Possíveis Colisões (Ordenado) ==========\n");
        boolean foundCollisions = false;

        for (Map.Entry<Integer, List<ScanResult>> entry : sortedByChannel.entrySet()) {
            List<ScanResult> list = entry.getValue();
            // Considerar colisão se mais de uma rede estiver no mesmo canal
            if (list.size() > 1) {
                foundCollisions = true;
                sb.append("⚠️ Canal ").append(entry.getKey()).append(" ocupado por ")
                        .append(list.size()).append(" redes:\n");
                for (ScanResult sr : list) {
                    String ssidDisplay = (sr.SSID == null || sr.SSID.isEmpty()) ? "<Oculto>" : sr.SSID;
                    sb.append("   • ").append(ssidDisplay)
                            .append(" (").append(sr.BSSID).append(" - ")
                            .append(analyzer.getSignalType(sr.frequency)).append(")\n");
                }
            }
        }

        if (!foundCollisions) {
            return "Nenhuma colisão de canal detectada entre as redes encontradas.\n";
        }

        return sb.toString();
    }

}
