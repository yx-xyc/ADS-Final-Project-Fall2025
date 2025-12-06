package com.ads;

import com.ads.interfaces.ITransactionManager;
import com.ads.interfaces.IDataManager;
import java.util.Map;
import java.util.Set;
import java.util.Queue;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Iterator;


/**
 * Concrete implementation of the Transaction Manager.
 * @author Vincent Xu
 * @version 1.0 (Created: 2025-11-27, Last Modified: 2025-12-04)
 */
public class TransactionManager implements ITransactionManager {
    private static final int NUM_SITES = 10;
    private static final int NUM_VARS = 20;

    // Global logical clock
    private int logicalClock;

    // DataManager instances (one per site)
    private final Map<Integer, IDataManager> dataManagers;

    // Site Directory for UP/DOWN status
    private final SiteDirectory siteDirectory;

    // Active Transactions
    private final Map<String, TxRecord> transactions;

    // Stale Variables (Replicated variables at a site that are not yet readable after recovery)
    // Site ID -> Set of Variable IDs
    private final Map<Integer, Set<String>> staleVariables;

    // Blocked Transactions handling
    private final Set<String> blockedTransactions;
    private final Queue<PendingOperation> waitQueue;

    // Serialization Graph for cycle detection
    private final SerializationGraph serializationGraph;

    /**
     * Constructor for TransactionManager.
     * Accepts DataManager instances created externally (by teammate).
     * @param dataManagers Map of site ID to DataManager instance
     */
    public TransactionManager(Map<Integer, IDataManager> dataManagers) {
        this.logicalClock = 0;
        this.dataManagers = dataManagers;
        this.siteDirectory = new SiteDirectory();
        this.transactions = new HashMap<>();
        this.staleVariables = new HashMap<>();
        this.blockedTransactions = new HashSet<>();
        this.waitQueue = new LinkedList<>();
        this.serializationGraph = new SerializationGraph();

        // Initialize stale variables sets for all sites
        for (int i = 1; i <= NUM_SITES; i++) {
            staleVariables.put(i, new HashSet<>());
        }
    }

    /**
     * No-argument constructor for compatibility with Simulator.
     * Creates TransactionManager with StubDataManager instances for testing.
     */
    public TransactionManager() {
        this(createDataManagers());
    }

    /**
     * Factory method to create real DataManagers for all 10 sites.
     * @return Map of site ID to DataManager instance
     */
    private static Map<Integer, IDataManager> createDataManagers() {
        Map<Integer, IDataManager> managers = new HashMap<>();
        for (int i = 1; i <= NUM_SITES; i++) {
            managers.put(i, new DataManager(i));
        }
        return managers;
    }

    // ==================== Helper Methods ====================

    /**
     * Get the set of site IDs that should hold a given variable.
     * - Replicated variables (even IDs): all sites 1-10
     * - Non-replicated variables (odd IDs): site = 1 + (id mod 10)
     * @param varId Variable ID (e.g., "x5")
     * @return Set of site IDs
     */
    private Set<Integer> getSitesForVariable(String varId) {
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
    private boolean isReplicated(String varId) {
        int id = Integer.parseInt(varId.substring(1));
        return id % 2 == 0;
    }

    // ==================== ITransactionManager Interface Methods ====================

    @Override
    public void execute(final Command command) throws UnsupportedOperationException {
        if (command != null) {
            switch (command.getType()) {
                case BEGIN -> begin(command.getArgs()[0]);
                case READ -> read(command.getArgs()[0], command.getArgs()[1]);
                case WRITE -> write(command.getArgs()[0], command.getArgs()[1], Integer.parseInt(command.getArgs()[2]));
                case DUMP -> dump();
                case END -> end(command.getArgs()[0]);
                case FAIL -> fail(Integer.parseInt(command.getArgs()[0]));
                case RECOVER -> recover(Integer.parseInt(command.getArgs()[0]));
                default -> throw new UnsupportedOperationException("Unknown command: " + command.getType());
            }
        } else {
            throw new UnsupportedOperationException("Null command");
        }
    }

    // ==================== TransactionManager Primary Methods ====================

    /**
     * Begin a new transaction.
     * Creates a TxRecord with the current logical clock as the transaction's start time.
     * This start time determines the transaction's snapshot view.
     * @param txnId Transaction ID
     */
    private void begin(String txnId) {
        logicalClock++;
        TxRecord txRecord = new TxRecord(txnId, logicalClock);
        transactions.put(txnId, txRecord);
        System.out.println("Transaction " + txnId + " begins at time " + logicalClock);
    }

    /**
     * Write operation - buffers the write in the transaction's writeSet.
     * Writes are not persisted until commit (Snapshot Isolation).
     * @param txnId Transaction ID
     * @param varId Variable ID
     * @param value Value to write
     */
    private void write(String txnId, String varId, int value) {
        logicalClock++;

        TxRecord tx = transactions.get(txnId);
        if (tx == null || tx.getStatus() != TxRecord.Status.ACTIVE) {
            System.out.println("Transaction " + txnId + " is not active");
            return;
        }

        // Buffer the write
        tx.addWrite(varId, value);

        // Record which sites were UP at write time (critical for Available Copies validation)
        // Must track the write time to properly implement: "if T writes to site s and THEN s fails"
        Set<Integer> targetSites = getSitesForVariable(varId);
        Set<Integer> availableSites = new HashSet<>();
        for (int siteId : targetSites) {
            if (siteDirectory.isUp(siteId)) {
                availableSites.add(siteId);
            }
        }

        // Block if no sites are available for the write
        if (availableSites.isEmpty()) {
            System.out.println(txnId + " waits for " + varId + " (no sites available for write)");
            blockedTransactions.add(txnId);
            waitQueue.add(new PendingOperation(txnId, "WRITE", varId, logicalClock));
            return;
        }

        tx.addWriteTarget(varId, logicalClock, availableSites);

        System.out.println("Transaction " + txnId + " writes " + value + " to " + varId);
    }

    /**
     * Read operation - implements snapshot isolation with own-write check.
     * Transactions always read their own writes first, otherwise read from snapshot.
     * @param txnId Transaction ID
     * @param varId Variable ID
     */
    private void read(String txnId, String varId) {
        logicalClock++;

        TxRecord tx = transactions.get(txnId);
        if (tx == null || tx.getStatus() != TxRecord.Status.ACTIVE) {
            System.out.println("Transaction " + txnId + " is not active");
            return;
        }

        // Check own writes first
        if (tx.getWriteSet().containsKey(varId)) {
            int value = tx.getWriteSet().get(varId);
            System.out.println(varId + ": " + value);
            return;
        }

        // Try reading from available sites
        tryReadFromSites(tx, varId);
    }

    /**
     * Try to read from available sites using Available Copies algorithm.
     * A site is valid for reading if:
     * 1. Site is currently UP
     * 2. If replicated, variable is not stale
     * 3. If replicated, site must have been continuously UP since tx start
     *    (For non-replicated, skip this check - they're authoritative and immediately readable)
     *
     * If no valid site found, block the transaction.
     * @param tx Transaction record
     * @param varId Variable ID
     */
    private void tryReadFromSites(TxRecord tx, String varId) {
        Set<Integer> candidateSites = getSitesForVariable(varId);
        boolean isRepl = isReplicated(varId);

        for (int siteId : candidateSites) {
            // Check 1: Site must be UP
            if (!siteDirectory.isUp(siteId)) continue;

            // Check 2: If replicated, must not be stale
            if (isRepl && staleVariables.get(siteId).contains(varId)) continue;

            // Check 3: If replicated, site must have been continuously UP since tx start
            // For non-replicated variables, skip this check (they're authoritative)
            if (isRepl && !siteDirectory.wasContinuouslyUp(siteId, tx.getStartTime())) continue;

            // Attempt read from DataManager
            try {
                IDataManager dm = dataManagers.get(siteId);
                if (dm == null) continue; // DataManager not initialized yet

                int value = dm.read(tx.getTxnId(), varId, tx.getStartTime());

                // Success!
                tx.addRead(varId, logicalClock); // Record version time
                tx.addSiteAccess(siteId);
                System.out.println(varId + ": " + value);
                return;

            } catch (Exception e) {
                // Site failed or stale, try next
                continue;
            }
        }

        // No valid site found - block transaction
        System.out.println(tx.getTxnId() + " waits for " + varId);
        blockedTransactions.add(tx.getTxnId());

        // Only add to wait queue if not already waiting for this operation
        boolean alreadyWaiting = waitQueue.stream()
            .anyMatch(op -> op.txnId.equals(tx.getTxnId()) &&
                           op.operation.equals("READ") &&
                           op.varId.equals(varId));

        if (!alreadyWaiting) {
            waitQueue.add(new PendingOperation(tx.getTxnId(), "READ", varId, logicalClock));
        }
    }

    /**
     * Abort a transaction and cleanup its state.
     * Notifies DataManagers to discard any buffered writes.
     * @param txnId Transaction ID
     * @param reason Reason for abort
     */
    private void abortTransaction(String txnId, String reason) {
        TxRecord tx = transactions.get(txnId);
        if (tx == null) return;

        tx.setStatus(TxRecord.Status.ABORTED);
        blockedTransactions.remove(txnId);
        waitQueue.removeIf(op -> op.txnId.equals(txnId));

        // Notify DataManagers to abort
        for (int siteId : tx.getSitesAccessed()) {
            try {
                IDataManager dm = dataManagers.get(siteId);
                if (dm != null) {
                    dm.abort(txnId);
                }
            } catch (Exception e) {
                // Ignore errors during abort
            }
        }

        System.out.println(txnId + " aborts");
    }

    /**
     * Mark a site as failed.
     * Aborts all active transactions that have accessed this site (Available Copies requirement).
     * @param siteId Site ID
     */
    private void fail(int siteId) {
        logicalClock++;

        // Mark site down
        siteDirectory.fail(siteId, logicalClock);

        // Notify DataManager
        try {
            IDataManager dm = dataManagers.get(siteId);
            if (dm != null) {
                dm.fail();
            }
        } catch (Exception e) {
            // Ignore
        }

        // Abort transactions that accessed this site
        List<String> toAbort = new ArrayList<>();
        for (TxRecord tx : transactions.values()) {
            if (tx.getStatus() == TxRecord.Status.ACTIVE &&
                tx.getSitesAccessed().contains(siteId)) {
                toAbort.add(tx.getTxnId());
            }
        }

        for (String txnId : toAbort) {
            abortTransaction(txnId, "Site " + siteId + " failed");
        }

        System.out.println("Site " + siteId + " fails");
    }

    /**
     * Recover a failed site.
     * Marks all replicated variables as stale until new writes commit.
     * Non-replicated variables remain immediately readable (authoritative copy).
     * @param siteId Site ID
     */
    private void recover(int siteId) {
        logicalClock++;

        // Mark site UP
        siteDirectory.recover(siteId, logicalClock);

        // Notify DataManager with recovery time
        try {
            IDataManager dm = dataManagers.get(siteId);
            if (dm != null) {
                dm.recover(logicalClock);
            }
        } catch (Exception e) {
            // Ignore
        }

        // Mark all replicated variables as stale
        Set<String> staleVars = new HashSet<>();
        for (int varId = 2; varId <= NUM_VARS; varId += 2) {
            staleVars.add("x" + varId);
        }
        staleVariables.put(siteId, staleVars);

        // Retry blocked transactions
        processWaitQueue();

        System.out.println("Site " + siteId + " recovers");
    }

    /**
     * Process the wait queue to retry blocked read operations.
     * Called after site recovery or transaction commit.
     */
    private void processWaitQueue() {
        Iterator<PendingOperation> iter = waitQueue.iterator();

        while (iter.hasNext()) {
            PendingOperation op = iter.next();
            TxRecord tx = transactions.get(op.txnId);

            // Skip if transaction no longer active
            if (tx == null || tx.getStatus() != TxRecord.Status.ACTIVE) {
                iter.remove();
                blockedTransactions.remove(op.txnId);
                continue;
            }

            // Retry read
            if (op.operation.equals("READ")) {
                blockedTransactions.remove(op.txnId);

                tryReadFromSites(tx, op.varId);

                // If no longer blocked, remove from queue
                if (!blockedTransactions.contains(op.txnId)) {
                    iter.remove();
                }
            }
            // Retry write
            else if (op.operation.equals("WRITE")) {
                blockedTransactions.remove(op.txnId);

                // Re-check if sites are now available
                Set<Integer> targetSites = getSitesForVariable(op.varId);
                Set<Integer> availableSites = new HashSet<>();
                for (int siteId : targetSites) {
                    if (siteDirectory.isUp(siteId)) {
                        availableSites.add(siteId);
                    }
                }

                // If sites are now available, complete the write
                if (!availableSites.isEmpty()) {
                    tx.addWriteTarget(op.varId, logicalClock, availableSites);
                    Integer value = tx.getWriteSet().get(op.varId);
                    System.out.println("Transaction " + tx.getTxnId() + " writes " + value + " to " + op.varId);
                    iter.remove();
                } else {
                    // Still no sites available, keep waiting
                    blockedTransactions.add(op.txnId);
                }
            }
        }
    }

    /**
     * Build serialization graph edges when a transaction is about to commit.
     * Creates WW, WR, and RW edges according to SSI rules:
     * - WW: both write x, commit(T1) < start(T2)
     * - WR: T1 writes x, T2 reads x, commit(T1) < start(T2)
     * - RW: T1 reads x, T2 writes x, start(T1) < commit(T2)
     * @param committingTx Transaction about to commit
     */
    private void buildSerializationGraphEdges(TxRecord committingTx) {
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
     * Check first-committer-wins rule.
     * Returns false if a concurrent transaction already committed a conflicting write.
     * Two transactions are concurrent if: start(T1) < start(T2) < commit(T1)
     * @param committingTx Transaction about to commit
     * @return true if validation passes, false if conflict detected
     */
    private boolean checkFirstCommitterWins(TxRecord committingTx) {
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
     * Commit a transaction - applies writes to DataManagers.
     * Clears stale flags for written replicated variables.
     * @param tx Transaction to commit
     */
    private void commitTransaction(TxRecord tx) {
        tx.setCommitTime(logicalClock);
        tx.setStatus(TxRecord.Status.COMMITTED);

        Set<Integer> sitesWithPreparedWrites = new HashSet<>();
        // Prepare writes at all relevant DataManagers
        // Use writeTargets to write only to sites that were UP when write was issued
        for (Map.Entry<String, Integer> write : tx.getWriteSet().entrySet()) {
            String varId = write.getKey();
            int value = write.getValue();

            // Get the sites that were available when this write was issued
            TxRecord.WriteMetadata metadata = tx.getWriteTargets().get(varId);
            if (metadata == null) {
                // This should never happen - writeSet and writeTargets must be in sync
                // If metadata is missing, abort rather than violating Available Copies
                tx.setStatus(TxRecord.Status.ACTIVE);
                abortTransaction(tx.getTxnId(), "Missing write metadata for " + varId);
                return;
            }
            Set<Integer> targetSites = metadata.getTargetSites();

            for (int siteId : targetSites) {
                if (!siteDirectory.isUp(siteId)) continue;  // Site must still be UP now

                try {
                    IDataManager dm = dataManagers.get(siteId);
                    if (dm != null) {
                        dm.prepareWrite(tx.getTxnId(), varId, value);
                        sitesWithPreparedWrites.add(siteId);
                        tx.addSiteAccess(siteId);
                    }
                } catch (Exception e) {
                    // Log but continue
                }
            }
        }

        // Validate that at least one site successfully prepared the writes
        if (sitesWithPreparedWrites.isEmpty() && !tx.getWriteSet().isEmpty()) {
            // Revert status since we're aborting
            tx.setStatus(TxRecord.Status.ACTIVE);
            abortTransaction(tx.getTxnId(), "No sites available to persist writes");
            return;
        }

        // Commit at all accessed sites
        for (int siteId : sitesWithPreparedWrites) {
            try {
                IDataManager dm = dataManagers.get(siteId);
                if (dm != null) {
                    dm.commit(tx.getTxnId(), logicalClock);
                }
            } catch (Exception e) {
                // Log but continue
            }
        }

        // Clear stale flags only for UP sites that received the write
        for (String varId : tx.getWriteSet().keySet()) {
            if (isReplicated(varId)) {
                for (int siteId : sitesWithPreparedWrites) {
                    staleVariables.get(siteId).remove(varId);
                }
            }
        }

        // Retry blocked transactions
        processWaitQueue();

        System.out.println(tx.getTxnId() + " commits");
    }

    /**
     * End (commit) a transaction.
     * Performs full validation:
     * 1. Read-only optimization (skip validation)
     * 2. First-committer-wins check
     * 3. Serialization graph cycle detection
     * @param txnId Transaction ID
     */
    private void end(String txnId) {
        logicalClock++;

        TxRecord tx = transactions.get(txnId);
        if (tx == null || tx.getStatus() != TxRecord.Status.ACTIVE) {
            System.out.println("Transaction " + txnId + " cannot commit");
            return;
        }

        // Check if transaction is blocked on pending operations
        if (blockedTransactions.contains(txnId)) {
            System.out.println(txnId + " aborts");
            abortTransaction(txnId, "Blocked on pending operations");
            return;
        }

        // Read-only optimization
        if (tx.getWriteSet().isEmpty()) {
            tx.setStatus(TxRecord.Status.COMMITTED);
            tx.setCommitTime(logicalClock);
            System.out.println(txnId + " commits");
            return;
        }

        // VALIDATION: Available Copies - check if write target sites failed AFTER the write
        // Spec: "if T writes to a site s and THEN s fails before T commits, then T should abort"
        // Key: Check from WRITE time, not transaction start time
        for (Map.Entry<String, TxRecord.WriteMetadata> entry : tx.getWriteTargets().entrySet()) {
            TxRecord.WriteMetadata metadata = entry.getValue();
            int writeTime = metadata.getWriteTime();
            Set<Integer> targetedSites = metadata.getTargetSites();

            for (int siteId : targetedSites) {
                // Check if this site was continuously up from WRITE time until now
                if (!siteDirectory.wasContinuouslyUp(siteId, writeTime)) {
                    abortTransaction(txnId, "Site " + siteId + " failed (Available Copies)");
                    return;
                }
            }
        }

        // Validation: First-committer-wins
        if (!checkFirstCommitterWins(tx)) {
            abortTransaction(txnId, "Write-write conflict (first-committer-wins)");
            return;
        }

        // Validation: Build graph edges and check cycles
        buildSerializationGraphEdges(tx);

        if (serializationGraph.hasCycleWithTwoConsecutiveRW()) {
            abortTransaction(txnId, "Cycle with consecutive RW edges");
            serializationGraph.removeTransaction(txnId);
            return;
        }

        // Commit phase
        commitTransaction(tx);

        // Cleanup: Remove old transactions from graph when safe
        cleanupSerializationGraph();
    }

    /**
     * Remove committed transactions from serialization graph when safe.
     * Conservative approach: only cleanup when no active transactions remain.
     *
     * This ensures no future committing transaction will need to create edges
     * to/from the removed transactions.
     */
    private void cleanupSerializationGraph() {
        // Check if there are any active transactions
        boolean hasActive = transactions.values().stream()
            .anyMatch(t -> t.getStatus() == TxRecord.Status.ACTIVE);

        if (hasActive) {
            return; // Cannot safely remove - active txns may need these for edge building
        }

        // Safe to remove all committed transactions from graph
        // (Keep them in transactions map for history/debugging)
        for (TxRecord tx : transactions.values()) {
            if (tx.getStatus() == TxRecord.Status.COMMITTED) {
                serializationGraph.removeTransaction(tx.getTxnId());
            }
        }
    }

    /**
     * Dump the current state of all variables across all sites.
     * Shows committed values at each site, marking stale replicated variables.
     */
    private void dump() {
        System.out.println("=== Database Dump ===");
        System.out.println();

        // Print non-replicated variables (odd indices)
        System.out.println("Non-Replicated Variables:");
        for (int varId = 1; varId <= NUM_VARS; varId += 2) {
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
        for (int varId = 2; varId <= NUM_VARS; varId += 2) {
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
