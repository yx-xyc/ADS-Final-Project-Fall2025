package com.ads;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a transaction in the system.
 * Stores metadata, read/write sets, and status.
 * @author Vincent Xu
 * @version 1.0 (Created: 2025-11-27, Last Modified: 2025-11-30)
 */
public class TxRecord {
    public enum Status {
        ACTIVE,
        COMMITTED,
        ABORTED
    }

    private final String txnId;
    private final int startTime;
    private Status status;
    private int commitTime;

    // Map<VariableId, VersionTime> - tracks which version of a variable was read
    private final Map<String, Integer> readSet;

    // Map<VariableId, Value> - buffers writes until commit
    private final Map<String, Integer> writeSet;

    // Set<SiteId> - tracks which sites were accessed (for failure validation)
    private final Set<Integer> sitesAccessed;

    public TxRecord(String txnId, int startTime) {
        this.txnId = txnId;
        this.startTime = startTime;
        this.status = Status.ACTIVE;
        this.readSet = new HashMap<>();
        this.writeSet = new HashMap<>();
        this.sitesAccessed = new HashSet<>();
        this.commitTime = -1;
    }

    public String getTxnId() {
        return txnId;
    }

    public int getStartTime() {
        return startTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(int commitTime) {
        this.commitTime = commitTime;
    }

    public Map<String, Integer> getReadSet() {
        return readSet;
    }

    public Map<String, Integer> getWriteSet() {
        return writeSet;
    }

    public Set<Integer> getSitesAccessed() {
        return sitesAccessed;
    }

    public void addRead(String varId, int versionTime) {
        readSet.put(varId, versionTime);
    }

    public void addWrite(String varId, int value) {
        writeSet.put(varId, value);
    }

    public void addSiteAccess(int siteId) {
        sitesAccessed.add(siteId);
    }
}
