# MathServer

TCP server that evaluates mathematical expressions sent by clients through a CLI.

Created for CS 4390 - Computer Networks at the University of Texas at Dallas.

---

## How to Compile and Run

### Compile

All source files are in `src/`.  
Compile with the provided Make command:
```bash
  make all
```

### Run the Server
```bash
  make server
  make server PORT=9090
```
- Defaults to port `8080` if none is provided.

### Run a Client
```bash
  java MathClient <host> <port> <clientName> [--demo]
```

- Without `--demo` the client runs in interactive mode — type expressions one at a time, `quit` to exit.  
- With `--demo` the client automatically connects, sends 3 sample expressions at random intervals, then quits.

### Run the Demo

Start the server and several clients in separate terminals, 
```bash
    make server
    make client
    make client2
```

or use the provided Make command to run the server and two clients in the same terminal:
```bash
  make demo
```

### Run Tests
```bash
  java -cp bin ExpressionEvaluatorTest
```

---

## Project Status

### What Works

- Multi-client TCP server with one thread per client
- Strict FIFO ordering across all clients via single worker thread
- Thread-safe logging via synchronized `ServerLogger`
- `JOIN` / `ACK JOIN` handshake with name validation
- `QUIT` / `ACK QUIT` with session duration logging
- `CALC` requests with basic arithmetic evaluation (`+`, `-`, `*`, `/`, `%`)
- Unary negation (e.g. `-3+5`, `2*-3`)
- Parenthesized expressions with correct precedence
- Division and modulus by zero error handling
- Mismatched parenthesis detection
- Client CLI (interactive and demo modes)

### Known Limitations

- No GUI
- No persistent log file (stdout only — though easily redirected with `java MathServer > server.log`)
- Scientific notation not supported (e.g. `1e5`)
- Exponent operators not supported (e.g. `**` or `^`)
- CS 5390 group chat: not applicable