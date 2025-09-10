package Utility;

import PTI.Rs232Validator.Rs232Event;
import PTI.Rs232Validator.Rs232State;

public class Rs232Payloads {

    public static byte[] ZeroAckValidPollResponsePayload =
            {0x02, 0x0B, 0x20, 0b00000001, 0b00010000, 0b00000000, 0x00, 0x01, 0x02, 0x03, 0x39};


    public static byte[] OneAckValidPollResponsePayload =
            {0x02, 0x0B, 0x21, 0b00000001, 0b00010000, 0b00000000, 0x00, 0x01, 0x02, 0x03, 0x38};


    public static Object[][] PollResponsePayloadAndStatePairs =
            {
                    {
                            new byte[]{0x02, 0x0B, 0x21, 0b00000001, 0b00010000, 0b00000000, 0x00, 0x01, 0x02, 0x03, 0x38},
                            Rs232State.Idling
                    },
                    {
                            new byte[] { 0x02, 0x0B, 0x21, 0b00000010, 0b00010000, 0b00000000, 0x00, 0x01, 0x02, 0x03, 0x3B },
                            Rs232State.Accepting
                    },
                    {
                            new byte[] { 0x02, 0x0B, 0x21, 0b00000100, 0b00010000, 0b00000000, 0x00, 0x01, 0x02, 0x03, 0x3D },
                            Rs232State.Escrowed
                    },
                    {
                            new byte[] { 0x02, 0x0B, 0x21, 0b00001000, 0b00010000, 0b00000000, 0x00, 0x01, 0x02, 0x03, 0x31 },
                            Rs232State.Stacking
                    },
                    {
                            new byte[] { 0x02, 0x0B, 0x21, 0b00100000, 0b00010000, 0b00000000, 0x00, 0x01, 0x02, 0x03, 0x19 },
                            Rs232State.Returning
                    },
                    {
                            new byte[] { 0x02, 0x0B, 0x21, 0b00000000, 0b00010100, 0b00000000, 0x00, 0x01, 0x02, 0x03, 0x3D },
                            Rs232State.BillJammed
                    },
                    {
                            new byte[] { 0x02, 0x0B, 0x21, 0b00000000, 0b00011000, 0b00000000, 0x00, 0x01, 0x02, 0x03, 0x31 },
                            Rs232State.StackerFull
                    },
                    {
                            new byte[] { 0x02, 0x0B, 0x21, 0b00000000, 0b00010000, 0b00000100, 0x00, 0x01, 0x02, 0x03, 0x3D },
                            Rs232State.Failure
                    }
            };

    public static Object[][] PollResponsePayloadAndEventPairs =
            {
                    {
                            new byte[] { 0x02, 0x0B, 0x21, 0b00010001, 0b00010000, 0b00000000, 0x00, 0x01, 0x02, 0x03, 0x28 },
                            Rs232Event.Stacked
                    },
                    {
                            new byte[] { 0x02, 0x0B, 0x21, 0b01000001, 0b00010000, 0b00000000, 0x00, 0x01, 0x02, 0x03, 0x78 },
                            Rs232Event.Returned
                    },
                    {
                            new byte[] { 0x02, 0x0B, 0x21, 0b00000001, 0b00010001, 0b00000000, 0x00, 0x01, 0x02, 0x03, 0x39 },
                            Rs232Event.Cheated
                    },
                    {
                            new byte[] { 0x02, 0x0B, 0x21, 0b00000001, 0b00010010, 0b00000000, 0x00, 0x01, 0x02, 0x03, 0x3A },
                            Rs232Event.BillRejected
                    },
                    {
                            new byte[] { 0x02, 0x0B, 0x21, 0b00000001, 0b00010000, 0b00000010, 0x00, 0x01, 0x02, 0x03, 0x3A },
                            Rs232Event.InvalidCommand
                    },
                    {
                            new byte[] { 0x02, 0x0B, 0x21, 0b00000001, 0b00010000, 0b00000001, 0x00, 0x01, 0x02, 0x03, 0x39 },
                            Rs232Event.PowerUp
                    }
            };


    public static byte[] PollResponsePayloadWithEveryEvent =
            {0x02, 0x0B, 0x21, 0b01010001, 0b00010011, 0b00000011, 0x00, 0x01, 0x02, 0x03, 0x68};


    public static Object[][] PollResponsePayloadAndCashboxPresencePairs = {
            {
                    new byte[] { 0x02, 0x0B, 0x21, 0b00000001, 0b00000000, 0b00000000, 0x00, 0x01, 0x02, 0x03, 0x28 },
                    false
            },
            {
                    new byte[] { 0x02, 0x0B, 0x21, 0b00000001, 0b00010000, 0b00000000, 0x00, 0x01, 0x02, 0x03, 0x38 },
                    true
            }
    };


    public static Object[][] PollResponsePayloadAndStackedBillPairs = {
            {
                    new byte[] { 0x02, 0x0B, 0x21, 0b00010001, 0b00010000, 0b00001000, 0x00, 0x01, 0x02, 0x03, 0x20 },
                    (byte)1
            },
            {
                    new byte[] { 0x02, 0x0B, 0x21, 0b00010001, 0b00010000, 0b00010000, 0x00, 0x01, 0x02, 0x03, 0x38 },
                    (byte)2
            },
            {
                    new byte[] { 0x02, 0x0B, 0x21, 0b00010001, 0b00010000, 0b00011000, 0x00, 0x01, 0x02, 0x03, 0x30 },
                    (byte)3
            },
            {
                    new byte[] { 0x02, 0x0B, 0x21, 0b00010001, 0b00010000, 0b00100000, 0x00, 0x01, 0x02, 0x03, 0x08 },
                    (byte)4
            },
            {
                    new byte[] { 0x02, 0x0B, 0x21, 0b00010001, 0b00010000, 0b00101000, 0x00, 0x01, 0x02, 0x03, 0x00 },
                    (byte)5
            },
            {
                    new byte[] { 0x02, 0x0B, 0x21, 0b00010001, 0b00010000, 0b00110000, 0x00, 0x01, 0x02, 0x03, 0x18 },
                    (byte)6
            },
            {
                    new byte[] { 0x02, 0x0B, 0x21, 0b00010001, 0b00010000, 0b00111000, 0x00, 0x01, 0x02, 0x03, 0x10 },
                    (byte)7
            }
    };


    public static Object[][] PollResponsePayloadAndEscrowedBillPairs = {
            {
                    new byte[] { 0x02, 0x0B, 0x21, 0b00000100, 0b00010000, 0b00001000, 0x00, 0x01, 0x02, 0x03, 0x35 },
                    (byte)1
            },
            {
                    new byte[] { 0x02, 0x0B, 0x21, 0b00000100, 0b00010000, 0b00010000, 0x00, 0x01, 0x02, 0x03, 0x2D },
                    (byte)2
            },
            {
                    new byte[] { 0x02, 0x0B, 0x21, 0b00000100, 0b00010000, 0b00011000, 0x00, 0x01, 0x02, 0x03, 0x25 },
                    (byte)3
            },
            {
                    new byte[] { 0x02, 0x0B, 0x21, 0b00000100, 0b00010000, 0b00100000, 0x00, 0x01, 0x02, 0x03, 0x1D },
                    (byte)4
            },
            {
                    new byte[] { 0x02, 0x0B, 0x21, 0b00000100, 0b00010000, 0b00101000, 0x00, 0x01, 0x02, 0x03, 0x15 },
                    (byte)5
            },
            {
                    new byte[] { 0x02, 0x0B, 0x21, 0b00000100, 0b00010000, 0b00110000, 0x00, 0x01, 0x02, 0x03, 0x0D },
                    (byte)6
            },
            {
                    new byte[] { 0x02, 0x0B, 0x21, 0b00000100, 0b00010000, 0b00111000, 0x00, 0x01, 0x02, 0x03, 0x05 },
                    (byte)7
            }
    };


    public static Object[] BarcodeDetectedResponsePayloadAndBarcodePair = {
            new byte[]{
                    0x02, 0x28, 0x71, 0x01, 0x01, 0x10, 0x00, 0x00, 0x01, 0x02, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37,
                    0x38, 0x39, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50,
                    0x51, 0x52, 0x03, 0x58
            },
            "0123456789ABCDEFGHIJKLMNOPQR"
    };
}
