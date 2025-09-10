package PTI.Rs232Validator;

public enum CorrectableComponent {

    /// <summary>
    /// The tach sensor.
    /// </summary>
    TachSensor((byte) 0),

    /// <summary>
    /// The bill path.
    /// </summary>
    BillPath((byte) 1),

    /// <summary>
    /// The cashbox belt.
    /// </summary>
    CashboxBelt((byte) 2),

    /// <summary>
    /// The cashbox stacking mechanism.
    /// </summary>
    CashboxMechanism((byte) 3),

    /// <summary>
    /// The mechanical anti-stringing lever (MAS).
    /// </summary>
    MAS((byte) 4),

    /// <summary>
    /// The spring rollers.
    /// </summary>
    SpringRollers((byte) 5),

    /// <summary>
    /// All components.
    /// </summary>
    All((byte) 0x7F);



    private final byte value;

    private CorrectableComponent(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return this.value;
    }

    public static CorrectableComponent fromValue(byte b) {
        for (CorrectableComponent c : CorrectableComponent.values()) {
            if (c.value == b) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown PTI.Rs232Validator.CorrectableComponent value: " + b);
    }
}
