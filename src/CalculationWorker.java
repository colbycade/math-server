import java.util.concurrent.BlockingQueue;

/**
 * CalculationWorker
 * The assignment requires the server to "respond to clients in order of the
 * requests it gets from different clients."  If each ClientHandler thread
 * evaluated expressions on its own, requests from different clients would be
 * processed in parallel with no guaranteed ordering.
 *
 * The solution is to have every ClientHandler enqueue CalculationRequests into
 * a single shared BlockingQueue (a thread-safe FIFO).  This worker thread is
 * the ONLY thread that dequeues and evaluates requests, so:
 *
 *   - Request #1 (from Alice) is always processed before Request #2 (from Bob)
 *     if Alice's CALC message arrived at the server first.
 *   - No two calculations ever run concurrently.
 *   - FIFO order is maintained across all connected clients.
 *
 * This worker runs as a daemon thread for the lifetime of the server.
 */
public class CalculationWorker implements Runnable {

    // The shared queue that ClientHandler threads write to
    private final BlockingQueue<CalculationRequest> queue;

    /**
     * @param queue the shared, thread-safe FIFO queue
     */
    public CalculationWorker(BlockingQueue<CalculationRequest> queue) {
        this.queue = queue;
    }

    /**
     * Main loop: block until a request is available, evaluate it, send the
     * result (or error) back to the originating client.
     *
     * BlockingQueue.take() blocks without busy-waiting, so this thread uses
     * zero CPU when the queue is empty.
     */
    @Override
    public void run() {
        ServerLogger.log("CalculationWorker started — waiting for requests.");

        while (true) {
            CalculationRequest req;
            try {
                // take() blocks until an item is available
                req = queue.take();
            } catch (InterruptedException e) {
                // The server is shutting down
                Thread.currentThread().interrupt();
                ServerLogger.log("CalculationWorker interrupted — shutting down.");
                break;
            }

            processRequest(req);
        }
    }

    /**
     * Evaluates one request and sends the result or error to the client.
     *
     * @param req the dequeued request
     */
    private void processRequest(CalculationRequest req) {
        long   seq    = req.getSequenceNumber();
        String client = req.getClientName();
        String expr   = req.getExpression();
        ClientSession session = req.getSession();

        ServerLogger.log("Request #%d from %s: CALC %s", seq, client, expr);

        // Guard: client may have disconnected while this request sat in the queue
        if (!session.isConnected()) {
            ServerLogger.log("Request #%d skipped — %s already disconnected.", seq, client);
            return;
        }

        try {
            double result      = ExpressionEvaluator.evaluate(expr);
            String resultStr   = ExpressionEvaluator.formatResult(result);
            String response    = "RESULT " + resultStr;

            session.send(response);
            ServerLogger.log("Request #%d completed for %s with %s", seq, client, response);

        } catch (ExpressionEvaluator.EvaluationException e) {
            String errorMsg = "ERROR " + e.getMessage();
            session.send(errorMsg);
            ServerLogger.log("Request #%d failed for %s with %s", seq, client, errorMsg);
        }
    }
}
