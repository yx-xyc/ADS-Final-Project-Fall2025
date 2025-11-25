# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Replicated Concurrency Control & Recovery system implementing Serializable Snapshot Isolation (SSI) with the Available Copies replication algorithm. The system simulates a distributed database with multiple sites, handling concurrent transactions, site failures, and recovery.

**Team:** Tejas Choudhary (tkc8441) and Vincent Xu (yx2021)

**Language:** Java

## High-Level Architecture

The system is divided into 4 major components:

1. **Transaction Manager (TM)** - Control plane that:
   - Parses textual commands into operations
   - Maintains global site directory and per-transaction state
   - Routes reads/writes to available sites
   - Performs SSI validation using dependency graphs
   - Manages transaction lifecycle (begin, read, write, end)

2. **Data Managers (DM)** - Data plane with one DM per site that:
   - Maintains commit timestamps and version chains for MVCC
   - Stores pending writes in per-transaction buffers
   - Handles site failures and recovery
   - Implements variable-level versioning

3. **Driver/Simulator** - Entry point that:
   - Parses input commands (begin, read, write, end, dump, fail, recover)
   - Maintains logical clock for timestamps
   - Drives the simulation by invoking TM methods

4. **Shared Models** - Common data structures and exceptions

## Core Concepts

### Concurrency Control: Serializable Snapshot Isolation (SSI)

- **Snapshot Reads:** Each transaction gets a start time and reads the newest committed version where commitTime <= startTime
- **Buffered Writes:** Writes are staged in per-transaction buffers; not visible until commit
- **SSI Validation:** At commit time, check for dangerous structures (two consecutive RW edges) in the dependency graph
- **First-Committer-Wins:** Direct WW conflicts cause the second transaction to abort

### Replication: Available Copies Algorithm

- **Read Eligibility:** For replicated variables, only read from sites that:
  - Are currently UP
  - Have been continuously UP since the transaction's start time
  - This prevents reading from sites that missed commits during downtime

- **Single-Copy Variables:** Must read from the unique home site (odd variables on odd sites, even on even)

- **Recovery Semantics:** After recovery, a site is ineligible for replicated reads until it observes a new commit

### Multiversion Concurrency Control (MVCC)

- Each variable maintains a commit-ordered version chain
- Versions are immutable once committed
- Read operations traverse the version chain to find the appropriate snapshot

## Key Classes and Interfaces

### Transaction Manager (Owner: Vincent)

**ITransactionManager Interface:**
- `begin(String txnId)` - Start new transaction
- `read(String txnId, String varId)` - Read variable
- `write(String txnId, String varId, int value)` - Buffer write
- `end(String txnId)` - Commit/abort with validation
- `dump()` - Print all committed state
- `fail(int siteId)` - Simulate site failure
- `recover(int siteId)` - Simulate site recovery

**TransactionManager Class:**
- Maintains `Map<String, TxRecord>` for active transactions
- Holds references to all DataManagers
- Uses `SiteDirectory` to track site status
- Uses `DependencyGraph` for SSI validation
- Uses `ReplicationMap` to determine variable locations

**SiteDirectory Class:**
- Tracks current UP/DOWN status per site
- Maintains `Map<Integer, List<UptimeInterval>>` for uptime history
- `wasContinuouslyUp(siteId, from, to)` validates read eligibility for replicated variables

**DependencyGraph Class:**
- Tracks RW (read-write) and WW (write-write) edges between transactions
- `hasTwoConsecutiveRWEdges(txnId)` detects dangerous structures for SSI
- Edges are removed when transactions commit or abort

**TxRecord Class:**
- Stores transaction metadata: txnId, startTime, status (ACTIVE/WAITING/COMMITTED/ABORTED)
- Maintains readSet (Map<String, ReadInfo>) to track which variables were read and from which versions
- Maintains writeSet (Map<String, Integer>) to buffer uncommitted writes

### Data Manager (Owner: Tejas)

**IDataManager Interface:**
- `read(txnId, varId, startTime)` - Returns versioned value or throws StaleReadException
- `prepareWrite(txnId, varId, value)` - Stage write in pending buffer
- `commit(txnId, commitTime)` - Publish pending writes as new versions
- `abort(txnId)` - Discard pending writes
- `fail()` - Mark site DOWN, discard pending buffers
- `recover()` - Mark site UP, retain committed data

**DataManager Class:**
- Has unique `siteId`
- Tracks `SiteStatus` (UP/DOWN)
- Maintains `Map<String, Variable>` for local variables
- Maintains `Map<String, Map<String, PendingWrite>>` for uncommitted writes (outer key: txnId, inner key: varId)

**Variable Class:**
- Holds `varId` and `isReplicated` flag
- Maintains `List<VersionedValue> commitLog` (ordered by commitTime)
- `getVersionFor(transactionStartTime)` - Binary search for appropriate snapshot
- `addCommittedVersion(version)` - Append new version to commit log

**VersionedValue:**
- `int value` - The data value
- `int commitTime` - When this version was committed

### Exception Hierarchy

**DataManagerException (base):**
- `SiteDownException` - Operation on DOWN site
- `StaleReadException` - Replicated read at ineligible site

**TransactionManagerException (base):**
- `TransactionNotFoundException` - Invalid transaction ID
- `TransactionWaitException` - Must wait for sites
- `TransactionAbortException` - Validation failed

## Command Format

Input commands follow this format:
- `begin(T1)` - Start transaction T1
- `R(T1, x4)` - Transaction T1 reads variable x4
- `W(T1, x6, 88)` - Transaction T1 writes 88 to x6
- `end(T1)` - Commit/abort T1
- `fail(3)` - Site 3 goes down
- `recover(3)` - Site 3 comes back up
- `dump()` - Print all committed values at all sites

## Building and Running

Since the repository is in early stages, typical Java project commands will be:

```bash
# Compile all Java files
javac -d bin $(find . -name "*.java")

# Run the simulator with an input file
java -cp bin Simulator < input.txt

# Or with explicit input file argument
java -cp bin Simulator input.txt
```

## Testing Strategy

Test cases should cover:
- Basic transaction lifecycle (begin, read, write, commit)
- Concurrent transactions with conflicts (WW, RW)
- SSI validation (detect dangerous structures)
- Site failures during transactions
- Recovery and read eligibility rules
- Multi-version reads (older snapshots)
- Replicated vs single-copy variable handling

## Implementation Notes

### Transaction Manager Responsibilities

1. **Command Parsing:** Parse input commands and route to appropriate methods
2. **Timestamp Assignment:** Assign monotonic start times at begin(), commit times at end()
3. **Read Routing:**
   - For replicated variables: find eligible sites (up + continuously up since txn start)
   - For single-copy: route to home site only
   - If no eligible sites, throw TransactionWaitException
4. **Write Buffering:** Call prepareWrite() on all UP replicas that hold the variable
5. **SSI Validation:** At end(), check dependency graph for cycles/dangerous structures
6. **Commit Protocol:** If validation passes, assign commit time and call commit() on all DMs with pending writes
7. **Abort Handling:** On abort, call abort() on all DMs and remove from dependency graph

### Data Manager Responsibilities

1. **Version Management:** Maintain ordered commit log per variable
2. **Read Serving:**
   - Search commit log for newest version with commitTime <= txn startTime
   - Throw StaleReadException if site just recovered and variable is replicated
3. **Write Staging:** Store pending writes in per-transaction map (not visible to other transactions)
4. **Commit Processing:** Move pending writes from staging to commit log with proper commitTime
5. **Failure Handling:** On fail(), discard all pending writes, keep committed versions, reject all operations
6. **Recovery Handling:** On recover(), come back UP but mark as "stale" for replicated reads until first commit

### Dependency Graph Edge Cases

- **RW Edge:** T1 reads variable, T2 writes same variable, T2 commits before T1 → add RW edge T1→T2
- **WW Edge:** T1 and T2 both write same variable → first committer wins, second aborts
- **Dangerous Structure:** If transaction T has path T→Tx→T (two consecutive RW edges), abort T

### Replication Rules

- **Odd Variables (x1, x3, x5, ...):** Only at odd sites (1, 3, 5, 7, 9)
- **Even Variables (x2, x4, x6, ...):** Replicated at all even sites (2, 4, 6, 8, 10)
- **Initial Values:** x1=10, x2=20, ..., x20=200
- **Total Sites:** 10 sites (1-10)

### Site Failure & Recovery Protocol

1. On `fail(siteId)`:
   - Mark site DOWN in SiteDirectory
   - Close current uptime interval
   - DM discards pending writes, retains committed data

2. On `recover(siteId)`:
   - Mark site UP in SiteDirectory
   - Start new uptime interval
   - DM comes back with committed data intact
   - Site is ineligible for replicated reads until next commit

3. Read Eligibility Check:
   - For transaction T starting at time t, reading replicated variable x at site s:
   - Site s must be UP now AND continuously UP from time t until now
   - This prevents reading from sites that were down during T's lifetime

## Owner Assignments

As per the design document:
- **Transaction Manager (Vincent):** All TM classes, validation logic, routing
- **Data Manager (Tejas):** All DM classes, version management, site status
- **Shared Components (Both):** Driver, CommandParser, exceptions, shared models
