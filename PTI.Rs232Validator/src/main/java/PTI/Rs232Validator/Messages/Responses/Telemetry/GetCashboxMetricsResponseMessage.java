package PTI.Rs232Validator.Messages.Responses.Telemetry;

import PTI.Rs232Validator.Utility.ByteExtensions;

import java.util.List;

public class GetCashboxMetricsResponseMessage extends TelemetryResponseMessage{

    private final byte PayloadByteSize = 53;

    public GetCashboxMetricsResponseMessage(List<Byte> payload) {
        super(payload);

        if(!PayloadIssues.isEmpty()){
            return;
        }

        if(payload.size() != PayloadByteSize){
            PayloadIssues.add(String.format("The payload size is %d bytes, but %d bytes are expected", payload.size(), PayloadByteSize));
            return;
        }

        CashboxRemovedCount = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(0, 8));
        CashboxFullCount = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(8, 16));
        BillsStackedSinceCashboxRemoved = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(16, 24));
        BillsStackedSincePowerUp = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(24, 32));
        AverageTimeToStack = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(32, 40));
        TotalBillsStacked = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(40, 48));


    }

    private long CashboxRemovedCount;
    public long getCashboxRemovedCount()
    {
        return CashboxRemovedCount;
    }

    private long CashboxFullCount;
    public long getCashboxFullCount()
    {
        return CashboxFullCount;
    }

    private long BillsStackedSinceCashboxRemoved;
    public long getBillsStackedSinceCashboxRemoved()
    {
        return BillsStackedSinceCashboxRemoved;
    }

    private long BillsStackedSincePowerUp;

    public long getBillsStackedSincePowerUp() {
        return BillsStackedSincePowerUp;
    }

    private long AverageTimeToStack;
    public long getAverageTimeToStack()
    {
        return AverageTimeToStack;
    }

    private long TotalBillsStacked;
    public long getTotalBillsStacked()
    {
        return TotalBillsStacked;
    }

    @Override
    public String toString() {
        return IsValid
                ? String.format("Cashbox Removed Count: %d | " +
                    "Cashbox Full Count: %d | " +
                    "Bills Stacked Since Cashbox Removed: %d | " +
                    "Bills Stacked Since Power Up: %d | " +
                    "Average Time to Stack: %d | " +
                    "Total Bills Stacked: %d", CashboxRemovedCount, CashboxFullCount, BillsStackedSinceCashboxRemoved, BillsStackedSincePowerUp, AverageTimeToStack, TotalBillsStacked)
                : super.toString();
    }
}
