# Replicated Concurrency Control & Recovery (repcrec)

**Authors:** Vincent Xu, Tejas Choudhary
**Course:** CSCI-GA.2434 Advanced Database Systems, Fall 2025
**Institution:** New York University

Maven-based Java implementation of Serializable Snapshot Isolation (SSI) with the Available Copies replication algorithm.

**For detailed architecture and design information, please refer to [Design Doc.pdf](Design%20Doc.pdf)**

## Reprozip Package

This project includes a reprozip package for complete reproducibility across different systems and architectures.

### What is Reprozip?

ReproZip captures the complete execution environment including:
- Java 17 JVM and all system libraries
- All compiled `.class` files from `repcrec/target/classes`
- System dependencies and dynamic libraries
- Original file structure and paths

### Package Location

```text
ADS-Final-Project-Fall2025/
└── reprozip_submission/
    └── repcrec.rpz           # 108MB reprozip package
```

Test input files are available in `repcrec/in/` (27 test files: `1.in` through `27.in`).

### Installation

**Prerequisites:** Install the `reprozip` and `reprounzip` tools:

```bash
pip3 install --user reprozip reprounzip
export PATH=$PATH:~/.local/bin
```

---

### Usage Guide

#### Step 1: Initial Setup
Before running any mode, you must unpack the package once.

```bash
# Unpack the package into a directory named 'repcrec_test'
reprounzip directory setup repcrec.rpz repcrec_test
```

#### Step 2: Choose Execution Mode

There are four ways to run the package.

##### Method A: Basic Run (Default Test)
This runs the default captured execution (using test file `1.in`).

```bash
reprounzip directory run repcrec_test
```

To run other tests including the ones inside the package, you first need to identify the internal command path.

1.  **Get the Command Path:** Run with verbose mode to find the full internal command.
    ```bash
    reprounzip -v directory run repcrec_test | grep WARNING
    ```
    *Look for the output: `WARNING: Rewrote command-line as: ...`* like `[REPROUNZIP] 20:13:53.859 WARNING: Rewrote command-line as: /scratch/tkc8441/repcrec_test/root/usr/lib/jvm/java-17-openjdk/bin/java -cp /scratch/tkc8441/repcrec_test/root/home/yx2021/Courses/ADS/ADS-Final-Project-Fall2025/repcrec/target/classes com.ads.Main /scratch/tkc8441/repcrec_test/root/home/yx2021/Courses/ADS/ADS-Final-Project-Fall2025/repcrec/in/1.in`
2. Copy the part after `WARNING: Rewrote command-line as: ...`. In the above case that will be:
    ```bash
   /scratch/tkc8441/repcrec_test/root/usr/lib/jvm/java-17-openjdk/bin/java -cp /scratch/tkc8441/repcrec_test/root/home/yx2021/Courses/ADS/ADS-Final-Project-Fall2025/repcrec/target/classes com.ads.Main /scratch/tkc8441/repcrec_test/root/home/yx2021/Courses/ADS/ADS-Final-Project-Fall2025/repcrec/in/1.in 
    ```

##### Method B: Running with Different Internal Test Files (Files 1-27)
To execute a specific test copy that command and replace the input file number (e.g. '1.in' with '10.in'), and run using `--cmdline`.

*Example (Running Test 10):*
  ```bash
  reprounzip directory run repcrec_test --cmdline \
    /scratch/tkc8441/repcrec_test/root/usr/lib/jvm/java-17-openjdk/bin/java \
    -cp /scratch/tkc8441/repcrec_test/root/home/yx2021/Courses/ADS/ADS-Final-Project-Fall2025/repcrec/target/classes \
    com.ads.Main \
    /scratch/tkc8441/repcrec_test/root/home/yx2021/Courses/ADS/ADS-Final-Project-Fall2025/repcrec/in/10.in
  ```

##### Method C: Interactive Mode
To run the program interactively (typing commands manually), omit the input file argument entirely from the command.

```bash
reprounzip directory run repcrec_test --cmdline \
  /scratch/tkc8441/repcrec_test/root/usr/lib/jvm/java-17-openjdk/bin/java \
  -cp /scratch/tkc8441/repcrec_test/root/home/yx2021/Courses/ADS/ADS-Final-Project-Fall2025/repcrec/target/classes \
  com.ads.Main
```

##### Method D: Custom External Test Case
To run a test file located on your current machine (outside the package), provide the absolute path to your local file after com.ads.Main in the command.

```bash
reprounzip directory run repcrec_test --cmdline \
  /scratch/tkc8441/repcrec_test/root/usr/lib/jvm/java-17-openjdk/bin/java \
  -cp /scratch/tkc8441/repcrec_test/root/home/yx2021/Courses/ADS/ADS-Final-Project-Fall2025/repcrec/target/classes \
  com.ads.Main <ABSOLUTE_PATH_OF_YOUR_LOCAL_TEST_FILE>
```

---

### Expected Output

A successful run regardless of the method used should display:
- Transaction operations (begin, read, write, end)
- Commit/abort decisions with reasons
- Final database state via `dump()` output
- Exit with status code 0

### Cleanup

When finished, remove the unpacked directory to free up space:

```bash
rm -rf repcrec_test
```

## Build & Run

Requirements: JDK 17+, Maven 3+

```bash
# Compile
cd repcrec
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
mvn compile

# Run simulator with input file
java -cp target/classes com.ads.Main < in/1.in

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


### Package Creation (for reference)

The reprozip package was created using:
```bash
cd reprozip_submission

# Trace execution with absolute classpath
reprozip trace -d trace_run \
  java -cp /home/repcrec/target/classes \
  com.ads.Main \
  /home/repcrec/in/1.in

# Manually edit trace_run/config.yml to ensure all .class files are included
# (Add SerializationGraph$EdgeType.class and SiteStatus.class if missing)

# Pack the final .rpz file
reprozip pack -d trace_run repcrec.rpz
```

**Note:** The package includes all 26 compiled `.class` files. Manual config editing was necessary to include inner classes (`SerializationGraph$EdgeType.class`, `SiteDirectory$UptimeInterval.class`) that weren't accessed during the initial trace.

## License

MIT — see LICENSE