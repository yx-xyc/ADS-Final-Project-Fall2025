package com.ads.exceptions;

/**
 * Exception indicating a stale read error.
 * @author Tejas Choudhary
 * @version 1.0 (Created: 2025-11-27, Last Modified: 2025-11-27)
 */
public class StaleReadException extends DataManagerException {
    public StaleReadException(String message) {
        super(message);
    }
}
