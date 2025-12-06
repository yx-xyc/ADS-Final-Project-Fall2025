package com.ads.helpers;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import com.ads.interfaces.IDataManager;
import com.ads.DataManager;
import com.ads.TxRecord;
import com.ads.SerializationGraph;
import com.ads.SiteDirectory;


/**
 * Helper class for TransactionManager 
 * @author Tejas Choudhary
 * @version 1.0 (Created: 2025-12-05, Last Modified: 2025-12-05)
 */
public class TransactionManagerHelper {
    private static final int NUM_SITES = 10;

    /**
     * Method to create real DataManagers for all 10 sites.
     * @return Map of site ID to DataManager instance
     */
    public static Map<Integer, IDataManager> createDataManagers() {
        Map<Integer, IDataManager> managers = new HashMap<>();
        for (int i = 1; i <= NUM_SITES; i++) {
            managers.put(i, new DataManager(i));
        }
        return managers;
    }

    /**
     * Get the set of site IDs that should hold a given variable.
     * - Replicated variables (even IDs): all sites 1-10
     * - Non-replicated variables (odd IDs): site = 1 + (id mod 10)
     * @param varId Variable ID (e.g., "x5")
     * @return Set of site IDs
     */
    public static Set<Integer> getSitesForVariable(String varId) {
        int id = Integer.parseInt(varId.substring(1)); // "x5" -> 5
        Set<Integer> sites = new HashSet<>();

        if (id % 2 == 0) {
            // Replicated: all sites 1-10
            for (int i = 1; i <= NUM_SITES; i++) {
                sites.add(i);
            }
        } else {
            // Non-replicated: site = 1 + (id mod 10)
            sites.add(1 + (id % 10));
        }
        return sites;
    }

    /**
     * Check if a variable is replicated (even ID) or not.
     * @param varId Variable ID (e.g., "x5")
     * @return true if replicated, false otherwise
     */
    public static boolean isReplicated(String varId) {
        int id = Integer.parseInt(varId.substring(1));
        return id % 2 == 0;
    }

    /**
     * Check first-committer-wins rule.
     * Returns false if a concurrent transaction already committed a conflicting write.
     * Two transactions are concurrent if: start(T1) < start(T2) < commit(T1)
     * @param committingTx Transaction about to commit
     * @param transactions Map of all transactions
     * @param logicalClock Current logical clock
     * @return true if validation passes, false if conflict detected
     */
    public static boolean checkFirstCommitterWins(TxRecord committingTx, Map<String, TxRecord> transactions, int logicalClock) {
        for (String varId : committingTx.getWriteSet().keySet()) {
            for (TxRecord other : transactions.values()) {
                if (other.getStatus() != TxRecord.Status.COMMITTED) continue;
                if (!other.getWriteSet().containsKey(varId)) continue;

                // Concurrent if execution intervals overlap:
                // committingTx: [start(committingTx), logicalClock (about to commit)]
                // other: [start(other), commit(other)]
                // Overlap if: start(committingTx) < commit(other) AND start(other) < logicalClock
                boolean concurrent = (committingTx.getStartTime() < other.getCommitTime()) &&
                                   (other.getStartTime() < logicalClock);

                if (concurrent) {
                    return false; // Conflict - other committed first
                }
            }
        }
        return true;
    }

    /**
     * Build serialization graph edges when a transaction is about to commit.
     * Creates WW, WR, and RW edges according to SSI rules.
     * @param committingTx Transaction about to commit
     * @param transactions Map of all transactions
     * @param serializationGraph The serialization graph to update
     * @param logicalClock Current logical clock (commit time)
     */
    public static void buildSerializationGraphEdges(TxRecord committingTx, Map<String, TxRecord> transactions, 
                                                  SerializationGraph serializationGraph, int logicalClock) {
        String txnId = committingTx.getTxnId();
        int commitTime = logicalClock; // Current time is commit time

        for (TxRecord other : transactions.values()) {
            if (other.getTxnId().equals(txnId)) continue;

            // Only consider committed transactions for WW/WR edges
            if (other.getStatus() == TxRecord.Status.COMMITTED) {
                // WW: both write x, commit(other) < start(committingTx)
                for (String varId : committingTx.getWriteSet().keySet()) {
                    if (other.getWriteSet().containsKey(varId) &&
                        other.getCommitTime() < committingTx.getStartTime()) {
                        serializationGraph.addEdge(other.getTxnId(), txnId,
                                                  SerializationGraph.EdgeType.WW);
                    }
                }

                // WR: other writes x, committingTx reads x, commit(other) < start(committingTx)
                for (String varId : committingTx.getReadSet().keySet()) {
                    if (other.getWriteSet().containsKey(varId) &&
                        other.getCommitTime() < committingTx.getStartTime()) {
                        serializationGraph.addEdge(other.getTxnId(), txnId,
                                                  SerializationGraph.EdgeType.WR);
                    }
                }
            }

            // RW: other reads x, committingTx writes x, start(other) < commit(committingTx)
            // Check both committed and active transactions
            if (other.getStatus() == TxRecord.Status.COMMITTED ||
                other.getStatus() == TxRecord.Status.ACTIVE) {
                for (String varId : committingTx.getWriteSet().keySet()) {
                    if (other.getReadSet().containsKey(varId) &&
                        other.getStartTime() < commitTime) {
                        serializationGraph.addEdge(other.getTxnId(), txnId,
                                                  SerializationGraph.EdgeType.RW);
                    }
                }
            }
        }
    }

    /**
     * Remove committed transactions from serialization graph when safe.
     * Conservative approach: only cleanup when no active transactions remain.
     * @param transactions Map of all transactions
     * @param serializationGraph The serialization graph to update
     */
    public static void cleanupSerializationGraph(Map<String, TxRecord> transactions, SerializationGraph serializationGraph) {
        // Check if there are any active transactions
        boolean hasActive = transactions.values().stream()
            .anyMatch(t -> t.getStatus() == TxRecord.Status.ACTIVE);

        if (hasActive) {
            return; // Cannot safely remove - active txns may need these for edge building
        }

        // Safe to remove all committed transactions from graph
        for (TxRecord tx : transactions.values()) {
            if (tx.getStatus() == TxRecord.Status.COMMITTED) {
                serializationGraph.removeTransaction(tx.getTxnId());
            }
        }
    }

    /**
     * Dump the current state of all variables across all sites.
     * @param dataManagers Map of DataManagers
     * @param siteDirectory SiteDirectory instance
     * @param staleVariables Map of stale variables per site
     */
    public static void dump(Map<Integer, IDataManager> dataManagers, SiteDirectory siteDirectory, 
                          Map<Integer, Set<String>> staleVariables) {
        System.out.println("=== Database Dump ===");
        System.out.println();

        // Print non-replicated variables (odd indices)
        System.out.println("Non-Replicated Variables:");
        for (int varId = 1; varId <= 20; varId += 2) {
            String varName = "x" + varId;
            Set<Integer> validSites = getSitesForVariable(varName);

            for (int siteId : validSites) {
                if (!siteDirectory.isUp(siteId)) continue;

                try {
                    IDataManager dm = dataManagers.get(siteId);
                    if (dm == null) continue;

                    int value = dm.read("DUMP", varName, Integer.MAX_VALUE);
                    System.out.printf("  %-4s site%-2d: %d%n", varName, siteId, value);
                } catch (Exception e) {
                    // Variable not available or site error
                }
            }
        }

        System.out.println();
        System.out.println("Replicated Variables:");
        System.out.println("  Var  | Site1 | Site2 | Site3 | Site4 | Site5 | Site6 | Site7 | Site8 | Site9 | Site10");
        System.out.println("  -----|-------|-------|-------|-------|-------|-------|-------|-------|-------|-------");

        // Print replicated variables (even indices) in table format
        for (int varId = 2; varId <= 20; varId += 2) {
            String varName = "x" + varId;
            System.out.printf("  %-4s |", varName);

            Set<Integer> validSites = getSitesForVariable(varName);

            for (int siteId = 1; siteId <= NUM_SITES; siteId++) {
                String cellContent = "  -  ";  // Default: site down or no data

                if (validSites.contains(siteId) && siteDirectory.isUp(siteId)) {
                    try {
                        IDataManager dm = dataManagers.get(siteId);
                        if (dm != null) {
                            int value = dm.read("DUMP", varName, Integer.MAX_VALUE);
                            Set<String> staleVars = staleVariables.get(siteId);
                            boolean stale = staleVars != null && staleVars.contains(varName);
                            cellContent = stale ? String.format("%3d*", value) : String.format(" %3d ", value);
                        }
                    } catch (Exception e) {
                        // Variable not available or site error
                    }
                }

                System.out.printf(" %5s |", cellContent);
            }
            System.out.println();
        }

        System.out.println();
        System.out.println("  Note: * indicates stale (unreadable until new write committed)");
        System.out.println("        - indicates site down or no data");
        System.out.println("====================");
    }
}
