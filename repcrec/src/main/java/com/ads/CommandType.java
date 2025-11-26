package com.ads;

/**
 * Enum representing different command types for transaction processing.
 * 
 * @author Tejas Choudhary
 * @version 1.0 (Created: 2025-11-25, Last Modified: 2025-11-26)
 */

public enum CommandType {
    BEGIN,
    READ,
    WRITE,
    END,
    DUMP,
    FAIL,
    RECOVER
}