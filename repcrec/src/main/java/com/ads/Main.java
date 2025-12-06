package com.ads;

import com.ads.interfaces.IDataManager;
import com.ads.interfaces.ITransactionManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Interactive console for manually testing TransactionManager.
 * Allows users to enter transactions and see results in real-time.
 * @author Vincent Xu
 * @version 1.0 (Created: 2025-12-01)
 */
public class Main {
    private final ITransactionManager tm;
    private final CommandParser parser;
    private final Scanner scanner;

    public Main() {
        // Create DataManagers for all 10 sites
        Map<Integer, IDataManager> dataManagers = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            dataManagers.put(i, new DataManager(i));
        }

        this.tm = new TransactionManager(dataManagers);
        this.parser = new CommandParser();
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        printWelcome();

        while (true) {
            System.out.print("\n> ");

            if (!scanner.hasNextLine()) {
                break;
            }

            String line = scanner.nextLine().trim();

            // Handle special commands
            if (line.isEmpty()) {
                tm.incrementLogicalClock();
                continue;
            }

            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                System.out.println("Goodbye!");
                break;
            }

            if (line.equalsIgnoreCase("help")) {
                printHelp();
                continue;
            }

            if (line.equalsIgnoreCase("clear")) {
                clearScreen();
                continue;
            }

            // Parse and execute command
            try {
                Command command = parser.parse(line);
                if (command != null) {
                    tm.execute(command);
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid command: " + e.getMessage());
                System.out.println("Type 'help' for command syntax.");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void printWelcome() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║   RepCRec Interactive Console                                ║");
        System.out.println("║                                                              ║");
        System.out.println("║  Testing Serializable Snapshot Isolation (SSI) with          ║");
        System.out.println("║  Available Copies Replication Algorithm                      ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println("\nType 'help' for commands, 'exit' to quit.");
        System.out.println("\n📊 Initial State:");
        System.out.println("   • 10 sites (all UP)");
        System.out.println("   • 20 variables: x1=10, x2=20, ..., x20=200");
        System.out.println("   • Replicated (even): x2, x4, x6, ..., x20 on all sites");
        System.out.println("   • Non-replicated (odd): x1, x3, x5, ... on specific sites");
    }

    private void printHelp() {
        System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║                    COMMAND REFERENCE                      ║");
        System.out.println("╠═══════════════════════════════════════════════════════════╣");
        System.out.println("║ Transaction Operations:                                   ║");
        System.out.println("║   begin(T1)           - Start transaction T1             ║");
        System.out.println("║   R(T1,x1)            - T1 reads variable x1             ║");
        System.out.println("║   W(T1,x2,100)        - T1 writes 100 to variable x2     ║");
        System.out.println("║   end(T1)             - Commit transaction T1            ║");
        System.out.println("║                                                           ║");
        System.out.println("║ Site Management:                                          ║");
        System.out.println("║   fail(3)             - Fail site 3                      ║");
        System.out.println("║   recover(3)          - Recover site 3                   ║");
        System.out.println("║                                                           ║");
        System.out.println("║ Display:                                                  ║");
        System.out.println("║   dump()              - Show all variable values         ║");
        System.out.println("║                                                           ║");
        System.out.println("║ Console Commands:                                         ║");
        System.out.println("║   help                - Show this help                   ║");
        System.out.println("║   clear               - Clear screen                     ║");
        System.out.println("║   exit/quit           - Exit console                     ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        System.out.println("\n💡 Example Session:");
        System.out.println("   > begin(T1)");
        System.out.println("   > begin(T2)");
        System.out.println("   > W(T1,x1,101)");
        System.out.println("   > W(T2,x1,201)");
        System.out.println("   > end(T1)          // T1 commits");
        System.out.println("   > end(T2)          // T2 aborts (first-committer-wins)");
        System.out.println("   > dump()");
        System.out.println("\n📚 Try these test scenarios:");
        System.out.println("   • Test 1: First-committer-wins (write-write conflict)");
        System.out.println("   • Test 2: Snapshot isolation (reads see old values)");
        System.out.println("   • Test 3: Site failure (transactions abort)");
    }

    private void clearScreen() {
        // ANSI escape code to clear screen
        System.out.print("\033[H\033[2J");
        System.out.flush();
        printWelcome();
    }

    public static void main(String[] args) {
        Main console = new Main();
        if (args != null && args.length > 0) {
            console.runFile(args[0]);
        } else {
            console.run();
        }
    }

    /**
     * Run commands from a file; supports line comments starting with //
     * either on their own line or trailing after a command.
     */
    private void runFile(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            System.out.println("File not found: " + filePath);
            System.out.println("Please provide the absolute path");
            return;
        }

        System.out.println("Running commands from file: " + filePath);
        try {
            for (String rawLine : Files.readAllLines(path)) {
                if (rawLine.isEmpty()) {
                    // Increment time for blank lines
                    tm.incrementLogicalClock();
                    continue;
                }
                String line = stripInlineComment(rawLine).trim();
                if (line.isEmpty()) {
                    // Ignore comment lines
                    continue;
                }
                try {
                    Command command = parser.parse(line);
                    if (command != null) {
                        tm.execute(command);
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid command: " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to read file: " + e.getMessage());
        }
    }

    /**
     * Remove inline // comments; if the entire line is a comment, returns empty.
     */
    private String stripInlineComment(String line) {
        int idx = line.indexOf("//");
        return (idx >= 0) ? line.substring(0, idx) : line;
    }
}
