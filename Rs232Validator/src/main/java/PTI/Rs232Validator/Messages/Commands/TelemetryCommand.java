package PTI.Rs232Validator.Messages.Commands;

/**
 * The RS-232 telemetry commands
 */
public enum TelemetryCommand {

    /**
     * A command to verify communications are working
     */
    Ping((byte) 0x00),

    /**
     * A command to get the serial number assigned to an acceptor
     */
    GetSerialNumber((byte) 0x01),

    /**
     * A command to get the telemetry metics about the cashbox
     */
    GetCashboxMetrics((byte) 0x02),

    /**
     * A command to clear the count of bills in the cashbox
     */
    ClearCashboxCount((byte) 0x03),

    /**
     * A command to get the general telemetry metrics for an acceptor
     */
    GetUnitMetrics((byte) 0x04),

    /**
     * A command to get the telemetry metrics since the last time an acceptor was serviced
     */
    GetServiceUsageCounters((byte) 0x05),

    /**
     * A command to get the flags about what needs to be serviced
     */
    GetServiceFlags((byte) 0x06),

    /**
     * A command to clear 1 or more service flags
     */
    ClearServiceFlags((byte) 0x07),

    /**
     * A command to get the info that was attached to the last service
     */
    GetServiceInfo((byte) 0x08),

    /**
     * A command to get the telemetry metrics that pertain to an acceptor's firmware
     */
    GetFirmwareMetrics((byte) 0x09);

    private final byte value;
    TelemetryCommand(byte value) {
        this.value = value;
    }

    /**
     * Gets the byte value of the {@link TelemetryCommand}
     * @return A {@code byte} that corresponds with the {@link TelemetryCommand} type
     */
    public byte getValue() {
        return value;
    }

    /**
     * Gets the {@link TelemetryCommand} type from a given value
     * @param value The {@code byte} value for a {@link TelemetryCommand} type
     * @return A {@link TelemetryCommand} type
     */
    public static TelemetryCommand fromValue(byte value) {
        for (TelemetryCommand command : TelemetryCommand.values()) {
            if (command.value == value) {
                return command;
            }
        }
        throw new IllegalArgumentException("Unknown TelemetryCommand value: " + value);
    }

}
