package PTI.Rs232Validator.Messages.Responses.Telemetry;

import PTI.Rs232Validator.Utility.ByteUtils;

import java.util.List;

/**
 * An RS-232 message from an acceptor to a host for {@link PTI.Rs232Validator.Messages.Commands.TelemetryCommand#GetCashboxMetrics GetCashboxMetrics}
 */
public class GetCashboxMetricsResponseMessage extends TelemetryResponseMessage{

    private final byte PayloadByteSize = 53;

    /**
     * Initializes a new instance of {@link GetCashboxMetricsResponseMessage}
     */
    public GetCashboxMetricsResponseMessage(List<Byte> payload) {
        super(payload);

        if(!PayloadIssues.isEmpty()){
            return;
        }

        if(payload.size() != PayloadByteSize){
            PayloadIssues.add(String.format("The payload size is %d bytes, but %d bytes are expected", payload.size(), PayloadByteSize));
            return;
        }

        CashboxRemovedCount = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(0, 8));
        CashboxFullCount = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(8, 16));
        BillsStackedSinceCashboxRemoved = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(16, 24));
        BillsStackedSincePowerUp = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(24, 32));
        AverageTimeToStack = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(32, 40));
        TotalBillsStacked = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(40, 48));


    }


    private long CashboxRemovedCount;

    /**
     * Returns The number of times the cashbox has been removed
     */
    public long getCashboxRemovedCount()
    {
        return CashboxRemovedCount;
    }

    private long CashboxFullCount;

    /**
     * Returns The number of times the cashbox has been full
     */
    public long getCashboxFullCount()
    {
        return CashboxFullCount;
    }

    private long BillsStackedSinceCashboxRemoved;

    /**
     * The count of bills stacked since the cashbox was last removed
     */
    public long getBillsStackedSinceCashboxRemoved()
    {
        return BillsStackedSinceCashboxRemoved;
    }

    private long BillsStackedSincePowerUp;

    /**
     * The count of bills stacked since the unit has been powered.
     */
    public long getBillsStackedSincePowerUp() {
        return BillsStackedSincePowerUp;
    }

    private long AverageTimeToStack;

    /**
     * The average time, in milliseconds, it takes to stack a bill.
     */
    public long getAverageTimeToStack()
    {
        return AverageTimeToStack;
    }

    private long TotalBillsStacked;

    /**
     * The total number of bills put in the cashbox for the lifetime of the unit.
     */
    public long getTotalBillsStacked()
    {
        return TotalBillsStacked;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return IsValid.get()
                ? String.format("Cashbox Removed Count: %d | " +
                    "Cashbox Full Count: %d | " +
                    "Bills Stacked Since Cashbox Removed: %d | " +
                    "Bills Stacked Since Power Up: %d | " +
                    "Average Time to Stack: %d | " +
                    "Total Bills Stacked: %d", CashboxRemovedCount, CashboxFullCount, BillsStackedSinceCashboxRemoved, BillsStackedSincePowerUp, AverageTimeToStack, TotalBillsStacked)
                : super.toString();
    }
}
