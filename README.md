# ADS-Final-Project-Fall2025

Replicated Concurrency Control & Recovery System implementing Serializable Snapshot Isolation (SSI).

## Build Instructions

### Quick Start

```bash
# Compile all source files
make compile

# Clean build artifacts
make clean

# Compile and run tests
make test

# Run the simulator with an input file
make run INPUT=input.txt
```

### Directory Structure

```
.
├── src/          # Java source files
├── test/         # Test files
├── bin/          # Compiled .class files (auto-generated, gitignored)
├── Makefile      # Build configuration
└── CLAUDE.md     # Project documentation
```

### Development Workflow

1. Write your Java code in `src/`
2. Run `make compile` to build
3. Add tests in `test/`
4. Run `make test` to compile and verify tests
5. Run `make clean` to remove compiled files

### Manual Compilation (if needed)

```bash
# Compile manually
javac -d bin -sourcepath src src/*.java

# Run manually
java -cp bin Simulator input.txt
```
