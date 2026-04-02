import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ServerLogger
 *
 * logging helper for the Math Server.
 * All server-side output goes through here so the format stays consistent
 * and can later be redirected to a file without touching other classes.
 *
 * Usage:
 *   ServerLogger.log("Client Alice connected from 127.0.0.1:52341");
 */
public class ServerLogger {

    // Timestamp format used in every log line
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Prints a single log line to stdout.
     * Synchronized so that concurrent client threads don't interleave output.
     *
     * @param message the human-readable log message
     */
    public static synchronized void log(String message) {
        String timestamp = LocalDateTime.now().format(FMT);
        System.out.println("[" + timestamp + "] " + message);
    }

    /**
     * Convenience overload that formats a message with printf-style arguments.
     *
     * @param format  printf format string
     * @param args    format arguments
     */
    public static void log(String format, Object... args) {
        log(String.format(format, args));
    }
}
