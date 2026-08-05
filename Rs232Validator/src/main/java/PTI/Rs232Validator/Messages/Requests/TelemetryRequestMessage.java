package PTI.Rs232Validator.Messages.Requests;

import PTI.Rs232Validator.Messages.Commands.*;
import PTI.Rs232Validator.Messages.Rs232MessageType;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link Rs232RequestMessage} for {@link TelemetryCommand}
 */
public class TelemetryRequestMessage extends Rs232RequestMessage{

    /**
     * Initializes a new instance of {@link TelemetryRequestMessage}
     * @param ack {@link PTI.Rs232Validator.Messages.Rs232Message#Ack Ack}
     * @param command An enumerator of {@link TelemetryCommand}
     * @param data The data
     */
    public TelemetryRequestMessage(boolean ack, TelemetryCommand command, List<Byte> data) {
        super(BuildPayload(ack, command, data));

        Command = command;
    }

    private final TelemetryCommand Command;

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return super.toString() + String.format("Command: %s", Command.name());
    }

    private static List<Byte> BuildPayload(boolean ack, TelemetryCommand command, List<Byte> data) {
        List<Byte> payload = new ArrayList<>();

        payload.add(Stx);
        payload.add((byte) 0);
        payload.add((byte) (Rs232MessageType.TelemetryCommand.getValue() | (ack ? 1 : 0)));
        payload.add(command.getValue());

        payload.addAll(data);
        payload.add(Etx);
        payload.add((byte) 0);
        payload.set(1, (byte) payload.size());

        return payload;
    }
}
