package PTI.Rs232Validator.Messages.Responses.Telemetry;

import PTI.Rs232Validator.Utility.ByteUtils;

import java.util.List;

/**
 * An RS-232 message from an acceptor to a host for {@link PTI.Rs232Validator.Messages.Commands.TelemetryCommand#GetServiceUsageCounters GetServiceUsageCounters}
 */
public class GetServiceUsageCountersResponseMessage extends TelemetryResponseMessage{

    private final byte PayloadByteSize = 53;

    /**
     * Initializes a new instance of {@link GetServiceUsageCountersResponseMessage}
     */
    public GetServiceUsageCountersResponseMessage(List<Byte> payload) {
        super(payload);

        if(!PayloadIssues.isEmpty()){
            return;
        }

        if(payload.size() != PayloadByteSize){
            PayloadIssues.add(String.format("The payload size is %d bytes, but %d bytes are expected", payload.size(), PayloadByteSize));
            return;
        }

        DistanceMovedSinceLastTachSensorService = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(0, 8));
        DistanceMovedSinceLastBillPathService = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(8, 16));
        DistanceMovedSinceLastBeltService = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(16, 24));
        BillsStackedSinceLastCashboxService = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(24, 32));
        DistanceMovedSinceLastMasService = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(32, 40));
        DistanceMovedSinceLastSpringRollerService = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(40, 48));
    }

    private long DistanceMovedSinceLastTachSensorService;

    /**
     * The total amount of movement since the last tach sensor service in mm.
     */
    public long getDistanceMovedSinceLastTachSensorService(){
        return DistanceMovedSinceLastTachSensorService;
    }

    private long DistanceMovedSinceLastBillPathService;

    /**
     * The total amount of movement since the last bill path service in mm.
     */
    public long getDistanceMovedSinceLastBillPathService(){
        return DistanceMovedSinceLastBillPathService;
    }

    private long DistanceMovedSinceLastBeltService;

    /**
     * The total amount of movement since the last belt service in mm.
     */
    public long getDistanceMovedSinceLastBeltService(){
        return DistanceMovedSinceLastBeltService;
    }

    private long BillsStackedSinceLastCashboxService;

    /**
     * The total amount of bills stacked since the last cashbox mechanism service in mm.
     */
    public long getBillsStackedSinceLastCashboxService(){
        return BillsStackedSinceLastCashboxService;
    }

    private long DistanceMovedSinceLastMasService;

    /**
     * The total amount of movement since the last mechanical anti-stringing lever (MAS) service in mm.
     */
    public long getDistanceMovedSinceLastMasService(){
        return DistanceMovedSinceLastMasService;
    }

    public long DistanceMovedSinceLastSpringRollerService;

    /**
     * The total amount of movement since the last spring roller service in mm.
     */
    public long getDistanceMovedSinceLastSpringRollerService(){
        return DistanceMovedSinceLastSpringRollerService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return IsValid.get()
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
