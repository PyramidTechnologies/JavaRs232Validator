package PTI.Rs232Validator.Messages.Commands;

public enum ExtendedCommand {

    BarcodeDetected((byte) 0x01);

    private final byte value;
    ExtendedCommand(byte value) {
        this.value = value;
    }
    public byte getValue() {
        return value;
    }

    public static ExtendedCommand fromValue(byte value) {
        for (ExtendedCommand command : ExtendedCommand.values()) {
            if (command.getValue() == value) {
                return command;
            }
        }
        throw new IllegalArgumentException("Unknown ExtendedCommand value: " + value);
    }
}
