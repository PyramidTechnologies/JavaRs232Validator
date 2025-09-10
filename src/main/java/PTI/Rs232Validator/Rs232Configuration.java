package PTI.Rs232Validator;

import java.time.Duration;

public class Rs232Configuration {

    public byte EnableMask = 0x07;


    public boolean ShouldEscrow;


    public boolean ShouldDetectBarcodes;


    public Duration PollingPeriod =  Duration.ofMillis(100);


}
