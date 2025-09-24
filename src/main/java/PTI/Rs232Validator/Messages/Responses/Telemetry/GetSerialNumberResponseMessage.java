package PTI.Rs232Validator.Messages.Responses.Telemetry;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class GetSerialNumberResponseMessage extends TelemetryResponseMessage {

    private final byte PayloadByteSize = 14;

    public GetSerialNumberResponseMessage(List<Byte> payload) {
        super(payload);

        if(!PayloadIssues.isEmpty()){
            return;
        }

        if(payload.size() != PayloadByteSize){
            PayloadIssues.add(String.format("The payload size is %d bytes, but %d bytes are expected", payload.size(), PayloadByteSize));
            return;
        }

        byte[] data = new byte[Data.size()];
        for(int i = 0; i < Data.size(); i++){
            data[i] = Data.get(i);
        }

        String serialNumber = new String(data, StandardCharsets.US_ASCII);

        for(char c : serialNumber.toCharArray()){
            if(!Character.isDigit(c)){
                PayloadIssues.add(String.format("The data contains a non-digit character: %s", serialNumber));
                return;
            }
        }

        SerialNumber = serialNumber;
    }

    public String SerialNumber = "";
    public String getSerialNumber(){
        return SerialNumber;
    }

    @Override
    public String toString(){
        return IsValid ? String.format("SerialNumber: %s", SerialNumber) : super.toString();
    }

}
