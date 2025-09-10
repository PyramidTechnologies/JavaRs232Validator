package PTI.Rs232Validator;

public enum Rs232Event {

    None((byte) 0),


    Stacked((byte) (1 << 0)),


    Returned((byte) (1 << 1)),


    Cheated((byte) (1 << 2)),


    BillRejected((byte) (1 << 3)),


    InvalidCommand((byte) (1 << 4)),


    PowerUp((byte) (1 << 5));

    private final byte value;

    private Rs232Event(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return this.value;
    }

    public static Rs232Event fromValue(byte value) {
        for (Rs232Event event : Rs232Event.values()) {
            if (event.value == value) {
                return event;
            }
        }

        throw new IllegalArgumentException("Unknown PTI.Rs232Validator.Rs232Event value: " + value);
    }

    public static String toFlagString(byte value) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;

        for(Rs232Event event : Rs232Event.values()){
            if((value & event.getValue()) != 0){
                if(!first){
                    builder.append(", ");
                }
                builder.append(event.name());
                first = false;
            }
        }

        return builder.toString();
    }

}
