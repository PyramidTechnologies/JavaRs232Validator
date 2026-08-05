package PTI.Rs232Validator.Messages.Requests;

import PTI.Rs232Validator.Messages.Rs232MessageType;
import PTI.Rs232Validator.Utility.ByteUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link Rs232RequestMessage} for polling an acceptor
 */
public class PollRequestMessage extends Rs232RequestMessage{

    /**
     * A new instance of {@link PollRequestMessage}
     * @param ack {@link PTI.Rs232Validator.Messages.Rs232Message#Ack Ack}
     */
    public PollRequestMessage(boolean ack) {
        super(BuildPayload(ack));
    }


    private byte AcceptanceMask;

    private boolean IsEscrowRequested;

    private boolean IsStackRequested;

    private boolean IsReturnRequested;

    private boolean IsBarcodeDetectionRequested;

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString(){
        return super.toString() + " | " +
                String.format("Ack: %b | ", Ack.get()) +
                String.format("Acceptance Mask: %s | ", ByteUtils.ConvertToBinaryString(AcceptanceMask, true)) +
                String.format("Is Escrow Requested: %b | ", IsEscrowRequested) +
                String.format("Is Stack Requested: %b | ", IsStackRequested) +
                String.format("Is Return Requested: %b | ", IsReturnRequested) +
                String.format("Is Barcode Detection Requested: %b", IsBarcodeDetectionRequested);
    }

    /**
     * Sets the enable mask, which represents types of bills to accept
     * @param acceptanceMask The new acceptance mask
     * @return This instance
     * @implNote
     * 0b00000001: only accept the 1st bill type (e.g. $1).<br>
     * 0b00000010: only accept the 2nd bill type (e.g. $2).<br>
     * 0b00000100: only accept the 3rd bill type (e.g. $5).<br>
     * 0b00001000: only accept the 4th bill type (e.g. $10).<br>
     * 0b00010000: only accept the 5th bill type (e.g. $20).<br>
     * 0b00100000: only accept the 6th bill type (e.g. $50).<br>
     * 0b01000000: only accept the 7th bill type (e.g. $100).<br>
     */
    public PollRequestMessage SetEnableMask(byte acceptanceMask){
        AcceptanceMask = acceptanceMask;
        MutatePayload((byte)3, (byte)(acceptanceMask & 0x7F));
        return this;
    }

    /**
     * Sets whether to request a bill to be escrowed
     * @param isEscrowRequested {@code true} to request a bill escrow
     * @return This instance
     */
    public PollRequestMessage SetEscrowRequested(boolean isEscrowRequested){
        IsEscrowRequested = isEscrowRequested;
        MutatePayload((byte)4, isEscrowRequested ? ByteUtils.SetBit(getPayload().get(4), (byte) 4) : ByteUtils.ClearBit(getPayload().get(4), (byte) 4));
        return this;
    }

    /**
     * Sets whether to request a bill to be stacked
     * @param isStackRequested {@code true} to request a bill to be stacked
     * @return This instance
     */
    public PollRequestMessage SetStackRequested(boolean isStackRequested){
        IsStackRequested = isStackRequested;
        MutatePayload((byte) 4, isStackRequested ? ByteUtils.SetBit(getPayload().get(4), (byte) 5) : ByteUtils.ClearBit(getPayload().get(4), (byte) 5));
        return this;
    }

    /**
     * Sets whether to request a bill to be returned
     * @param isReturnRequested {@code true} to request a bill to be returned
     * @return This instance
     * @implNote This method is only relevant if a bill is in escrow
     */
    public PollRequestMessage SetReturnRequested(boolean isReturnRequested){
        IsReturnRequested = isReturnRequested;
        MutatePayload((byte) 4, isReturnRequested ? ByteUtils.SetBit(getPayload().get(4), (byte) 6) : ByteUtils.ClearBit(getPayload().get(4), (byte) 6));
        return this;
    }

    /**
     * Sets whether to request barcode detection
     * @param isBarcodeDetectionRequested {@code true} to request barcode detection
     * @return This instance
     */
    public PollRequestMessage SetBarcodeDetectionRequested(boolean isBarcodeDetectionRequested){
        IsBarcodeDetectionRequested = isBarcodeDetectionRequested;
        MutatePayload((byte) 5, isBarcodeDetectionRequested ? ByteUtils.SetBit(getPayload().get(5), (byte) 1) : ByteUtils.ClearBit(getPayload().get(5), (byte) 1));
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
