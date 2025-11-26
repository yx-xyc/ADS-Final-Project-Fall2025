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
     * Purpose: Find the correct version of the data for a transaction based on its start time.
     * Iterate backwards through the commit log and return the first version it finds with a commit time less than the transaction's start time.
     */
    public VersionedValue getVersionFor(int transactionStartTime) {
        return null; // TODO: Implement me
    }

    // Purpose: Get the absolute latest committed value for dump().
    public VersionedValue getLatestVersion() {
        return null; // TODO: Implement me
    }

    // Purpose: Adds a new committed version to this variable's history
    public void addCommittedVersion(VersionedValue version) {} 
}
