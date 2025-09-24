package PTI.Rs232Validator.Messages.Requests;

import PTI.Rs232Validator.Messages.Rs232Message;
import PTI.Rs232Validator.Utility.ByteExtensions;

import java.util.List;
import java.util.function.Supplier;

public abstract class Rs232RequestMessage extends Rs232Message {

    private byte[] _payloadSource;

    protected Rs232RequestMessage(List<Byte> payload) {
        _payloadSource = ByteExtensions.convertListToByteArray(payload);
        _payloadSource[_payloadSource.length - 1] = CalculateChecksum(_payloadSource);
    }

    public final Supplier<List<Byte>> Payload = () -> ByteExtensions.convertByteArrayToList(_payloadSource);

    @Override
    public String toString() {
        return String.format("Ack: %b | Message Type: %s", Ack.get(), MessageType.get().name());
    }

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
