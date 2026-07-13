package PTI.Rs232Validator.BillValidators;

import PTI.Rs232Validator.CorrectableComponent;
import PTI.Rs232Validator.Loggers.ILogger;
import PTI.Rs232Validator.Messages.Commands.ExtendedCommand;
import PTI.Rs232Validator.Messages.Commands.TelemetryCommand;
import PTI.Rs232Validator.Messages.Requests.*;
import PTI.Rs232Validator.Messages.Responses.Extended.BarcodeDetectedResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Extended.ExtendedResponseMessage;
import PTI.Rs232Validator.Messages.Responses.PollResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Rs232ResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Telemetry.*;
import PTI.Rs232Validator.Messages.Rs232MessageType;
import PTI.Rs232Validator.*;
import PTI.Rs232Validator.SerialProviders.ISerialProvider;
import PTI.Rs232Validator.Utility.ByteUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

/**
* A hardware connection to a bill acceptor.
*/
public class BillValidator implements AutoCloseable {

    private static final byte SuccessfulPollsRequiredToStartPollingLoop = 2;
    private static final byte MaxReadAttempts = 4;
    private static final byte MaxIncorrectPayloadPardons = 2;
    private static final long BackoffIncrement = 50L;
    private static final long StopLoopTimeout = 3_000L;


    private static ILogger _logger = null;

    /**
     * An instance of ISerialProvider that handles port communication.
     */
    private final ISerialProvider _serialProvider;
    /**
     * The configuration for communicating with an RS-232 bill acceptor
     */
    private final Rs232Configuration Configuration;

    private static final Queue<Supplier<Boolean>> _messageCallbacks = new LinkedList<Supplier<Boolean>>();
    private static final Queue<CompletableFuture> _futureCallbacks = new LinkedList<>();
    private Supplier<Boolean> _lastMessageCallback;

    private final BlockingQueue<PendingMessage<?>> messageQueue = new LinkedBlockingQueue<>();
    private final CopyOnWriteArrayList<ValidatorEvent> listeners = new CopyOnWriteArrayList<>();

    private final AtomicBoolean _isPolling = new AtomicBoolean(false);
    private final AtomicBoolean _closed = new AtomicBoolean(false);

    private final ExecutorService commandExecutor = Executors.newSingleThreadExecutor(createThreadFactory("BillValidator-CommandWorker"));
    private ExecutorService pollingExecutor;
    private PendingMessage<?> retryMessage;
    private boolean retryPoll;

    private volatile boolean _lastAck;
    private volatile Rs232State _state;
    private volatile boolean IsConnectionPresent;
    private volatile boolean _shouldRequestBillStack;
    private volatile boolean _shouldRequestBillReturn;
    private volatile boolean _wasCashboxAttachmentReported;
    private volatile boolean _wasCashboxRemovalReported;
    private volatile boolean _wasEscrowedBillReported;
    private volatile boolean _wasBarcodeDetectedReported;
    private volatile boolean _wasConnectionLostReported;

    /**
    * Initializes a new instance of {@link BillValidator} with a provided serial connection
    */
    public BillValidator(ILogger logger, ISerialProvider serialProvider, Rs232Configuration configuration) {
        _logger = Objects.requireNonNull(logger);
        Configuration = Objects.requireNonNull(configuration);
        _serialProvider = Objects.requireNonNull(serialProvider);

        _state = Rs232State.None;
    }

    /**
    * An event that is raised when an attempt to communicate with the acceptor is carried out

    public ValidatorEvent OnCommunicationAttempted = new ValidatorEvent(0);

    /**
    * An event that is raised when the state of the acceptor changes

    public ValidatorEvent OnStateChanged = new ValidatorEvent(1);

    /**
    * An event that is raised when 1 or more events are reported by the acceptor

    public ValidatorEvent OnEventReported = new ValidatorEvent(2);

    /**
    * An event that is raised when the cashbox is attached

    public ValidatorEvent OnCashboxAttached = new ValidatorEvent(3);

    /**
    * An event that is raised when the cashbox is removed

    public ValidatorEvent OnCashboxRemoved = new ValidatorEvent(4);

    /**
    * An event that is raised when a bill is stacked

    public ValidatorEvent OnBillStacked = new ValidatorEvent(5);

    /**
    * An event that is raised when a bill is escrowed

    public ValidatorEvent OnBillEscrowed = new ValidatorEvent(6);

    /**
    * An event that is raised when a barcode is detected

    public ValidatorEvent OnBarcodeDetected = new ValidatorEvent(7);

    /**
    * An event that is raised when the connection to the acceptor seems to be lost

    public ValidatorEvent OnConnectionLost = new ValidatorEvent(8);

    /**
     * Is the connection to the acceptor present?
     */

    public Rs232Configuration getConfiguration(){
        return Configuration;
    }

    public boolean isConnectionPresent() {
        return IsConnectionPresent;
    }

    public boolean isPolling() {
        return _isPolling.get();
    }

    public Rs232State getState() {
        return _state;
    }

    public void addListener(ValidatorEvent listener) {
        listeners.addIfAbsent(Objects.requireNonNull(listener));
    }

    public void removeListener(ValidatorEvent listener) {
        listeners.remove(listener);
    }

    private synchronized boolean startPollingLoop() {
        ensureNotClosed();

        if(_isPolling.get()){
            _logger.LogDebug("The polling loop is running, so ignoring the start request.");
            return false;
        }

        if(!checkForDevice()){
            _logger.LogDebug("Failed to communicate with the bill validator");
            IsConnectionPresent = false;
            return false;
        }

        retryMessage = null;
        retryPoll = false;

        _isPolling.set(true);

        pollingExecutor = Executors.newSingleThreadExecutor(createThreadFactory("BillValidator-PollWorker"));
        pollingExecutor.execute(this::loopPollMessages);

        return true;
    }

    public CompletableFuture<Boolean> startPollingLoopAsync() {
        return CompletableFuture.supplyAsync(this::startPollingLoop, commandExecutor);
    }

    public synchronized void stopPollingLoop(){
        if(!_isPolling.get()){
            _logger.LogDebug("The polling loop is not running, so ignoring the stop request.");
            return;
        }

        _logger.LogDebug("Stopping the polling loop...");

        if(pollingExecutor != null){
            pollingExecutor.shutdown();
            try {
                boolean stopped = pollingExecutor.awaitTermination(StopLoopTimeout, TimeUnit.MILLISECONDS);

                if(stopped){
                    _logger.LogDebug("Stopped the polling loop.");
                } else{
                    _logger.LogError("Failed to stop the polling loop within the timeout");

                    pollingExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                pollingExecutor.shutdownNow();
                _logger.LogError("Failed to stop the polling loop: " + e.getMessage());
            }

            pollingExecutor = null;
        }

        failAllPendingMessages(new IllegalStateException("The polling loop has been stopped."));

        retryMessage = null;
        retryPoll = false;

        _shouldRequestBillStack = false;
        _shouldRequestBillReturn = false;
        _wasCashboxAttachmentReported = false;
        _wasCashboxRemovalReported = false;
        _wasEscrowedBillReported = false;
        _wasBarcodeDetectedReported = false;
        _wasConnectionLostReported = false;

        IsConnectionPresent = false;
    }

    public void stackBill() {
        if (_state != Rs232State.Escrowed){
            _logger.LogDebug("Cannot stack a bill that is not in escrow");
            return;
        }
        _shouldRequestBillStack = true;
    }

    public void returnBill() {
        if (_state != Rs232State.Escrowed){
            _logger.LogDebug("Cannot return a bill that is not in escrow");
            return;
        }
        _shouldRequestBillReturn = true;
    }

    public <TResponseMessage extends Rs232ResponseMessage> CompletableFuture<TResponseMessage> SendNonPollMessageAsync(
            Function<Boolean, Rs232RequestMessage> createRequestMessage,
            Function<List<Byte>, TResponseMessage> createResponseMessage){

        ensureNotClosed();

        Objects.requireNonNull(createRequestMessage);
        Objects.requireNonNull(createResponseMessage);

        PendingMessage<TResponseMessage> pendingMessage = new PendingMessage<>(createRequestMessage, createResponseMessage);

        if(_isPolling.get()){
            messageQueue.offer(pendingMessage);
            return pendingMessage.future;
        }

        commandExecutor.execute(() -> {
            try{
                if(!checkForDevice()){
                    TResponseMessage emptyResponse = createResponseMessage.apply(new ArrayList<>());
                    pendingMessage.future.complete(emptyResponse);
                    return;
                }

                while(!pendingMessage.future.isDone()){
                    boolean finished = processPendingMessage(pendingMessage);
                    if(finished){
                        break;
                    }

                    sleep(Configuration.PollingPeriod);
                }
            } catch(Throwable throwable){
                pendingMessage.future.completeExceptionally(throwable);
            }
        });

        return pendingMessage.future;
    }

    private <TResponseMessage extends Rs232ResponseMessage> boolean processPendingMessage(PendingMessage<TResponseMessage> pendingMessage) {
        if (pendingMessage.future.isDone() || pendingMessage.future.isCancelled()) {
            return true;
        }

        try{
            MessageRetrievalResult<TResponse> messageResult = trySendMessage(pendingMessage.createRequestMessage, pendingMessage.createResponseMessage);

            switch (messageResult.result) {
                case IncorrectAck:
                    return false;

                case IncorrectPayload:
                    int incorrectPayloadCount = pendingMessage.incorrectPayloadCount.incrementAndGet();
                    if(incorrectPayloadCount <= MaxIncorrectPayloadPardons){
                        return false;
                    }

                    logPayloadIssues(messageResult.response);
                    break;

                case Success:
                case Timeout:
                    break;
            }

            pendingMessage.future.complete(messageResult.response);

            return true;
        } catch (Throwable throwable){
            pendingMessage.future.completeExceptionally(throwable);
            return true;
        }
    }

    private boolean processUnknownPendingMessage(PendingMessage<?> pendingMessage){
        return processPendingMessage((PendingMessage) pendingMessage);
    }

    private <TResponseMessage extends Rs232ResponseMessage> MessageRetrievalResult<TResponseMessage> trySendMessage(
            Function<Boolean, Rs232RequestMessage> createRequestMessage,
            Function<List<Byte>, TResponseMessage> createResponseMessage) {

        Rs232RequestMessage requestMessage = createRequestMessage.apply(!_lastAck);

        byte[] requestPayload = ByteUtils.convertListToByteArray(requestMessage.getPayload());

        byte[] responsePayload = new byte[4096];
        byte[] firstBytes = new byte[2];
        int[] actualResponseLength = new int[1];

        long backoffMilliseconds = Configuration.PollingPeriod;

        for (int attempt = 0; attempt < MaxReadAttempts; attempt++){

            _serialProvider.Write(requestPayload);

            _serialProvider.Read(2, firstBytes, actualResponseLength);

            List<Byte> responseList = ByteUtils.convertByteArrayToList(firstBytes);
            if(responseList.isEmpty()){

            }
        }
    }


    @Override
    public void close() throws Exception {

    }

    private enum MessageRetrievalResult {

        Success((byte) 0),
        Timeout((byte) 1),
        IncorrectAck((byte) 2),
        IncorrectPayload((byte) 3);

        private final byte value;

        MessageRetrievalResult(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }

        public static MessageRetrievalResult fromValue(byte value) {
            for (MessageRetrievalResult result : MessageRetrievalResult.values()) {
                if (result.getValue() == value) {
                    return result;
                }
            }
            throw new IllegalArgumentException("No such MessageRetrievalResult");
        }
    }

    /// Send Telemetry Request Functions

    /**
     * Pings the acceptor
     * @return An instance of {@link TelemetryResponseMessage} with {@link TelemetryResponseMessage#IsValid IsValid} set to {@code true} if successful
     * @implNote The work is queued on the thread pool
     */
    public CompletableFuture<TelemetryResponseMessage> PingAsync() {
        return SendTelemetryMessageAsync(TelemetryCommand.Ping, new ArrayList<Byte>(), TelemetryResponseMessage::new);
    }

    /**
     * Gets the serial number assigned to the acceptor
     * @return An instance of {@link GetSerialNumberResponseMessage} with {@link TelemetryResponseMessage#IsValid IsValid} set to {@code true} if successful
     * @implNote The work is queued on the thread pool
     */
    public CompletableFuture<GetSerialNumberResponseMessage> GetSerialNumberAsync() {
        return SendTelemetryMessageAsync(TelemetryCommand.GetSerialNumber, new ArrayList<Byte>(), GetSerialNumberResponseMessage::new);
    }

    /**
     * Gets the telemetry metrics about the cashbox
     * @return An instance of {@link GetCashboxMetricsResponseMessage} with {@link TelemetryResponseMessage#IsValid IsValid} set to {@code true} if successful
     * @implNote The work is queued on the thread pool
     */
    public CompletableFuture<GetCashboxMetricsResponseMessage> GetCashboxMetrics() {
        return SendTelemetryMessageAsync(TelemetryCommand.GetCashboxMetrics, new ArrayList<Byte>(), GetCashboxMetricsResponseMessage::new);
    }

    /**
     * Clears the count of bills in the cashbox
     * @return An instance of {@link TelemetryResponseMessage} with {@link TelemetryResponseMessage#IsValid IsValid} set to {@code true} if successful
     * @implNote The work is queued on the thread pool
     */
    public CompletableFuture<TelemetryResponseMessage> ClearCashboxCount() {
        return SendTelemetryMessageAsync(TelemetryCommand.ClearCashboxCount, new ArrayList<Byte>(), TelemetryResponseMessage::new);
    }

    /**
     * Gets the general telemetry metrics for an acceptor
     * @return An instance of {@link GetUnitMetricsResponseMessage} with {@link TelemetryResponseMessage#IsValid IsValid} set to {@code true} if successful
     * @implNote The work is queued on the thread pool
     */
    public CompletableFuture<GetUnitMetricsResponseMessage> GetUnitMetrics() {
        return SendTelemetryMessageAsync(TelemetryCommand.GetUnitMetrics, new ArrayList<Byte>(), GetUnitMetricsResponseMessage::new);
    }

    /**
     * Gets the telemetry metrics since the last time an acceptor has been serviced
     * @return An instance of {@link GetServiceUsageCountersResponseMessage} with {@link TelemetryResponseMessage#IsValid IsValid} set to {@code true} if successful
     * @implNote The work is queued on the thread pool
     */
    public CompletableFuture<GetServiceUsageCountersResponseMessage> GetServiceUsageCounters() {
        return SendTelemetryMessageAsync(TelemetryCommand.GetServiceUsageCounters, new ArrayList<Byte>(), GetServiceUsageCountersResponseMessage::new);
    }

    /**
     * Gets the flags about what needs to be serviced
     * @return An instance of {@link GetServiceFlagsResponseMessage} with {@link TelemetryResponseMessage#IsValid IsValid} set to {@code true} if successful
     * @implNote The work is queued on the thread pool
     */
    public CompletableFuture<GetServiceFlagsResponseMessage> GetServiceFlags() {
        return SendTelemetryMessageAsync(TelemetryCommand.GetServiceFlags, new ArrayList<Byte>(), GetServiceFlagsResponseMessage::new);
    }

    /**
     * Clears 1 or more service flags
     * @return An instance of {@link TelemetryResponseMessage} with {@link TelemetryResponseMessage#IsValid IsValid} set to {@code true} if successful
     * @implNote The work is queued on the thread pool
     */
    public CompletableFuture<TelemetryResponseMessage> ClearServiceFlags(CorrectableComponent correctableComponent) {
        return SendTelemetryMessageAsync(TelemetryCommand.ClearServiceFlags, new ArrayList<Byte>() {
            {
                add(correctableComponent.getValue());
            }
        }, TelemetryResponseMessage::new);
    }

    /**
     * Gets the info attached to the last service
     * @return An instance of {@link GetServiceInfoResponseMessage} with {@link TelemetryResponseMessage#IsValid IsValid} set to {@code true} if successful
     * @implNote The work is queued on the thread pool
     */
    public CompletableFuture<GetServiceInfoResponseMessage> GetServiceInfo() {
        return SendTelemetryMessageAsync(TelemetryCommand.GetServiceInfo, new ArrayList<Byte>(), GetServiceInfoResponseMessage::new);
    }

    /**
     * Gets the telemetry metrics that pertain to an acceptor's firmware
     * @return An instance of {@link GetFirmwareMetricsResponseMessage} with {@link TelemetryResponseMessage#IsValid IsValid} set to {@code true} if successful
     * @implNote The work is queued on the thread pool
     */
    public CompletableFuture<GetFirmwareMetricsResponseMessage> GetFirmwareMetrics() {
        return SendTelemetryMessageAsync(TelemetryCommand.GetFirmwareMetrics, new ArrayList<Byte>(), GetFirmwareMetricsResponseMessage::new);
    }

    private <TResponseMessage extends TelemetryResponseMessage> CompletableFuture<TResponseMessage> SendTelemetryMessageAsync(TelemetryCommand command,
                                                                                                                             List<Byte> requestData,
                                                                                                                             Function<List<Byte>, TResponseMessage> createResponseMessage) {
        return SendNonPollMessageAsync(ack -> new TelemetryRequestMessage(ack, command, requestData), createResponseMessage);
    }

    /// Send Extended Command functions

    /**
     * Gets the last detected barcode after a power cycle
     * @return An instance of {@link BarcodeDetectedResponseMessage} with {@link ExtendedResponseMessage#IsValid IsValid} set to {@code true} if successful
     * @implNote The work is queued on the thread pool
     */
    public CompletableFuture<BarcodeDetectedResponseMessage> GetDetectedBarcode() {
        return SendExtendedMessageAsync(ExtendedCommand.BarcodeDetected, new ArrayList<Byte>(), BarcodeDetectedResponseMessage::new);
    }

    private <TResponseMessage extends ExtendedResponseMessage> CompletableFuture<TResponseMessage> SendExtendedMessageAsync(ExtendedCommand command,
                                                                                                                           List<Byte> requestData,
                                                                                                                           Function<List<Byte>, TResponseMessage> createResponseMessage) {
        return SendNonPollMessageAsync(ack -> new ExtendedRequestMessage(ack, command, requestData), createResponseMessage);
    }
}
