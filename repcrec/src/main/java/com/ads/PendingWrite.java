package com.ads;

/**
 * Class representing a pending write operation.
 * @author Tejas Choudhary
 * @version 1.0 (Created: 2025-11-25, Last Modified: 2025-11-26)
 */

public class PendingWrite {
    public String transactionId;
    public String variable;
    public int value;

    public PendingWrite(String transactionId, String variable, int value) {
        this.transactionId = transactionId;
        this.variable = variable;
        this.value = value;
    }
}
