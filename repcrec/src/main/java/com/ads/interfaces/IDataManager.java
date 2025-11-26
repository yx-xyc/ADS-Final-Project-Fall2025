package com.ads.interfaces;

/**
 * Interface for data management operations.
 * @author Vincent Xu
 * @version 1.0 (Created: 2025-11-25, Last Modified: 2025-11-26)
 */

public interface IDataManager {
    int read(String transactionId, String variableId, int startTime) throws Exception;
    void prepareWrite(String transactionId, String variableId, int value) throws Exception;
    void commit(String transactionId, int commitTime) throws Exception;
    void abort(String transactionId);
    void fail();
    void recover();
}
