package PTI.Rs232Validator.Messages.Requests;

import PTI.Rs232Validator.Messages.Commands.ExtendedCommand;
import PTI.Rs232Validator.Messages.Rs232MessageType;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link Rs232RequestMessage} for {@link ExtendedCommand}
 */
public class ExtendedRequestMessage extends Rs232RequestMessage {

    /**
     * Initializes a new instance of {@link ExtendedRequestMessage}
     * @param ack {@link PTI.Rs232Validator.Messages.Rs232Message#Ack Ack}
     * @param command An enumerator of {@link ExtendedCommand}
     * @param data The data
     */
    public ExtendedRequestMessage(boolean ack, ExtendedCommand command, List<Byte> data) {
        super(BuildPayload(ack, command, data));

        Command = command;
    }

    private final ExtendedCommand Command;

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.toString() + String.format("Command: %s", Command);
    }

    private static List<Byte> BuildPayload(boolean ack, ExtendedCommand command, List<Byte> data) {

        List<Byte> payload = new ArrayList<>();
        payload.add(Stx);
        payload.add((byte) 0);
        payload.add((byte)(Rs232MessageType.ExtendedCommand.getValue() | (ack ? 1 : 0)));
        payload.add(command.getValue());

        payload.addAll(data);
        payload.add(Etx);
        payload.add((byte) 0);
        payload.set(1, (byte) payload.size());

        return payload;
    }
}
