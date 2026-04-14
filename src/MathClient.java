import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

/**
 * MathClient  —  entry point for the client application.
 *
 * MODES
 *   Interactive (default): user types expressions one at a time, 'quit' to exit.
 *   Demo (--demo flag):    sends 3 hardcoded expressions at random delays then quits.
 *
 * USAGE
 *   java MathClient <host> <port> <clientName>           (interactive)
 *   java MathClient <host> <port> <clientName> --demo    (automated demo)
 *
 * DEMO BEHAVIOR
 *   1. Connect to the server via TCP.
 *   2. Send "JOIN <name>" and wait for "ACK JOIN <name>".
 *   3. Send 3 sample CALC expressions with random delays (500–2000 ms) between them.
 *   4. Print each server response as it arrives.
 *   5. Send "QUIT" and wait for "ACK QUIT".
 *   6. Close the connection and exit.
 */
public class MathClient {

    /** Sample expressions sent to the server in demo mode */
    private static final String[] SAMPLE_EXPRESSIONS = {
            "2+2",
            "5+4*(14%3)-8/2",
            "10/0"  // intentional error to demonstrate error handling
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
        
        String  host       = args[0];
        int     port;
        String  clientName = args[2];
        boolean demoMode   = args.length >= 4 && args[3].equalsIgnoreCase("--demo");

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

            // JOIN handshake
            String joinMsg = "JOIN " + clientName;
            String ack = sendAndReceive(clientName, joinMsg, writer, reader);
            if (ack == null) return; // connection closed
            if (!ack.toUpperCase().startsWith("ACK JOIN")) {
                System.err.println("[" + clientName + "] Unexpected response to JOIN: " + ack);
                return;
            }
            System.out.println("[" + clientName + "] Successfully joined.");

            // Run chosen mode
            if (demoMode) {
                runDemoMode(clientName, writer, reader);
            } else {
                runInteractiveMode(clientName, writer, reader);
            }
        } catch (IOException e) {
            System.err.println("[" + clientName + "] Connection error: " + e.getMessage());
            System.exit(1);
        }
    }
    
    /** Interactive CLI mode: user types expressions, 'quit' to exit */
    private static void runInteractiveMode(String clientName,
                                           PrintWriter writer,
                                           BufferedReader reader) throws IOException {
        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter an expression to calculate, or 'quit' to exit.");
        
        while (true) {
            System.out.print("[" + clientName + "] > ");
            String line = userInput.readLine();
            
            // Check for 'quit' command or if user closed input stream (e.g. Ctrl+D)
            if (line == null || line.trim().equalsIgnoreCase("quit")) {
                sendQuit(clientName, writer, reader);
                break;
            }
            
            // Ignore all whitespace input
            String expr = line.trim();
            if (expr.isEmpty()) continue;
            
            // Send CALC request to server and print response
            String response = sendAndReceive(clientName, "CALC " + expr, writer, reader);
            if (response == null) return; // connection closed
        }
    }
    
     /** ends 3 sample expressions at random delays then quits */
    private static void runDemoMode(String clientName,
                                    PrintWriter writer,
                                    BufferedReader reader) throws IOException {
        System.out.println("[" + clientName + "] Demo mode: sending "
                + SAMPLE_EXPRESSIONS.length + " expressions at random intervals...");
        
        Random random = new Random();
        
        for (String expr : SAMPLE_EXPRESSIONS) {
            // Random delay before each request to simulate a real user
            int delay = MIN_DELAY_MS + random.nextInt(MAX_DELAY_MS - MIN_DELAY_MS + 1);
            sleep(delay);
            
            // Send CALC request
            String calcMsg = "CALC " + expr;
            String response = sendAndReceive(clientName, calcMsg, writer, reader);
            if (response == null) return;
        }
        
        // After sending all sample expressions, send QUIT
        sendQuit(clientName, writer, reader);
    }

    // Helpers
    
        /** Helper method to send a message and print the server's response. */
    private static String sendAndReceive(String clientName,
                                         String message,
                                         PrintWriter writer,
                                         BufferedReader reader) throws IOException {
        // Send message to server
        System.out.println("[" + clientName + "] Sending: " + message);
        writer.println(message);
        
        // Wait for server response
        String response = reader.readLine();
        if (response == null) {
            System.err.println("[" + clientName + "] Server closed connection unexpectedly.");
            return null;
        }
        System.out.println("[" + clientName + "] Server: " + response);
        return response;
    }
    
    /** Helper method to send QUIT command and wait for ACK before closing connection. */
    private static void sendQuit(String clientName,
                                 PrintWriter writer,
                                 BufferedReader reader) throws IOException {
        sendAndReceive(clientName, "QUIT", writer, reader);
        System.out.println("[" + clientName + "] Connection closed. Goodbye!");
    }
    
    /** Sleep for the given number of milliseconds, ignoring interrupts. */
    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
