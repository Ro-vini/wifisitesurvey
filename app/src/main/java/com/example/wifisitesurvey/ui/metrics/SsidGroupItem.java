package com.example.wifisitesurvey.ui.metrics;

import java.util.List;
public class SsidGroupItem implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private final String ssidName;
    private final boolean isCurrentNetwork;
    private final List<BssidInfo> bssids;
    private boolean isExpanded;

    public SsidGroupItem(String ssidName, boolean isCurrentNetwork, List<BssidInfo> bssids) {
        //... (o resto do código fica igual)
        this.ssidName = ssidName;
        this.isCurrentNetwork = isCurrentNetwork;
        this.bssids = bssids;
        this.isExpanded = false;
    }

    // Getters
    public String getSsidName() { return ssidName; }
    public boolean isCurrentNetwork() { return isCurrentNetwork; }
    public List<BssidInfo> getBssids() { return bssids; }
    public int getBssidCount() { return bssids.size(); }
}