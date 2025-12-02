package com.ads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the status (UP/DOWN) of all sites.
 * Tracks uptime history to validate read eligibility for Available Copies
 * algorithm.
 * @author Vincent Xu
 * @version 1.0 (Created: 2025-12-01)
 */
public class SiteDirectory {
    private static final int NUM_SITES = 10;

    // Map<SiteId, List<UptimeInterval>>
    private final Map<Integer, List<UptimeInterval>> uptimeHistory;

    // Current status of each site
    private final Map<Integer, Boolean> currentStatus;

    private static class UptimeInterval {
        int start;
        int end; // -1 means currently open (still UP)

        UptimeInterval(int start) {
            this.start = start;
            this.end = -1;
        }
    }

    public SiteDirectory() {
        uptimeHistory = new HashMap<>();
        currentStatus = new HashMap<>();

        // Initialize all sites as UP from time 0
        for (int i = 1; i <= NUM_SITES; i++) {
            currentStatus.put(i, true);
            List<UptimeInterval> history = new ArrayList<>();
            history.add(new UptimeInterval(0));
            uptimeHistory.put(i, history);
        }
    }

    /**
     * Mark a site as failed at a specific time.
     */
    public void fail(int siteId, int time) {
        if (!currentStatus.get(siteId)) {
            return; // Already down
        }

        currentStatus.put(siteId, false);

        // Close the current uptime interval
        List<UptimeInterval> history = uptimeHistory.get(siteId);
        UptimeInterval current = history.get(history.size() - 1);
        current.end = time;
    }

    /**
     * Recover a site at a specific time.
     */
    public void recover(int siteId, int time) {
        if (currentStatus.get(siteId)) {
            return; // Already up
        }

        currentStatus.put(siteId, true);

        // Start a new uptime interval
        List<UptimeInterval> history = uptimeHistory.get(siteId);
        history.add(new UptimeInterval(time));
    }

    /**
     * Check if a site is currently UP.
     */
    public boolean isUp(int siteId) {
        return currentStatus.getOrDefault(siteId, false);
    }

    /**
     * Check if a site has been continuously UP since the given time.
     * This is crucial for the Available Copies algorithm.
     * 
     * @param siteId    The site to check
     * @param sinceTime The time to check from (usually transaction start time)
     * @return true if site was up from sinceTime until now
     */
    public boolean wasContinuouslyUp(int siteId, int sinceTime) {
        if (!isUp(siteId)) {
            return false;
        }

        List<UptimeInterval> history = uptimeHistory.get(siteId);
        if (history.isEmpty()) {
            return false;
        }

        for (int i = history.size() - 1; i >=0; i--) {
            UptimeInterval interval = history.get(i);
            if (interval.start <= sinceTime) {
                return interval.end == -1;
            }
        }
        return false;
    }
}
