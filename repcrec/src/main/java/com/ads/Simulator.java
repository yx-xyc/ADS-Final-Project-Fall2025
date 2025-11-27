package com.ads;

import java.io.FileNotFoundException;
import java.util.Scanner;

import com.ads.interfaces.ITransactionManager;

/**
 * Class representing the simulator for the ADS project.
 * @author Tejas Choudhary
 * @version 1.0 (Created: 2025-11-25, Last Modified: 2025-11-26)
 */

public class Simulator {
    private final ITransactionManager tm;
    private final Scanner scanner;
    private final CommandParser parser;

    public Simulator(ITransactionManager tm) {
        this.tm = tm;
        scanner = new Scanner(System.in);
        parser = new CommandParser();   
    }

    public static void main(String[] args) {
        // Check if file input is provided otherwise take input from stdin
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-f") || args[i].equals("--file") && i + 1 < args.length) {
                try {
                    System.setIn(new java.io.FileInputStream(args[i + 1]));
                } catch (FileNotFoundException e) {
                    System.err.println("File not found: " + args[i + 1]);
                    e.printStackTrace();
                    return;
                }
                break;
            }
        }

        final Simulator simulator = new Simulator(null);
        simulator.run();
        
        // TODO: Instantiate your concrete TransactionManager here once implemented
        // ITransactionManager tm = new TransactionManager(); 
        
        // For now, we can't run this without the concrete TM.
        System.out.println("TransactionManager not yet implemented. Please instantiate in Driver.main()");
        
        // Driver driver = new Driver(tm);
        // driver.run(args);
    }

    /**
     * Runs the simulator, reading commands from input and executing them.
     * @params none
     * @return void
     */
    public void run() {
        while (scanner.hasNextLine()) {
            try {
                String line = scanner.nextLine();
                final Command command = parser.parse(line);
                if (command != null) {
                    // Call TM with this command
                    System.out.println("Executing command: " + command);
                } else {
                    System.out.println("Invalid command: " + line);
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Ignoring invalid argument: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("An unexpected error occurred: " + e.getMessage());
                e.printStackTrace();
                return;
            }
        }
    }
}
