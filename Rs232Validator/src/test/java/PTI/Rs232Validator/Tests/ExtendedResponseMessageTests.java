package PTI.Rs232Validator.Tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import PTI.Rs232Validator.Messages.Commands.ExtendedCommand;
import PTI.Rs232Validator.Messages.Responses.Extended.BarcodeDetectedResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Extended.ExtendedResponseMessage;
import PTI.Rs232Validator.Utility.ByteUtils;
import PTI.Rs232Validator.Utility.Rs232Payloads;

public class ExtendedResponseMessageTests {

    @Test
    public void ExtendedResponseMessage_DeserializesCommandAndStatusAndData(){

        var responsePayload = new byte[]{
                0x02, 0x28, 0x71, 0x01, 0x01, 0x10, 0x00, 0x00, 0x01, 0x02, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37,
                0x38, 0x39, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50,
                0x51, 0x52, 0x03, 0x58
        };

        final ExtendedCommand expectedCommand = ExtendedCommand.BarcodeDetected;
        var expectedStatus = new byte[] { 0x01, 0x10, 0x00, 0x00, 0x01, 0x02 };
        var expectedData = new byte[]{
                0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37,
                0x38, 0x39, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50,
                0x51, 0x52
        };

        var extendedResponseMessage = new ExtendedResponseMessage(ByteUtils.convertByteArrayToList(responsePayload));

        assertTrue(extendedResponseMessage.IsValid.get());
        assertEquals(expectedCommand, extendedResponseMessage.getCommand());
        assertArrayEquals(expectedStatus, ByteUtils.convertListToByteArray(extendedResponseMessage.getStatus()));
        assertArrayEquals(expectedData, ByteUtils.convertListToByteArray(extendedResponseMessage.Data));
    }

    @Test
    public void BarcodeDetectedResponseMessage_DeserializedBarcode(){
        var responsePayload = (byte[]) Rs232Payloads.BarcodeDetectedResponsePayloadAndBarcodePair[0];
        var barcode = (String) Rs232Payloads.BarcodeDetectedResponsePayloadAndBarcodePair[1];

        var barcodeDetectedResponseMessage = new BarcodeDetectedResponseMessage(ByteUtils.convertByteArrayToList(responsePayload));

        assertTrue(barcodeDetectedResponseMessage.IsValid.get());
        assertEquals(barcode, barcodeDetectedResponseMessage.getBarcode());
    }
}
