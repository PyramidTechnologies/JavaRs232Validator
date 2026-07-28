package PTI.Rs232Validator.BillValidators;

import PTI.Rs232Validator.CorrectableComponent;
import PTI.Rs232Validator.EventListener.CommunicationAttemptedEventArgs;
import PTI.Rs232Validator.EventListener.StateChangedEventArgs;
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
import PTI.Rs232Validator.SerialProviders.SerialPort;
import PTI.Rs232Validator.Utility.ByteUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
* A hardware connection to a bill acceptor.
*/
public class BillValidator implements AutoCloseable {

    /**
     * The number of successful polls required to start the polling loop.
     */
    private static final byte SuccessfulPollsRequiredToStartPollingLoop = 2;
    /**
     * The maximum number of attempts to write to and read from the acceptor before giving up.
     */
    private static final byte MaxReadAttempts = 4;
    /**
     * The maximum number of times to allow an incorrect payload before giving up on a message.
     */
    private static final byte MaxIncorrectPayloadPardons = 2;

    /**
     * The serial status code indicating a successful operation.
     */
    private static final int SERIAL_STATUS_SUCCESS = 0x00;
    /**
     * The serial status code indicating that no data was available to read from the acceptor.
     */
    private static final int SERIAL_STATUS_NO_DATA = 0x01;

    /**
     * The amount of time to add to the read timeout after each failed attempt to read from the acceptor.
     */
    private static final long BACKOFF_INCREMENT_MILLISECONDS = 50L;
    /**
     * The amount of time to wait before retrying a read operation after a failed attempt.
     */
    private static final long READ_RETRY_DELAY_MILLISECONDS = 5L;
    /**
     * The amount of time to wait for the polling loop to stop gracefully before forcing a shutdown.
     */
    private static final long STOP_LOOP_TIMEOUT_MILLISECONDS = 3_000L;

    /**
     * An instance of ILogger that handles logging
     */
    private static ILogger _logger = null;

    /**
     * An instance of ISerialProvider that handles port communication.
     */
    private final SerialPort _serialProvider;
    /**
     * The configuration for communicating with an RS-232 bill acceptor
     */
    private final Rs232Configuration Configuration;

    /**
     * A queue of pending messages to be sent to the acceptor.
     */
    private final BlockingQueue<PendingMessage<?>> messageQueue = new LinkedBlockingQueue<>();

    /**
     * Flag indicating whether the polling loop is currently running.
     */
    private final AtomicBoolean _isPolling = new AtomicBoolean(false);
    /**
     * Flag indicating whether the BillValidator has been closed.
     */
    private final AtomicBoolean _closed = new AtomicBoolean(false);

    /**
     * An executor service for handling command execution in a single thread.
     */
    private final ExecutorService commandExecutor = Executors.newSingleThreadExecutor(createThreadFactory("BillValidator-CommandWorker"));
    /**
     * An executor service for handling polling in a single thread.
     */
    private ExecutorService pollingExecutor;
    /**
     * The thread that is currently running the polling loop.
     */
    private volatile Thread pollingThread;
    /**
     * The last message that failed to be sent to the acceptor and needs to be retried.
     */
    private PendingMessage<?> retryMessage;
    /**
     * Flag indicating whether the polling loop should retry sending a poll message to the acceptor.
     */
    private boolean retryPoll;

    /**
     * Flag indicating the ack value of the last message sent to the acceptor.
     */
    private volatile boolean _lastAck;
    /**
     * The current state of the acceptor.
     */
    private volatile Rs232State _state;
    /**
     * Flag indicating whether the connection to the acceptor is present.
     */
    private volatile boolean IsConnectionPresent;

    /**
     * Flag indicating whether a bill stack request should be sent to the acceptor.
     */
    private volatile boolean _shouldRequestBillStack;
    /**
     * Flag indicating whether a bill return request should be sent to the acceptor.
     */
    private volatile boolean _shouldRequestBillReturn;

    /**
     * Flag indicating whether the cashbox attachment event has been reported.
     */
    private volatile boolean _wasCashboxAttachmentReported;
    /**
     * Flag indicating whether the cashbox removal event has been reported.
     */
    private volatile boolean _wasCashboxRemovalReported;
    /**
     * Flag indicating whether a bill escrowed event has been reported.
     */
    private volatile boolean _wasEscrowedBillReported;
    /**
     * Flag indicating whether a barcode detected event has been reported.
     */
    private volatile boolean _wasBarcodeDetectedReported;
    /**
     * Flag indicating whether a connection lost event has been reported.
     */
    private volatile boolean _wasConnectionLostReported;

    /**
     * Initializes a new instance of {@link BillValidator}
     * @param logger An instance of {@link ILogger} that handles logging
     * @param serialProvider An instance of {@link SerialPort} that handles port communication
     * @param configuration The configuration for communicating with an RS-232 bill acceptor
     */
    public BillValidator(ILogger logger, SerialPort serialProvider, Rs232Configuration configuration) {
        _logger = Objects.requireNonNull(logger);
        Configuration = Objects.requireNonNull(configuration);
        _serialProvider = Objects.requireNonNull(serialProvider);
        _serialProvider.logger = _logger;

        _state = Rs232State.None;
    }

    /**
    * An event that is raised when an attempt to communicate with the acceptor is carried out
    */
    public ValidatorEvent OnCommunicationAttempted = new ValidatorEvent(0);

    /**
    * An event that is raised when the state of the acceptor changes
    */
    public ValidatorEvent OnStateChanged = new ValidatorEvent(1);

    /**
    * An event that is raised when 1 or more events are reported by the acceptor
     */
    public ValidatorEvent OnEventReported = new ValidatorEvent(2);

    /**
    * An event that is raised when the cashbox is attached
    */
    public ValidatorEvent OnCashboxAttached = new ValidatorEvent(3);

    /**
    * An event that is raised when the cashbox is removed
     */
    public ValidatorEvent OnCashboxRemoved = new ValidatorEvent(4);

    /**
    * An event that is raised when a bill is stacked
     */
    public ValidatorEvent OnBillStacked = new ValidatorEvent(5);

    /**
    * An event that is raised when a bill is escrowed
     */
    public ValidatorEvent OnBillEscrowed = new ValidatorEvent(6);

    /**
    * An event that is raised when a barcode is detected
     */
    public ValidatorEvent OnBarcodeDetected = new ValidatorEvent(7);

    /**
    * An event that is raised when the connection to the acceptor seems to be lost
     */
    public ValidatorEvent OnConnectionLost = new ValidatorEvent(8);

    /**
     * Gets the configuration for communicating with an RS-232 bill acceptor
     * @return {@link Rs232Configuration}
     */
    public Rs232Configuration getConfiguration(){
        return Configuration;
    }

    /**
     * Gets the flag indicating whether the connection to the acceptor is present
     * @return {@code true} if the connection is present; otherwise, {@code false}
     */
    public boolean isConnectionPresent() {
        return IsConnectionPresent;
    }

    /**
     * Gets the flag indicating whether the polling loop is currently running
     * @return {@code true} if the polling loop is running; otherwise, {@code false}
     */
    public boolean isPolling() {
        return _isPolling.get();
    }

    /**
     * Gets the current state of the acceptor
     * @return {@link Rs232State}
     */
    public Rs232State getState() {
        return _state;
    }


    /**
     * Starts the polling loop to continuously poll the acceptor for messages. For internal use only;
     * use {@link #startPollingLoopAsync()} to start the polling loop asynchronously.
     * @return {@code true} if the polling loop was started; otherwise, {@code false}
     */
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

    /**
     * Starts the polling loop to continuously poll the acceptor for messages asynchronously.
     * @return A {@link CompletableFuture} that completes with {@code true} if the polling loop was started; otherwise, {@code false}
     */
    public CompletableFuture<Boolean> startPollingLoopAsync() {
        return CompletableFuture.supplyAsync(this::startPollingLoop, commandExecutor);
    }

    /**
     * Stops the RS-232 polling loop
     */
    public synchronized void stopPollingLoop() {
        if (!_isPolling.get()) {
            _logger.LogDebug("The polling loop is not running, so ignoring the stop request.");
            return;
        }

        _logger.LogDebug("Stopping the polling loop...");


        _isPolling.set(false);

        ExecutorService executor = pollingExecutor;
        if (executor != null) {

            executor.shutdown();
            try {

                boolean stopped = executor.awaitTermination(STOP_LOOP_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);

                if (stopped) {
                    _logger.LogDebug("Stopped the polling loop.");
                } else {

                    _logger.LogError("Polling loop did not terminate gracefully within the timeout. Forcing shutdown.");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
                _logger.LogError("Interrupted while waiting for the polling loop to stop. Forcing shutdown: %s", e.getMessage());
            } finally {
                pollingExecutor = null;
            }
        }

        // Clean up state after the loop has definitely stopped.
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

    /**
     * Stacks a bill in escrow.
     */
    public void stackBill() {
        synchronized (this) {
            if (_state != Rs232State.Escrowed) {
                _logger.LogDebug("Cannot stack a bill that is not in escrow");
                return;
            }
            _shouldRequestBillStack = true;
        }
    }

    /**
     * Returns a bill in escrow.
     */
    public void returnBill() {
        synchronized (this) {
            if (_state != Rs232State.Escrowed) {
                _logger.LogDebug("Cannot return a bill that is not in escrow");
                return;
            }
            _shouldRequestBillReturn = true;
        }
    }

    /**
     * Sends an instance of {@link Rs232RequestMessage}, which should not be an instance of
     * {@link PollRequestMessage}, to the acceptor and returns an instance of {@link TResponseMessage}
     * created from the response payload.
     */
    public <TResponseMessage extends Rs232ResponseMessage> CompletableFuture<TResponseMessage> SendNonPollMessageAsync(
            Function<Boolean, Rs232RequestMessage> createRequestMessage,
            Function<List<Byte>, TResponseMessage> createResponseMessage){

        //ensureNotClosed();

        Objects.requireNonNull(createRequestMessage);
        Objects.requireNonNull(createResponseMessage);



        PendingMessage<TResponseMessage> pendingMessage = new PendingMessage<>(createRequestMessage, createResponseMessage);

        if(_isPolling.get()){
            enqueuePendingMessage(pendingMessage);
            return pendingMessage.future;
        }

        commandExecutor.execute(() -> {
            synchronized(BillValidator.this) {
                try {

                    if(_closed.get()){
                        pendingMessage.future.completeExceptionally(new IllegalStateException("The BillValidator has been closed."));
                        return;
                    }

                    if(_isPolling.get()){
                        enqueuePendingMessage(pendingMessage);
                        return;
                    }

                    /*if (!checkForDevice()) {
                        TResponseMessage emptyResponse = createResponseMessage.apply(new ArrayList<>());
                        pendingMessage.future.complete(emptyResponse);
                        return;
                    }*/

                    while (!pendingMessage.future.isDone() && !Thread.currentThread().isInterrupted()) {
                        boolean finished = processPendingMessage(pendingMessage);
                        if (finished) {
                            break;
                        }

                        sleep(Configuration.PollingPeriod);
                    }

                    if(Thread.currentThread().isInterrupted() && !pendingMessage.future.isDone()){
                        pendingMessage.future.completeExceptionally(new CancellationException("The standalone message operation was interrupted."));
                    }
                } catch (Throwable throwable) {
                    pendingMessage.future.completeExceptionally(throwable);
                }
            }
        });

        return pendingMessage.future;
    }

    /**
     * Enqueues a pending message to be sent to the acceptor.
     * @param pendingMessage The pending message to enqueue
     */
    private void enqueuePendingMessage(PendingMessage<?> pendingMessage){
        messageQueue.offer(pendingMessage);

        if(!_isPolling.get() && messageQueue.remove(pendingMessage)){
            pendingMessage.future.completeExceptionally(new IllegalStateException("The polling loop stopped before the message could be processed."));
        }
    }

    /**
     * Processes a pending message by sending it to the acceptor and handling the response.
     */
    private <TResponse extends Rs232ResponseMessage>
    boolean processPendingMessage(
            PendingMessage<TResponse> pendingMessage
    ) {
        if (pendingMessage.future.isDone()
                || pendingMessage.future.isCancelled()) {

            return true;
        }

        try {
            MessageResult<TResponse> messageResult =
                    trySendMessage(
                            pendingMessage.requestFactory,
                            pendingMessage.responseFactory
                    );

            switch (messageResult.result) {
                case IncorrectAck:
                    return false;

                case IncorrectPayload:
                    int incorrectPayloadCount =
                            pendingMessage
                                    .incorrectPayloadCount
                                    .incrementAndGet();

                    if (incorrectPayloadCount
                            <= MaxIncorrectPayloadPardons) {

                        return false;
                    }

                    logPayloadIssues(
                            messageResult.response
                    );

                    break;

                case Success:
                case Timeout:
                    break;
            }

            pendingMessage.future.complete(
                    messageResult.response
            );

            return true;

        } catch (Throwable throwable) {
            pendingMessage.future.completeExceptionally(
                    throwable
            );

            return true;
        }
    }

    /*private <TResponseMessage extends Rs232ResponseMessage> boolean pendingMessage(PendingMessage<TResponseMessage> pendingMessage) {
        if (pendingMessage.future.isDone() || pendingMessage.future.isCancelled()) {
            return true;
        }

        try{
            MessageResult<TResponseMessage> messageResult = trySendMessage(pendingMessage.requestFactory, pendingMessage.responseFactory);

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
    }*/

    private boolean processUnknownPendingMessage(PendingMessage<?> pendingMessage){
        return processPendingMessage((PendingMessage) pendingMessage);
    }

    private <TResponseMessage extends Rs232ResponseMessage> MessageResult<TResponseMessage> trySendMessage(
            Function<Boolean, Rs232RequestMessage> createRequestMessage,
            Function<List<Byte>, TResponseMessage> createResponseMessage) {


        Rs232RequestMessage requestMessage = createRequestMessage.apply(!_lastAck);

        byte[] requestPayload = ByteUtils.convertListToByteArray(requestMessage.getPayload());

        byte[] responsePayload = new byte[0];
        long readTimeoutMilliseconds = Math.max(READ_RETRY_DELAY_MILLISECONDS, Configuration.PollingPeriod);

        for (int attempt = 0; attempt < MaxReadAttempts; attempt++){

            int writeStatus = Byte.toUnsignedInt(_serialProvider.Write(requestPayload.length, requestPayload));

            if(writeStatus != SERIAL_STATUS_SUCCESS){

                sleep(
                        readTimeoutMilliseconds
                );

                readTimeoutMilliseconds +=
                        BACKOFF_INCREMENT_MILLISECONDS;

                continue;
            }

            sleep(
                    readTimeoutMilliseconds
            );


            byte[] header = readFromProvider(2, readTimeoutMilliseconds);

            if(header.length == 2){

                int totalMessageLength = Byte.toUnsignedInt(header[1]);

                int remainingByteCount = totalMessageLength - 2;

                if(remainingByteCount < 0){
                    responsePayload = header;
                    break;
                }

                byte[] remainingPayload = readFromProvider(remainingByteCount, readTimeoutMilliseconds);

                responsePayload = ByteUtils.concatArrays(
                        header,
                        remainingPayload
                );

                break;
            }

            readTimeoutMilliseconds += BACKOFF_INCREMENT_MILLISECONDS;
        }

        List<Byte> responsePayloadList = ByteUtils.convertByteArrayToList(responsePayload);

        TResponseMessage responseMessage = createResponseMessage.apply(responsePayloadList);

        _logger.LogTrace("Sent data to acceptor: %s", ByteUtils.ConvertToHexString(requestMessage.getPayload(), true, false));

        _logger.LogTrace("Received data from acceptor: %s", ByteUtils.ConvertToHexString(responseMessage.getPayload(), true, false));

        notifyCommunicationAttempted(requestMessage, responseMessage);

        if (responsePayload.length == 0){
            _logger.LogDebug("Experienced a communication timeout.");

            if(!_wasConnectionLostReported){
                notifyConnectionLost();
                _wasConnectionLostReported = true;
            }

            IsConnectionPresent = false;
            return new MessageResult<>(MessageRetrievalResult.Timeout, responseMessage);
        }

        _wasConnectionLostReported = false;
        IsConnectionPresent = true;

        if(!responseMessage.IsValid.get()){
            return new MessageResult<>(MessageRetrievalResult.IncorrectPayload, responseMessage);
        }

        if(responseMessage.Ack.get() != requestMessage.Ack.get()){
            return new MessageResult<>(MessageRetrievalResult.IncorrectAck, responseMessage);
        }

        _lastAck = responseMessage.Ack.get();

        return new MessageResult<>(MessageRetrievalResult.Success, responseMessage);
    }

    private byte[] readFromProvider(
            int requestedByteCount,
            long timeoutMilliseconds
    ) {

        if (requestedByteCount <= 0) {
            return new byte[0];
        }

        ByteArrayOutputStream result =
                new ByteArrayOutputStream(
                        requestedByteCount
                );

        long safeTimeoutMilliseconds =
                Math.max(
                        0L,
                        timeoutMilliseconds
                );

        long deadlineNanoseconds =
                System.nanoTime()
                        + TimeUnit.MILLISECONDS.toNanos(
                        safeTimeoutMilliseconds
                );

        while (result.size() < requestedByteCount
                && !Thread.currentThread().isInterrupted()) {

            int remainingByteCount =
                    requestedByteCount
                            - result.size();

            byte[] readBuffer =
                    new byte[remainingByteCount];

            int[] actualNumBytes =
                    new int[]{0};

            int readStatus =
                    Byte.toUnsignedInt(
                            _serialProvider.Read(
                                    remainingByteCount,
                                    readBuffer,
                                    actualNumBytes
                            )
                    );

            int actualCount =
                    actualNumBytes[0];

            if (actualCount < 0) {
                actualCount = 0;
            }

            if (actualCount > remainingByteCount) {
                actualCount =
                        remainingByteCount;
            }

            if (actualCount > 0) {
                result.write(
                        readBuffer,
                        0,
                        actualCount
                );
                continue;
            }


            if (readStatus != SERIAL_STATUS_SUCCESS
                    && readStatus != SERIAL_STATUS_NO_DATA) {

                _logger.LogError(
                        "The serial provider returned read status %b.",
                        readStatus
                );
            }

            if (System.nanoTime()
                    >= deadlineNanoseconds) {

                break;
            }

            sleep(
                    READ_RETRY_DELAY_MILLISECONDS
            );
        }

        return result.toByteArray();
    }

    private boolean trySendPollMessage(
            Function<Boolean, PollRequestMessage>
                    createPollRequestMessage
    ) {
        MessageResult<Rs232ResponseMessage> messageResult =
                trySendMessage(
                        createPollRequestMessage::apply,
                        payload -> {
                            PollResponseMessage pollResponse =
                                    new PollResponseMessage(
                                            payload
                                    );

                            if (pollResponse
                                    .getPayloadIssues()
                                    .isEmpty()) {

                                return pollResponse;
                            }

                            ExtendedResponseMessage extendedResponse =
                                    new ExtendedResponseMessage(
                                            payload
                                    );

                            if (extendedResponse
                                    .getPayloadIssues()
                                    .isEmpty()) {

                                return extendedResponse;
                            }

                            return pollResponse;
                        }
                );

        if (messageResult.result
                != MessageRetrievalResult.Success) {

            if (messageResult.result
                    == MessageRetrievalResult.IncorrectPayload) {

                logPayloadIssues(
                        messageResult.response
                );
            }

            return false;
        }

        PollResponseMessage responseMessage =
                (PollResponseMessage) messageResult.response;

        processStateChange(
                responseMessage
        );

        processReportedEvents(
                responseMessage
        );

        processCashboxState(
                responseMessage
        );

        processStackedBill(
                responseMessage
        );

        processEscrowedBill(
                responseMessage
        );

        processEscrowFinished(
                responseMessage
        );

        processExtendedResponse(
                responseMessage
        );

        return true;
    }

    private void processStateChange(
            PollResponseMessage responseMessage
    ) {
        Rs232State newState =
                responseMessage.getState();

        if (newState == _state) {
            return;
        }

        Rs232State oldState =
                _state;

        _logger.LogDebug(
                "The state changed from %s to %s.",
                oldState.name(),
                newState.name()
        );

        _state =
                newState;

        notifyStateChanged(
                oldState,
                newState
        );
    }

    private void processReportedEvents(
            PollResponseMessage responseMessage
    ) {
        Rs232Event reportedEvent =
                responseMessage.getEvent();

        if (reportedEvent.flags == Rs232Event.None) {
            return;
        }

        _logger.LogDebug(
                "Received event(s): %s.",
                reportedEvent.Flags()
        );

        notifyEventReported(
                reportedEvent
        );
    }

    private void processCashboxState(
            PollResponseMessage responseMessage
    ) {
        if (responseMessage.getIsCashboxPresent()
                && !_wasCashboxAttachmentReported) {

            _logger.LogDebug(
                    "The cashbox was attached."
            );

            notifyCashboxAttached();

            _wasCashboxAttachmentReported = true;
            _wasCashboxRemovalReported = false;
        }

        if (!responseMessage.getIsCashboxPresent()
                && !_wasCashboxRemovalReported) {

            _logger.LogDebug(
                    "The cashbox was removed."
            );

            notifyCashboxRemoved();

            _wasCashboxRemovalReported = true;
            _wasCashboxAttachmentReported = false;
        }
    }

    private void processStackedBill(
            PollResponseMessage responseMessage
    ) {
        if (!responseMessage
                .getEvent()
                .hasFlag(Rs232Event.Stacked)) {

            return;
        }

        int billType =
                Byte.toUnsignedInt(
                        responseMessage.getBillType()
                );

        if (billType == 0) {
            _logger.LogError(
                    "Stacked an unknown bill."
            );
        } else {
            _logger.LogDebug(
                    "Stacked a bill of type %d.",
                    billType
            );
        }

        notifyBillStacked(
                billType
        );
    }

    private void processEscrowedBill(
            PollResponseMessage responseMessage
    ) {
        if (responseMessage.getState()
                != Rs232State.Escrowed
                || _wasEscrowedBillReported) {
            return;
        }

        int billType =
                Byte.toUnsignedInt(
                        responseMessage.getBillType()
                );

        if (billType == 0) {
            _logger.LogError(
                    "Escrowed an unknown bill."
            );
        } else {
            _logger.LogDebug(
                    "Escrowed a bill of type %d.",
                    billType
            );
        }

        notifyBillEscrowed(
                billType
        );

        _wasEscrowedBillReported = true;
    }

    private void processEscrowFinished(
            PollResponseMessage responseMessage
    ) {
        if (responseMessage.getState()
                == Rs232State.Escrowed) {

            return;
        }

        _shouldRequestBillStack = false;
        _shouldRequestBillReturn = false;

        _wasEscrowedBillReported = false;
        _wasBarcodeDetectedReported = false;
    }

    private void processExtendedResponse(
            PollResponseMessage responseMessage
    ) {
        if (responseMessage.MessageType.get()
                != Rs232MessageType.ExtendedCommand) {

            return;
        }

        if (!(responseMessage
                instanceof ExtendedResponseMessage)) {

            _logger.LogError(
                    "An extended response was not an ExtendedResponseMessage."
            );

            return;
        }

        _logger.LogDebug(
                "Received an extended response message."
        );

        ExtendedResponseMessage extendedResponse =
                (ExtendedResponseMessage) responseMessage;

        switch (extendedResponse.getCommand()) {
            case BarcodeDetected:
                BarcodeDetectedResponseMessage barcodeResponse =
                        new BarcodeDetectedResponseMessage(
                                extendedResponse.getPayload()
                        );

                if (!barcodeResponse.IsValid.get()) {
                    logPayloadIssues(
                            barcodeResponse
                    );

                    return;
                }

                if (!_wasBarcodeDetectedReported) {
                    _logger.LogDebug(
                            "Detected a barcode: %s",
                            barcodeResponse.getBarcode()
                    );

                    notifyBarcodeDetected(
                            barcodeResponse.getBarcode()
                    );

                    _wasBarcodeDetectedReported = true;
                }

                break;

            default:
                _logger.LogError(
                        "Received an unknown extended command: %s.",
                        extendedResponse.getCommand().name()
                );

                break;
        }
    }

    private boolean checkForDevice() {
        int successfulPolls = 0;
        boolean wasAckFlipped = false;

        while (successfulPolls
                < SuccessfulPollsRequiredToStartPollingLoop
                && !Thread.currentThread().isInterrupted()) {

            MessageResult<PollResponseMessage> messageResult =
                    trySendMessage(
                            PollRequestMessage::new,
                            PollResponseMessage::new
                    );

            if (messageResult.result
                    != MessageRetrievalResult.Success) {

                if (messageResult.result
                        != MessageRetrievalResult.IncorrectPayload) {

                    return false;
                }

                if (wasAckFlipped) {
                    logPayloadIssues(
                            messageResult.response
                    );

                    return false;
                }

                wasAckFlipped = true;
                _lastAck = !_lastAck;
                continue;
            }

            successfulPolls++;

            sleep(
                    Configuration.PollingPeriod
            );
        }

        return successfulPolls
                >= SuccessfulPollsRequiredToStartPollingLoop;
    }

    private void loopPollMessages() {
        pollingThread = Thread.currentThread();

        try {
            while (_isPolling.get()
                    && !Thread.currentThread().isInterrupted()) {

                if (retryMessage != null) {
                    boolean completed =
                            processUnknownPendingMessage(
                                    retryMessage
                            );

                    if (completed) {
                        retryMessage = null;
                    }
                } else if (retryPoll) {
                    retryPoll =
                            !sendStandardPoll();
                } else {
                    PendingMessage<?> pendingMessage =
                            messageQueue.poll();

                    if (pendingMessage != null) {
                        boolean completed =
                                processUnknownPendingMessage(
                                        pendingMessage
                                );

                        if (!completed) {
                            retryMessage =
                                    pendingMessage;
                        }
                    } else {
                        retryPoll =
                                !sendStandardPoll();
                    }
                }

                sleep(
                        Configuration.PollingPeriod
                );
            }
        } catch (Throwable throwable) {
            _logger.LogError(
                    "Serial communication failed: %s",
                    throwable.getMessage()
            );

            IsConnectionPresent = false;
            _isPolling.set(false);

            failAllPendingMessages(
                    throwable
            );

            if (!_wasConnectionLostReported) {
                notifyConnectionLost();
                _wasConnectionLostReported = true;
            }
        } finally {
            pollingThread = null;

            _logger.LogDebug(
                    "Received the stop signal."
            );
        }
    }

    private boolean sendStandardPoll() {
        return trySendPollMessage(
                ack -> new PollRequestMessage(ack)
                        .SetEnableMask(
                                Configuration.EnableMask
                        )
                        .SetEscrowRequested(
                                Configuration.ShouldEscrow
                                        || _shouldRequestBillStack
                                        || _shouldRequestBillReturn
                        )
                        .SetStackRequested(
                                _shouldRequestBillStack
                        )
                        .SetReturnRequested(
                                _shouldRequestBillReturn
                        )
                        .SetBarcodeDetectionRequested(
                                Configuration.ShouldDetectBarcodes
                        )
        );
    }

    private void logPayloadIssues(
            Rs232ResponseMessage responseMessage
    ) {
        List<String> payloadIssues =
                responseMessage.getPayloadIssues();

        if (payloadIssues == null
                || payloadIssues.isEmpty()) {

            return;
        }

        StringBuilder errorMessage = new StringBuilder("Received an invalid response for a %s:");

        Object[] errorArgs = new Object[payloadIssues.size() + 1];
        errorArgs[0] = responseMessage.getClass().getSimpleName();
        for (int i = 0; i < payloadIssues.size(); i++) {
            errorMessage.append("\n\t{{%s}}");
            errorArgs[i + 1] = payloadIssues.get(i);
        }

        _logger.LogError(
                errorMessage.toString(), errorArgs
        );
    }

    private void failAllPendingMessages(
            Throwable throwable
    ) {
        PendingMessage<?> pendingMessage;

        while ((pendingMessage = messageQueue.poll())
                != null) {

            pendingMessage.future
                    .completeExceptionally(
                            throwable
                    );
        }

        if (retryMessage != null) {
            retryMessage.future
                    .completeExceptionally(
                            throwable
                    );
        }
    }

    private void notifyCommunicationAttempted(
            Rs232RequestMessage request,
            Rs232ResponseMessage response
    ) {
        CommunicationAttemptedEventArgs eventArgs =
                new CommunicationAttemptedEventArgs(
                        request,
                        response
                );

        if(OnCommunicationAttempted != null) {
            OnCommunicationAttempted.invoke(eventArgs);
        }
    }

    private void notifyStateChanged(
            Rs232State oldState,
            Rs232State newState
    ) {
        StateChangedEventArgs eventArgs =
                new StateChangedEventArgs(
                        oldState,
                        newState
                );

        if(OnStateChanged != null) {
            OnStateChanged.invoke(eventArgs);
        }
    }

    private void notifyEventReported(
            Rs232Event event
    ) {
        if(OnEventReported != null) {
            OnEventReported.invoke(event);
        }
    }

    private void notifyCashboxAttached() {
        if(OnCashboxAttached != null) {
            OnCashboxAttached.invoke();
        }
    }

    private void notifyCashboxRemoved() {
        if(OnCashboxRemoved != null) {
            OnCashboxRemoved.invoke();
        }
    }

    private void notifyBillStacked(
            int billType
    ) {
        if(OnBillStacked != null) {
            OnBillStacked.invoke(billType);
        }
    }

    private void notifyBillEscrowed(
            int billType
    ) {
        if(OnBillEscrowed != null) {
            OnBillEscrowed.invoke(billType);
        }
    }

    private void notifyBarcodeDetected(
            String barcode
    ) {
        if(OnBarcodeDetected != null) {
            OnBarcodeDetected.invoke(barcode);
        }
    }

    private void notifyConnectionLost() {
        if(OnConnectionLost != null) {
            OnConnectionLost.invoke();
        }
    }

    private static void sleep(
            long milliseconds
    ) {
        if (milliseconds <= 0) {
            return;
        }

        try {
            Thread.sleep(
                    milliseconds
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static ThreadFactory createThreadFactory(
            String name
    ) {
        return runnable -> {
            Thread thread =
                    new Thread(
                            runnable,
                            name
                    );

            thread.setDaemon(true);

            return thread;
        };
    }

    private void ensureNotClosed() {
        if (_closed.get()) {
            throw new IllegalStateException(
                    "The BillValidator has already been closed."
            );
        }
    }

    @Override
    public void close() throws Exception {

    }

    private static final class PendingMessage<
            TResponse extends Rs232ResponseMessage> {

        private final Function<Boolean, Rs232RequestMessage>
                requestFactory;

        private final Function<List<Byte>, TResponse>
                responseFactory;

        private final CompletableFuture<TResponse> future =
                new CompletableFuture<>();

        private final AtomicInteger incorrectPayloadCount =
                new AtomicInteger(0);

        private PendingMessage(
                Function<Boolean, Rs232RequestMessage>
                        requestFactory,
                Function<List<Byte>, TResponse>
                        responseFactory
        ) {
            this.requestFactory =
                    Objects.requireNonNull(
                            requestFactory,
                            "requestFactory"
                    );

            this.responseFactory =
                    Objects.requireNonNull(
                            responseFactory,
                            "responseFactory"
                    );
        }
    }

    private static final class MessageResult<
            TResponse extends Rs232ResponseMessage> {

        private final MessageRetrievalResult result;
        private final TResponse response;

        private MessageResult(
                MessageRetrievalResult result,
                TResponse response
        ) {
            this.result =
                    Objects.requireNonNull(
                            result,
                            "result"
                    );

            this.response =
                    Objects.requireNonNull(
                            response,
                            "response"
                    );
        }
    }

    private enum MessageRetrievalResult {

        Success,
        Timeout,
        IncorrectAck,
        IncorrectPayload;

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
