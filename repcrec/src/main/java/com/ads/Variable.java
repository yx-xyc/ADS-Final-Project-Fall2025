package com.ads;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a variable in the ADS project.
 * @author Tejas Choudhary
 * @version 1.0 (Created: 2025-11-25, Last Modified: 2025-11-26)
 */

public class Variable {
   private final String id;
    private final boolean isReplicated;
    private final List<VersionedValue> commitLog;

    public Variable(String id, boolean isReplicated) {
        this.id = id;
        this.isReplicated = isReplicated;
        this.commitLog = new ArrayList<>();
    }

    /**
     * Purpose: Find the latest version of the data for a transaction based on its start time.
     * @param transactionStartTime
     * @return VersionedValue corresponding to the transaction's start time.
     */
    public VersionedValue getVersionFor(int transactionStartTime) {
        for (int i = commitLog.size() - 1; i >= 0; i--) {
            final VersionedValue version = commitLog.get(i);
            if (version.getCommitTime() <= transactionStartTime) {
                return version;
            }
        }
        return null; 
    }

    /**
     * Returns the latest committed version of the variable.
     * @return Latest VersionedValue or null if no versions exist.
     */
    public VersionedValue getLatestVersion() {
        if (commitLog.isEmpty()) {
            return null; 
        }
        return commitLog.get(commitLog.size() - 1);
    }

    /**
     * Adds a new committed version to the variable's commit log.
     * @param version
     */
    public void addCommittedVersion(VersionedValue version) {
        commitLog.add(version);
    } 
}
