package Tests;

import PTI.Rs232Validator.Messages.Responses.PollResponseMessage;
import PTI.Rs232Validator.Rs232Event;
import PTI.Rs232Validator.Rs232State;
import PTI.Rs232Validator.Utility.ByteExtensions;
import Utility.Rs232Payloads;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

public class PollResponseMessageTests {

    @ParameterizedTest
    @MethodSource("loadPollResponsePayloadAndStatePairs")
    public void PollResponseMessage_DeserializesSingleStates(byte[] responsePayload, Rs232State expectedState){
        PollResponseMessage pollResponseMessage = new PollResponseMessage(ByteExtensions.convertByteArrayToList(responsePayload));

        Assertions.assertTrue(pollResponseMessage.IsValid);
        Assertions.assertEquals(expectedState,pollResponseMessage.getState());
    }

    public static Stream<Arguments> loadPollResponsePayloadAndStatePairs(){
        return Arrays.stream(Rs232Payloads.PollResponsePayloadAndStatePairs).map(Arguments::of);
    }


    @ParameterizedTest
    @MethodSource("loadPollResponsePayloadAndEventPairs")
    public void PollResponseMessage_DeserializesSingleEvents(byte[] responsePayload, Rs232Event expectedEvent){
        PollResponseMessage pollResponseMessage = new PollResponseMessage(ByteExtensions.convertByteArrayToList(responsePayload));
        Assertions.assertTrue(pollResponseMessage.IsValid);
        Assertions.assertEquals(expectedEvent.getValue(), pollResponseMessage.getEvent());
    }

    public static Stream<Arguments> loadPollResponsePayloadAndEventPairs(){
        return Arrays.stream(Rs232Payloads.PollResponsePayloadAndEventPairs).map(Arguments::of);
    }

    @Test
    public void PollResponseMessage_DeserializesMultipleStates(){
        PollResponseMessage pollResponseMessage = new PollResponseMessage(ByteExtensions.convertByteArrayToList(Rs232Payloads.PollResponsePayloadWithEveryEvent));

        byte expectedEvent = Rs232Event.None.getValue();
        for(Rs232Event value : Rs232Event.values()){
            expectedEvent |= value.getValue();
        }

        Assertions.assertTrue(pollResponseMessage.IsValid);
        Assertions.assertEquals(expectedEvent, pollResponseMessage.getEvent());
    }

    @ParameterizedTest
    @MethodSource("loadPollResponsePayloadAndCashboxPresencePairs")
    public void PollResponseMessage_DeserializesCashboxPresence(byte[] responsePayload, boolean expectedCashboxPresence){
        PollResponseMessage pollResponseMessage = new PollResponseMessage(ByteExtensions.convertByteArrayToList(responsePayload));

        Assertions.assertTrue(pollResponseMessage.IsValid);
        Assertions.assertEquals(expectedCashboxPresence, pollResponseMessage.getIsCashboxPresent());
    }

    public static Stream<Arguments> loadPollResponsePayloadAndCashboxPresencePairs(){
        return Arrays.stream(Rs232Payloads.PollResponsePayloadAndCashboxPresencePairs).map(Arguments::of);
    }


    @ParameterizedTest
    @MethodSource("loadPollResponsePayloadAndStackedBillPairs")
    public void PollResponseMessage_DeserializesBillType(byte[] responsePayload, byte expectedBillType){
        PollResponseMessage pollResponseMessage = new PollResponseMessage(ByteExtensions.convertByteArrayToList(responsePayload));

        Assertions.assertTrue(pollResponseMessage.IsValid);
        Assertions.assertEquals(expectedBillType, pollResponseMessage.getBillType());
    }
    public static Stream<Arguments> loadPollResponsePayloadAndStackedBillPairs(){
        return Arrays.stream(Rs232Payloads.PollResponsePayloadAndStackedBillPairs).map(Arguments::of);
    }


    @Test
    public void PollResponseMessage_DeserializesModelNumberAndFirmwareRevision() {
        byte[] responsePayload = new byte[]{ 0x02, 0x0B, 0x20, 0b00000001, 0b00010000, 0b00000000, 0, 1, 2, 0x03, 0x39 };
        final byte expectedModelNumber = 1;
        final byte expectedFirmwareRevision = 2;

        PollResponseMessage pollResponseMessage = new PollResponseMessage(ByteExtensions.convertByteArrayToList(responsePayload));

        Assertions.assertTrue(pollResponseMessage.IsValid);
        Assertions.assertEquals(expectedModelNumber, pollResponseMessage.getModelNumber());
        Assertions.assertEquals(expectedFirmwareRevision, pollResponseMessage.getFirmwareRevision());
    }
}
