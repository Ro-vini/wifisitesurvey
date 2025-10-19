package com.example.wifisitesurvey.ui.metrics;

import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;

import com.example.wifisitesurvey.services.WifiService;
import com.example.wifisitesurvey.services.PingService;
import com.example.wifisitesurvey.utils.WifiAnalyzer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WifiHealthEvaluator {
    private final WifiAnalyzer analyzer;
    private final PingService pingService;
    private final WifiService wifiService;

    public WifiHealthEvaluator(WifiAnalyzer analyzer, PingService pingService, WifiService wifiService) {
        this.analyzer = analyzer;
        this.pingService = pingService;
        this.wifiService = wifiService;
    }

    /**
     * Avalia a saúde da rede atual em 0-100.
     * Base: qualidade do sinal (rssi), colisões de canal e ping ao gateway.
     */
    public int evaluateNetworkHealth(WifiInfo info) {
        if (info == null) return 0;

        int rssiScore = analyzer.calculateSignalQuality(info.getRssi()); // 0-100
        int collisionPenalty = 0;

        List<ScanResult> results = wifiService.scanNetworks();
        Map<Integer, Integer> channelCount = new HashMap<>();
        if (results != null) {
            for (ScanResult sr : results) {
                int ch = analyzer.frequencyToChannel(sr.frequency);
                if (ch != -1) {
                    channelCount.put(ch, channelCount.getOrDefault(ch, 0) + 1);
                }
            }
        }

        int currentChannel = analyzer.frequencyToChannel(info.getFrequency());
        if (currentChannel != -1 && channelCount.getOrDefault(currentChannel, 0) > 1) {
            collisionPenalty = 20;
        }

        // Ping ao gateway
        int pingScore = 10;
        try {
            DhcpInfo dhcp = wifiService.getDhcpInfo();
            if (dhcp != null) {
                String gateway = analyzer.formatIpAddress(dhcp.gateway);
                if (gateway != null && !gateway.equals("0.0.0.0")) {
                    String out = pingService.pingHost(gateway);
                    if (out != null && (out.contains("time=") || out.contains("OK"))) {
                        pingScore = 20;
                    } else {
                        pingScore = 10;
                    }
                }
            }
        } catch (Exception ignored) {
            pingScore = 10;
        }

        int health = rssiScore + pingScore - collisionPenalty;
        if (health > 100) health = 100;
        if (health < 0) health = 0;
        return health;
    }
}
