import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;

/**
 * ClientSession
 *
 * Holds all state associated with a single connected client:
 *   - The TCP socket
 *   - Buffered reader / print writer for line-based I/O
 *   - Client name (set after a successful JOIN)
 *   - Connection start time (used to compute session duration on disconnect)
 *   - A flag indicating whether the client is still connected
 *
 * The send() method is synchronized so that the ClientHandler thread (which
 * sends ACK JOIN / ACK QUIT) and the CalculationWorker thread (which sends
 * RESULT / ERROR) never interleave their writes to the same socket.
 */
public class ClientSession {

    private final Socket       socket;
    private final BufferedReader reader;
    private final PrintWriter   writer;
    private final long          connectTimeMillis;   // epoch ms at connect
    private final Instant       connectInstant;      // for human-readable logging

    private String  name;          // set when JOIN is processed
    private boolean connected;     // set to false on QUIT or error

    /**
     * Wraps an accepted Socket in buffered I/O streams.
     *
     * @param socket the accepted client socket
     * @throws IOException if stream creation fails
     */
    public ClientSession(Socket socket) throws IOException {
        this.socket            = socket;
        this.connectTimeMillis = System.currentTimeMillis();
        this.connectInstant    = Instant.now();
        this.connected         = true;

        // autoFlush=true so every println() is sent immediately without
        // having to call flush() manually after each message
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    // -------------------------------------------------------------------------
    // I/O helpers
    // -------------------------------------------------------------------------

    /**
     * Sends one line of text to the client.
     * Synchronized to prevent concurrent writes from interleaving.
     *
     * @param message the protocol message (no newline needed; println adds it)
     */
    public synchronized void send(String message) {
        if (connected) {
            writer.println(message);
        }
    }

    /**
     * Reads one line from the client (blocks until data arrives or socket closes).
     *
     * @return the line, or null if the connection was closed by the remote end
     * @throws IOException on I/O error
     */
    public String readLine() throws IOException {
        return reader.readLine();
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Marks the session as disconnected and closes the underlying socket.
     * Safe to call multiple times.
     */
    public void close() {
        connected = false;
        try {
            socket.close();
        } catch (IOException e) {
            // best-effort close; ignore
        }
    }

    /**
     * Returns how long the client has been connected, in seconds.
     */
    public double sessionDurationSeconds() {
        return (System.currentTimeMillis() - connectTimeMillis) / 1000.0;
    }

    // -------------------------------------------------------------------------
    // Getters / setters
    // -------------------------------------------------------------------------

    public String  getName()          { return name; }
    public void    setName(String n)  { this.name = n; }
    public boolean isConnected()      { return connected; }
    public String  getRemoteAddress() { return socket.getRemoteSocketAddress().toString(); }
    public Instant getConnectInstant(){ return connectInstant; }
}
