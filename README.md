# Replicated Concurrency Control & Recovery (repcrec)

Lightweight Maven-based Java implementation of Serializable Snapshot Isolation (SSI) with the Available Copies replication algorithm.

## Key Components

**Core Classes:**
- Main application starter: [`com.ads.Main`](repcrec/src/main/java/com/ads/Main.java)
- Simulator/driver: [`com.ads.Simulator`](repcrec/src/main/java/com/ads/Simulator.java)
- Transaction manager: [`com.ads.TransactionManager`](repcrec/src/main/java/com/ads/TransactionManager.java)
- Transaction manager helper: [`com.ads.helpers.TransactionManagerHelper`](repcrec/src/main/java/com/ads/helpers/TransactionManagerHelper.java)
- Data manager: [`com.ads.DataManager`](repcrec/src/main/java/com/ads/DataManager.java)
- Variable model: [`com.ads.Variable`](repcrec/src/main/java/com/ads/Variable.java)

**Interfaces:**
- Transaction manager interface: [`com.ads.interfaces.ITransactionManager`](repcrec/src/main/java/com/ads/interfaces/ITransactionManager.java)
- Data manager interface: [`com.ads.interfaces.IDataManager`](repcrec/src/main/java/com/ads/interfaces/IDataManager.java)

**Supporting Classes:**
- Site directory: [`com.ads.SiteDirectory`](repcrec/src/main/java/com/ads/SiteDirectory.java)
- Transaction record: [`com.ads.TxRecord`](repcrec/src/main/java/com/ads/TxRecord.java)
- Serialization graph: [`com.ads.SerializationGraph`](repcrec/src/main/java/com/ads/SerializationGraph.java)
- Command parser: [`com.ads.CommandParser`](repcrec/src/main/java/com/ads/CommandParser.java)

**Configuration:**
- Maven project file: [repcrec/pom.xml](repcrec/pom.xml)

## Build & Run

Requirements: JDK 17+, Maven 3+

```bash
# Compile
cd repcrec
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
mvn compile

# Run simulator with input file
java -cp target/classes com.ads.Simulator -f in/1.in

# Run interactive console
java -cp target/classes com.ads.Main
```

## Testing

```bash
cd repcrec

# Run all unit tests
mvn test

# Run specific test file
mvn test -Dtest=RunSampleTests

# Run all integration test cases from files
./run-tests.sh

# Show detailed differences for failures
./run-tests.sh compare -v

# Regenerate expected output after fixes
./run-tests.sh generate
```

**Test Coverage:**
- Unit tests: `repcrec/src/test/java/`
  - `RunSampleTests.java` - Comprehensive test suite (46 tests)
  - `TransactionManagerTest.java` - Transaction manager tests
  - `SimpleTest.java` - Basic functionality tests
- Integration test inputs: `repcrec/in/*.in`
- Expected outputs: `repcrec/out/*.out`

## Project Layout

```
repcrec/
├── pom.xml                          # Maven configuration
├── src/
│   ├── main/java/com/ads/
│   │   ├── Main.java                # Interactive console entry point
│   │   ├── Simulator.java           # File-based simulator driver
│   │   ├── TransactionManager.java  # Transaction coordination
│   │   ├── DataManager.java         # Site-level data management
│   │   ├── Variable.java            # Multiversion variable storage
│   │   ├── TxRecord.java            # Transaction metadata
│   │   ├── SiteDirectory.java       # Site status tracking
│   │   ├── SerializationGraph.java  # SSI cycle detection
│   │   ├── CommandParser.java       # Command parsing
│   │   ├── helpers/
│   │   │   └── TransactionManagerHelper.java  # Utility methods
│   │   └── interfaces/
│   │       ├── ITransactionManager.java       # TM interface
│   │       └── IDataManager.java              # DM interface
│   └── test/java/
│       ├── RunSampleTests.java      # Comprehensive test suite (46 tests)
│       ├── TransactionManagerTest.java
│       └── SimpleTest.java
├── in/                              # Test input files (*.in)
└── out/                             # Expected output files (*.out)
```

## Architecture

**Command Flow:**
1. `Simulator` reads commands from stdin/file
2. `CommandParser` parses text into `Command` objects
3. `Simulator` calls `TransactionManager.execute(command)`
4. `TransactionManager` coordinates with 10 `DataManager` instances
5. `TransactionManagerHelper` provides utility functions

**Key Design Patterns:**
- **Multiversion Concurrency Control (MVCC)**: Variables maintain commit logs
- **Snapshot Isolation**: Transactions read from their start-time snapshot
- **Serializable Snapshot Isolation (SSI)**: Cycle detection with FLOOS theorem
- **Available Copies**: Replication with site failure handling

## Development Notes

**Recent Refactoring (PR#4):**
- Simplified `ITransactionManager` to single `execute(Command)` method
- Extracted helper methods to `TransactionManagerHelper` for better modularity
- Added comprehensive `RunSampleTests.java` with 46 test cases
- Note: 2 test failures (test4, test25) to be addressed in follow-up commits

## License

MIT — see LICENSE