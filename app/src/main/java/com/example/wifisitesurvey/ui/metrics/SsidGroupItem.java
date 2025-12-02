package com.example.wifisitesurvey.ui.metrics;

import com.example.wifisitesurvey.ui.bssidDetail.BssidInfo;

import java.util.List;
public class SsidGroupItem implements java.io.Serializable {
    private final String ssidName;
    private final boolean isCurrentNetwork;
    private final List<BssidInfo> bssids;
    private boolean isExpanded;

    private static final long serialVersionUID = 1L;

    public SsidGroupItem(String ssidName, boolean isCurrentNetwork, List<BssidInfo> bssids) {
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