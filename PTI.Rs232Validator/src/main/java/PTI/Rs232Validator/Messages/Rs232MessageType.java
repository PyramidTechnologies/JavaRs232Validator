package PTI.Rs232Validator.Messages;

public enum Rs232MessageType {


    Unknown((byte) 0x00),


    HostToAcceptor((byte) 0x10),


    AcceptorToHost((byte) 0x20),


    TelemetryCommand((byte) 0x60),


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
