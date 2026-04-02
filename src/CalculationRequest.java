/**
 * CalculationRequest
 *
 * An immutable value object placed into the shared FIFO queue by a ClientHandler
 * and consumed by the single CalculationWorker thread.
 *
 * Holding a reference to the ClientSession (rather than a raw PrintWriter) lets
 * the worker check whether the client is still connected before responding, and
 * use the session's thread-safe send() method.
 *
 * Fields:
 *   sequenceNumber - global request counter assigned at enqueue time; used for
 *                    logging and for verifying strict FIFO ordering.
 *   clientName     - display name the client registered with JOIN.
 *   expression     - the raw arithmetic string (e.g. "5+4*(6*7)").
 *   enqueueTime    - System.currentTimeMillis() when the request was queued.
 *   session        - the ClientSession that owns this request.
 */
public class CalculationRequest {

    private final long   sequenceNumber;
    private final String clientName;
    private final String expression;
    private final long   enqueueTime;
    private final ClientSession session;

    public CalculationRequest(long sequenceNumber,
                              String clientName,
                              String expression,
                              ClientSession session) {
        this.sequenceNumber = sequenceNumber;
        this.clientName     = clientName;
        this.expression     = expression;
        this.enqueueTime    = System.currentTimeMillis();
        this.session        = session;
    }

    public long          getSequenceNumber() { return sequenceNumber; }
    public String        getClientName()     { return clientName; }
    public String        getExpression()     { return expression; }
    public long          getEnqueueTime()    { return enqueueTime; }
    public ClientSession getSession()        { return session; }
}
