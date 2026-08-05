package PTI.Rs232Validator.Messages.Requests;

import PTI.Rs232Validator.Messages.Rs232Message;
import PTI.Rs232Validator.Utility.ByteUtils;

import java.util.List;
import java.util.function.Supplier;

/**
 * An RS-232 message from a host to an acceptor
 */
public abstract class Rs232RequestMessage extends Rs232Message {

    private byte[] _payloadSource;

    /**
     * Initializes a new instance of {@link Rs232RequestMessage}
     * @param payload {@link Rs232Message#Payload Payload}
     */
    protected Rs232RequestMessage(List<Byte> payload) {
        _payloadSource = ByteUtils.convertListToByteArray(payload);
        _payloadSource[_payloadSource.length - 1] = CalculateChecksum(_payloadSource);
    }

    /**
     * The byte collection representing this instance.
     */
    public final Supplier<List<Byte>> Payload = () -> ByteUtils.convertByteArrayToList(_payloadSource);

    /**
     * Returns a string that represents the current object.
     * @return A string that represents the current object
     */
    @Override
    public String toString() {
        return String.format("Ack: %b | Message Type: %s", Ack.get(), MessageType.get().name());
    }

    /**
     * Mutates {@link #Payload} at the specified index and calculates the checksum for the last byte.
     * @param index The index to mutate
     * @param value The value to set at the specified index
     */
    protected void MutatePayload(byte index, byte value){
        if(index >= _payloadSource.length){
            return;
        }

        _payloadSource[index] = value;
        _payloadSource[_payloadSource.length - 1] = CalculateChecksum(_payloadSource);
    }

    @Override
    public List<Byte> getPayload() {
        return Payload.get();
    }


}
