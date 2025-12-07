# Replicated Concurrency Control & Recovery (repcrec)

**Authors:** Vincent Xu, Tejas Choudhary
**Course:** CSCI-GA.2434 Advanced Database Systems, Fall 2025
**Institution:** New York University

Maven-based Java implementation of Serializable Snapshot Isolation (SSI) with the Available Copies replication algorithm.

**For detailed architecture and design information, please refer to [Design Doc.pdf](Design%20Doc.pdf)**

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
```

**Test Coverage:**
- Unit tests: `repcrec/src/test/java/`
  - `RunSampleTests.java` - Comprehensive test suite (46 tests)
  - `TransactionManagerTest.java` - Transaction manager tests
  - `SimpleTest.java` - Basic functionality tests

## Documentation

**Javadoc API Documentation:**

The project includes comprehensive Javadoc documentation for all classes and interfaces.

To generate the Javadoc:
```bash
cd repcrec
mvn javadoc:javadoc
```

To view the documentation:
1. Open `javadoc/index.html` in your web browser
2. Or navigate to `javadoc/com/ads/package-summary.html` for the main package overview

The Javadoc covers all core components including:
- Transaction management (`TransactionManager`, `TxRecord`)
- Data management (`DataManager`, `Variable`, `VersionedValue`)
- Serialization graph cycle detection (`SerializationGraph`)
- Site management (`SiteDirectory`, `SiteStatus`)
- Command parsing and execution (`Command`, `CommandParser`)

## Project Structure

```
ADS-Final-Project-Fall2025/
├── Design Doc.pdf               # Detailed architecture and design document
├── README.md                    # This file
├── javadoc/                     # Generated API documentation
└── repcrec/                     # Main project directory
    ├── pom.xml                  # Maven configuration
    └── src/
        ├── main/java/com/ads/   # Source code
        └── test/java/           # Unit tests
```

For detailed component descriptions and architecture information, see **Design Doc.pdf**.

## Submission Package

This project includes reprozip packaging for reproducibility across different architectures. See the reprozip package for the complete executable environment.

## License

MIT — see LICENSE