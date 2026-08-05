package PTI.Rs232Validator.Messages.Responses.Telemetry;

import PTI.Rs232Validator.Messages.Responses.Rs232ResponseMessage;
import PTI.Rs232Validator.Messages.Rs232MessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An RS-232 message from an acceptor to a host
 * */
public class TelemetryResponseMessage extends Rs232ResponseMessage {

    /**
     * Initializes a new instance of {@link TelemetryResponseMessage}
     */
    public TelemetryResponseMessage(List<Byte> payload) {
        super(payload);

        if(!PayloadIssues.isEmpty()){
            return;
        }

        if(payload.size() < MinPayloadByteSize){
            return;
        }

        if(MessageType.get().getValue() != Rs232MessageType.TelemetryCommand.getValue()){
            PayloadIssues.add(String.format("The message type is %s, but %s is expected",  MessageType.get(), Rs232MessageType.TelemetryCommand));
            return;
        }

        Data = payload
                .stream()
                .skip(3)
                .limit(payload.size() - MinPayloadByteSize).collect(Collectors.toList());
    }

    protected List<Byte> Data = new ArrayList<Byte>();

    /**
     * The data
     */
    public List<Byte> getData() {
        return Data;
    }

}
