# Replicated Concurrency Control & Recovery (repcrec)

Lightweight Maven-based Java implementation of Serializable Snapshot Isolation (SSI) with the Available Copies replication algorithm.

Key entry points and references:
- Main application starter: [`com.ads.Main`](repcrec/src/main/java/com/ads/Main.java)
- Simulator/driver: [`com.ads.Simulator`](repcrec/src/main/java/com/ads/Simulator.java)
- Transaction manager interface: [`com.ads.interfaces.ITransactionManager`](repcrec/src/main/java/com/ads/interfaces/ITransactionManager.java)
- Data manager interface: [`com.ads.interfaces.IDataManager`](repcrec/src/main/java/com/ads/interfaces/IDataManager.java)
- Variable model: [`com.ads.Variable`](repcrec/src/main/java/com/ads/Variable.java)
- Maven project file: [repcrec/pom.xml](repcrec/pom.xml)

Requirements
- JDK 21 (configured in [repcrec/pom.xml](repcrec/pom.xml))
- Maven 3+

Build
- Compile:
  mvn -f repcrec/pom.xml compile

- Package:
  mvn -f repcrec/pom.xml package

Test
- Run unit tests:
  mvn -f repcrec/pom.xml test

Run
- Run the simulator or main class from the built classes:
  java -cp repcrec/target/classes com.ads.Simulator
  or
  java -cp repcrec/target/classes com.ads.Main

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

For details on design and responsibilities, see [CLAUDE.md](CLAUDE.md).
