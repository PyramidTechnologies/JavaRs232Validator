package PTI.Rs232Validator.Messages.Responses.Telemetry;

import PTI.Rs232Validator.Utility.ByteUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * An RS-232 message from an acceptor to a host for {@link PTI.Rs232Validator.Messages.Commands.TelemetryCommand#GetServiceInfo GetServiceInfo}
 */
public class GetServiceInfoResponseMessage extends TelemetryResponseMessage{

    private final byte PayloadByteSize = 17;

    /**
     * Initializes a new instance of {@link GetServiceInfoResponseMessage}
     */
    public GetServiceInfoResponseMessage(List<Byte> payload) {
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
        LastCustomerService = ByteUtils.ClearEighthBits(Data.subList(0, 4));
        LastServiceCenterService = ByteUtils.ClearEighthBits(Data.subList(4, 8));
        LastOemService = ByteUtils.ClearEighthBits(Data.subList(8, 12));
    }

    private List<Byte> LastCustomerService = new ArrayList<Byte>();

    /**
     * The 4 bytes of custom data that a customer wrote to an acceptor on the last service.
     */
    public List<Byte> getLastCustomerService(){
        return LastCustomerService;
    }

    private List<Byte> LastServiceCenterService = new ArrayList<Byte>();

    /**
     * The 4 bytes of custom data that a service center wrote to an acceptor on the last service.
     */
    public List<Byte> getLastServiceCenterService(){
        return LastServiceCenterService;
    }

    private List<Byte> LastOemService = new ArrayList<Byte>();

    /**
     * The 4 bytes of custom data that an OEM wrote to an acceptor on the last service.
     */
    public List<Byte> getLastOemService(){
        return LastOemService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return IsValid.get()
                ? String.format("Last Customer Service: " + ByteUtils.ConvertToHexString(LastCustomerService, true, false) + " | " +
                    "Last Service Center Service: " + ByteUtils.ConvertToHexString(LastServiceCenterService, true, false) + " | " +
                    "Last Oem Service: " + ByteUtils.ConvertToHexString(LastOemService, true, false))
                : super.toString();
    }
}
