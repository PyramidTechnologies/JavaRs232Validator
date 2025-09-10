package PTI.Rs232Validator.Messages.Responses.Extended;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class BarcodeDetectedResponseMessage extends ExtendedResponseMessage{

    private final byte PayloadByteSize = 40;

    public BarcodeDetectedResponseMessage(List<Byte> payload) {
        super(payload);

        if(!IsValid){
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

    private String Barcode = "";

    public String getBarcode() {
        return Barcode;
    }

    @Override
    public String toString() {
        return IsValid ? String.format("Barcode: %s", Barcode) : super.toString();
    }
}
