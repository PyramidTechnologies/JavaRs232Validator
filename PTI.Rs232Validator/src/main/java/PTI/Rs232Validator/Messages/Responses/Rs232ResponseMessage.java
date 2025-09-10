package PTI.Rs232Validator.Messages.Responses;

import PTI.Rs232Validator.Messages.Rs232Message;
import PTI.Rs232Validator.Messages.Rs232MessageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static PTI.Rs232Validator.Utility.StringExtensions.AddSpacesToCamelCase;

public abstract class Rs232ResponseMessage extends Rs232Message {


    public boolean IsValid = true;


    protected List<String> PayloadIssues = new ArrayList<String>();



    protected Rs232ResponseMessage(List<Byte> payload) {
        Payload = payload;

        if(payload.isEmpty()){
            IsValid = false;
            PayloadIssues.add("The payload is empty");
            return;
        }

        if(payload.size() < MinPayloadByteSize){
            IsValid = false;
            PayloadIssues.add("The payload size is " + payload.size() + " bytes, but at least " +  MinPayloadByteSize + " bytes are expected.");
            return;
        }

        if(payload.get(0) != Stx){
            IsValid = false;
            PayloadIssues.add(String.format("The payload starts with 0x%s, but 0x%s is expected", Integer.toHexString(payload.get(0)), Integer.toHexString(Stx)));
        }

        if(payload.get(1) != payload.size()){
            IsValid = false;
            PayloadIssues.add(String.format("The payload size is %d bytes, but the payload reported a size of %d bytes", payload.size(), (int) payload.get(1)));
        }

        if(MessageType.get().getValue() == Rs232MessageType.HostToAcceptor.getValue()){
            IsValid = false;
            PayloadIssues.add(String.format("The message type is %s, which should never occur", MessageType.get().name()));
        }

        if(payload.get(payload.size() - 2) != Etx){
            IsValid = false;
            PayloadIssues.add(String.format("The payload ends with 0x%s, but 0x%s is expected", Integer.toHexString(payload.get(payload.size() - 2)), Integer.toHexString(Etx)));
        }

        byte actualChecksum = payload.get(payload.size() - 1);
        byte expected = CalculateChecksum(payload);
        if(actualChecksum != expected){
            IsValid = false;
            PayloadIssues.add(String.format("The payload has a checksum of 0x%s, but 0x%s is expected",  Integer.toHexString(actualChecksum), Integer.toHexString(expected)));
        }
    }

    @Override
    public List<Byte> getPayload() {
        return Collections.unmodifiableList(Payload);
    }

    public String ToString(){
        return IsValid
                ? String.format("Valid %s", AddSpacesToCamelCase(this.getClass().getName()))
                : String.format("Invalid %s", AddSpacesToCamelCase(this.getClass().getName()));
    }


    public List<String> getPayloadIssues() {
        return Collections.unmodifiableList(PayloadIssues);
    }

}
