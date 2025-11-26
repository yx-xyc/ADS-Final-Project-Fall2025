package com.ads;

import com.ads.interfaces.ITransactionManager;

/**
 * Class representing the simulator for the ADS project.
 * @author Tejas Choudhary
 * @version 1.0 (Created: 2025-11-25, Last Modified: 2025-11-26)
 */

public class Simulator {
    private final ITransactionManager tm;

    public Simulator(ITransactionManager tm) {
        this.tm = tm;
    }

    public static void main(String[] args) {
        // TODO: Instantiate your concrete TransactionManager here once implemented
        // ITransactionManager tm = new TransactionManager(); 
        
        // For now, we can't run this without the concrete TM.
        System.out.println("TransactionManager not yet implemented. Please instantiate in Driver.main()");
        
        // Driver driver = new Driver(tm);
        // driver.run(args);
    }

    public void run(String[] args) {
        //TODO: Implement reading from file or stdin
    }
}
