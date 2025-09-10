package PTI.Rs232Validator.Messages.Requests;

import PTI.Rs232Validator.Messages.Rs232MessageType;
import PTI.Rs232Validator.Utility.ByteExtensions;

import java.util.ArrayList;
import java.util.List;

public class PollRequestMessage extends Rs232RequestMessage{


    public PollRequestMessage(boolean ack) {
        super(BuildPayload(ack));
    }

    private byte AcceptanceMask;

    private boolean IsEscrowRequested;

    private boolean IsStackRequested;

    private boolean IsReturnRequested;

    private boolean IsBarcodeDetectionRequested;


    @Override
    public String toString(){
        return super.toString() + " | " +
                String.format("Ack: %b | ", Ack.get()) +
                String.format("Acceptance Mask: %s | ", ByteExtensions.ConvertToBinaryString(AcceptanceMask, true)) +
                String.format("Is Escrow Requested: %b | ", IsEscrowRequested) +
                String.format("Is Stack Requested: %b | ", IsStackRequested) +
                String.format("Is Return Requested: %b | ", IsReturnRequested) +
                String.format("Is Barcode Detection Requested: %b", IsBarcodeDetectionRequested);
    }

    public PollRequestMessage SetEnableMask(byte acceptanceMask){
        AcceptanceMask = acceptanceMask;
        MutatePayload((byte)3, (byte)(acceptanceMask & 0x7F));
        return this;
    }

    public PollRequestMessage SetEscrowRequested(boolean isEscrowRequested){
        IsEscrowRequested = isEscrowRequested;
        MutatePayload((byte)4, isEscrowRequested ? ByteExtensions.SetBit(Payload.get().get(4), (byte) 4) : ByteExtensions.ClearBit(Payload.get().get(4), (byte) 4));
        return this;
    }


    public PollRequestMessage SetStackRequested(boolean isStackRequested){
        IsStackRequested = isStackRequested;
        MutatePayload((byte) 4, isStackRequested ? ByteExtensions.SetBit(Payload.get().get(4), (byte) 5) : ByteExtensions.ClearBit(Payload.get().get(4), (byte) 5));
        return this;
    }

    public PollRequestMessage SetReturnRequested(boolean isReturnRequested){
        IsReturnRequested = isReturnRequested;
        MutatePayload((byte) 4, isReturnRequested ? ByteExtensions.SetBit(Payload.get().get(4), (byte) 6) : ByteExtensions.ClearBit(Payload.get().get(4), (byte) 6));
        return this;
    }


    public PollRequestMessage SetBarcodeDetectionRequested(boolean isBarcodeDetectionRequested){
        IsBarcodeDetectionRequested = isBarcodeDetectionRequested;
        MutatePayload((byte) 4, isBarcodeDetectionRequested ? ByteExtensions.SetBit(Payload.get().get(5), (byte) 1) : ByteExtensions.ClearBit(Payload.get().get(5), (byte) 1));
        return this;
    }


    private static List<Byte> BuildPayload(boolean ack){
        List<Byte> payload = new ArrayList<>();
        payload.add(Stx);
        payload.add((byte) 8);
        payload.add((byte) (Rs232MessageType.HostToAcceptor.getValue() | (ack ? 1 : 0)));
        payload.add((byte) 0);
        payload.add((byte) 0);
        payload.add((byte) 0);
        payload.add(Etx);
        payload.add((byte) 0);
        return payload;
    }

}
