package com.example.wifisitesurvey.ui.metrics;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;

import com.example.wifisitesurvey.services.ShellPingService;
import com.example.wifisitesurvey.services.WifiService;
import com.example.wifisitesurvey.utils.WifiAnalyzer;

import java.util.List;

public class WifiFacade {
    private final WifiService wifiService;
    private final WifiAnalyzer analyzer;
    private final WifiReportFormatter formatter;
    private final WifiHealthEvaluator healthEvaluator;

    public WifiFacade(Context ctx) {
        this.wifiService = new WifiService(ctx);
        this.analyzer = new WifiAnalyzer();
        this.formatter = new WifiReportFormatter(analyzer);
        this.healthEvaluator = new WifiHealthEvaluator(analyzer, new ShellPingService(), wifiService);
    }

    /**
     * Executa um ciclo completo de análise de Wi-Fi e retorna relatório textual.
     */
    public String generateFullReport() {
        StringBuilder sb = new StringBuilder();

        WifiInfo current = wifiService.getCurrentConnection();
        List<ScanResult> scans = wifiService.scanNetworks();
        ScanResult currentScan = findCurrentScan(current, scans);

        // IP
        DhcpInfo dhcp = wifiService.getDhcpInfo();
        String mobile = wifiService.getMobileIpAddress();

        // Health
        int health = healthEvaluator.evaluateNetworkHealth(current);

        // Seção rede atual
        sb.append(formatter.formatCurrentNetwork(current, currentScan, dhcp, health, mobile));

        // Seção redes próximas
        sb.append(formatter.formatNearbyNetwork(scans));

        sb.append("\n").append(formatter.formatCollisionsReport(scans));

        return sb.toString();
    }

    /**
     * Tenta localizar no scan a entrada correspondente ao BSSID atual.
     */
    private ScanResult findCurrentScan(WifiInfo current, List<ScanResult> scans) {
        if (current == null || scans == null) return null;
        for (ScanResult sr : scans) {
            if (sr.BSSID != null && sr.BSSID.equals(current.getBSSID())) {
                return sr;
            }
        }
        return null;
    }
}
