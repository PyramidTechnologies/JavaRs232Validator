package PTI.Rs232Validator.BillValidators;

import PTI.Rs232Validator.Loggers.ILogger;
import PTI.Rs232Validator.Messages.Commands.ExtendedCommand;
import PTI.Rs232Validator.Messages.Commands.TelemetryCommand;
import PTI.Rs232Validator.Messages.Requests.ExtendedRequestMessage;
import PTI.Rs232Validator.Messages.Requests.PollRequestMessage;
import PTI.Rs232Validator.Messages.Requests.Rs232RequestMessage;
import PTI.Rs232Validator.Messages.Requests.TelemetryRequestMessage;
import PTI.Rs232Validator.Messages.Responses.Extended.BarcodeDetectedResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Extended.ExtendedResponseMessage;
import PTI.Rs232Validator.Messages.Responses.PollResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Rs232ResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Telemetry.GetCashboxMetricsResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Telemetry.GetFirmwareMetricsResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Telemetry.GetSerialNumberResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Telemetry.GetServiceFlagsResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Telemetry.GetServiceInfoResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Telemetry.GetServiceUsageCountersResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Telemetry.GetUnitMetricsResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Telemetry.TelemetryResponseMessage;
import PTI.Rs232Validator.Messages.Rs232MessageType;
import PTI.Rs232Validator.Rs232Configuration;
import PTI.Rs232Validator.Rs232Event;
import PTI.Rs232Validator.Rs232State;
import PTI.Rs232Validator.CustomEvent;
import PTI.Rs232Validator.SerialProviders.ISerialProvider;
import PTI.Rs232Validator.Utility.ByteExtensions;
import PTI.Rs232Validator.Utility.StringExtensions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

public class BillValidator {

    private static final byte SuccessfulPollsRequiredToStartPollingLoop = 2;
    private static final byte MaxReadAttempts = 4;
    private static final byte MaxIncorrectPayloadPardons = 2;
    private static final Duration BackoffIncrement = Duration.ofMillis(50);
    private static final Duration StopLoopTimeout = Duration.ofSeconds(3);


    private static ILogger _logger = null;
    private static final Object _mutex = new Object();
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
    private static boolean _wasConnectionLostReported;

    public BillValidator(ILogger logger, ISerialProvider serialProvider, Rs232Configuration configuration) {
        _logger = logger;
        Configuration = configuration;
        _serialProvider = serialProvider;
    }

    public static CustomEvent OnCommunicationAttempted;
    public CustomEvent OnStateChanged;
    public CustomEvent OnEventReported;
    public CustomEvent OnCashboxAttached;
    public CustomEvent OnCashboxRemoved;
    public CustomEvent OnBillStacked;
    public CustomEvent OnBillEscrowed;
    public CustomEvent OnBarcodeDetected;
    public static CustomEvent OnConnectionLost;
    public static Rs232Configuration Configuration;
    public static boolean IsConnectionPresent;

    public boolean StartPollingLoop(){
        synchronized (_mutex) {
            if (_isPolling) {
                _logger.LogDebug("The polling loop is running, so ignoring the start request");
            }
        }

        if(!TryOpenPort()){
            return false;
        }

        if(!CheckForDevice()){
            return false;
        }

        synchronized (_mutex) {
            _isPolling = true;
        }

        _worker = CompletableFuture.runAsync(this::LoopPollMessages, _executor);
        IsConnectionPresent = true;
        return true;
    }

    public void StopPollingLoop(){
        synchronized (_mutex) {
            if(!_isPolling){
                _logger.LogDebug("The polling loop is not running, so ignoring the stop request");
                return;
            }

            _isPolling = false;
        }

        _logger.LogDebug("Stopping the polling loop");


        try {
            _worker.get(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }


         _messageCallbacks.clear();
        _lastMessageCallback = null;
        _shouldRequestBillStack = false;
        _shouldRequestBillReturn = false;
        _wasCashboxAttachmentReported = false;
        _wasCashboxRemovalReported = false;
        _wasConnectionLostReported = false;
        IsConnectionPresent = false;

        ClosePort();
    }

public void StackBill(){
    synchronized (_mutex) {
        if(_state != Rs232State.Escrowed) {
            _logger.LogDebug("Cannot stack a bill that is not in escrow");
            return;
        }
        _shouldRequestBillStack = true;
    }
}


public void ReturnBill(){
    synchronized (_mutex) {
        if(_state != Rs232State.Escrowed) {
            _logger.LogDebug("Cannot return a bill that is not in escrow");
            return;
        }
        _shouldRequestBillReturn = true;
    }
}

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

        if(messageRetrievalResult == null){
            return null;
        }

        switch(messageRetrievalResult) {
            case IncorrectPayload:
                if(incorrectPayloadCount.incrementAndGet() <= MaxIncorrectPayloadPardons){
                    return false;
                }
            case IncorrectAck:
                return false;
        }

        if(messageRetrievalResult.getValue() == MessageRetrievalResult.IncorrectPayload.getValue()) {
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
        if(!TryOpenPort()) {
            return responseMessageMutable.value;
        }

        if(!CheckForDevice()) {
            ClosePort();
            return responseMessageMutable.value;
        }

        while(!messageCallback.get()) {
            try {
                Thread.sleep(Configuration.PollingPeriod.toMillis());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return responseMessageMutable.value;
    });
}

private static void EnqueueMessageCallback(Supplier<Boolean> messageCallback){
    synchronized (_mutex) {
        _messageCallbacks.add(messageCallback);
    }
}


private Supplier<Boolean> DequeueMessageCallback(){
    synchronized (_mutex) {
        return _messageCallbacks.poll();
    }
}


private boolean TryOpenPort() {
    if(_serialProvider.TryOpen()){
        return true;
    }

    _logger.LogDebug("Failed to open the serial provider");
    return false;
}


private void ClosePort() {
    try{
        _serialProvider.Close();
    } catch(Exception e){
        _logger.LogError("Failed to close the serial provider: %s", e.getMessage());
    }
}


private static <TResponseMessage extends Rs232ResponseMessage> void LogPayloadIssues(Rs232ResponseMessage responseMessage){
    List<String> payloadIssues = responseMessage.getPayloadIssues();
    if(payloadIssues.isEmpty()){
        return;
    }

    String errorMessage = "Received an invalid response for a %s";
    Object[] errorArgs = new Object[payloadIssues.size() + 1];
    errorArgs[0] = StringExtensions.AddSpacesToCamelCase(responseMessage.getClass().getSimpleName());

    for(int i = 0; i < payloadIssues.size(); i++){
        errorMessage += "\n\t{{%s}}";
        errorArgs[i + 1] = payloadIssues.get(i);
    }
    _logger.LogError(errorMessage, errorArgs);
}

public static class Mutable<TResponseMessage> {
    public TResponseMessage value;
}

public <TResponseMessage extends Rs232ResponseMessage> MessageRetrievalResult TrySendMessage(Function<Boolean, Rs232RequestMessage> createRequestMessage,
                                                                                             Function<List<Byte>, TResponseMessage> createResponseMessage, Mutable<TResponseMessage> responseMessageMutix){

    //_logger.LogTrace("[TrySendMessage(%s, %s, %s)]", createRequestMessage, createResponseMessage, responseMessageMutix.toString());
    Rs232RequestMessage requestMessage = createRequestMessage.apply(!_lastAck);
    List<Byte> requestPayload = requestMessage.getPayload();

    List<Byte> responsePayload = new LinkedList<Byte>();
    Duration backoffTime = Configuration.PollingPeriod;
    for(int i = 0; i< MaxReadAttempts; i++){

        _serialProvider.Write(ByteExtensions.convertListToByteArray(requestPayload));


        responsePayload = ByteExtensions.convertByteArrayToList(_serialProvider.Read(2));
        if(responsePayload.size() == 2){
            long remainingByteCount = (long)(responsePayload.get(1) - 2);
            responsePayload.addAll(ByteExtensions.convertByteArrayToList(_serialProvider.Read(remainingByteCount)));
            break;


        }

        try {
            Thread.sleep(backoffTime.toMillis());
        } catch (InterruptedException e) {
            _logger.LogError(e.getMessage());
        }
    }

    responseMessageMutix.value = createResponseMessage.apply(responsePayload);
    if(OnCommunicationAttempted != null){
        OnCommunicationAttempted.Invoke(requestMessage, responseMessageMutix.value);
    }


    if(responsePayload.isEmpty()){
        if(!_wasConnectionLostReported){
            if(OnConnectionLost != null){
                OnConnectionLost.Invoke();
            }
            _wasConnectionLostReported = true;
        }

        IsConnectionPresent = false;
        return MessageRetrievalResult.Timeout;
    }

    _wasConnectionLostReported = false;
    IsConnectionPresent = true;



    if(!responseMessageMutix.value.IsValid){
        return MessageRetrievalResult.IncorrectPayload;
    }


    if(requestMessage.Ack.get() != responseMessageMutix.value.Ack.get()){
        return MessageRetrievalResult.IncorrectAck;
    }

    _lastAck = responseMessageMutix.value.Ack.get();
    return MessageRetrievalResult.Success;
}


private boolean TrySendPollMessage(Function<Boolean, Rs232RequestMessage> createPollRequestMessage){
    Mutable<PollResponseMessage> responseMessageMutix  = new Mutable<PollResponseMessage>();
    MessageRetrievalResult messageRetrievalResult = TrySendMessage(createPollRequestMessage,
            payload -> {
                PollResponseMessage pollResponseMessage = new PollResponseMessage(payload);
                if(pollResponseMessage.GetPayloadIssues().isEmpty()){
                    return pollResponseMessage;
                }

                ExtendedResponseMessage extendedResponseMessage = new ExtendedResponseMessage(payload);
                if(extendedResponseMessage.GetPayloadIssues().isEmpty()){
                    return extendedResponseMessage;
                }

                return pollResponseMessage;
            },
            responseMessageMutix);

    PollResponseMessage responseMessage = responseMessageMutix.value;
    if(messageRetrievalResult.getValue() != MessageRetrievalResult.Success.getValue()){
        if(messageRetrievalResult.getValue() == MessageRetrievalResult.IncorrectPayload.getValue()){
            LogPayloadIssues(responseMessage);
        }

        return false;
    }

    if(responseMessage.getState() != _state){
        _logger.LogDebug("The state changed from %s to %s", _state, responseMessage.getState());
        if(OnStateChanged != null){
            OnStateChanged.Invoke(_state, responseMessage.getState());
        }

        synchronized(_mutex){
            _state = responseMessage.getState();
        }
    }

    if(responseMessage.getEvent() != Rs232Event.None.getValue()){
        _logger.LogDebug("Received event(s): %s", responseMessage.getEvent());
        if(OnEventReported != null) {
            OnEventReported.Invoke(responseMessage.getEvent());
        }
    }

    if(responseMessage.getIsCashboxPresent() && !_wasCashboxAttachmentReported){
        _logger.LogDebug("The cashbox was attached.");
        if(OnCashboxAttached != null){
            OnCashboxAttached.Invoke();
        }

        _wasCashboxAttachmentReported = true;
        _wasCashboxRemovalReported = false;
    }

    if(!responseMessage.getIsCashboxPresent() && !_wasCashboxRemovalReported){
        _logger.LogDebug("The cashbox was removed.");
        if(OnCashboxRemoved != null){
            OnCashboxRemoved.Invoke();
        }

        _wasCashboxAttachmentReported = false;
        _wasCashboxRemovalReported = true;
    }

    if((responseMessage.getEvent() & Rs232Event.Stacked.getValue()) != 0){
        if(responseMessage.getBillType() == 0){
            _logger.LogError("Stacked an unknown bill");
        } else{
            _logger.LogDebug("Stacked a bill of type %d", responseMessage.getBillType());
        }

        if(OnBillStacked != null){
            OnBillStacked.Invoke(responseMessage.getBillType());
        }
    }

    if(responseMessage.getState() == Rs232State.Escrowed && !_wasEscrowedBillReported){
        if(responseMessage.getBillType() == 0){
            _logger.LogError("Escrowed an unknown bill");
        } else{
            _logger.LogDebug("Escrowed a bill of type %d", responseMessage.getBillType());
        }

        if(OnBillEscrowed != null){
            OnBillEscrowed.Invoke(responseMessage.getBillType());
        }
        _wasEscrowedBillReported = true;
    }

    if(responseMessage.getState() != Rs232State.Escrowed){
        synchronized(_mutex){
            _shouldRequestBillStack = false;
            _shouldRequestBillReturn =  false;
        }

        _wasEscrowedBillReported = false;
        _wasBarcodeDetectedReported = false;
    }

    if(responseMessage.MessageType.get().getValue() == Rs232MessageType.ExtendedCommand.getValue()){
        _logger.LogDebug("Received extended command response message.");
        ExtendedResponseMessage extendedResponseMessage = (ExtendedResponseMessage)responseMessage;
        switch (extendedResponseMessage.getCommand()){
            case BarcodeDetected:
                BarcodeDetectedResponseMessage barcodeDetectedResponseMessage = new BarcodeDetectedResponseMessage(extendedResponseMessage.getPayload());
                if(!barcodeDetectedResponseMessage.IsValid){
                    LogPayloadIssues(barcodeDetectedResponseMessage);
                    return false;
                }

                if(!_wasBarcodeDetectedReported){
                    _logger.LogDebug("Detected a barcode: %s", barcodeDetectedResponseMessage.getBarcode());
                    if(OnBarcodeDetected != null){
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
    Mutable<PollResponseMessage> pollResponseMessageMutix  = new Mutable<PollResponseMessage>();

    while(successfulPolls < SuccessfulPollsRequiredToStartPollingLoop){
        MessageRetrievalResult messageRetrievalResult = TrySendMessage(PollRequestMessage::new, PollResponseMessage::new, pollResponseMessageMutix);
        PollResponseMessage pollResponseMessage = pollResponseMessageMutix.value;
        if(messageRetrievalResult.getValue() != MessageRetrievalResult.Success.getValue()){
            if(messageRetrievalResult.getValue() == MessageRetrievalResult.IncorrectPayload.getValue()){
                return false;
            }

            if(wasAckFlipped){
                LogPayloadIssues(pollResponseMessage);
                return false;
            }

            wasAckFlipped = true;
            _lastAck = !_lastAck;
            continue;
        }

        successfulPolls++;
        try{
            Thread.sleep(Configuration.PollingPeriod.toMillis());
        } catch(InterruptedException e){}

    }

    return true;
}

private void LoopPollMessages() {
    _logger.LogDebug("Starting polling loop");
    while(true){
        synchronized(_mutex){
            if(!_isPolling){
                _logger.LogDebug("Received the stop signal");
                return;
            }
        }

        if(_lastMessageCallback != null){
            if(Boolean.TRUE.equals(_lastMessageCallback.get())){
                _lastMessageCallback = null;
            }
        } else {
            Supplier<Boolean> messageCallback = DequeueMessageCallback();
            if(messageCallback != null){
                if(!Boolean.TRUE.equals(messageCallback.get())){
                    _lastMessageCallback = messageCallback;
                }
            } else{
                messageCallback = () -> TrySendPollMessage(ack ->
                        new PollRequestMessage(ack)
                                .SetEnableMask(Configuration.EnableMask)
                                .SetEscrowRequested(Configuration.ShouldEscrow
                                        || _shouldRequestBillStack
                                        || _shouldRequestBillReturn)
                                .SetStackRequested(_shouldRequestBillStack)
                                .SetReturnRequested(_shouldRequestBillReturn)
                                .SetBarcodeDetectionRequested(Configuration.ShouldDetectBarcodes));
                if(!Boolean.TRUE.equals(messageCallback.get())){
                    _lastMessageCallback = messageCallback;
                }
            }
        }

        try{
            Thread.sleep(Configuration.PollingPeriod.toMillis());
        } catch(InterruptedException e){}
    }
}

public enum MessageRetrievalResult{

    Success((byte) 0),
    Timeout((byte) 1),
    IncorrectAck((byte) 2),
    IncorrectPayload((byte) 3);

    private final byte value;
    MessageRetrievalResult(byte value){
        this.value = value;
    }
    public byte getValue() {
        return value;
    }
    public static MessageRetrievalResult fromValue(byte value) {
        for(MessageRetrievalResult result : MessageRetrievalResult.values()){
            if(result.getValue() == value){
                return result;
            }
        }
        throw new IllegalArgumentException("No such MessageRetrievalResult");
    }
}


/// Send Telemetry Request Functions
public CompletableFuture<TelemetryResponseMessage> PingAsync(){
    return SendTelemetryMessageAsync(TelemetryCommand.Ping, new ArrayList<Byte>(), payload -> new TelemetryResponseMessage(payload));
}

public CompletableFuture<GetSerialNumberResponseMessage> GetSerialNumberAsync(){
    return SendTelemetryMessageAsync(TelemetryCommand.GetSerialNumber, new ArrayList<Byte>(), payload -> new GetSerialNumberResponseMessage(payload));
}

public CompletableFuture<GetCashboxMetricsResponseMessage> GetCashboxMetrics(){
    return SendTelemetryMessageAsync(TelemetryCommand.GetCashboxMetrics, new ArrayList<Byte>(), payload -> new GetCashboxMetricsResponseMessage(payload));
}

public CompletableFuture<TelemetryResponseMessage> ClearCashboxCount(){
    return SendTelemetryMessageAsync(TelemetryCommand.ClearCashboxCount, new ArrayList<Byte>(), payload -> new TelemetryResponseMessage(payload));
}

public CompletableFuture<GetUnitMetricsResponseMessage> GetUnitMetrics(){
    return SendTelemetryMessageAsync(TelemetryCommand.GetUnitMetrics, new ArrayList<Byte>(), payload -> new GetUnitMetricsResponseMessage(payload));
}

public CompletableFuture<GetServiceUsageCountersResponseMessage> GetServiceUsageCounters(){
    return SendTelemetryMessageAsync(TelemetryCommand.GetServiceUsageCounters, new ArrayList<Byte>(), payload -> new GetServiceUsageCountersResponseMessage(payload));
}

public CompletableFuture<GetServiceFlagsResponseMessage> GetServiceFlags(){
    return SendTelemetryMessageAsync(TelemetryCommand.GetServiceFlags, new ArrayList<Byte>(), payload -> new GetServiceFlagsResponseMessage(payload));
}

public CompletableFuture<TelemetryResponseMessage> ClearServiceFlags(){
    return SendTelemetryMessageAsync(TelemetryCommand.ClearServiceFlags, new ArrayList<Byte>(), payload -> new TelemetryResponseMessage(payload));
}

public CompletableFuture<GetServiceInfoResponseMessage> GetServiceInfo(){
    return SendTelemetryMessageAsync(TelemetryCommand.GetServiceInfo, new ArrayList<Byte>(), payload -> new GetServiceInfoResponseMessage(payload));
}

public CompletableFuture<GetFirmwareMetricsResponseMessage> GetFirmwareMetrics() {
    return SendTelemetryMessageAsync(TelemetryCommand.GetFirmwareMetrics, new ArrayList<Byte>(), payload -> new GetFirmwareMetricsResponseMessage(payload));
}

public <TResponseMessage extends TelemetryResponseMessage> CompletableFuture<TResponseMessage> SendTelemetryMessageAsync(TelemetryCommand command,
                                                                                                                         List<Byte> requestData,
                                                                                                                         Function<List<Byte>, TResponseMessage> createResponseMessage){
    return SendNonPollMessageAsync(ack -> new TelemetryRequestMessage(ack, command, requestData), createResponseMessage);
}

/// Send Extended Command functions

public CompletableFuture<BarcodeDetectedResponseMessage> GetDetectedBarcode(){
    return SendExtendedMessageAsync(ExtendedCommand.BarcodeDetected, new ArrayList<Byte>(), payload -> new BarcodeDetectedResponseMessage(payload));
}

public <TResponseMessage extends ExtendedResponseMessage> CompletableFuture<TResponseMessage> SendExtendedMessageAsync(ExtendedCommand command,
                                                                                                                       List<Byte> requestData,
                                                                                                                       Function<List<Byte>, TResponseMessage> createResponseMessage){
    return SendNonPollMessageAsync(ack -> new ExtendedRequestMessage(ack, command, requestData), createResponseMessage);
}
}
