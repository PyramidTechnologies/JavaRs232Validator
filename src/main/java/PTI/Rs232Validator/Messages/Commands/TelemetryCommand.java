package PTI.Rs232Validator.Messages.Commands;

public enum TelemetryCommand {

    Ping((byte) 0x00),


    GetSerialNumber((byte) 0x01),


    GetCashboxMetrics((byte) 0x02),


    ClearCashboxCount((byte) 0x03),


    GetUnitMetrics((byte) 0x04),


    GetServiceUsageCounters((byte) 0x05),


    GetServiceFlags((byte) 0x06),


    ClearServiceFlags((byte) 0x07),


    GetServiceInfo((byte) 0x08),


    GetFirmwareMetrics((byte) 0x09);

    private final byte value;
    TelemetryCommand(byte value) {
        this.value = value;
    }
    public byte getValue() {
        return value;
    }

    public static TelemetryCommand fromValue(byte value) {
        for (TelemetryCommand command : TelemetryCommand.values()) {
            if (command.value == value) {
                return command;
            }
        }
        throw new IllegalArgumentException("Unknown TelemetryCommand value: " + value);
    }

}
