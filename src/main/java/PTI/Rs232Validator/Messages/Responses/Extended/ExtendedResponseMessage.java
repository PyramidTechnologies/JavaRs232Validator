package PTI.Rs232Validator.Messages.Responses.Extended;

import PTI.Rs232Validator.Messages.Commands.ExtendedCommand;
import PTI.Rs232Validator.Messages.Responses.PollResponseMessage;
import PTI.Rs232Validator.Messages.Rs232MessageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExtendedResponseMessage extends PollResponseMessage {

    protected static final byte MinPayloadByteSize = 12;

    public ExtendedResponseMessage(List<Byte> payload) {
        super(payload, GetStatus(payload));

        if(!PayloadIssues.isEmpty()){
            return;
        }

        if(payload.size() < MinPayloadByteSize){
            PayloadIssues.add(String.format("The payload size is %d bytes, but at least %d bytes are expected.",  payload.size(), MinPayloadByteSize));
            return;
        }

        if(MessageType.get().getValue() != Rs232MessageType.ExtendedCommand.getValue()){
            PayloadIssues.add(String.format("The message type is %s, but %s is expected", MessageType.get().name(), Rs232MessageType.ExtendedCommand.name()));
            return;
        }

        Command = ExtendedCommand.fromValue(payload.get(3));

        Data = payload
                .stream()
                .skip(10)
                .limit(payload.size() - MinPayloadByteSize)
                .toList();
    }

    private ExtendedCommand Command;

    public ExtendedCommand getCommand() {
        return Command;
    }

    protected List<Byte> Data = new ArrayList<Byte>();

    public List<Byte> getData() {
        return Data;
    }

    private static List<Byte> GetStatus(List<Byte> payload) {
        if(payload.size() < MinPayloadByteSize){
            return Collections.emptyList();
        }

        return payload
                .stream()
                .skip(4)
                .limit(StatusByteSize)
                .toList();
    }
}
