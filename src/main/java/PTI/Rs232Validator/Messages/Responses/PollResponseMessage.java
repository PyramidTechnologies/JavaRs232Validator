package PTI.Rs232Validator.Messages.Responses;

import java.util.*;

import PTI.Rs232Validator.Messages.Rs232MessageType;
import PTI.Rs232Validator.Rs232Event;
import PTI.Rs232Validator.Rs232State;
import PTI.Rs232Validator.Utility.ByteExtensions;
import PTI.Rs232Validator.Utility.Tuple;

import static PTI.Rs232Validator.Utility.ByteExtensions.IsBitSet;

public class PollResponseMessage extends Rs232ResponseMessage{

    private final byte PayloadByteSize = 11;


    protected static final byte StatusByteSize = 6;


    private static final Map<Tuple<Byte, Byte>, Rs232State> StateMap = Map.of(
            new Tuple<>((byte) 0, (byte) 0), Rs232State.Idling,
            new Tuple<>((byte) 0, (byte) 1), Rs232State.Accepting,
            new Tuple<>((byte) 0, (byte) 2), Rs232State.Escrowed,
            new Tuple<>((byte) 0, (byte) 3), Rs232State.Stacking,
            new Tuple<>((byte) 0, (byte) 5), Rs232State.Returning,
            new Tuple<>((byte) 1, (byte) 2), Rs232State.BillJammed,
            new Tuple<>((byte) 1, (byte) 3), Rs232State.StackerFull,
            new Tuple<>((byte) 2, (byte) 2), Rs232State.Failure);


    private static final Map<Tuple<Byte, Byte>, Byte> EventMap = Map.of(
            new Tuple<>((byte) 0, (byte) 4), Rs232Event.Stacked.getValue(),
            new Tuple<>((byte) 0, (byte) 6), Rs232Event.Returned.getValue(),
            new Tuple<>((byte) 1, (byte) 0), Rs232Event.Cheated.getValue(),
            new Tuple<>((byte) 1, (byte) 1), Rs232Event.BillRejected.getValue(),
            new Tuple<>((byte) 2, (byte) 0), Rs232Event.PowerUp.getValue(),
            new Tuple<>((byte) 2, (byte) 1), Rs232Event.InvalidCommand.getValue());


    private static final Map<Byte, Byte[]> ReservedBitIndices = Map.of(
            (byte) 0, new Byte[]{(byte) 7},
            (byte) 1, new Byte[]{(byte) 5, (byte) 6, (byte) 7},
            (byte) 2, new Byte[]{(byte) 6, (byte) 7},
            (byte) 3, new Byte[]{(byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7},
            (byte) 4, new Byte[]{(byte) 7},
            (byte) 5, new Byte[]{(byte) 7});

    public PollResponseMessage(List<Byte> payload) {
        super(payload);

        if(!IsValid){
            return;
        }

        if(payload.size() != PayloadByteSize){
            IsValid = false;
            PayloadIssues.add(String.format("The payload size is %d bytes, but %d bytes are expected.", payload.size(), PayloadByteSize));
            return;
        }

        if(MessageType.get().getValue() != Rs232MessageType.AcceptorToHost.getValue()){
            IsValid = false;
            PayloadIssues.add(String.format("The message type is %s, but %s is expected.", MessageType.get().name(), Rs232MessageType.AcceptorToHost.name()));
        }

        Status = payload
                .stream()
                .skip(3)
                .limit(StatusByteSize)
                .toList();
        DeserializeStatus();
    }



    protected PollResponseMessage(List<Byte> payload, List<Byte> status) {
        super(payload);
        if(!PayloadIssues.isEmpty() | payload.size() < PayloadByteSize | status.size() != StatusByteSize){
            return;
        }

        Status = Collections.unmodifiableList(status);
        DeserializeStatus();
    }

    private Rs232State State;

    public Rs232State getState(){
        return State;
    }


    private byte Event;

    public byte getEvent(){
        return Event;
    }

    private boolean IsCashboxPresent;

    public boolean getIsCashboxPresent(){
        return IsCashboxPresent;
    }

    private byte BillType;

    public byte getBillType(){
        return BillType;
    }

    private byte ModelNumber;

    public byte getModelNumber(){
        return ModelNumber;
    }

    private byte FirmwareRevision;

    public byte getFirmwareRevision(){
        return FirmwareRevision;
    }

    private List<Byte> Status;

    public List<Byte> getStatus(){
        return Status;
    }

    @Override
    public String ToString(){
        return IsValid
                ? String.format(
                "State: %s | " +
                "Event(s): %s | " +
                "Bill Type: %d | " +
                "Model Number: %d | " +
                "Firmware Revision: %d | " +
                "Is Cashbox Present: %b", State.name(), Rs232Event.toFlagString(Event), BillType, ModelNumber, FirmwareRevision, IsCashboxPresent)
                : super.ToString();
    }

    private void DeserializeStatus(){
        List<Rs232State> states = new ArrayList<Rs232State>();
        for(Map.Entry<Tuple<Byte, Byte>, Rs232State> pair : StateMap.entrySet()){
            Byte byteIndex = pair.getKey().x;
            Byte bitIndex = pair.getKey().y;
            if(IsBitSet(Status.get(byteIndex.intValue()), bitIndex)){
                states.add(pair.getValue());
            }
        }

        for(Map.Entry<Tuple<Byte, Byte>, Byte> pair : EventMap.entrySet()){
            Byte byteIndex = pair.getKey().x;
            Byte bitIndex = pair.getKey().y;
            if(IsBitSet(Status.get(byteIndex.intValue()), bitIndex)){
                Event = (byte)(Event | pair.getValue());
            }
        }

        IsCashboxPresent = ByteExtensions.IsBitSet(Status.get(1), (byte)4);
        ModelNumber = Status.get(4);
        FirmwareRevision = Status.get(5);

        for(Map.Entry<Byte, Byte[]> pair : ReservedBitIndices.entrySet()){
            Byte byteIndex = pair.getKey();
            List<Byte> bitIndex = new ArrayList<Byte>(Arrays.asList(pair.getValue()));

            List<Byte> setBitIndex = bitIndex.stream()
                                             .filter(n -> IsBitSet(Status.get(byteIndex.intValue()), n))
                                             .toList();

            if(setBitIndex.isEmpty()){
                continue;
            }

            PayloadIssues.add(String.format("The status byte %d has 1 or more reserved bits set: %s", byteIndex.intValue(), setBitIndex));
        }

        if(states.isEmpty()){
            PayloadIssues.add("The status has no state set.");
        } else if (states.size() > 1) {
            PayloadIssues.add(String.format("The status has more than 1 state set: %s", states));
        } else{
            State = states.get(0);
        }

        BillType = (byte)(Status.get(2) >> 3);
    }

    public List<String> GetPayloadIssues(){
        return PayloadIssues;
    }
}
