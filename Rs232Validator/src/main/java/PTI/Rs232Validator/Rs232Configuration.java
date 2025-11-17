package PTI.Rs232Validator;

import java.time.Duration;

/**
 * The configuration for communicating with an RS-232 bill acceptor
 */
public class Rs232Configuration {

    /**
     * The enable mask, which represents types of bills to accept
     * @implNote
     * 0b00000001: only accept the 1st bill type (e.g. $1) <br>
     * 0b00000010: only accept the 2nd bill type (e.g. $2) <br>
     * 0b00000100: only accept the 3rd bill type (e.g. $5) <br>
     * 0b00001000: only accept the 4th bill type (e.g. $10) <br>
     * 0b00010000: only accept the 5th bill type (e.g. $20) <br>
     * 0b00100000: only accept the 6th bill type (e.g. $50) <br>
     * 0b01000000: only accept the 7th bill type (e.g. $100) <br>
     */
    public byte EnableMask = 0x07;

    /**
     * Should the acceptor escrow each bill?
     * @implNote
     * Setting this to {@code true} will cause the acceptor to place each bill in escrow and wait for the host to stack or return it. <br>
     * Setting this to {@code false} will cause the acceptor to automatically stack or return each bill
     */
    public boolean ShouldEscrow;

    /**
     * Should the acceptor detect barcodes?
     */
    public boolean ShouldDetectBarcodes;

    /**
     * The time period between messages sent from the host to the acceptor
     */
    public Duration PollingPeriod =  Duration.ofMillis(100);
}
