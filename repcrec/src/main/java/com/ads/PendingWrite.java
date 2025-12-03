package com.ads;

/**
 * Class representing a pending write operation.
 * @author Tejas Choudhary
 * @version 1.0 (Created: 2025-11-25, Last Modified: 2025-11-26)
 */

public class PendingWrite {
    private final String transactionId;
    private final String variable;
    private final int value;

    public PendingWrite(String transactionId, String variable, int value) {
        this.transactionId = transactionId;
        this.variable = variable;
        this.value = value;
    }

    /**
     * Get the transaction ID associated with this pending write.
     * @return the transaction ID
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Get the variable associated with this pending write.
     * @return the variable
     */
    public String getVariable() {
        return variable;
    }

    /**
     * Get the value associated with this pending write.
     * @return the value
     */
    public int getValue() {
        return value;   
    }
}
