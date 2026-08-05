package PTI.Rs232Validator.Messages.Responses.Telemetry;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * An RS-232 message from an acceptor to a host for {@link PTI.Rs232Validator.Messages.Commands.TelemetryCommand#GetSerialNumber GetSerialNumber}
 */
public class GetSerialNumberResponseMessage extends TelemetryResponseMessage {

    private final byte PayloadByteSize = 14;

    /**
     * Initializes a new instance of {@link GetSerialNumberResponseMessage}
     */
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

    private String SerialNumber = "";

    /**
     * The serial number of an acceptor
     * @implNote If the string is empty, then the acceptor was not assigned a serial number
     */
    public String getSerialNumber(){
        return SerialNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString(){
        return IsValid.get() ? String.format("SerialNumber: %s", SerialNumber) : super.toString();
    }

}
