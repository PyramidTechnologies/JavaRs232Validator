package PTI.Rs232Validator.Messages.Commands;

/**
 * The RS-232 extended commands
 */
public enum ExtendedCommand {

    /**
     * A command to get the last barcode string
     */
    BarcodeDetected((byte) 0x01);

    private final byte value;
    ExtendedCommand(byte value) {
        this.value = value;
    }

    /**
     * Gets the byte value of the {@link ExtendedCommand}
     * @return A {@code byte} that corresponds with the {@link ExtendedCommand} type
     */
    public byte getValue() {
        return value;
    }

    /**
     * Gets the Enum type from a given value
     * @param value The {@code byte} value for a {@link ExtendedCommand} type
     * @return A {@link ExtendedCommand} type
     */
    public static ExtendedCommand fromValue(byte value) {
        for (ExtendedCommand command : ExtendedCommand.values()) {
            if (command.getValue() == value) {
                return command;
            }
        }
        throw new IllegalArgumentException("Unknown ExtendedCommand value: " + value);
    }
}
