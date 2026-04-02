# =============================================================================
# Makefile for Math Server / Client
# CE/CS 4390 Computer Networks Project – Spring 2026
#
# Targets:
#   make         → compile all Java source files into bin/
#   make server  → compile + run the server on the default port (5000)
#   make client  → compile + run a demo client (Alice) on localhost:5000
#   make client2 → compile + run a second demo client (Bob) on localhost:5000
#   make clean   → remove compiled .class files
# =============================================================================

# Source and output directories
SRC_DIR = src
BIN_DIR = bin

# Java compiler and flags
JAVAC   = javac
JAVA    = java

# Default server port and host (can be overridden on the command line)
PORT    = 5000
HOST    = localhost

# All Java source files (order matters: dependencies before dependents)
SOURCES = $(SRC_DIR)/ServerLogger.java        \
          $(SRC_DIR)/ExpressionEvaluator.java  \
          $(SRC_DIR)/ClientSession.java         \
          $(SRC_DIR)/CalculationRequest.java    \
          $(SRC_DIR)/CalculationWorker.java     \
          $(SRC_DIR)/ClientHandler.java         \
          $(SRC_DIR)/MathServer.java            \
          $(SRC_DIR)/MathClient.java

# Default target: compile everything
.PHONY: all
all: $(BIN_DIR)
	$(JAVAC) -d $(BIN_DIR) $(SOURCES)
	@echo ""
	@echo "Build successful. Run 'make server' to start the server."

# Create the output directory if it doesn't exist
$(BIN_DIR):
	mkdir -p $(BIN_DIR)

# Run the server
.PHONY: server
server: all
	$(JAVA) -cp $(BIN_DIR) MathServer $(PORT)

# Run a first demo client (Alice)
.PHONY: client
client: all
	$(JAVA) -cp $(BIN_DIR) MathClient $(HOST) $(PORT) Alice

# Run a second demo client (Bob) — open in a separate terminal
.PHONY: client2
client2: all
	$(JAVA) -cp $(BIN_DIR) MathClient $(HOST) $(PORT) Bob

# Remove compiled classes
.PHONY: clean
clean:
	rm -rf $(BIN_DIR)
	@echo "Cleaned compiled files."
