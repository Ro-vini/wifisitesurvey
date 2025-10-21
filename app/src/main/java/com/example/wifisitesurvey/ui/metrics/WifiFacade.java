package com.example.wifisitesurvey.ui.metrics;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;

import com.example.wifisitesurvey.services.ShellPingService;
import com.example.wifisitesurvey.services.WifiService;
import com.example.wifisitesurvey.utils.WifiAnalyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class WifiFacade {
    private final WifiService wifiService;
    private final WifiAnalyzer analyzer;
    private final WifiReportFormatter formatter;
    private final WifiHealthEvaluator healthEvaluator;

    // Campos para armazenar os dados do último scan
    private WifiInfo current;
    private List<ScanResult> scans;
    private ScanResult currentScan;
    private DhcpInfo dhcp;
    private String mobile;
    private int health;

    public WifiFacade(Context ctx) {
        this.wifiService = new WifiService(ctx);
        this.analyzer = new WifiAnalyzer();
        this.formatter = new WifiReportFormatter(analyzer);
        this.healthEvaluator = new WifiHealthEvaluator(analyzer, new ShellPingService(), wifiService);
    }

    /**
     * Executa um ciclo completo de análise de Wi-Fi e armazena os dados internamente.
     */
    public void performScan() {
        current = wifiService.getCurrentConnection();
        scans = wifiService.scanNetworks();
        currentScan = findCurrentScan(current, scans);
        dhcp = wifiService.getDhcpInfo();
        mobile = wifiService.getMobileIpAddress();
        health = healthEvaluator.evaluateNetworkHealth(current);
    }

    /**
     * Constrói a lista de NetworkItems para o RecyclerView.
     * Deve ser chamado DEPOIS de performScan().
     */
    public List<NetworkItem> buildNetworkItems() {
        List<NetworkItem> items = new ArrayList<>();
        if (scans == null) {
            scans = new ArrayList<>(); // Evitar NullPointerException
        }

        // Adicionar a rede atual (se estiver conectada)
        String currentBssid = (current != null) ? current.getBSSID() : null;
        if (current != null && currentScan != null) {
            String ssid = (current.getSSID() == null || current.getSSID().isEmpty()) ? "<unknown ssid>" : current.getSSID().replace("\"", "");

            String baseDetails = formatter.formatNetworkDetails(currentScan);
            String extraDetails = formatter.formatCurrentNetworkExtras(current, health, dhcp, mobile);
            String details = baseDetails + "\n" + extraDetails;

            // Relatório de colisão
            String collisionReport = formatter.formatCollisionsForCurrentChannel(current, scans);

            items.add(new NetworkItem(ssid, current.getBSSID(), details, collisionReport, true));
        }

        // Adicionar outras redes (ordenadas por SSID)
        List<ScanResult> otherScans = new ArrayList<>();
        for (ScanResult sr : scans) {
            // Não adicionar a rede atual novamente
            if (currentBssid != null && sr.BSSID.equals(currentBssid)) {
                continue;
            }
            otherScans.add(sr);
        }

        // Ordenar por SSID (ignorando maiúsculas/minúsculas)
        otherScans.sort((sr1, sr2) -> {
            String ssid1 = (sr1.SSID == null) ? "" : sr1.SSID;
            String ssid2 = (sr2.SSID == null) ? "" : sr2.SSID;
            return ssid1.compareToIgnoreCase(ssid2);
        });

        for (ScanResult sr : otherScans) {
            String ssid = (sr.SSID == null || sr.SSID.isEmpty()) ? "<Oculto>" : sr.SSID;
            String details = formatter.formatNetworkDetails(sr);
            items.add(new NetworkItem(ssid, sr.BSSID, details, null, false));
        }

        return items;
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

    /**
     * Constrói a lista de SsidGroupItems para o RecyclerView.
     * Deve ser chamado DEPOIS de performScan().
     */
    public List<SsidGroupItem> buildSsidGroups() {
        List<SsidGroupItem> groupItems = new ArrayList<>();
        if (scans == null) {
            scans = new ArrayList<>(); // Evitar NullPointerException
        }

        // 1. Agrupar ScanResults por SSID
        Map<String, List<ScanResult>> scansBySsid = new HashMap<>();
        for (ScanResult sr : scans) {
            String ssid = (sr.SSID == null || sr.SSID.isEmpty()) ? "<Oculto>" : sr.SSID;
            if (!scansBySsid.containsKey(ssid)) {
                scansBySsid.put(ssid, new ArrayList<>());
            }
            scansBySsid.get(ssid).add(sr);
        }

        String currentSsid = (current != null && current.getSSID() != null) ?
                current.getSSID().replace("\"", "") :
                null;
        if (currentSsid != null && currentSsid.isEmpty()) {
            currentSsid = "<unknown ssid>";
        }

        // 2. Criar o SsidGroupItem para a rede ATUAL (se conectada)
        if (currentSsid != null && scansBySsid.containsKey(currentSsid)) {
            groupItems.add(createSsidGroup(
                    currentSsid,
                    scansBySsid.get(currentSsid),
                    true // é a rede atual
            ));
            // Remover da lista para não adicionar de novo
            scansBySsid.remove(currentSsid);
        }

        // 3. Ordenar os SSIDs restantes alfabeticamente
        List<String> sortedSsids = new ArrayList<>(scansBySsid.keySet());
        Collections.sort(sortedSsids, String::compareToIgnoreCase);

        // 4. Criar SsidGroupItems para as outras redes
        for (String ssid : sortedSsids) {
            groupItems.add(createSsidGroup(
                    ssid,
                    scansBySsid.get(ssid),
                    false // não é a rede atual
            ));
        }

        return groupItems;
    }

    /**
     * Método auxiliar para criar um SsidGroupItem.
     */
    private SsidGroupItem createSsidGroup(String ssid, List<ScanResult> scansInGroup, boolean isCurrentGroup) {

        // Ordenar os BSSIDs dentro do grupo por BSSID
        scansInGroup.sort(new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult sr1, ScanResult sr2) {
                String currentBssid = (current != null) ? current.getBSSID() : null;

                // A lógica de priorização só se aplica se for o grupo da rede atual
                if (isCurrentGroup && currentBssid != null) {
                    boolean isSr1Current = sr1.BSSID.equals(currentBssid);
                    boolean isSr2Current = sr2.BSSID.equals(currentBssid);

                    if (isSr1Current && !isSr2Current) {
                        return -1; // sr1 (o atual) vem antes
                    }
                    if (!isSr1Current && isSr2Current) {
                        return 1;  // sr2 (o atual) vem antes (coloca o sr1 depois)
                    }
                }

                // Se nenhum for o atual, ou se não for o grupo atual, ordena alfabeticamente
                return sr1.BSSID.compareToIgnoreCase(sr2.BSSID);
            }
        });

        List<BssidInfo> bssidInfoList = new ArrayList<>();

        for (ScanResult sr : scansInGroup) {
            String details = formatter.formatNetworkDetails(sr);
            String collisionReport = null;

            // Se for a rede atual, adicionar os extras
            if (isCurrentGroup && current != null && sr.BSSID.equals(current.getBSSID())) {
                String extraDetails = formatter.formatCurrentNetworkExtras(current, health, dhcp, mobile);
                details += "\n" + extraDetails;

                // Adicionar colisão SOMENTE ao BSSID específico ao qual estamos conectados
                collisionReport = formatter.formatCollisionsForCurrentChannel(current, this.scans);
            }

            bssidInfoList.add(new BssidInfo(sr.BSSID, details, collisionReport));
        }

        return new SsidGroupItem(ssid, isCurrentGroup, bssidInfoList);
    }
}