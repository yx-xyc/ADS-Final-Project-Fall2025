package com.ads.interfaces;

/**
 * Interface for transaction management operations.
 * @author Vincent Xu
 * @version 1.0 (Created: 2025-11-25, Last Modified: 2025-11-26)
 */

public interface ITransactionManager {
    void begin(String txnId);
    void read(String txnId, String varId);
    void write(String txnId, String varId, int value);
    void end(String txnId);
    void dump();
    void fail(int siteId);
    void recover(int siteId);
}
