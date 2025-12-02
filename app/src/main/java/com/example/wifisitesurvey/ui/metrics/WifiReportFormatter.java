package com.example.wifisitesurvey.ui.metrics;

import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;

import com.example.wifisitesurvey.services.ShellPingService;
import com.example.wifisitesurvey.utils.WifiAnalyzer;

import java.util.ArrayList;
import java.util.List;

public class WifiReportFormatter {
    private final WifiAnalyzer analyzer;

    public WifiReportFormatter(WifiAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    /**
     * Formata os detalhes de uma rede (para o card expandido).
     * Este método é genérico para qualquer ScanResult.
     */
    public String formatNetworkDetails(ScanResult scan) {
        if (scan == null) return "Sem dados de scan.";

        StringBuilder sb = new StringBuilder();
        sb.append("RSSI: ").append(scan.level).append(" dBm (")
                .append(analyzer.classifySignal(scan.level)).append(")");
        sb.append("\nQualidade: ").append(analyzer.calculateSignalQuality(scan.level)).append("%");
        sb.append("\nModo: ").append(analyzer.mapWifiStandard(scan));
        sb.append("\nSegurança: ").append(analyzer.getSecurityType(scan.capabilities));
        sb.append("\nLargura de Canal: ").append(analyzer.mapChannelWidth(scan));
        sb.append("\nFrequência: ").append(scan.frequency).append(" MHz / ").append(analyzer.getSignalType(scan.frequency));

        int channel = analyzer.frequencyToChannel(scan.frequency);
        sb.append("\nCanal: ").append(channel >= 0 ? channel : "Desconhecido");

        return sb.toString();
    }

    /**
     * Formata os detalhes ADICIONAIS que só existem para a rede ATUAL.
     */
    public String formatCurrentNetworkExtras(WifiInfo info, DhcpInfo dhcp) {
        if (info == null) return "";

        StringBuilder sb = new StringBuilder();
        //sb.append("\nSaúde da rede: ").append(health).append("%");
        sb.append("\nVelocidade: ").append(info.getLinkSpeed()).append(" Mbps");

        String wifiIp = "N/A";
        if (dhcp != null && dhcp.ipAddress != 0) {
            wifiIp = analyzer.formatIpAddress(dhcp.ipAddress);
        } else if (info.getIpAddress() != 0) {
            wifiIp = analyzer.formatIpAddress(info.getIpAddress());
        }

        sb.append("\nIP (Wi-Fi): ").append(wifiIp);

        String pingTarget = "8.8.8.8";
        String pingResult = new ShellPingService().pingHost(pingTarget);
        sb.append("\n").append(pingResult);

        return sb.toString();
    }

    /**
     * Formata o relatório de colisão APENAS para o canal da rede atual.
     */
    public String formatCollisionsForCurrentChannel(WifiInfo info, List<ScanResult> allScans) {
        if (info == null || allScans == null || allScans.isEmpty()) {
            return "N/A";
        }

        int currentChannel = analyzer.frequencyToChannel(info.getFrequency());
        if (currentChannel == -1) {
            return "Não foi possível determinar o canal da rede atual.";
        }

        List<String> collidingSsids = new ArrayList<>();
        for (ScanResult sr : allScans) {
            // Não contar a si mesmo
            if (sr.BSSID.equals(info.getBSSID())) {
                continue;
            }

            int channel = analyzer.frequencyToChannel(sr.frequency);
            if (channel == currentChannel) {
                String ssidDisplay = (sr.SSID == null || sr.SSID.isEmpty()) ? "<Oculto>" : sr.SSID;
                collidingSsids.add(" • " + ssidDisplay + " (" + sr.BSSID + ")");
            }
        }

        if (collidingSsids.isEmpty()) {
            return "Nenhuma outra rede detectada no canal " + currentChannel + ".";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("⚠Análise de Colisão (Canal ").append(currentChannel).append("):\n");
        sb.append("    Concorrendo com ").append(collidingSsids.size()).append(" outra(s) rede(s):\n");
        for (String ssid : collidingSsids) {
            sb.append("   ").append(ssid).append("\n");
        }

        return sb.toString().trim();
    }
}