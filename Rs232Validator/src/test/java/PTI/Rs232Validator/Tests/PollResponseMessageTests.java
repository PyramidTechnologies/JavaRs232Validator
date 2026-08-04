package PTI.Rs232Validator.Tests;



import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import PTI.Rs232Validator.Messages.Responses.PollResponseMessage;
import PTI.Rs232Validator.Rs232Event;
import PTI.Rs232Validator.Rs232State;
import PTI.Rs232Validator.Utility.ByteUtils;
import PTI.Rs232Validator.Utility.Rs232Payloads;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class PollResponseMessageTests {

    private static Stream<Arguments> pollResponsePayloadAndStatePairs(){
        return Stream.of(Rs232Payloads.PollResponsePayloadAndStatePairs).map(pair -> {
            Object[] pairArray = (Object[]) pair;
            return Arguments.of(pairArray[0], pairArray[1]);
        });
    }

    @ParameterizedTest
    @MethodSource("pollResponsePayloadAndStatePairs")
    public void PollResponseMessage_DeserializesSingleStates(byte[] payload, Rs232State expectedState) {
        PollResponseMessage message = new PollResponseMessage(ByteUtils.convertByteArrayToList(payload));

        assertTrue(message.IsValid.get());
        assertEquals(expectedState, message.getState());
    }

    private static Stream<Arguments> pollResponsePayloadAndEventPairs(){
        return Stream.of(Rs232Payloads.PollResponsePayloadAndEventPairs).map(pair -> {
            Object[] pairArray = (Object[]) pair;
            return Arguments.of(pairArray[0], pairArray[1]);
        });
    }

    @ParameterizedTest
    @MethodSource("pollResponsePayloadAndEventPairs")
    public void PollResponseMessage_DeserializesSingleEvents(byte[] payload, byte expectedEvent) {
        PollResponseMessage message = new PollResponseMessage(ByteUtils.convertByteArrayToList(payload));

        assertTrue(message.IsValid.get());
        assertEquals(expectedEvent, message.getEvent().flags);
    }

    @Test
    public void PollResponseMessage_DeserializesMultipleEvents() {

        var pollResponsePayload = new PollResponseMessage(ByteUtils.convertByteArrayToList(Rs232Payloads.PollResponsePayloadWithEveryEvent));

        var expectedEvent = Rs232Event.None | Rs232Event.Stacked | Rs232Event.Returned | Rs232Event.Cheated | Rs232Event.BillRejected | Rs232Event.InvalidCommand | Rs232Event.PowerUp;

        assertTrue(pollResponsePayload.IsValid.get());
        assertEquals(expectedEvent, pollResponsePayload.getEvent().flags);
    }

    private static Stream<Arguments> pollResponsePayloadAndCashboxPresencePair(){
        return Stream.of(Rs232Payloads.PollResponsePayloadAndCashboxPresencePairs).map(pair -> {
            Object[] pairArray = (Object[]) pair;
            return Arguments.of(pairArray[0], pairArray[1]);
        });
    }

    @ParameterizedTest
    @MethodSource("pollResponsePayloadAndCashboxPresencePair")
    public void PollResponseMessage_DeserializesCashboxPresence(byte[] payload, boolean expectedIsCashboxPresent) {
        var pollResponseMessage = new PollResponseMessage(ByteUtils.convertByteArrayToList(payload));

        assertTrue(pollResponseMessage.IsValid.get());
        assertEquals(expectedIsCashboxPresent, pollResponseMessage.getIsCashboxPresent());
    }

    private static Stream<Arguments> pollResponsePayloadAndStackedBillPairs(){
        return Stream.of(Rs232Payloads.PollResponsePayloadAndStackedBillPairs).map(pair -> {
            Object[] pairArray = (Object[]) pair;
            return Arguments.of(pairArray[0], pairArray[1]);
        });
    }

    @ParameterizedTest
    @MethodSource("pollResponsePayloadAndStackedBillPairs")
    public void PollResponseMessage_DeserializesCashboxPresence(byte[] payload, byte expectedBillType) {
        var pollResponseMessage = new PollResponseMessage(ByteUtils.convertByteArrayToList(payload));

        assertTrue(pollResponseMessage.IsValid.get());
        assertEquals(expectedBillType, pollResponseMessage.getBillType());
    }

    @Test
    public void PollResponseMessage_DeserializesModelNumberAndFirmwareRevision(){
        var responsePayload = new byte[] { 0x02, 0x0B, 0x20, 0b00000001, 0b00010000, 0b00000000, 0, 1, 2, 0x03, 0x39 };
        final byte expectedModelNumber = 1;
        final byte expectedFirmwareRevision = 2;

        var pollResponseMessage = new PollResponseMessage(ByteUtils.convertByteArrayToList(responsePayload));

        assertTrue(pollResponseMessage.IsValid.get());
        assertEquals(expectedModelNumber, pollResponseMessage.getModelNumber());
        assertEquals(expectedFirmwareRevision, pollResponseMessage.getFirmwareRevision());
    }
}
