package PTI.Rs232Validator.Messages;

import java.util.List;
import java.util.function.Supplier;

import static PTI.Rs232Validator.Utility.ByteExtensions.IsBitSet;

public abstract class Rs232Message {

    protected final byte MinPayloadByteSize = 5;


    protected static final byte Stx = 0x02;


    protected static final byte Etx = 0x03;


    protected List<Byte> Payload;


    public final Supplier<Boolean> Ack = () -> getPayload().size() >= 3 && IsBitSet(getPayload().get(2), (byte) 0);

    public final Supplier<Rs232MessageType> MessageType = () -> getPayload().size() >= 3 ? Rs232MessageType.fromValue((byte) (getPayload().get(2) & 0b11110000)) : Rs232MessageType.Unknown;


    public abstract List<Byte> getPayload();

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
