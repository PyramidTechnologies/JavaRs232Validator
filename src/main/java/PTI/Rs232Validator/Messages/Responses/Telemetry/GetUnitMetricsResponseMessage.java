package PTI.Rs232Validator.Messages.Responses.Telemetry;

import PTI.Rs232Validator.Utility.ByteExtensions;

import java.util.List;

public class GetUnitMetricsResponseMessage extends TelemetryResponseMessage{

    private final byte PayloadByteSize = 69;

    public GetUnitMetricsResponseMessage(List<Byte> payload) {
        super(payload);

        if(!PayloadIssues.isEmpty()){
            return;
        }

        if(payload.size() != PayloadByteSize){
            PayloadIssues.add(String.format("The payload size is %d bytes, but %d bytes are expected", payload.size(), PayloadByteSize));
            return;
        }

        TotalValueStacked = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(0, 8));
        TotalDistanceMoved = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(8, 16));
        PowerUpCount = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(16, 24));
        PushButtonCount = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(24, 32));
        ConfigurationCount = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(32, 40));
        UsbEnumerationsCount = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(40, 48));
        TotalCheatAttemptsDetected = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(48, 56));
        TotalSecurityLockupCount = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(56, 64));
    }

    private long TotalValueStacked;
    public long getTotalValueStacked() {
        return TotalValueStacked;
    }

    private long TotalDistanceMoved;
    public long getTotalDistanceMoved() {
        return TotalDistanceMoved;
    }

    private long PowerUpCount;
    public long getPowerUpCount() {
        return PowerUpCount;
    }

    private long PushButtonCount;
    public long getPushButtonCount() {
        return PushButtonCount;
    }

    private long ConfigurationCount;
    public long getConfigurationCount() {
        return ConfigurationCount;
    }

    private long UsbEnumerationsCount;
    public long getUsbEnumerationsCount() {
        return UsbEnumerationsCount;
    }

    private long TotalCheatAttemptsDetected;
    public long getTotalCheatAttemptsDetected() {
        return TotalCheatAttemptsDetected;
    }

    private long TotalSecurityLockupCount;
    public long getTotalSecurityLockupCount() {
        return TotalSecurityLockupCount;
    }

    @Override
    public String toString() {
        return IsValid
                ? String.format("Total Value Stacked: %d | " +
                    "Total Distance Moved: %d | " +
                    "Power Up Count: %d | " +
                    "Push Button Count: %d | " +
                    "Configuration Count: %d | " +
                    "Usb Enumerations Count: %d | " +
                    "Total Cheat Attempts Detected: %d | " +
                    "Total Security Lockup Count: %d | ",
                    TotalValueStacked,
                    TotalDistanceMoved,
                    PowerUpCount,
                    PushButtonCount,
                    ConfigurationCount,
                    UsbEnumerationsCount,
                    TotalCheatAttemptsDetected,
                    TotalSecurityLockupCount)
                : super.toString();
    }
}
