package com.ads;

public class PendingOperation {
    public final String txnId;
    public final String operation;
    public final String varId;
    public final int timestamp;

    public PendingOperation(String txnId, String operation, String varId, int timestamp) {
        this.txnId = txnId;
        this.operation = operation;
        this.varId = varId;
        this.timestamp = timestamp;
    }
}