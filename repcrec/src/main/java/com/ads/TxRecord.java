package com.ads;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a transaction in the system.
 * Stores metadata, read/write sets, and status.
 * @author Vincent Xu
 * @version 1.0 (Created: 2025-11-27, Last Modified: 2025-12-01)
 */
public class TxRecord {
    public enum Status {
        ACTIVE,
        COMMITTED,
        ABORTED
    }

    /**
     * Metadata for a write operation.
     * Tracks when a write was issued and which sites were available at that time.
     * This is essential for Available Copies validation: a transaction must abort
     * if any site it wrote to fails AFTER the write but BEFORE commit.
     */
    public static class WriteMetadata {
        private final int writeTime;
        private final Set<Integer> targetSites;

        public WriteMetadata(int writeTime, Set<Integer> targetSites) {
            this.writeTime = writeTime;
            this.targetSites = new HashSet<>(targetSites);
        }

        public int getWriteTime() {
            return writeTime;
        }

        public Set<Integer> getTargetSites() {
            return targetSites;
        }
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

    // Map<VariableId, WriteMetadata> - tracks when each write was issued and which sites were targeted
    // This is critical for Available Copies validation
    private final Map<String, WriteMetadata> writeTargets;

    public TxRecord(String txnId, int startTime) {
        this.txnId = txnId;
        this.startTime = startTime;
        this.status = Status.ACTIVE;
        this.readSet = new HashMap<>();
        this.writeSet = new HashMap<>();
        this.sitesAccessed = new HashSet<>();
        this.writeTargets = new HashMap<>();
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

    /**
     * Record the target sites for a write operation.
     * @param varId Variable being written
     * @param writeTime Logical clock time when write was issued
     * @param targetSites Sites that were UP when the write was issued
     */
    public void addWriteTarget(String varId, int writeTime, Set<Integer> targetSites) {
        writeTargets.put(varId, new WriteMetadata(writeTime, targetSites));
    }

    public Map<String, WriteMetadata> getWriteTargets() {
        return writeTargets;
    }
}
