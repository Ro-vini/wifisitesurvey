package com.example.wifisitesurvey.utils;

import android.net.wifi.ScanResult;
import android.os.Build;

import java.util.List;

public class WifiAnalyzer {
    public int calculateSignalQuality(int rssi) {
        if (rssi >= -50) return 100;
        if (rssi >= -60) return 80;
        if (rssi >= -70) return 60;
        if (rssi >= -80) return 40;
        if (rssi >= -90) return 20;

        return 0;
    }

    public String classifySignal(int rssi) {
        if (rssi <= -90) return "📉 Muito fraco";
        if (rssi <= -80) return "📶 Fraco";
        if (rssi <= -67) return "📡 Bom";
        return "🚀 Excelente";
    }

    public String mapWifiStandard(ScanResult sr) {
        if (sr == null)
            return "Indisponível";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            switch (sr.getWifiStandard()) {
                case ScanResult.WIFI_STANDARD_11BE:
                    return "Wi-Fi 7 (802.11be)";
                case ScanResult.WIFI_STANDARD_11AX:
                    return "Wi-Fi 6 (802.11ax)";
                case ScanResult.WIFI_STANDARD_11AC:
                    return "Wi-Fi 5 (802.11ac)";
                case ScanResult.WIFI_STANDARD_11N:
                    return "Wi-Fi 4 (802.11n)";
                case ScanResult.WIFI_STANDARD_LEGACY:
                    return "Wi-Fi Legacy (802.11a/b/g)";
                case ScanResult.WIFI_STANDARD_11AD:
                    return "Wi-Fi AD (802.11ad / WiGig)";
                case ScanResult.WIFI_STANDARD_UNKNOWN:
                default:
                    return "Padrão desconhecido";
            }
        }

        return "Não é possível retornar o padrão do Wi-Fi, pois a versão do Android neste dispositivo é inferior a 12";
    }

    public String mapChannelWidth(ScanResult sr) {
        if (sr == null)
            return "Indisponível";

        switch (sr.channelWidth) {
            case ScanResult.CHANNEL_WIDTH_20MHZ:
                return "20 MHz";
            case ScanResult.CHANNEL_WIDTH_40MHZ:
                return "40 MHz";
            case ScanResult.CHANNEL_WIDTH_80MHZ:
                return "80 MHz";
            case ScanResult.CHANNEL_WIDTH_160MHZ:
                return "160 MHz";
            case ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ:
                return "80+80 MHz";
            case ScanResult.CHANNEL_WIDTH_320MHZ:
                return "320 MHZ";
            default:
                return "Desconhecida";
        }
    }

    public String formatIpAddress(int ip) {
        return String.format(
                "%d.%d.%d.%d",
                (ip & 0xff),
                (ip >> 8 & 0xff),
                (ip >> 16 & 0xff),
                (ip >> 24 & 0xff)
        );
    }

    public int frequencyToChannel(int freq) {
        if (freq >= 2412 && freq <= 2472)
            return (freq - 2407) / 5;
        if (freq == 2484)
            return 14;
        if (freq >= 5180 && freq <= 5825)
            return (freq - 5000) / 5;
        if (freq >= 5955 && freq <= 7115)
            return (freq - 5950) / 5;

        return -1;
    }

    public String getSignalType(int freq) {
        if (freq >= 2400 && freq < 2500)
            return "2.4 GHz";
        if (freq >= 4900 && freq < 5900)
            return "5 GHz";
        if (freq >= 5925 && freq < 7125)
            return "6 GHz";
        return "Frequência desconhecida";
    }

    public String getSecurityType(String security) {
        if (security == null || security.isEmpty())
            return "Desconhecido";

        if (security.contains("WEP"))
            return "WEP";
        if (security.contains("PSK"))
            return "WPA/WPA2-PSK";
        if (security.contains("EAP"))
            return "WPA/WPA2-EAP";

        return "Aberta";
    }

    public double dbmToMilliwatt(int dbm) {
        return Math.pow(10, dbm / 10.0);
    }

    public double milliwattToDbm(double mW) {
        return 10.0 * Math.log10(mW);
    }

    public double calculateAccumulatedDbm(List<ScanResult> scans) {
        double totalMilliwatt = 0.0;

        for (int i = 0; i < scans.size(); i++) {
            ScanResult sr = scans.get(i);
            totalMilliwatt += dbmToMilliwatt(sr.level);
        }

        if (totalMilliwatt <= 0) return -150; // fallback caso não haja sinal

        return milliwattToDbm(totalMilliwatt);
    }
}
