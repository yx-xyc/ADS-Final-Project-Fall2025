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

## Reprozip Package

This project includes a reprozip package for complete reproducibility across different systems and architectures.

### What is Reprozip?

ReproZip captures the complete execution environment including:
- Java 17 JVM and all system libraries
- All compiled `.class` files from `repcrec/target/classes`
- System dependencies and dynamic libraries
- Original file structure and paths

### Package Location

```
ADS-Final-Project-Fall2025/
└── reprozip_submission/
    └── repcrec.rpz           # 108MB reprozip package
```

Test input files are available in `repcrec/in/` (27 test files: 1.in through 27.in).

### Using the Reprozip Package

**Prerequisites:** Install reprozip/reprounzip tools:
```bash
pip3 install --user reprozip reprounzip
export PATH=$PATH:~/.local/bin
```

**Basic Usage (runs with default test):**
```bash
# Unpack the package
reprounzip directory setup repcrec.rpz repcrec_test

# Run the default captured execution (test file 1.in)
reprounzip directory run repcrec_test
```

**Running with Different Test Files:**

The package supports running with any test file from `repcrec/in/`:

```bash
# Setup once
reprounzip directory setup repcrec.rpz repcrec_test

# Run with specific test file (replace XX with test number 1-27)
reprounzip directory run repcrec_test --cmdline \
  /usr/lib/jvm/java-17-openjdk/bin/java \
  -cp /home/yx2021/Courses/ADS/ADS-Final-Project-Fall2025/repcrec/target/classes \
  com.ads.Main \
  /home/yx2021/Courses/ADS/ADS-Final-Project-Fall2025/repcrec/in/XX.in
```

**Example - Running test 10:**
```bash
reprounzip directory run repcrec_test --cmdline \
  /usr/lib/jvm/java-17-openjdk/bin/java \
  -cp /home/yx2021/Courses/ADS/ADS-Final-Project-Fall2025/repcrec/target/classes \
  com.ads.Main \
  /home/yx2021/Courses/ADS/ADS-Final-Project-Fall2025/repcrec/in/10.in
```

**Expected Output:**

A successful run should display:
- Transaction operations (begin, read, write, end)
- Commit/abort decisions with reasons
- Final database state via `dump()` output
- Exit with status code 0

**Cleanup:**
```bash
# Remove unpacked directory when done
rm -rf repcrec_test
```

### Package Creation (for reference)

The reprozip package was created using:
```bash
cd reprozip_submission

# Trace execution with absolute classpath
reprozip trace -d trace_run \
  java -cp /home/yx2021/Courses/ADS/ADS-Final-Project-Fall2025/repcrec/target/classes \
  com.ads.Main \
  /home/yx2021/Courses/ADS/ADS-Final-Project-Fall2025/repcrec/in/1.in

# Manually edit trace_run/config.yml to ensure all .class files are included
# (Add SerializationGraph$EdgeType.class and SiteStatus.class if missing)

# Pack the final .rpz file
reprozip pack -d trace_run repcrec.rpz
```

**Note:** The package includes all 26 compiled `.class` files. Manual config editing was necessary to include inner classes (`SerializationGraph$EdgeType.class`, `SiteDirectory$UptimeInterval.class`) that weren't accessed during the initial trace.

## License

MIT — see LICENSE