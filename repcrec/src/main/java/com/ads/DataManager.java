package com.ads;


import com.ads.interfaces.IDataManager;
import com.ads.exceptions.DataManagerException;
import com.ads.exceptions.SiteDownException;
import com.ads.exceptions.StaleReadException;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import com.ads.exceptions.VariableNotFoundException;


/**
 * DataManager is responsible for managing data operations such as reads, writes,
 * commits, aborts, and handling site failures and recoveries.
 * @author Tejas Choudhary
 * @version 1.0 (Created: 2025-11-27, Last Modified: 2025-11-27)
 */
public class DataManager implements IDataManager {
    private final int siteId;
    private boolean isUp;
    
    // Time of last recovery. -1 implies no failure/initial state.
    private int lastRecoveryTime = -1;

    // Map<VariableId, Variable>
    private final Map<String, Variable> variables;

    // Map<TransactionId, List<PendingWrite>>
    // Changed from Map<Var, Val> to List<PendingWrite> to support proper object usage
    private final Map<String, List<PendingWrite>> pendingWrites;

    private void initializeVariables() {
        // Initialize variables x1 to x20
        // Even indexed variables are at all sites.
        // Odd indexed variables are at one site: 1 + (index % 10).
        for (int i = 1; i <= 20; i++) {
            String varId = "x" + i;
            boolean isReplicated = false;
            boolean isOnThisSite = false;

            if (i % 2 == 0) {
                isReplicated = true;
                isOnThisSite = true;
            } else {
                int targetSite = 1 + (i % 10);
                if (targetSite == this.siteId) {
                    isOnThisSite = true;
                }
            }

            if (isOnThisSite) {
                variables.put(varId, new Variable(varId, isReplicated, 10 * i));
            }
        }
    }

    public DataManager(int siteId) {
        this.siteId = siteId;
        this.isUp = true;
        this.variables = new HashMap<>();
        this.pendingWrites = new HashMap<>();
        initializeVariables();
    }

    @Override
    public int read(String transactionId, String variableId, int startTime) 
            throws StaleReadException, SiteDownException, DataManagerException {
        
        if (!isUp) {
            throw new SiteDownException("Site " + siteId + " is down.");
        }

        if (!variables.containsKey(variableId)) {
            throw new VariableNotFoundException("Variable " + variableId + " not found on site " + siteId);
        }

        final Variable var = variables.get(variableId);
        
        // RECOVERY CHECK:
        // "A read from a transaction that begins after the recovery of site s for a replicated variable x 
        // will not be allowed at s until a committed write to x takes place on s."
        if (var.isReplicated() && lastRecoveryTime != -1) {
            if (startTime > lastRecoveryTime) {
                VersionedValue latest = var.getLatestVersion();
                // If the latest commit happened BEFORE the recovery, we are stale.
                if (latest.getCommitTime() < lastRecoveryTime) {
                    throw new StaleReadException("Site " + siteId + " recovered but " + variableId + " has not been written yet.");
                }
            }
        }

        final VersionedValue version = var.getVersionFor(startTime);
        if (version == null) {
             // Should not happen if initialized correctly at time 0
            throw new DataManagerException("No version found for " + variableId);
        }
        
        return version.getValue();
    }

    @Override
    public void prepareWrite(String transactionId, String variableId, int value) throws SiteDownException {
        if (!isUp) {
            throw new SiteDownException("Site " + siteId + " is down.");
        }
        
        if (!variables.containsKey(variableId)) {
            return; // Not managed here
        }

        pendingWrites.putIfAbsent(transactionId, new ArrayList<>());
        pendingWrites.get(transactionId).add(new PendingWrite(transactionId, variableId, value));
    }

    @Override
    public void commit(String transactionId, int commitTime) throws SiteDownException {
        if (!isUp) {
            throw new SiteDownException("Site " + siteId + " is down.");
        }

        if (pendingWrites.containsKey(transactionId)) {
            List<PendingWrite> writes = pendingWrites.get(transactionId);
            for (PendingWrite pw : writes) {
                Variable var = variables.get(pw.getVariable());
                if (var != null) {
                    var.addCommittedVersion(new VersionedValue(pw.getValue(), commitTime));
                }
            }
            pendingWrites.remove(transactionId);
        }
    }

    @Override
    public void abort(String transactionId) {
        pendingWrites.remove(transactionId);
    }

    @Override
    public void fail() {
        this.isUp = false;
        this.pendingWrites.clear(); // Volatile memory lost
    }

    @Override
    public void recover(int time) {
        this.isUp = true;
        this.lastRecoveryTime = time;
    }
}
