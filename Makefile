.PHONY: all compile test clean run

# Directories
SRC_DIR = src
TEST_DIR = test
BIN_DIR = bin

# Java compiler
JAVAC = javac
JAVA = java
JAVAC_FLAGS = -d $(BIN_DIR) -sourcepath $(SRC_DIR)

# Find all Java source files
SOURCES = $(wildcard $(SRC_DIR)/*.java)
TEST_SOURCES = $(wildcard $(TEST_DIR)/*.java)

all: compile

# Create bin directory and compile all source files
compile: $(BIN_DIR)
	$(JAVAC) $(JAVAC_FLAGS) $(SOURCES)
	@echo "✓ Compilation successful"

# Compile tests (after compiling main sources)
test: compile
	@if [ -d "$(TEST_DIR)" ] && [ -n "$(TEST_SOURCES)" ]; then \
		$(JAVAC) -cp $(BIN_DIR) -d $(BIN_DIR) $(TEST_SOURCES); \
		echo "✓ Tests compiled"; \
	else \
		echo "No tests found in $(TEST_DIR)"; \
	fi

# Run the simulator with an input file
# Usage: make run INPUT=input.txt
run: compile
	@if [ -z "$(INPUT)" ]; then \
		echo "Usage: make run INPUT=<input_file>"; \
		exit 1; \
	fi
	$(JAVA) -cp $(BIN_DIR) Simulator $(INPUT)

# Clean compiled files
clean:
	rm -rf $(BIN_DIR)
	@echo "✓ Cleaned build directory"

# Create bin directory
$(BIN_DIR):
	mkdir -p $(BIN_DIR)
