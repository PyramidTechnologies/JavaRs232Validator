package PTI.Rs232Validator.BillValidators;

import static PTI.Rs232Validator.Utility.ByteUtils.convertByteArrayToList;
import static PTI.Rs232Validator.Utility.ByteUtils.convertListToByteArray;

import android.content.Context;
import android.content.SharedPreferences;

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
import PTI.Rs232Validator.SerialProviders.FT311UARTInterface;
import PTI.Rs232Validator.SerialProviders.ISerialProvider;
import PTI.Rs232Validator.Utility.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

/**
* A hardware connection to a bill acceptor.
*/
public class BillValidator {

    private static final byte SuccessfulPollsRequiredToStartPollingLoop = 2;
    private static final byte MaxReadAttempts = 4;
    private static final byte MaxIncorrectPayloadPardons = 2;
    private static final Duration BackoffIncrement = Duration.ofMillis(50);
    private static final Duration StopLoopTimeout = Duration.ofSeconds(3);


    private static ILogger _logger = null;
    private static final Object _mutex = new Object();

    /**
     * An instance of ISerialProvider that handles port communication.
     */
    public ISerialProvider _serialProvider;

    private static final Queue<Supplier<Boolean>> _messageCallbacks = new LinkedList<Supplier<Boolean>>();
    private Supplier<Boolean> _lastMessageCallback;

    private CompletableFuture<Void> _worker = CompletableFuture.completedFuture(null);
    private final ExecutorService _executor = Executors.newSingleThreadExecutor();
    private static boolean _isPolling;
    private static boolean _lastAck;
    private Rs232State _state;
    private boolean _shouldRequestBillStack;
    private boolean _shouldRequestBillReturn;
    private boolean _wasCashboxAttachmentReported;
    private boolean _wasCashboxRemovalReported;
    private boolean _wasEscrowedBillReported;
    private boolean _wasBarcodeDetectedReported;
    private boolean _wasConnectionLostReported;

    /**
    * Initializes a new instance of {@link BillValidator} with a provided serial connection
    */
    public BillValidator(ILogger logger, ISerialProvider serialProvider, Rs232Configuration configuration) {
        _logger = logger;
        Configuration = configuration;
        _serialProvider = serialProvider;
    }

    /**
     * Initializes a new instance of {@link BillValidator} without a pre-existing serial connection
     */
    public BillValidator(ILogger logger, Rs232Configuration configuration, Context context, SharedPreferences sharePrefSettings, String permissionString) {
        _logger = logger;
        Configuration = configuration;
        _serialProvider = new FT311UARTInterface(context, sharePrefSettings, logger, permissionString);
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
     * The configuration for communicating with an RS-232 bill acceptor
     */
    public Rs232Configuration Configuration;

    /**
     * Is the connection to the acceptor present?
     */
    public static boolean IsConnectionPresent;

    /**
     * Starts the RS-232 polling loop
     * @return True if the polling loop starts; otherwise, false
     */
    public boolean StartPollingLoop() {
        synchronized (_mutex) {
            if (_isPolling) {
                _logger.LogDebug("The polling loop is running, so ignoring the start request");
            }
        }

        /*if(!CheckForDevice()){
            return false;
        }*/

        synchronized (_mutex) {
            _isPolling = true;
        }

        _worker = CompletableFuture.runAsync(this::LoopPollMessages, _executor);
        IsConnectionPresent = true;
        return true;
    }

    /**
     * Stops the RS-232 polling loop
     */
    public void StopPollingLoop() {
        synchronized (_mutex) {
            if (!_isPolling) {
                _logger.LogDebug("The polling loop is not running, so ignoring the stop request");
                return;
            }

            _isPolling = false;
        }

        _logger.LogDebug("Stopping the polling loop");


        try {
            _worker.get(StopLoopTimeout.getSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            //throw new RuntimeException(e);
        }


        _messageCallbacks.clear();
        _lastMessageCallback = null;
        _shouldRequestBillStack = false;
        _shouldRequestBillReturn = false;
        _wasCashboxAttachmentReported = false;
        _wasCashboxRemovalReported = false;
        _wasConnectionLostReported = false;
        IsConnectionPresent = false;

    }

    /**
     * Stacks a bill in escrow
     */
    public void StackBill() {
        synchronized (_mutex) {
            if (_state != Rs232State.Escrowed) {
                _logger.LogDebug("Cannot stack a bill that is not in escrow");
                return;
            }
            _shouldRequestBillStack = true;
        }
    }

    /**
     * Returns a bill in escrow
     */
    public void ReturnBill() {
        synchronized (_mutex) {
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
     * @param createRequestMessage A function that returns an instance of {@link Rs232RequestMessage}
     * @param createResponseMessage A function that returns an instance of {@link Rs232ResponseMessage}
     * @return A completable future that returns an instance of {@link Rs232ResponseMessage}
     * @param <TResponseMessage> An object that extends {@link Rs232ResponseMessage}
     */
    public <TResponseMessage extends Rs232ResponseMessage> CompletableFuture<TResponseMessage> SendNonPollMessageAsync(
            Function<Boolean, Rs232RequestMessage> createRequestMessage,
            Function<List<Byte>, TResponseMessage> createResponseMessage) {

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger incorrectPayloadCount = new AtomicInteger();

        Mutable<TResponseMessage> responseMessageMutable = new Mutable<TResponseMessage>();

        responseMessageMutable.value = (TResponseMessage) createResponseMessage.apply(new ArrayList<Byte>());

        Supplier<Boolean> messageCallback = () -> {
            MessageRetrievalResult messageRetrievalResult = null;
            messageRetrievalResult = TrySendMessage(createRequestMessage, createResponseMessage, responseMessageMutable);

            TResponseMessage responseMessage = responseMessageMutable.value;

            if (messageRetrievalResult == null) {
                return null;
            }

            switch (messageRetrievalResult) {
                case IncorrectPayload:
                    if (incorrectPayloadCount.incrementAndGet() <= MaxIncorrectPayloadPardons) {
                        return false;
                    }
                case IncorrectAck:
                    return false;
            }

            if (messageRetrievalResult.getValue() == MessageRetrievalResult.IncorrectPayload.getValue()) {
                LogPayloadIssues(responseMessage);
            }

            latch.countDown();
            return true;
        };

        boolean isPolling;
        synchronized (_mutex) {
            isPolling = _isPolling;
        }

        if (isPolling) {
            EnqueueMessageCallback(messageCallback);
            return CompletableFuture.supplyAsync(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                return responseMessageMutable.value;
            });
        }

        return CompletableFuture.supplyAsync(() -> {

        if(!CheckForDevice()) {

            return responseMessageMutable.value;
        }

            while (!messageCallback.get()) {
                try {
                    Thread.sleep(Configuration.PollingPeriod.toMillis());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            return responseMessageMutable.value;
        });
    }

    private static void EnqueueMessageCallback(Supplier<Boolean> messageCallback) {
        synchronized (_mutex) {
            _messageCallbacks.add(messageCallback);
        }
    }


    private Supplier<Boolean> DequeueMessageCallback() {
        synchronized (_mutex) {
            return _messageCallbacks.poll();
        }
    }


    private static <TResponseMessage extends Rs232ResponseMessage> void LogPayloadIssues(Rs232ResponseMessage responseMessage) {
        List<String> payloadIssues = responseMessage.getPayloadIssues();
        if (payloadIssues.isEmpty()) {
            return;
        }

        String errorMessage = "Received an invalid response for a %s";
        Object[] errorArgs = new Object[payloadIssues.size() + 1];
        errorArgs[0] = StringUtils.AddSpacesToCamelCase(responseMessage.getClass().getSimpleName());

        for (int i = 0; i < payloadIssues.size(); i++) {
            errorMessage += "\n\t{{%s}}";
            errorArgs[i + 1] = payloadIssues.get(i);
        }
        _logger.LogError(errorMessage, errorArgs);
    }

    private static class Mutable<TResponseMessage> {
        public TResponseMessage value;
    }

    private <TResponseMessage extends Rs232ResponseMessage> MessageRetrievalResult TrySendMessage(Function<Boolean, Rs232RequestMessage> createRequestMessage,
                                                                                                 Function<List<Byte>, TResponseMessage> createResponseMessage, Mutable<TResponseMessage> responseMessageMutix) {
        Rs232RequestMessage requestMessage = createRequestMessage.apply(!_lastAck);
        List<Byte> requestPayload = requestMessage.getPayload();

        List<Byte> responsePayload = new LinkedList<Byte>();
        Duration backoffTime = Configuration.PollingPeriod;
        for (int i = 0; i < MaxReadAttempts; i++) {
            _serialProvider.Write(convertListToByteArray(requestPayload));
            responsePayload = convertByteArrayToList(_serialProvider.Read(2));
            
            if (responsePayload.size() == 2) {
                long remainingByteCount = (long) (responsePayload.get(1) - 2);
                responsePayload.addAll(convertByteArrayToList(_serialProvider.Read(remainingByteCount)));
                break;
            }

            try {
                Thread.sleep(backoffTime.toMillis());
            } catch (InterruptedException e) {
                _logger.LogError(e.getMessage());
            }
            backoffTime.plus(BackoffIncrement);
        }

        responseMessageMutix.value = createResponseMessage.apply(responsePayload);
        if (OnCommunicationAttempted != null) {
            OnCommunicationAttempted.Invoke(requestMessage, responseMessageMutix.value);
        }

        if (responsePayload.isEmpty()) {
            if (!_wasConnectionLostReported) {
                if (OnConnectionLost != null) {
                    OnConnectionLost.Invoke();
                }
                _wasConnectionLostReported = true;
            }

            IsConnectionPresent = false;
            return MessageRetrievalResult.Timeout;
        }

        _wasConnectionLostReported = false;
        IsConnectionPresent = true;


        if (!responseMessageMutix.value.IsValid.get()) {
            return MessageRetrievalResult.IncorrectPayload;
        }


        if (requestMessage.Ack.get() != responseMessageMutix.value.Ack.get()) {
            return MessageRetrievalResult.IncorrectAck;
        }

        _lastAck = responseMessageMutix.value.Ack.get();
        return MessageRetrievalResult.Success;
    }
    
    private boolean TrySendPollMessage(Function<Boolean, Rs232RequestMessage> createPollRequestMessage) {
        Mutable<PollResponseMessage> responseMessageMutix = new Mutable<PollResponseMessage>();
        MessageRetrievalResult messageRetrievalResult = TrySendMessage(createPollRequestMessage,
                payload -> {
                    PollResponseMessage pollResponseMessage = new PollResponseMessage(payload);
                    if (pollResponseMessage.GetPayloadIssues().isEmpty()) {
                        return pollResponseMessage;
                    }

                    ExtendedResponseMessage extendedResponseMessage = new ExtendedResponseMessage(payload);
                    if (extendedResponseMessage.GetPayloadIssues().isEmpty()) {
                        return extendedResponseMessage;
                    }

                    return pollResponseMessage;
                },
                responseMessageMutix);

        PollResponseMessage responseMessage = responseMessageMutix.value;
        if (messageRetrievalResult.getValue() != MessageRetrievalResult.Success.getValue()) {
            if (messageRetrievalResult.getValue() == MessageRetrievalResult.IncorrectPayload.getValue()) {
                LogPayloadIssues(responseMessage);
            }

            return false;
        }

        if (responseMessage.getState() != _state) {
            _logger.LogDebug("The state changed from %s to %s", _state, responseMessage.getState());
            if (OnStateChanged != null) {
                OnStateChanged.Invoke(_state, responseMessage.getState());
            }

            synchronized (_mutex) {
                _state = responseMessage.getState();
            }
        }

        if (responseMessage.getEvent().flags != Rs232Event.None) {
            _logger.LogDebug("Received event(s): %s", responseMessage.getEvent().Flags());
            if (OnEventReported != null) {
                OnEventReported.Invoke(responseMessage.getEvent());
            }
        }

        if (responseMessage.getIsCashboxPresent() && !_wasCashboxAttachmentReported) {
            _logger.LogDebug("The cashbox was attached.");
            if (OnCashboxAttached != null) {
                OnCashboxAttached.Invoke();
            }

            _wasCashboxAttachmentReported = true;
            _wasCashboxRemovalReported = false;
        }

        if (!responseMessage.getIsCashboxPresent() && !_wasCashboxRemovalReported) {
            _logger.LogDebug("The cashbox was removed.");
            if (OnCashboxRemoved != null) {
                OnCashboxRemoved.Invoke();
            }

            _wasCashboxAttachmentReported = false;
            _wasCashboxRemovalReported = true;
        }

        if (responseMessage.getEvent().hasFlag(Rs232Event.Stacked)) {
            if (responseMessage.getBillType() == 0) {
                _logger.LogError("Stacked an unknown bill");
            } else {
                _logger.LogDebug("Stacked a bill of type %d", responseMessage.getBillType());
            }

            if (OnBillStacked != null) {
                OnBillStacked.Invoke(responseMessage.getBillType());
            }
        }

        if (responseMessage.getState() == Rs232State.Escrowed && !_wasEscrowedBillReported) {
            if (responseMessage.getBillType() == 0) {
                _logger.LogError("Escrowed an unknown bill");
            } else {
                _logger.LogDebug("Escrowed a bill of type %d", responseMessage.getBillType());
            }

            if (OnBillEscrowed != null) {
                OnBillEscrowed.Invoke(responseMessage.getBillType());
            }
            _wasEscrowedBillReported = true;
        }

        if (responseMessage.getState() != Rs232State.Escrowed) {
            synchronized (_mutex) {
                _shouldRequestBillStack = false;
                _shouldRequestBillReturn = false;
            }

            _wasEscrowedBillReported = false;
            _wasBarcodeDetectedReported = false;
        }

        if (responseMessage.MessageType.get().getValue() == Rs232MessageType.ExtendedCommand.getValue()) {
            _logger.LogDebug("Received extended command response message.");
            ExtendedResponseMessage extendedResponseMessage = (ExtendedResponseMessage) responseMessage;
            switch (extendedResponseMessage.getCommand()) {
                case BarcodeDetected:
                    BarcodeDetectedResponseMessage barcodeDetectedResponseMessage = new BarcodeDetectedResponseMessage(extendedResponseMessage.getPayload());
                    if (!barcodeDetectedResponseMessage.IsValid.get()) {
                        LogPayloadIssues(barcodeDetectedResponseMessage);
                        return false;
                    }

                    if (!_wasBarcodeDetectedReported) {
                        _logger.LogDebug("Detected a barcode: %s", barcodeDetectedResponseMessage.getBarcode());
                        if (OnBarcodeDetected != null) {
                            OnBarcodeDetected.Invoke(barcodeDetectedResponseMessage.getBarcode());
                        }
                        _wasBarcodeDetectedReported = true;
                    }

                    break;
                default:
                    _logger.LogDebug("Received an unknown extended command:  %s", extendedResponseMessage.getCommand().name());
                    break;
            }
        }
        return true;
    }

    private boolean CheckForDevice() {
        int successfulPolls = 0;
        boolean wasAckFlipped = false;
        Mutable<PollResponseMessage> pollResponseMessageMutix = new Mutable<PollResponseMessage>();

        while (successfulPolls < SuccessfulPollsRequiredToStartPollingLoop) {
            MessageRetrievalResult messageRetrievalResult = TrySendMessage(PollRequestMessage::new, PollResponseMessage::new, pollResponseMessageMutix);
            PollResponseMessage pollResponseMessage = pollResponseMessageMutix.value;
            if (messageRetrievalResult.getValue() != MessageRetrievalResult.Success.getValue()) {
                if (messageRetrievalResult.getValue() == MessageRetrievalResult.IncorrectPayload.getValue()) {
                    return false;
                }

                if (wasAckFlipped) {
                    LogPayloadIssues(pollResponseMessage);
                    return false;
                }

                wasAckFlipped = true;
                _lastAck = !_lastAck;
                continue;
            }

            successfulPolls++;
            try {
                Thread.sleep(Configuration.PollingPeriod.toMillis());
            } catch (InterruptedException e) {
            }

        }

        return true;
    }

    private void LoopPollMessages() {
        while (true) {
            synchronized (_mutex) {
                if (!_isPolling) {
                    _logger.LogDebug("Received the stop signal");
                    return;
                }
            }

            if (_lastMessageCallback != null) {
                if (Boolean.TRUE.equals(_lastMessageCallback.get())) {
                    _lastMessageCallback = null;
                }
            } else {
                Supplier<Boolean> messageCallback = DequeueMessageCallback();
                if (messageCallback != null) {
                    if (!Boolean.TRUE.equals(messageCallback.get())) {
                        _lastMessageCallback = messageCallback;
                    }
                } else {
                    messageCallback = () -> TrySendPollMessage(ack ->
                            new PollRequestMessage(ack)
                                    .SetEnableMask(Configuration.EnableMask)
                                    .SetEscrowRequested(Configuration.ShouldEscrow
                                            || _shouldRequestBillStack
                                            || _shouldRequestBillReturn)
                                    .SetStackRequested(_shouldRequestBillStack)
                                    .SetReturnRequested(_shouldRequestBillReturn)
                                    .SetBarcodeDetectionRequested(Configuration.ShouldDetectBarcodes));
                    if (!Boolean.TRUE.equals(messageCallback.get())) {
                        _lastMessageCallback = messageCallback;
                    }
                }
            }

            try {
                Thread.sleep(Configuration.PollingPeriod.toMillis());
            } catch (InterruptedException e) {
            }
        }
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
