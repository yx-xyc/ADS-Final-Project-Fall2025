package com.ads.interfaces;

import com.ads.Command;

/**
 * Interface for transaction management operations.
 * @author Vincent Xu, Tejas Choudhary
 * @version 1.0 (Created: 2025-11-25, Last Modified: 2025-12-04)
 */

public interface ITransactionManager {
    void execute(Command command);
    void incrementLogicalClock();
}
