package com.example.wifisitesurvey.services;

public class ShellPingService implements PingService {
    @Override
    public String pingHost(String host) {
        try {
            Process process = Runtime.getRuntime().exec("/system/bin/ping -c 1 " + host);
            int result = process.waitFor();
            return "Ping " + host + (result == 0 ? " OK" : " falhou");
        }
        catch (Exception e) {
            return "Ping erro: " + e.getMessage();
        }
    }
}
