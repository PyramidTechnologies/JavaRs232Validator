package PTI.Rs232Validator.Messages.Responses.Extended;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * An RS-232 message from an acceptor to a host for {@link PTI.Rs232Validator.Messages.Commands.ExtendedCommand#BarcodeDetected BarcodeDetected}
 */
public class BarcodeDetectedResponseMessage extends ExtendedResponseMessage{

    /**
     * The expected payload size in bytes
     */
    private final byte PayloadByteSize = 40;

    /**
     * Initializes a new instance of {@link BarcodeDetectedResponseMessage}.
     */
    public BarcodeDetectedResponseMessage(List<Byte> payload) {
        super(payload);

        if(!IsValid.get()){
            return;
        }

        if(payload.size() < PayloadByteSize){
            PayloadIssues.add(String.format("The payload size is %d bytes, but %d bytes are expected",  payload.size(), PayloadByteSize));
        }

        byte[] data = new byte[Data.size()];
        for(int i = 0; i < Data.size(); i++){
            data[i] = Data.get(i);
        }

        Barcode = new String(data, StandardCharsets.US_ASCII);
    }

    /**
     * The last barcode string
     * @implNote If the string is empty, no barcode was detected
     */
    private String Barcode = "";

    /**
     * Gets the last barcode string
     * @return {@link #Barcode}
     */
    public String getBarcode() {
        return Barcode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return IsValid.get() ? String.format("Barcode: %s", Barcode) : super.toString();
    }
}
