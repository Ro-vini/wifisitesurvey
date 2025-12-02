package com.example.wifisitesurvey.ui.bssidDetail;
public class BssidInfo implements java.io.Serializable {
    private final String bssid;
    private final String details;
    private final String collisionReport;
    private boolean isExpanded;

    private static final long serialVersionUID = 1L;

    public BssidInfo(String bssid, String details, String collisionReport) {
        this.bssid = bssid;
        this.details = details;
        this.collisionReport = collisionReport;
    }

    public String getBssid() { return bssid; }
    public String getDetails() { return details; }
    public String getCollisionReport() { return collisionReport; }
    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
}