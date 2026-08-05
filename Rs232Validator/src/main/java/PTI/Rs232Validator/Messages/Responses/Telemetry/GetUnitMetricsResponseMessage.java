package PTI.Rs232Validator.Messages.Responses.Telemetry;

import PTI.Rs232Validator.Utility.ByteUtils;

import java.util.List;

/**
 * An RS-232 message from an acceptor to a host for {@link PTI.Rs232Validator.Messages.Commands.TelemetryCommand#GetUnitMetrics GetUnitMetrics}
 */
public class GetUnitMetricsResponseMessage extends TelemetryResponseMessage{

    private final byte PayloadByteSize = 69;

    /**
     * Initializes a new instance of {@link GetUnitMetricsResponseMessage}
     */
    public GetUnitMetricsResponseMessage(List<Byte> payload) {
        super(payload);

        if(!PayloadIssues.isEmpty()){
            return;
        }

        if(payload.size() != PayloadByteSize){
            PayloadIssues.add(String.format("The payload size is %d bytes, but %d bytes are expected", payload.size(), PayloadByteSize));
            return;
        }

        TotalValueStacked = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(0, 8));
        TotalDistanceMoved = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(8, 16));
        PowerUpCount = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(16, 24));
        PushButtonCount = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(24, 32));
        ConfigurationCount = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(32, 40));
        UsbEnumerationsCount = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(40, 48));
        TotalCheatAttemptsDetected = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(48, 56));
        TotalSecurityLockupCount = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(56, 64));
    }

    private long TotalValueStacked;

    /**
     * The total value of currency accepted for the lifetime of the acceptor.
     */
    public long getTotalValueStacked() {
        return TotalValueStacked;
    }

    private long TotalDistanceMoved;

    /**
     * The total distance the acceptance motor has moved in mm.
     */
    public long getTotalDistanceMoved() {
        return TotalDistanceMoved;
    }

    private long PowerUpCount;

    /**
     * The total times the acceptor has powered on.
     */
    public long getPowerUpCount() {
        return PowerUpCount;
    }

    private long PushButtonCount;

    /**
     * The total times the diagnostics push button has been pressed.
     */
    public long getPushButtonCount() {
        return PushButtonCount;
    }

    private long ConfigurationCount;

    /**
     * The total times the acceptor has been re-configured.
     */
    public long getConfigurationCount() {
        return ConfigurationCount;
    }

    private long UsbEnumerationsCount;

    /**
     * The total times the acceptor has had the USB device port plugged in.
     */
    public long getUsbEnumerationsCount() {
        return UsbEnumerationsCount;
    }

    private long TotalCheatAttemptsDetected;

    /**
     * The total times the acceptor has detected a cheat attempt.
     */
    public long getTotalCheatAttemptsDetected() {
        return TotalCheatAttemptsDetected;
    }

    private long TotalSecurityLockupCount;

    /**
     * The total times the acceptor went into a security lockup due to cheat attempts.
     */
    public long getTotalSecurityLockupCount() {
        return TotalSecurityLockupCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return IsValid.get()
                ? String.format("Total Value Stacked: %d | " +
                    "Total Distance Moved: %d | " +
                    "Power Up Count: %d | " +
                    "Push Button Count: %d | " +
                    "Configuration Count: %d | " +
                    "Usb Enumerations Count: %d | " +
                    "Total Cheat Attempts Detected: %d | " +
                    "Total Security Lockup Count: %d",
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
