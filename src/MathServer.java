import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MathServer  —  entry point for the server application.
 *
 * USAGE
 * -----
 *   java MathServer [port]
 *
 *   port - TCP port to listen on (default: 8080)
 */
public class MathServer {

    /** Default TCP port if none is provided on the command line. */
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {

        //  Parse command-line arguments
        int port = DEFAULT_PORT;
        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port '" + args[0] + "'. Using default: " + DEFAULT_PORT);
                port = DEFAULT_PORT;
            }
        }

        // Shared state 

        // The FIFO queue shared between all ClientHandlers and the CalculationWorker.
        // LinkedBlockingQueue is unbounded and thread-safe
        BlockingQueue<CalculationRequest> requestQueue = new LinkedBlockingQueue<>();

        // A single global counter for assigning unique sequence numbers to each CALC
        // request.  AtomicLong.getAndIncrement() is lock-free and thread-safe, so
        // multiple ClientHandler threads can call it concurrently without races.
        AtomicLong sequenceCounter = new AtomicLong(1);

        //  Start the single CalculationWorker thread
        Thread workerThread = new Thread(new CalculationWorker(requestQueue), "CalculationWorker");
        workerThread.setDaemon(true); // dies automatically when the JVM exits
        workerThread.start();

        //  Open the server socket and accept connections 
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            ServerLogger.log("MathServer listening on port %d", port);
            ServerLogger.log("Waiting for clients...");

            // This loop runs forever (until the process is killed).
            while (true) {
                // accept() blocks until a client connects
                Socket clientSocket = serverSocket.accept();

                // Wrap the raw socket in a ClientSession (sets up buffered I/O)
                ClientSession session;
                try {
                    session = new ClientSession(clientSocket);
                } catch (IOException e) {
                    ServerLogger.log("Failed to create session for incoming connection: %s",
                            e.getMessage());
                    clientSocket.close();
                    continue; // go back to accept()
                }

                // Spawn a dedicated handler thread for this client.
                // Naming the thread after the socket address helps with debugging.
                String threadName = "ClientHandler-" + clientSocket.getRemoteSocketAddress();
                Thread handlerThread = new Thread(
                        new ClientHandler(session, requestQueue, sequenceCounter),
                        threadName);
                handlerThread.setDaemon(true);
                handlerThread.start();

                ServerLogger.log("New connection accepted from %s — handler thread started.",
                        clientSocket.getRemoteSocketAddress());
            }

        } catch (IOException e) {
            ServerLogger.log("Server error: %s", e.getMessage());
            System.exit(1);
        }
    }
}
