package PTI.Rs232Validator.Messages.Responses.Telemetry;

import PTI.Rs232Validator.Utility.ByteExtensions;

import java.util.List;

public class GetServiceUsageCountersResponseMessage extends TelemetryResponseMessage{

    private final byte PayloadByteSize = 53;

    public GetServiceUsageCountersResponseMessage(List<Byte> payload) {
        super(payload);

        if(!PayloadIssues.isEmpty()){
            return;
        }

        if(payload.size() != PayloadByteSize){
            PayloadIssues.add(String.format("The payload size is %d bytes, but %d bytes are expected", payload.size(), PayloadByteSize));
            return;
        }

        DistanceMovedSinceLastTachSensorService = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(0, 8));
        DistanceMovedSinceLastBillPathService = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(8, 16));
        DistanceMovedSinceLastBeltService = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(16, 24));
        BillsStackedSinceLastCashboxService = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(24, 32));
        DistanceMovedSinceLastMasService = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(32, 40));
        DistanceMovedSinceLastSpringRollerService = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(40, 48));
    }

    private long DistanceMovedSinceLastTachSensorService;
    public long getDistanceMovedSinceLastTachSensorService(){
        return DistanceMovedSinceLastTachSensorService;
    }

    private long DistanceMovedSinceLastBillPathService;
    public long getDistanceMovedSinceLastBillPathService(){
        return DistanceMovedSinceLastBillPathService;
    }

    private long DistanceMovedSinceLastBeltService;
    public long getDistanceMovedSinceLastBeltService(){
        return DistanceMovedSinceLastBeltService;
    }

    private long BillsStackedSinceLastCashboxService;
    public long getBillsStackedSinceLastCashboxService(){
        return BillsStackedSinceLastCashboxService;
    }

    private long DistanceMovedSinceLastMasService;
    public long getDistanceMovedSinceLastMasService(){
        return DistanceMovedSinceLastMasService;
    }

    public long DistanceMovedSinceLastSpringRollerService;
    public long getDistanceMovedSinceLastSpringRollerService(){
        return DistanceMovedSinceLastSpringRollerService;
    }

    @Override
    public String toString() {
        return IsValid
                ? String.format("Distance Moved Since Last Tach Sensor Service: %d | " +
                    "Distance Moved Since Last Bill Path Service: %d | " +
                    "Distance Moved Since Last Belt Service: %d | " +
                    "Bills Stacked Since Last Cashbox Service: %d | " +
                    "Distance Moved Since Last Mas Service: %d | " +
                    "Distance Moved Since Last Spring Roller Service: %d",
                    DistanceMovedSinceLastTachSensorService,
                    DistanceMovedSinceLastBillPathService,
                    DistanceMovedSinceLastBeltService,
                    BillsStackedSinceLastCashboxService,
                    DistanceMovedSinceLastMasService,
                    DistanceMovedSinceLastSpringRollerService)
                : super.toString();
    }
}
