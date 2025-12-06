package com.ads.interfaces;

import com.ads.VersionedValue;

/**
 * Interface for data management operations.
 * @author Vincent Xu
 * @version 1.0 (Created: 2025-11-25, Last Modified: 2025-12-06)
 */

public interface IDataManager {
    /**
     * Read a variable's value at the given snapshot time.
     * Returns the VersionedValue containing both the value and its commit time,
     * which is needed for Available Copies algorithm validation.
     */
    VersionedValue read(String transactionId, String variableId, int startTime) throws Exception;
    void prepareWrite(String transactionId, String variableId, int value) throws Exception;
    void commit(String transactionId, int commitTime) throws Exception;
    void abort(String transactionId);
    void fail();
    void recover(int time);
}
