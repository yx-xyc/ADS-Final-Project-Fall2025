package com.ads;

/**
 * Class representing a versioned value
 * @author Tejas Choudhary
 * @version 1.0 (Created: 2025-11-25, Last Modified: 2025-11-26)    
 */

public class VersionedValue {
    private final int value;
    private final int commitTime;

    /**
     * Constructor for VersionedValue 
     * @param value
     * @param commitTime
     */
    public VersionedValue(int value, int commitTime) {
        this.value = value;
        this.commitTime = commitTime;
    }

    /**
     * Get the value
     * @return the value
     */
    public int getValue() {
        return value;
    }

    /**
     * Get the commit time
     * @return the commit time
     */
    public int getCommitTime() {
        return commitTime;
    }
}
