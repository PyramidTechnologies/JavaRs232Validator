package PTI.Rs232Validator;

import java.util.ArrayList;
import java.util.List;

/**
 * RS-232 bill validator events.
 * By definition, events are reported exactly once per occurrence
 */
public class Rs232Event {

    /**
     * No event flags are set
     */
    public static final byte None = 0;

    /**
     * A bill has been stacked
     */
    public static final byte Stacked = 1 << 0;

    /**
     * A bill has been returned
     */
    public static final byte Returned = 1 << 1;

    /**
     * A cheat attempt has been detected and blocked
     */
    public static final byte Cheated = 1 << 2;

    /**
     * A bill has been rejected
     */
    public static final byte BillRejected = 1 << 3;

    /**
     * An invalid host command has been received
     */
    public static final byte InvalidCommand = 1 << 4;

    /**
     * The bill validator just powered up
     */
    public static final byte PowerUp = 1 << 5;

    /**
     * A byte containing whether a flag has been set
     * @implNote  e.g. 0b000001 -> Stacked <br>
     * 0b101100 -> PowerUp, BillRejected, Cheated
     */
    public byte flags = 0;

    public Rs232Event(byte flags){
        this.flags = flags;
    }

    /**
     * Checks if a specified flag has been set
     * @param flag The value of the flag to check
     * @return {@code true} if the flag has been set; otherwise, {@code false}
     */
    public boolean hasFlag(byte flag){
        return (flags & flag) == flag;
    }

    /**
     * Sets a given flag
     * @param flag The flag to set
     */
    public void setFlag(byte flag){
        flags = (byte) (flags | flag);
    }

    /**
     * Returns a String representation of all of the set flags
     * @return A String representation of all of the set flags
     */
    public String Flags(){
        List<String> setFlag = new ArrayList<>();

        if((flags & Stacked) == Stacked)
            setFlag.add("Stacked");

        if((flags & Returned) == Returned)
            setFlag.add("Returned");

        if((flags & Cheated) == Cheated)
            setFlag.add("Cheated");

        if((flags & BillRejected) == BillRejected)
            setFlag.add("BillRejected");

        if((flags & InvalidCommand) == InvalidCommand)
            setFlag.add("InvalidCommand");

        if((flags & PowerUp) == PowerUp)
            setFlag.add("PowerUp");

        if(setFlag.isEmpty())
            setFlag.add("None");

        return String.join(", ", setFlag);
    }
}
