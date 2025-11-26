package com.ads;

/**
 * Class representing a versioned value in the ADS project.
 * @author Tejas Choudhary
 * @version 1.0 (Created: 2025-11-25, Last Modified: 2025-11-26)    
 */

public class VersionedValue {
    private final int value;
    private final int commitTime;

    public VersionedValue(int value, int commitTime) {
        this.value = value;
        this.commitTime = commitTime;
    }

    public int getValue() {
        return value;
    }

    public int getCommitTime() {
        return commitTime;
    }
}
