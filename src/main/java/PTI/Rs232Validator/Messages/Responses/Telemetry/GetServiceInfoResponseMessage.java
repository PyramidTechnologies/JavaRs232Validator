package PTI.Rs232Validator.Messages.Responses.Telemetry;

import PTI.Rs232Validator.Utility.ByteExtensions;

import java.util.ArrayList;
import java.util.List;

public class GetServiceInfoResponseMessage extends TelemetryResponseMessage{

    private final byte PayloadByteSize = 17;

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
        LastCustomerService = ByteExtensions.ClearEighthBits(Data.subList(0, 4));
        LastServiceCenterService = ByteExtensions.ClearEighthBits(Data.subList(4, 8));
        LastOemService = ByteExtensions.ClearEighthBits(Data.subList(8, 12));
    }

    private List<Byte> LastCustomerService = new ArrayList<Byte>();
    public List<Byte> getLastCustomerService(){
        return LastCustomerService;
    }

    private List<Byte> LastServiceCenterService = new ArrayList<Byte>();
    public List<Byte> getLastServiceCenterService(){
        return LastServiceCenterService;
    }

    private List<Byte> LastOemService = new ArrayList<Byte>();
    public List<Byte> getLastOemService(){
        return LastOemService;
    }

    @Override
    public String toString() {
        return IsValid
                ? String.format("Last Customer Service: " + ByteExtensions.ConvertToHexString(LastCustomerService, true, false) + " | " +
                    "Last Service Center Service: " + ByteExtensions.ConvertToHexString(LastServiceCenterService, true, false) + " | " +
                    "Last Oem Service: " + ByteExtensions.ConvertToHexString(LastOemService, true, false))
                : super.toString();
    }
}
