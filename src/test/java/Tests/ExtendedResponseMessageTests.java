package Tests;

import PTI.Rs232Validator.Messages.Commands.ExtendedCommand;
import PTI.Rs232Validator.Messages.Responses.Extended.BarcodeDetectedResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Extended.ExtendedResponseMessage;
import PTI.Rs232Validator.Utility.ByteExtensions;
import Utility.Rs232Payloads;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExtendedResponseMessageTests {

    @Test
    public void ExtendedResponseMessage_DeserializesCommandAndStatusAndData() {
        byte[] payload = new byte[]{
                0x02, 0x28, 0x71, 0x01, 0x01, 0x10, 0x00, 0x00, 0x01, 0x02, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37,
                0x38, 0x39, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50,
                0x51, 0x52, 0x03, 0x58
        };

        final ExtendedCommand expectedCommand = ExtendedCommand.BarcodeDetected;
        byte[] expectedStatus = new byte[]{ 0x01, 0x10, 0x00, 0x00, 0x01, 0x02 };
        byte[] expectedData = new byte[]{
                0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37,
                0x38, 0x39, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50,
                0x51, 0x52
        };

        ExtendedResponseMessage extendedResponseMessage = new ExtendedResponseMessage(ByteExtensions.convertByteArrayToList(payload));

        Assertions.assertTrue(extendedResponseMessage.IsValid);
        Assertions.assertEquals(expectedCommand, extendedResponseMessage.getCommand());
        Assertions.assertEquals(ByteExtensions.convertByteArrayToList(expectedStatus), extendedResponseMessage.getStatus());
        Assertions.assertEquals(ByteExtensions.convertByteArrayToList(expectedData), extendedResponseMessage.getData());
    }

    @Test
    public void BarcodeDetectedResponseMessage_DeserializesBarcode(){
        byte[] responsePayload = (byte[])Rs232Payloads.BarcodeDetectedResponsePayloadAndBarcodePair[0];
        String barcode = (String)Rs232Payloads.BarcodeDetectedResponsePayloadAndBarcodePair[1];

        BarcodeDetectedResponseMessage barcodeDetectedResponseMessage = new BarcodeDetectedResponseMessage(ByteExtensions.convertByteArrayToList(responsePayload));

        Assertions.assertTrue(barcodeDetectedResponseMessage.IsValid);
        Assertions.assertEquals(barcode, barcodeDetectedResponseMessage.getBarcode());
    }
}
