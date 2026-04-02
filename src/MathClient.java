import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

/**
 * MathClient  —  entry point for the client application.
 *
 * BEHAVIOR
 *   1. Connect to the server via TCP.
 *   2. Send "JOIN <name>" and wait for "ACK JOIN <name>".
 *   3. Send 3 sample CALC expressions with random delays (500–2000 ms) between them.
 *   4. Print each server response as it arrives.
 *   5. Send "QUIT" and wait for "ACK QUIT".
 *   6. Close the connection and exit.
 *
 * Running two instances simultaneously demonstrates that the server handles
 * concurrent clients and preserves FIFO ordering across them.
 *
 * EXAMPLE
 *   java MathClient localhost 5000 Alice
 *   java MathClient localhost 5000 Bob      ← run in a second terminal
 */
public class MathClient {

    /** Sample expressions sent to the server. Change these freely. */
    private static final String[] SAMPLE_EXPRESSIONS = {
            "2+2",
            "5+4*(6*7)",
            "10/0"          // intentional error to demonstrate error handling
    };

    /** Random delay range between requests (milliseconds). */
    private static final int MIN_DELAY_MS = 500;
    private static final int MAX_DELAY_MS = 2000;

    public static void main(String[] args) {

        // --- Parse arguments ---
        if (args.length < 3) {
            System.err.println("Usage: java MathClient <host> <port> <clientName>");
            System.exit(1);
        }

        String host       = args[0];
        int    port;
        String clientName = args[2];

        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port: " + args[1]);
            System.exit(1);
            return; // compiler needs this after System.exit
        }

        System.out.println("[" + clientName + "] Connecting to " + host + ":" + port + " ...");

        //  Connect 
        try (Socket socket = new Socket(host, port);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {

            // Step 1: JOIN handshake 
            String joinMsg = "JOIN " + clientName;
            System.out.println("[" + clientName + "] Sending: " + joinMsg);
            writer.println(joinMsg);

            // Wait for ACK JOIN
            String ack = reader.readLine();
            if (ack == null) {
                System.err.println("[" + clientName + "] Server closed connection unexpectedly.");
                return;
            }
            System.out.println("[" + clientName + "] Server: " + ack);

            if (!ack.toUpperCase().startsWith("ACK JOIN")) {
                System.err.println("[" + clientName + "] Unexpected response to JOIN: " + ack);
                return;
            }

            System.out.println("[" + clientName + "] Successfully joined. Sending calculation requests...\n");

            //  Step 2: Send CALC requests with random delays
            Random random = new Random();

            for (String expr : SAMPLE_EXPRESSIONS) {
                // Random delay before each request to simulate a real user
                int delay = MIN_DELAY_MS + random.nextInt(MAX_DELAY_MS - MIN_DELAY_MS + 1);
                sleep(delay);

                String calcMsg = "CALC " + expr;
                System.out.println("[" + clientName + "] Sending: " + calcMsg);
                writer.println(calcMsg);

                // Read the server's response for this request
                // Note: responses arrive in the order the server's worker processes them.
                // For a single client, this matches the send order.
                String response = reader.readLine();
                if (response == null) {
                    System.err.println("[" + clientName + "] Server closed connection unexpectedly.");
                    return;
                }
                System.out.println("[" + clientName + "] Server: " + response);
            }

            //  Step 3: QUIT 
            System.out.println("\n[" + clientName + "] Sending: QUIT");
            writer.println("QUIT");

            // Wait for ACK QUIT before closing
            String quitAck = reader.readLine();
            if (quitAck != null) {
                System.out.println("[" + clientName + "] Server: " + quitAck);
            }

            System.out.println("[" + clientName + "] Connection closed. Goodbye!");

        } catch (IOException e) {
            System.err.println("[" + clientName + "] Connection error: " + e.getMessage());
            System.exit(1);
        }
    }

    // Helper


    /** Sleep for the given number of milliseconds, ignoring interrupts. */
    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
