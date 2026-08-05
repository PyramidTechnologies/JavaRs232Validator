package PTI.Rs232Validator.Tests;



import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import PTI.Rs232Validator.Messages.Responses.Telemetry.GetCashboxMetricsResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Telemetry.GetFirmwareMetricsResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Telemetry.GetSerialNumberResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Telemetry.GetServiceFlagsResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Telemetry.GetServiceInfoResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Telemetry.GetServiceUsageCountersResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Telemetry.GetUnitMetricsResponseMessage;
import PTI.Rs232Validator.Messages.Responses.Telemetry.TelemetryResponseMessage;
import PTI.Rs232Validator.Utility.ByteUtils;

public class TelemetryResponseMessageTests {

    @Test
    public void TelemetryResponseMessage_DeserializesData(){
        var responsePayload = new byte[]
                { 0x02, 0x0E, 0x61, 0x32, 0x31, 0x30, 0x32, 0x36, 0x30, 0x30, 0x31, 0x30, 0x03, 0x59 };

        var expectedData = new byte[] { 0x32, 0x31, 0x30, 0x32, 0x36, 0x30, 0x30, 0x31, 0x30 };

        var telemetryResponseMessage = new TelemetryResponseMessage(ByteUtils.convertByteArrayToList(responsePayload));

        assertTrue(telemetryResponseMessage.IsValid.get());
        assertArrayEquals(expectedData, ByteUtils.convertListToByteArray(telemetryResponseMessage.getData()));
    }

    @Test
    public void GetSerialNumberResponseMessage_DeserializesSerialNumber(){
        var responsePayload = new byte[]
                { 0x02, 0x0E, 0x61, 0x32, 0x31, 0x30, 0x32, 0x36, 0x30, 0x30, 0x31, 0x30, 0x03, 0x59 };

        final String expectedSerialNumber = "210260010";

        var getSerialNumberResponseMessage = new GetSerialNumberResponseMessage(ByteUtils.convertByteArrayToList(responsePayload));

        assertTrue(getSerialNumberResponseMessage.IsValid.get());
        assertEquals(expectedSerialNumber, getSerialNumberResponseMessage.getSerialNumber());
    }

    @Test
    public void GetCashboxMetricsResponseMessage_DeserializesCashboxMetrics() {
        var responsePayload = new byte[]
                {
                        0x02, 0x35, 0x61,
                        0x0F, 0x0E, 0x0F, 0x0E, 0x0F, 0x0E, 0x0F, 0x0E,
                        0x0E, 0x0D, 0x0E, 0x0D, 0x0E, 0x0D, 0x0E, 0x0D,
                        0x0D, 0x0C, 0x0D, 0x0C, 0x0D, 0x0C, 0x0D, 0x0C,
                        0x0C, 0x0B, 0x0C, 0x0B, 0x0C, 0x0B, 0x0C, 0x0B,
                        0x0B, 0x0A, 0x0B, 0x0A, 0x0B, 0x0A, 0x0B, 0x0A,
                        0x0A, 0x09, 0x0A, 0x09, 0x0A, 0x09, 0x0A, 0x09,
                        0x03, 0x54
                };

        final long expectedCashboxRemovedCount = 0xFEFEFEFEL;
        final long expectedCashboxFullCount = 0xEDEDEDEDL;
        final long expectedBillsStackedSinceCashboxRemoved = 0xDCDCDCDCL;
        final long expectedBillsStackedSincePowerUp = 0xCBCBCBCBL;
        final long expectedAverageTimeToStack = 0xBABABABAL;
        final long expectedTotalBillsStacked = 0xA9A9A9A9L;

        var getCashboxMetricsResponseMessage = new GetCashboxMetricsResponseMessage(ByteUtils.convertByteArrayToList(responsePayload));

        assertTrue(getCashboxMetricsResponseMessage.IsValid.get());
        assertEquals(expectedCashboxRemovedCount, getCashboxMetricsResponseMessage.getCashboxRemovedCount());
        assertEquals(expectedCashboxFullCount, getCashboxMetricsResponseMessage.getCashboxFullCount());
        assertEquals(expectedBillsStackedSinceCashboxRemoved, getCashboxMetricsResponseMessage.getBillsStackedSinceCashboxRemoved());
        assertEquals(expectedBillsStackedSincePowerUp, getCashboxMetricsResponseMessage.getBillsStackedSincePowerUp());
        assertEquals(expectedAverageTimeToStack, getCashboxMetricsResponseMessage.getAverageTimeToStack());
        assertEquals(expectedTotalBillsStacked, getCashboxMetricsResponseMessage.getTotalBillsStacked());
    }

    @Test
    public void GetUnitMetricsResponseMessage_DeserializesUnitMetrics(){
        var responsePayload = new byte[]
                {
                        0x02, 0x45, 0x61,
                        0x0F, 0x0E, 0x0F, 0x0E, 0x0F, 0x0E, 0x0F, 0x0E,
                        0x0E, 0x0D, 0x0E, 0x0D, 0x0E, 0x0D, 0x0E, 0x0D,
                        0x0D, 0x0C, 0x0D, 0x0C, 0x0D, 0x0C, 0x0D, 0x0C,
                        0x0C, 0x0B, 0x0C, 0x0B, 0x0C, 0x0B, 0x0C, 0x0B,
                        0x0B, 0x0A, 0x0B, 0x0A, 0x0B, 0x0A, 0x0B, 0x0A,
                        0x0A, 0x09, 0x0A, 0x09, 0x0A, 0x09, 0x0A, 0x09,
                        0x09, 0x08, 0x09, 0x08, 0x09, 0x08, 0x09, 0x08,
                        0x08, 0x07, 0x08, 0x07, 0x08, 0x07, 0x08, 0x07,
                        0x03, 0x24
                };

        final long expectedTotalValueStacked = 0xFEFEFEFEL;
        final long expectedTotalDistanceMoved = 0xEDEDEDEDL;
        final long expectedPowerUpCount = 0xDCDCDCDCL;
        final long expectedPushButtonCount = 0xCBCBCBCBL;
        final long expectedConfigurationCount = 0xBABABABAL;
        final long expectedUsbEnumerationsCount = 0xA9A9A9A9L;
        final long expectedTotalCheatAttemptsDetected = 0x98989898L;
        final long expectedTotalSecurityLockupCount = 0x87878787L;

        var getUnitMetricsResponseMessage = new GetUnitMetricsResponseMessage(ByteUtils.convertByteArrayToList(responsePayload));

        assertTrue(getUnitMetricsResponseMessage.IsValid.get());
        assertEquals(expectedTotalValueStacked, getUnitMetricsResponseMessage.getTotalValueStacked());
        assertEquals(expectedTotalDistanceMoved, getUnitMetricsResponseMessage.getTotalDistanceMoved());
        assertEquals(expectedPowerUpCount, getUnitMetricsResponseMessage.getPowerUpCount());
        assertEquals(expectedPushButtonCount, getUnitMetricsResponseMessage.getPushButtonCount());
        assertEquals(expectedConfigurationCount, getUnitMetricsResponseMessage.getConfigurationCount());
        assertEquals(expectedUsbEnumerationsCount, getUnitMetricsResponseMessage.getUsbEnumerationsCount());
        assertEquals(expectedTotalCheatAttemptsDetected, getUnitMetricsResponseMessage.getTotalCheatAttemptsDetected());
        assertEquals(expectedTotalSecurityLockupCount, getUnitMetricsResponseMessage.getTotalSecurityLockupCount());
    }

    @Test
    public void GetServiceUsageCountersResponseMessage_DeserializesServiceUsageCounters() {
        var responsePayload = new byte[]
                {
                        0x02, 0x35, 0x61,
                        0x0F, 0x0E, 0x0F, 0x0E, 0x0F, 0x0E, 0x0F, 0x0E,
                        0x0E, 0x0D, 0x0E, 0x0D, 0x0E, 0x0D, 0x0E, 0x0D,
                        0x0D, 0x0C, 0x0D, 0x0C, 0x0D, 0x0C, 0x0D, 0x0C,
                        0x0C, 0x0B, 0x0C, 0x0B, 0x0C, 0x0B, 0x0C, 0x0B,
                        0x0B, 0x0A, 0x0B, 0x0A, 0x0B, 0x0A, 0x0B, 0x0A,
                        0x0A, 0x09, 0x0A, 0x09, 0x0A, 0x09, 0x0A, 0x09,
                        0x03, 0x54
                };

        final long expectedDistanceMovedSinceLastTachSensorService = 0xFEFEFEFEL;
        final long expectedDistanceMovedSinceLastBillPathService = 0xEDEDEDEDL;
        final long expectedDistanceMovedSinceLastBeltService = 0xDCDCDCDCL;
        final long expectedBillsStackedSinceLastCashboxService = 0xCBCBCBCBL;
        final long expectedDistanceMovedSinceLastMasService = 0xBABABABAL;
        final long expectedDistanceMovedSinceLastSpringRollerService = 0xA9A9A9A9L;

        var getServiceUsageCountersResponseMessage = new GetServiceUsageCountersResponseMessage(ByteUtils.convertByteArrayToList(responsePayload));

        assertTrue(getServiceUsageCountersResponseMessage.IsValid.get());
        assertEquals(expectedDistanceMovedSinceLastTachSensorService, getServiceUsageCountersResponseMessage.getDistanceMovedSinceLastTachSensorService());
        assertEquals(expectedDistanceMovedSinceLastBillPathService, getServiceUsageCountersResponseMessage.getDistanceMovedSinceLastBillPathService());
        assertEquals(expectedDistanceMovedSinceLastBeltService, getServiceUsageCountersResponseMessage.getDistanceMovedSinceLastBeltService());
        assertEquals(expectedBillsStackedSinceLastCashboxService, getServiceUsageCountersResponseMessage.getBillsStackedSinceLastCashboxService());
        assertEquals(expectedDistanceMovedSinceLastMasService, getServiceUsageCountersResponseMessage.getDistanceMovedSinceLastMasService());
        assertEquals(expectedDistanceMovedSinceLastSpringRollerService, getServiceUsageCountersResponseMessage.getDistanceMovedSinceLastSpringRollerService());
    }

    @Test
    public void GetServiceFlagsResponseMessage_DeserializesServiceFlags() {
        var responsePayload = new byte[]
                {
                        0x02, 0x0B, 0x61,
                        0b00000000, 0b00000001, 0b00000001, 0b00000010, 0b00000010, 0b00000011,
                        0x03, 0x69
                };

        final byte expectedTachSensorServiceSuggestor =
                GetServiceFlagsResponseMessage.ServiceSuggestor.None;
        final byte expectedBillPathServiceSuggestor =
                GetServiceFlagsResponseMessage.ServiceSuggestor.UsageMetrics;
        final byte expectedCashboxBeltServiceSuggestor =
                GetServiceFlagsResponseMessage.ServiceSuggestor.UsageMetrics;
        final byte expectedCashboxMechanismServiceSuggestor =
                GetServiceFlagsResponseMessage.ServiceSuggestor.DiagnosticsAndError;
        final byte expectedMasServiceSuggestor =
                GetServiceFlagsResponseMessage.ServiceSuggestor.DiagnosticsAndError;
        final byte expectedSpringRollerServiceSuggestor =
                GetServiceFlagsResponseMessage.ServiceSuggestor.UsageMetrics
                | GetServiceFlagsResponseMessage.ServiceSuggestor.DiagnosticsAndError;

        var getServiceFlagsResponseMessage = new GetServiceFlagsResponseMessage(ByteUtils.convertByteArrayToList(responsePayload));

        assertTrue(getServiceFlagsResponseMessage.IsValid.get());
        assertEquals(expectedTachSensorServiceSuggestor, getServiceFlagsResponseMessage.getTachSensorServiceSuggestor().flags);
        assertEquals(expectedBillPathServiceSuggestor, getServiceFlagsResponseMessage.getBillPathServiceSuggestor().flags);
        assertEquals(expectedCashboxBeltServiceSuggestor, getServiceFlagsResponseMessage.getCashboxBeltServiceSuggestor().flags);
        assertEquals(expectedCashboxMechanismServiceSuggestor, getServiceFlagsResponseMessage.getCashboxMechanismServiceSuggestor().flags);
        assertEquals(expectedMasServiceSuggestor, getServiceFlagsResponseMessage.getMasServiceSuggestor().flags);
        assertEquals(expectedSpringRollerServiceSuggestor, getServiceFlagsResponseMessage.getSpringRollersuggestor().flags);
    }

    @Test
    public void GetServiceInfoResponseMessage_DeserializesServiceInfo() {
        var responsePayload = new byte[]
                {
                        0x02, 0x11, 0x61,
                        (byte)0b11111111, (byte)0b11111111, (byte)0b11111111, (byte)0b11111111,
                        (byte)0b10111111, (byte)0b10111111, (byte)0b10111111, (byte)0b10111111,
                        (byte)0b10011111, (byte)0b10011111, (byte)0b10011111, (byte)0b10011111,
                        0x03, 0x70
                };

        byte[] expectedLastCustomerService = new byte[] { (byte)0b01111111, (byte)0b01111111, (byte)0b01111111, (byte)0b01111111 };
        byte[] expectedLastServiceCenterService = new byte[] { (byte)0b00111111, (byte)0b00111111, (byte)0b00111111, (byte)0b00111111 };
        byte[] expectedLastOemService = new byte[] { (byte)0b00011111, (byte)0b00011111, (byte)0b00011111, (byte)0b00011111 };

        var getServiceInfoResponseMessage = new GetServiceInfoResponseMessage(ByteUtils.convertByteArrayToList(responsePayload));

        assertTrue(getServiceInfoResponseMessage.IsValid.get());
        assertArrayEquals(expectedLastCustomerService, ByteUtils.convertListToByteArray(getServiceInfoResponseMessage.getLastCustomerService()));
        assertArrayEquals(expectedLastServiceCenterService, ByteUtils.convertListToByteArray(getServiceInfoResponseMessage.getLastServiceCenterService()));
        assertArrayEquals(expectedLastOemService, ByteUtils.convertListToByteArray(getServiceInfoResponseMessage.getLastOemService()));
    }

    @Test
    public void GetFirmwareMetricsResponseMessage_DeserializesFirmwareMetrics() {
        var responsePayload = new byte[]
                {
                        0x02, 0x45, 0x61,
                        0x0F, 0x0E, 0x0F, 0x0E, 0x0F, 0x0E, 0x0F, 0x0E,
                        0x0E, 0x0D, 0x0E, 0x0D, 0x0E, 0x0D, 0x0E, 0x0D,
                        0x0D, 0x0C, 0x0D, 0x0C, 0x0D, 0x0C, 0x0D, 0x0C,
                        0x0C, 0x0B, 0x0C, 0x0B,
                        0x0B, 0x0A, 0x0B, 0x0A,
                        0x0A, 0x09, 0x0A, 0x09, 0x0A, 0x09, 0x0A, 0x09,
                        0x09, 0x08, 0x09, 0x08, 0x09, 0x08, 0x09, 0x08,
                        0x08, 0x07, 0x08, 0x07,
                        0x07, 0x06, 0x07, 0x06,
                        0x06, 0x05, 0x06, 0x05, 0x06, 0x05, 0x06, 0x05,
                        0x03, 0x24
                };

        final long expectedFlashUpdateCount = 0xFEFEFEFEL;
        final long expectedUsbFlashDriveFirmwareUpdateCount = 0xEDEDEDEDL;
        final long expectedTotalFlashDriveInsertCount = 0xDCDCDCDCL;
        final int expectedFirmwareCountryRevision = 0xCBCB;
        final int expectedFirmwareCoreRevision = 0xBABA;
        final long expectedFirmwareBuildRevision = 0xA9A9A9A9L;
        final long expectedFirmwareCrc = 0x98989898L;
        final int expectedBootloaderMajorRevision = 0x8787;
        final int expectedBootloaderMinorRevision = 0x7676;
        final long expectedBootloaderBuildRevision = 0x65656565L;

        var getFirmwareMetricsResponseMessage = new GetFirmwareMetricsResponseMessage(ByteUtils.convertByteArrayToList(responsePayload));

        assertTrue(getFirmwareMetricsResponseMessage.IsValid.get());
        assertEquals(expectedFlashUpdateCount, getFirmwareMetricsResponseMessage.getFlashUpdateCount());
        assertEquals(expectedUsbFlashDriveFirmwareUpdateCount, getFirmwareMetricsResponseMessage.getUsbFlashDriveFirmwareUpdateCount());
        assertEquals(expectedTotalFlashDriveInsertCount, getFirmwareMetricsResponseMessage.getTotalFlashDriveInsertCount());
        assertEquals(expectedFirmwareCountryRevision, getFirmwareMetricsResponseMessage.getFirmwareCountryRevision());
        assertEquals(expectedFirmwareCoreRevision, getFirmwareMetricsResponseMessage.getFirmwareCoreRevision());
        assertEquals(expectedFirmwareBuildRevision, getFirmwareMetricsResponseMessage.getFirmwareBuildRevision());
        assertEquals(expectedFirmwareCrc, getFirmwareMetricsResponseMessage.getFirmwareCrc());
        assertEquals(expectedBootloaderMajorRevision, getFirmwareMetricsResponseMessage.getBootloaderMajorRevision());
        assertEquals(expectedBootloaderMinorRevision, getFirmwareMetricsResponseMessage.getBootloaderMinorRevision());
        assertEquals(expectedBootloaderBuildRevision, getFirmwareMetricsResponseMessage.getBootloaderBuildRevision());
    }
}
