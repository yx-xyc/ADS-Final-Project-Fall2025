package com.ads.exceptions;

/**
 * Custom exception for data manager errors.
 * @author Vincent Xu
 * @version 1.0 (Created: 2025-11-25, Last Modified: 2025-11-26)
 */

public class DataManagerException extends Exception {
    public DataManagerException(String message) {
        super(message);
    }
}
