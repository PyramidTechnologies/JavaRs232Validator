package PTI.Rs232Validator.Messages;

import java.util.List;
import java.util.function.Supplier;

import static PTI.Rs232Validator.Utility.ByteUtils.IsBitSet;

/**
 * An Rs-232 message.
 * Each message contains a message type and an ACK number
 */
public abstract class Rs232Message {

    /**
     * The minimum payload size in bytes.
     */
    protected final byte MinPayloadByteSize = 5;

    /**
     * The start of a message payload.
     */
    protected static final byte Stx = 0x02;

    /**
     * The end of a message payload.
     */
    protected static final byte Etx = 0x03;

    /**
     * The byte collection representing this instance.
     */
    public List<Byte> Payload;

    /**
     * The ACK number
     * @implNote {@code false} = 0; {@code true} = 1
     */
    public final Supplier<Boolean> Ack = () -> getPayload().size() >= 3 && IsBitSet(getPayload().get(2), (byte) 0);

    /**
     * An enumerator of {@link Rs232MessageType}
     */
    public final Supplier<Rs232MessageType> MessageType = () -> getPayload().size() >= 3 ? Rs232MessageType.fromValue((byte) (getPayload().get(2) & 0b11110000)) : Rs232MessageType.Unknown;

    /**
     * A method that returns the payload
     * @return {@link #Payload}
     */
    public abstract List<Byte> getPayload();

    /**
     * Calculates the 1-byte XOR checksum of the specified payload in the form of a list
     * @param payload The payload to calculate the checksum of
     * @return The checksum
     */
    protected byte CalculateChecksum(List<Byte> payload) {
        if(payload.size() < MinPayloadByteSize) {
            return 0;
        }

        byte checksum = 0;
        for(int i = 1; i < payload.size() - 2; i++) {
            checksum ^= payload.get(i);
        }
        return checksum;
    }

    /**
     * Calculates the 1-byte XOR checksum of the specified payload in the form of an array
     * @param payload The payload to calculate the checksum of
     * @return The checksum
     */
    protected byte CalculateChecksum(byte[] payload) {
        if(payload.length < MinPayloadByteSize) {
            return 0;
        }

        byte checksum = 0;
        for(int i = 1; i < payload.length - 2; i++) {
            checksum ^= payload[i];
        }
        return checksum;
    }


}
