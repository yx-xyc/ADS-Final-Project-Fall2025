package com.ads.exceptions;

/**
 * Custom exception for variable not found errors.
 * @author Tejas Choudhary
 * @version 1.0 (Created: 2025-11-25, Last Modified: 2025-11-26)
 */
public class VariableNotFoundException extends DataManagerException{
    public VariableNotFoundException(String message) {
        super(message);
    }
}
