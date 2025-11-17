package PTI.Rs232Validator.Messages;

/**
 * The RS-232 message types.
 */
public enum Rs232MessageType {

    /**
     * An unknown message type
     */
    Unknown((byte) 0x00),

    /**
     * A poll message from a host to an acceptor
     */
    HostToAcceptor((byte) 0x10),

    /**
     * A poll message from an acceptor to a host
     */
    AcceptorToHost((byte) 0x20),

    /**
     * A telemetry command message
     */
    TelemetryCommand((byte) 0x60),

    /**
     * An extended command message
     */
    ExtendedCommand((byte) 0x70),;

    private final byte value;

    private Rs232MessageType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return this.value;
    }

    public static Rs232MessageType fromValue(byte value) {
        for (Rs232MessageType v : Rs232MessageType.values()) {
            if (v.getValue() == value) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown Rs232MessageTypes value: " + value);
    }
}
