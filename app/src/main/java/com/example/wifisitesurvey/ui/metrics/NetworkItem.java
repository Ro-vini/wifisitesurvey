package com.example.wifisitesurvey.ui.metrics;

public class NetworkItem {
    private final String ssid;
    private final String bssid;
    private final String details;
    private final String collisionReport;
    private final boolean isCurrentNetwork;
    private boolean isExpanded;

    public NetworkItem(String ssid, String bssid, String details, String collisionReport, boolean isCurrentNetwork) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.details = details;
        this.collisionReport = collisionReport;
        this.isCurrentNetwork = isCurrentNetwork;
        this.isExpanded = false;
    }

    // Getters
    public String getSsid() { return ssid; }
    public String getBssid() { return bssid; }
    public String getDetails() { return details; }
    public String getCollisionReport() { return collisionReport; }
    public boolean isCurrentNetwork() { return isCurrentNetwork; }
    public boolean isExpanded() { return isExpanded; }

    // Setter
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
}