# Replicated Concurrency Control & Recovery (repcrec)

Lightweight Maven-based Java implementation of Serializable Snapshot Isolation (SSI) with the Available Copies replication algorithm.

Key entry points and references:
- Main application starter: [`com.ads.Main`](repcrec/src/main/java/com/ads/Main.java)
- Simulator/driver: [`com.ads.Simulator`](repcrec/src/main/java/com/ads/Simulator.java)
- Transaction manager interface: [`com.ads.interfaces.ITransactionManager`](repcrec/src/main/java/com/ads/interfaces/ITransactionManager.java)
- Data manager interface: [`com.ads.interfaces.IDataManager`](repcrec/src/main/java/com/ads/interfaces/IDataManager.java)
- Variable model: [`com.ads.Variable`](repcrec/src/main/java/com/ads/Variable.java)
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

# Run all 27 test cases
./run-tests.sh

# Show detailed differences for failures
./run-tests.sh compare -v

# Regenerate expected output after fixes
./run-tests.sh generate
```

Test inputs: `repcrec/in/*.in` | Expected outputs: `repcrec/out/*.out`

Project layout (important files)
- [repcrec/pom.xml](repcrec/pom.xml)
- Source: repcrec/src/main/java/com/ads/
  - [`com.ads.Main`](repcrec/src/main/java/com/ads/Main.java)
  - [`com.ads.Simulator`](repcrec/src/main/java/com/ads/Simulator.java)
  - [`com.ads.Variable`](repcrec/src/main/java/com/ads/Variable.java)
  - Interfaces: repcrec/src/main/java/com/ads/interfaces/
    - [`com.ads.interfaces.ITransactionManager`](repcrec/src/main/java/com/ads/interfaces/ITransactionManager.java)
    - [`com.ads.interfaces.IDataManager`](repcrec/src/main/java/com/ads/interfaces/IDataManager.java)

Development notes
- Implement the concrete TransactionManager and DataManager classes to wire up the simulator.
- The simulator expects a driver that reads textual commands (begin, R/W, end, fail, recover, dump) and uses the transaction/data manager APIs listed above.

License
- MIT — see LICENSE