package PTI.Rs232Validator.BillValidators;

import PTI.Rs232Validator.CorrectableComponent;
import PTI.Rs232Validator.EventListener.BillValidatorListener;
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
import PTI.Rs232Validator.SerialProviders.ISerialProvider;
import PTI.Rs232Validator.SerialProviders.SerialPort;
import PTI.Rs232Validator.Utility.ByteUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

/**
* A hardware connection to a bill acceptor.
*/
public class BillValidator implements AutoCloseable {

    private static final byte SuccessfulPollsRequiredToStartPollingLoop = 2;
    private static final byte MaxReadAttempts = 4;
    private static final byte MaxIncorrectPayloadPardons = 2;

    private static final int SERIAL_STATUS_SUCCESS = 0x00;
    private static final int SERIAL_STATUS_NO_DATA = 0x01;

    private static final long BACKOFF_INCREMENT_MILLISECONDS = 50L;
    private static final long READ_RETRY_DELAY_MILLISECONDS = 5L;
    private static final long STOP_LOOP_TIMEOUT_MILLISECONDS = 3_000L;


    private static ILogger _logger = null;

    /**
     * An instance of ISerialProvider that handles port communication.
     */
    private final SerialPort _serialProvider;
    /**
     * The configuration for communicating with an RS-232 bill acceptor
     */
    private final Rs232Configuration Configuration;

    private final BlockingQueue<PendingMessage<?>> messageQueue = new LinkedBlockingQueue<>();
    private final CopyOnWriteArrayList<BillValidatorListener> listeners = new CopyOnWriteArrayList<>();

    private final AtomicBoolean _isPolling = new AtomicBoolean(false);
    private final AtomicBoolean _closed = new AtomicBoolean(false);

    private final ExecutorService commandExecutor = Executors.newSingleThreadExecutor(createThreadFactory("BillValidator-CommandWorker"));
    private ExecutorService pollingExecutor;
    private volatile Thread pollingThread;
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
    public BillValidator(ILogger logger, SerialPort serialProvider, Rs232Configuration configuration) {
        _logger = Objects.requireNonNull(logger);
        Configuration = Objects.requireNonNull(configuration);
        _serialProvider = Objects.requireNonNull(serialProvider);
        _serialProvider.logger = _logger;

        //_serialProvider.SetConfig((byte) 9600, (byte) 7, (byte) 1, (byte) 2, (byte) 0);
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

    public void addListener(BillValidatorListener listener) {
        listeners.addIfAbsent(Objects.requireNonNull(listener));
    }

    public void removeListener(BillValidatorListener listener) {
        listeners.remove(listener);
    }

    /**
     * Starts the polling loop to continuously poll the acceptor for messages. For internal use only;
     * use {@link #startPollingLoopAsync()} to start the polling loop asynchronously.
     * @return {@code true} if the polling loop was started; otherwise, {@code false}
     */
    private synchronized boolean startPollingLoop() {
        //ensureNotClosed();

        if(_isPolling.get()){
            _logger.LogDebug("The polling loop is running, so ignoring the start request.");
            return false;
        }

        /*if(!checkForDevice()){
            _logger.LogDebug("Failed to communicate with the bill validator");
            IsConnectionPresent = false;
            return false;
        }*/

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

    public synchronized void stopPollingLoop() {
        if (!_isPolling.get()) {
            _logger.LogDebug("The polling loop is not running, so ignoring the stop request.");
            return;
        }

        _logger.LogDebug("Stopping the polling loop gracefully...");

        // 1. Signal the polling loop to stop by setting the flag to false.
        // The loop will complete its current iteration and exit.
        _isPolling.set(false);

        ExecutorService executor = pollingExecutor;
        if (executor != null) {
            // 2. Shutdown the executor. This prevents new tasks but allows the running one to finish.
            executor.shutdown();
            try {
                // 3. Wait for the polling loop to terminate cleanly.
                boolean stopped = executor.awaitTermination(STOP_LOOP_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);

                if (stopped) {
                    _logger.LogDebug("Stopped the polling loop gracefully.");
                } else {
                    // 4. If it doesn't stop in time, force it.
                    _logger.LogError("Polling loop did not terminate gracefully within the timeout. Forcing shutdown.");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
                _logger.LogError("Interrupted while waiting for the polling loop to stop. Forcing shutdown.", e);
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

        // The _isPolling flag is already false, so no need to set it again.
    }

    public void stackBill() {
        synchronized (this) {
            if (_state != Rs232State.Escrowed) {
                _logger.LogDebug("Cannot stack a bill that is not in escrow");
                return;
            }
            _shouldRequestBillStack = true;
        }
    }

    public void returnBill() {
        synchronized (this) {
            if (_state != Rs232State.Escrowed) {
                _logger.LogDebug("Cannot return a bill that is not in escrow");
                return;
            }
            _shouldRequestBillReturn = true;
        }
    }

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

    private void enqueuePendingMessage(PendingMessage<?> pendingMessage){
        messageQueue.offer(pendingMessage);

        if(!_isPolling.get() && messageQueue.remove(pendingMessage)){
            pendingMessage.future.completeExceptionally(new IllegalStateException("The polling loop stopped before the message could be processed."));
        }
    }

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

    private <TResponseMessage extends Rs232ResponseMessage> boolean pendingMessage(PendingMessage<TResponseMessage> pendingMessage) {
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
    }

    private boolean processUnknownPendingMessage(PendingMessage<?> pendingMessage){
        return processPendingMessage((PendingMessage) pendingMessage);
    }

    private <TResponseMessage extends Rs232ResponseMessage> MessageResult<TResponseMessage> trySendMessage(
            Function<Boolean, Rs232RequestMessage> createRequestMessage,
            Function<List<Byte>, TResponseMessage> createResponseMessage) {

        _logger.LogTrace("Attempting to send: " + createRequestMessage.toString());
        Rs232RequestMessage requestMessage = createRequestMessage.apply(!_lastAck);

        _logger.LogTrace("Created request message: " + requestMessage.toString());
        _logger.LogTrace("Request message payload: " + ByteUtils.ConvertToHexString(requestMessage.getPayload(), true, false));
        byte[] requestPayload = ByteUtils.convertListToByteArray(requestMessage.getPayload());

        byte[] responsePayload = new byte[0];
        long readTimeoutMilliseconds = Math.max(READ_RETRY_DELAY_MILLISECONDS, Configuration.PollingPeriod);

        for (int attempt = 0; attempt < MaxReadAttempts; attempt++){

            int writeStatus = Byte.toUnsignedInt(_serialProvider.Write(requestPayload.length, requestPayload));

            if(writeStatus != SERIAL_STATUS_SUCCESS){
                _logger.LogError(
                        "The serial provider returned write status {0}.",
                        writeStatus
                );

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

            _logger.LogTrace("Attempting to read header");
            byte[] header = readFromProvider(2, readTimeoutMilliseconds);

            if(header.length == 2){

                int totalMessageLength = Byte.toUnsignedInt(header[1]);

                _logger.LogTrace("Header read successfully. Total message length: " + totalMessageLength);

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

        _logger.LogTrace(
                "Sent message: " + ByteUtils.ConvertToHexString(ByteUtils.convertByteArrayToList(requestPayload), true, false)
        );

        _logger.LogTrace(
                "Received message: " + ByteUtils.ConvertToHexString(responsePayloadList, true, false)
        );

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

        _logger.LogTrace("Attempting to read bytes: " + String.valueOf(requestedByteCount));
        if (requestedByteCount <= 0) {
            _logger.LogTrace("Requested to read {0} bytes, so returning an empty array.", requestedByteCount);
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
            _logger.LogTrace(
                    "Read {0} bytes from the serial provider.",
                    actualCount
            );

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
                _logger.LogTrace("Wrote to the result buffer: " + ByteUtils.ConvertToHexString(ByteUtils.convertByteArrayToList(readBuffer), true, false));
                continue;
            }


            if (readStatus != SERIAL_STATUS_SUCCESS
                    && readStatus != SERIAL_STATUS_NO_DATA) {

                _logger.LogError(
                        "The serial provider returned read status {0}.",
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
                "The state changed from {0} to {1}.",
                oldState,
                newState
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
                "Received event(s): {0}.",
                reportedEvent
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
                    "Stacked a bill of type {0}.",
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
                || !_wasEscrowedBillReported) {

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
                    "Escrowed a bill of type {0}.",
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
                            "Detected a barcode: {0}",
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
                        "Received an unknown extended command: {0}.",
                        extendedResponse.getCommand()
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
                    "Serial communication failed: {0}",
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

        StringBuilder errorMessage =
                new StringBuilder(
                        "Received an invalid response for a "
                );

        errorMessage.append(
                responseMessage
                        .getClass()
                        .getSimpleName()
        );

        errorMessage.append(":");

        for (String issue : payloadIssues) {
            errorMessage
                    .append("\n\t")
                    .append(issue);
        }

        _logger.LogError(
                errorMessage.toString()
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

        for (BillValidatorListener listener : listeners) {
            listener.onCommunicationAttempted(
                    eventArgs
            );
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

        for (BillValidatorListener listener : listeners) {
            listener.onStateChanged(
                    eventArgs
            );
        }
    }

    private void notifyEventReported(
            Rs232Event event
    ) {
        for (BillValidatorListener listener : listeners) {
            listener.onEventReported(
                    event
            );
        }
    }

    private void notifyCashboxAttached() {
        for (BillValidatorListener listener : listeners) {
            listener.onCashboxAttached();
        }
    }

    private void notifyCashboxRemoved() {
        for (BillValidatorListener listener : listeners) {
            listener.onCashboxRemoved();
        }
    }

    private void notifyBillStacked(
            int billType
    ) {
        for (BillValidatorListener listener : listeners) {
            listener.onBillStacked(
                    billType
            );
        }
    }

    private void notifyBillEscrowed(
            int billType
    ) {
        for (BillValidatorListener listener : listeners) {
            listener.onBillEscrowed(
                    billType
            );
        }
    }

    private void notifyBarcodeDetected(
            String barcode
    ) {
        for (BillValidatorListener listener : listeners) {
            listener.onBarcodeDetected(
                    barcode
            );
        }
    }

    private void notifyConnectionLost() {
        for (BillValidatorListener listener : listeners) {
            listener.onConnectionLost();
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
        _logger.LogTrace("Sending telemetry message: " + command.name() + command.getValue());
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
