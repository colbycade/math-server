import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ClientHandler
 *
 * Handles the entire lifecycle of one connected client on its own thread.
 *
 * Protocol flow (server-side view):
 *
 *   1. Read the first line; it MUST be "JOIN <name>".
 *      - On success: log the connection and reply "ACK JOIN <name>".
 *      - On failure: send an error and close.
 *
 *   2. Loop, reading lines until the client disconnects or sends QUIT:
 *      - "CALC <expr>"  → create a CalculationRequest and add it to the shared queue.
 *      - "QUIT"         → log the session duration, send "ACK QUIT", close.
 *      - Anything else  → send "ERROR malformed request" and keep reading.
 *
 * The ClientHandler intentionally does NOT evaluate expressions.
 * Evaluation happens in the single CalculationWorker thread (see that class for
 * the reasoning).
 */
public class ClientHandler implements Runnable {

    // Shared queue where CALC requests are deposited for the CalculationWorker
    private final BlockingQueue<CalculationRequest> queue;

    // Global atomic counter so every request gets a unique, monotonically
    // increasing sequence number regardless of which client sent it.
    // AtomicLong.getAndIncrement() is thread-safe without explicit locks.
    private final AtomicLong sequenceCounter;

    // The session wrapper for this specific client
    private final ClientSession session;

    /**
     * @param session         the connected client session
     * @param queue           the shared calculation request queue
     * @param sequenceCounter shared counter for assigning request numbers
     */
    public ClientHandler(ClientSession session,
                         BlockingQueue<CalculationRequest> queue,
                         AtomicLong sequenceCounter) {
        this.session         = session;
        this.queue           = queue;
        this.sequenceCounter = sequenceCounter;
    }

    @Override
    public void run() {
        try {
            // Handshake — expect JOIN <name>
            if (!handleJoin()) {
                return; // invalid handshake; session already closed inside handleJoin
            }

            // Command loop
            handleCommands();

        } catch (IOException e) {
            String name = session.getName() != null ? session.getName() : "<unknown>";
            ServerLogger.log("I/O error for client %s: %s", name, e.getMessage());
        } finally {
            session.close();
        }
    }

    // Handshake

    /**
     * Reads the first line and verifies it is a valid JOIN command.
     *
     * @return true if the handshake succeeded; false if the session should close
     * @throws IOException on socket read error
     */
    private boolean handleJoin() throws IOException {
        String line = session.readLine();

        // Null means the client closed the connection immediately
        if (line == null) {
            ServerLogger.log("Client at %s disconnected before sending JOIN.",
                    session.getRemoteAddress());
            session.close();
            return false;
        }

        line = line.trim();

        // Validate: must start with "JOIN " followed by a non-empty name
        if (!line.toUpperCase().startsWith("JOIN ")) {
            session.send("ERROR First message must be JOIN <name>");
            ServerLogger.log("Rejected client at %s — bad JOIN: \"%s\"",
                    session.getRemoteAddress(), line);
            session.close();
            return false;
        }

        String name = line.substring(5).trim(); // everything after "JOIN "
        if (name.isEmpty()) {
            session.send("ERROR Name cannot be empty");
            session.close();
            return false;
        }

        // Successful join
        session.setName(name);
        session.send("ACK JOIN " + name);

        ServerLogger.log("Client %s connected from %s at %s",
                name,
                session.getRemoteAddress(),
                session.getConnectInstant());

        return true;
    }

    // Command loop

    /**
     * Reads and dispatches protocol messages until the connection ends.
     *
     * @throws IOException on socket read error
     */
    private void handleCommands() throws IOException {
        String name = session.getName();

        while (true) {
            String line = session.readLine();

            // Null means the remote end closed the connection (TCP FIN)
            if (line == null) {
                ServerLogger.log("Client %s disconnected unexpectedly. Duration: %.1f s",
                        name, session.sessionDurationSeconds());
                break;
            }

            line = line.trim();
            if (line.isEmpty()) {
                continue; // ignore blank lines
            }

            // Dispatch based on the command keyword (case-insensitive)
            String upperLine = line.toUpperCase();

            if (upperLine.startsWith("CALC ")) {
                handleCalc(line);

            } else if (upperLine.equals("QUIT")) {
                handleQuit();
                break; // session is now closed; exit the loop

            } else {
                // Unknown or malformed command
                session.send("ERROR Malformed request");
                ServerLogger.log("Malformed message from %s: \"%s\"", name, line);
            }
        }
    }

    // Command handlers

    /**
     * Handles a CALC command by extracting the expression and enqueuing a request.
     *
     * @param line the full raw line, e.g. "CALC 5+4*(6*7)"
     */
    private void handleCalc(String line) {
        // Extract the expression: everything after "CALC " (5 characters)
        String expression = line.substring(5).trim();

        if (expression.isEmpty()) {
            session.send("ERROR Empty expression");
            return;
        }

        // Assign the next global sequence number atomically
        long seq = sequenceCounter.getAndIncrement();

        // Build the request object and drop it into the shared queue.
        // The CalculationWorker will pick it up and process it in FIFO order.
        CalculationRequest req = new CalculationRequest(seq, session.getName(), expression, session);

        try {
            queue.put(req); // put() blocks if queue is full (LinkedBlockingQueue never fills)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            session.send("ERROR Server interrupted");
        }

        // Note: we do NOT log "Request #N from X" here.
        // That log line is emitted by CalculationWorker when it actually processes the request,
        // so the log reflects the actual processing order, not the enqueue order.
    }

    /**
     * Handles a QUIT command: log the disconnect and send ACK QUIT.
     */
    private void handleQuit() {
        String name     = session.getName();
        double duration = session.sessionDurationSeconds();

        session.send("ACK QUIT");
        session.close();

        ServerLogger.log("Client %s disconnected. Connection duration: %.1f seconds",
                name, duration);
    }
}
