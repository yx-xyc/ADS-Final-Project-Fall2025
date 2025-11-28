package com.ads.exceptions;

/**
 * Exception indicating that a site is down.
 * @author Vincent Xu
 * @version 1.0 (Created: 2025-11-25, Last Modified: 2025-11-26)
 */
public class SiteDownException extends DataManagerException {
    public SiteDownException(String message) {
        super(message);
    }
}
