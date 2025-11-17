package PTI.Rs232Validator.Messages.Responses;

import PTI.Rs232Validator.Messages.Rs232Message;
import PTI.Rs232Validator.Messages.Rs232MessageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static PTI.Rs232Validator.Utility.StringUtils.AddSpacesToCamelCase;

/**
 * An RS-232 message from an acceptor to a host
 */
public abstract class Rs232ResponseMessage extends Rs232Message {

    /**
     * Initializes a new instance of {@link Rs232ResponseMessage}
     */
    protected Rs232ResponseMessage(List<Byte> payload) {
        Payload = payload;

        if(payload.isEmpty()){
            PayloadIssues.add("The payload is empty");
            return;
        }

        if(payload.size() < MinPayloadByteSize){
            PayloadIssues.add("The payload size is " + payload.size() + " bytes, but at least " +  MinPayloadByteSize + " bytes are expected.");
            return;
        }

        if(payload.get(0) != Stx){
            PayloadIssues.add(String.format("The payload starts with 0x%s, but 0x%s is expected", Integer.toHexString(payload.get(0)), Integer.toHexString(Stx)));
        }

        if(payload.get(1) != payload.size()){
            PayloadIssues.add(String.format("The payload size is %d bytes, but the payload reported a size of %d bytes", payload.size(), (int) payload.get(1)));
        }

        if(MessageType.get().getValue() == Rs232MessageType.HostToAcceptor.getValue()){;
            PayloadIssues.add(String.format("The message type is %s, which should never occur", MessageType.get().name()));
        }

        if(payload.get(payload.size() - 2) != Etx){
            PayloadIssues.add(String.format("The payload ends with 0x%s, but 0x%s is expected", Integer.toHexString(payload.get(payload.size() - 2)), Integer.toHexString(Etx)));
        }

        byte actualChecksum = payload.get(payload.size() - 1);
        byte expected = CalculateChecksum(payload);
        if(actualChecksum != expected){
            PayloadIssues.add(String.format("The payload has a checksum of 0x%s, but 0x%s is expected",  Integer.toHexString(actualChecksum), Integer.toHexString(expected)));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Byte> getPayload() {
        return Collections.unmodifiableList(Payload);
    }

    /**
     * A collection of issues with {@link Rs232Message#Payload Payload}
     */
    protected List<String> PayloadIssues = new ArrayList<String>();

    /**
     * Is this instance valid (i.e. are there no issues with {@link Rs232Message#Payload Payload})?
     */
    public Supplier<Boolean> IsValid = () -> PayloadIssues.isEmpty();

    /**
     * Returns a string that represents the current object.
     * @return A string that represents the current object
     */
    @Override
    public String toString(){
        return IsValid.get()
                ? String.format("Valid %s", AddSpacesToCamelCase(this.getClass().getName()))
                : String.format("Invalid %s", AddSpacesToCamelCase(this.getClass().getName()));
    }

    /**
     * Gets the issues with {@link Rs232Message#Payload Payload}
     */
    public List<String> getPayloadIssues() {
        return Collections.unmodifiableList(PayloadIssues);
    }

}
