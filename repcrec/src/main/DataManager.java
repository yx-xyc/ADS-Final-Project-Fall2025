package com.ads;

import com.ads.interfaces.IDataManager;

/**
 * DataManager is responsible for managing data operations such as reads, writes,
 * commits, aborts, and handling site failures and recoveries.
 * @author Tejas Choudhary
 * @version 1.0 (Created: 2025-11-27, Last Modified: 2025-11-27)
 */
public class DataManager implements IDataManager {

    @Override
    public int read(String transactionId, String variableId, int startTime) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void prepareWrite(String transactionId, String variableId, int value) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void commit(String transactionId, int commitTime) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void abort(String transactionId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void fail() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void recover() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
