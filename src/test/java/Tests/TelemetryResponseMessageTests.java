package Tests;

import PTI.Rs232Validator.Messages.Responses.Telemetry.*;
import PTI.Rs232Validator.Utility.ByteExtensions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TelemetryResponseMessageTests {

    @Test
    public void TelemetryResponseMessage_DeserializesData(){
        byte[] payload = new byte[]{ 0x02, 0x0E, 0x61, 0x32, 0x31, 0x30, 0x32, 0x36, 0x30, 0x30, 0x31, 0x30, 0x03, 0x59 };

        byte[] expectedData = new byte[]{ 0x32, 0x31, 0x30, 0x32, 0x36, 0x30, 0x30, 0x31, 0x30 };

        TelemetryResponseMessage telemetryResponseMessage = new TelemetryResponseMessage(ByteExtensions.convertByteArrayToList(payload));

        Assertions.assertTrue(telemetryResponseMessage.IsValid);
        Assertions.assertEquals(ByteExtensions.convertByteArrayToList(expectedData), telemetryResponseMessage.getData());
    }

    @Test
    public void GetSerialNumberResponseMessage_DeserializesSerialNumber(){
        byte[] payload = new byte[]{ 0x02, 0x0E, 0x61, 0x32, 0x31, 0x30, 0x32, 0x36, 0x30, 0x30, 0x31, 0x30, 0x03, 0x59 };

        final String expectedSerialNumber = "210260010";

        GetSerialNumberResponseMessage getSerialNumberResponseMessage = new GetSerialNumberResponseMessage(ByteExtensions.convertByteArrayToList(payload));

        Assertions.assertTrue(getSerialNumberResponseMessage.IsValid);
        Assertions.assertEquals(expectedSerialNumber, getSerialNumberResponseMessage.getSerialNumber());
    }

    @Test
    public void GetCashboxMetricsResponseMessage_DeserializesCashboxMetrics(){
        byte[] payload = new byte[]{
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

        GetCashboxMetricsResponseMessage getCashboxMetricsResponseMessage = new GetCashboxMetricsResponseMessage(ByteExtensions.convertByteArrayToList(payload));

        Assertions.assertTrue(getCashboxMetricsResponseMessage.IsValid);
        Assertions.assertEquals(expectedCashboxRemovedCount, getCashboxMetricsResponseMessage.getCashboxRemovedCount());
        Assertions.assertEquals(expectedCashboxFullCount, getCashboxMetricsResponseMessage.getCashboxFullCount());
        Assertions.assertEquals(expectedBillsStackedSinceCashboxRemoved, getCashboxMetricsResponseMessage.getBillsStackedSinceCashboxRemoved());
        Assertions.assertEquals(expectedBillsStackedSincePowerUp, getCashboxMetricsResponseMessage.getBillsStackedSincePowerUp());
        Assertions.assertEquals(expectedAverageTimeToStack, getCashboxMetricsResponseMessage.getAverageTimeToStack());
        Assertions.assertEquals(expectedTotalBillsStacked, getCashboxMetricsResponseMessage.getTotalBillsStacked());
    }

    @Test
    public void GetUnitMetricsResponseMessage_DeserializesUnitMetrics(){
        byte[] payload = new byte[]{
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

        GetUnitMetricsResponseMessage getUnitMetricsResponseMessage = new GetUnitMetricsResponseMessage(ByteExtensions.convertByteArrayToList(payload));

        Assertions.assertTrue(getUnitMetricsResponseMessage.IsValid);
        Assertions.assertEquals(expectedTotalValueStacked, getUnitMetricsResponseMessage.getTotalValueStacked());
        Assertions.assertEquals(expectedTotalDistanceMoved, getUnitMetricsResponseMessage.getTotalDistanceMoved());
        Assertions.assertEquals(expectedPowerUpCount, getUnitMetricsResponseMessage.getPowerUpCount());
        Assertions.assertEquals(expectedPushButtonCount, getUnitMetricsResponseMessage.getPushButtonCount());
        Assertions.assertEquals(expectedConfigurationCount, getUnitMetricsResponseMessage.getConfigurationCount());
        Assertions.assertEquals(expectedUsbEnumerationsCount, getUnitMetricsResponseMessage.getUsbEnumerationsCount());
        Assertions.assertEquals(expectedTotalCheatAttemptsDetected, getUnitMetricsResponseMessage.getTotalCheatAttemptsDetected());
        Assertions.assertEquals(expectedTotalSecurityLockupCount, getUnitMetricsResponseMessage.getTotalSecurityLockupCount());
    }

    @Test
    public void GetServiceUsageCountersResponseMessage_DeserializesServiceUsageCounters(){
        byte[] payload = new byte[]{
                0x02, 0x35, 0x61,
                0x0F, 0x0E, 0x0F, 0x0E, 0x0F, 0x0E, 0x0F, 0x0E,
                0x0E, 0x0D, 0x0E, 0x0D, 0x0E, 0x0D, 0x0E, 0x0D,
                0x0D, 0x0C, 0x0D, 0x0C, 0x0D, 0x0C, 0x0D, 0x0C,
                0x0C, 0x0B, 0x0C, 0x0B, 0x0C, 0x0B, 0x0C, 0x0B,
                0x0B, 0x0A, 0x0B, 0x0A, 0x0B, 0x0A, 0x0B, 0x0A,
                0x0A, 0x09, 0x0A, 0x09, 0x0A, 0x09, 0x0A, 0x09,
                0x03, 0x54
        };

        final long expectedDistancedMovedSinceLastTachSensorService = 0xFEFEFEFEL;
        final long expectedDistanceMovedSinceLastBillPathService = 0xEDEDEDEDL;
        final long expectedDistancedMoveSinceLastBeltService = 0xDCDCDCDCL;
        final long expectedBillsStackedSinceLastCashboxService = 0xCBCBCBCBL;
        final long expectedDistanceMovedSinceLastMasService = 0xBABABABAL;
        final long expectedDistanceMovedSinceLastSpringRollerService = 0xA9A9A9A9L;

        GetServiceUsageCountersResponseMessage getServiceUsageCountersResponseMessage = new GetServiceUsageCountersResponseMessage(ByteExtensions.convertByteArrayToList(payload));

        Assertions.assertTrue(getServiceUsageCountersResponseMessage.IsValid);
        Assertions.assertEquals(expectedDistancedMovedSinceLastTachSensorService, getServiceUsageCountersResponseMessage.getDistanceMovedSinceLastTachSensorService());
        Assertions.assertEquals(expectedDistanceMovedSinceLastBillPathService, getServiceUsageCountersResponseMessage.getDistanceMovedSinceLastBillPathService());
        Assertions.assertEquals(expectedDistancedMoveSinceLastBeltService, getServiceUsageCountersResponseMessage.getDistanceMovedSinceLastBeltService());
        Assertions.assertEquals(expectedBillsStackedSinceLastCashboxService, getServiceUsageCountersResponseMessage.getBillsStackedSinceLastCashboxService());
        Assertions.assertEquals(expectedDistanceMovedSinceLastMasService, getServiceUsageCountersResponseMessage.getDistanceMovedSinceLastMasService());
        Assertions.assertEquals(expectedDistanceMovedSinceLastSpringRollerService, getServiceUsageCountersResponseMessage.getDistanceMovedSinceLastSpringRollerService());
    }

    @Test
    public void GetServiceFlagsResponseMessage_DeserializesServiceFlags(){
        byte[] payload = new byte[]{
                0x02, 0x0B, 0x61,
                0b00000000, 0b00000001, 0b00000001, 0b00000010, 0b00000010, 0b00000011,
                0x03, 0x69
        };

        final byte expectedTachSensorServiceSuggestor = GetServiceFlagsResponseMessage.ServiceSuggestor.None.getValue();
        final byte expectedBillPathServiceSuggestor = GetServiceFlagsResponseMessage.ServiceSuggestor.UsageMetrics.getValue();
        final byte expectedCashboxBeltServiceSuggestor = GetServiceFlagsResponseMessage.ServiceSuggestor.UsageMetrics.getValue();
        final byte expectedCashboxMechanismServiceSuggestor = GetServiceFlagsResponseMessage.ServiceSuggestor.DiagnosticsAndError.getValue();
        final byte expectedMasServiceSuggestor = GetServiceFlagsResponseMessage.ServiceSuggestor.DiagnosticsAndError.getValue();
        final byte expectedSpringRollersServiceSuggestor = (byte) (GetServiceFlagsResponseMessage.ServiceSuggestor.UsageMetrics.getValue() | GetServiceFlagsResponseMessage.ServiceSuggestor.DiagnosticsAndError.getValue());

        GetServiceFlagsResponseMessage getServiceFlagsResponseMessage = new GetServiceFlagsResponseMessage(ByteExtensions.convertByteArrayToList(payload));

        Assertions.assertTrue(getServiceFlagsResponseMessage.IsValid);
        Assertions.assertEquals(expectedTachSensorServiceSuggestor, getServiceFlagsResponseMessage.getTachSensorServiceSuggestor());
        Assertions.assertEquals(expectedBillPathServiceSuggestor, getServiceFlagsResponseMessage.getBillPathServiceSuggestor());
        Assertions.assertEquals(expectedCashboxBeltServiceSuggestor, getServiceFlagsResponseMessage.getCashboxBeltServiceSuggestor());
        Assertions.assertEquals(expectedCashboxMechanismServiceSuggestor, getServiceFlagsResponseMessage.getCashboxMechanismServiceSuggestor());
        Assertions.assertEquals(expectedMasServiceSuggestor, getServiceFlagsResponseMessage.getMasServiceSuggestor());
        Assertions.assertEquals(expectedSpringRollersServiceSuggestor, getServiceFlagsResponseMessage.getSpringRollersuggestor());
    }

    @Test
    public void GetServiceInfoResponseMessage_DeserializesServiceInfo(){
        byte[] payload = new byte[]{
                0x02, 0x11, 0x61,
                (byte)0b11111111, (byte)0b11111111, (byte)0b11111111, (byte) 0b11111111,
                (byte) 0b10111111, (byte) 0b10111111, (byte) 0b10111111, (byte) 0b10111111,
                (byte) 0b10011111, (byte) 0b10011111, (byte) 0b10011111, (byte) 0b10011111,
                0x03, 0x70
        };

        byte[] expectedLastCustomerService = {(byte)0b01111111, (byte)0b01111111, (byte)0b01111111, (byte) 0b01111111};
        byte[] expectedLastServiceCenterService = {(byte) 0b00111111, (byte) 0b00111111, (byte) 0b00111111, (byte) 0b00111111};
        byte[] expectedLastOemService = {(byte) 0b00011111, (byte) 0b00011111, (byte) 0b00011111, (byte) 0b00011111};

        GetServiceInfoResponseMessage getServiceInfoResponseMessage = new GetServiceInfoResponseMessage(ByteExtensions.convertByteArrayToList(payload));

        Assertions.assertTrue(getServiceInfoResponseMessage.IsValid);
        Assertions.assertEquals(ByteExtensions.convertByteArrayToList(expectedLastCustomerService), getServiceInfoResponseMessage.getLastCustomerService());
        Assertions.assertEquals(ByteExtensions.convertByteArrayToList(expectedLastServiceCenterService), getServiceInfoResponseMessage.getLastServiceCenterService());
        Assertions.assertEquals(ByteExtensions.convertByteArrayToList(expectedLastOemService), getServiceInfoResponseMessage.getLastOemService());
    }

    @Test
    public void GetFirmwareMetricsResponseMessage_DeserializesFirmwareMetrics(){
        byte[] payload = new byte[]{
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
        final int expectedFirmwareCountryRevision = 0xCBCB & 0xFFFF;
        final int expectedFirmwareCoreRevision = 0xBABA & 0xFFFF;
        final long expectedFirmwareBuildRevision = 0xA9A9A9A9L;
        final long expectedFirmwareCrc = 0x98989898L;
        final int expectedBootloaderMajorRevision = 0x8787 & 0xFFFF;
        final int expectedBootloaderMinorRevision = 0x7676 & 0xFFFF;
        final long expectedBootloaderBuildRevision = 0x65656565;

        GetFirmwareMetricsResponseMessage getFirmwareMetricsResponseMessage = new GetFirmwareMetricsResponseMessage(ByteExtensions.convertByteArrayToList(payload));

        Assertions.assertTrue(getFirmwareMetricsResponseMessage.IsValid);
        Assertions.assertEquals(expectedFlashUpdateCount, getFirmwareMetricsResponseMessage.getFlashUpdateCount());
        Assertions.assertEquals(expectedUsbFlashDriveFirmwareUpdateCount, getFirmwareMetricsResponseMessage.getUsbFlashDriveFirmwareUpdateCount());
        Assertions.assertEquals(expectedTotalFlashDriveInsertCount, getFirmwareMetricsResponseMessage.getTotalFlashDriveInsertCount());
        Assertions.assertEquals(expectedFirmwareCountryRevision, getFirmwareMetricsResponseMessage.getFirmwareCountryRevision());
        Assertions.assertEquals(expectedFirmwareCoreRevision, getFirmwareMetricsResponseMessage.getFirmwareCoreRevision());
        Assertions.assertEquals(expectedFirmwareBuildRevision, getFirmwareMetricsResponseMessage.getFirmwareBuildRevision());
        Assertions.assertEquals(expectedFirmwareCrc, getFirmwareMetricsResponseMessage.getFirmwareCrc());
        Assertions.assertEquals(expectedBootloaderMajorRevision, getFirmwareMetricsResponseMessage.getBootloaderMajorRevision());
        Assertions.assertEquals(expectedBootloaderMinorRevision, getFirmwareMetricsResponseMessage.getBootloaderMinorRevision());
        Assertions.assertEquals(expectedBootloaderBuildRevision, getFirmwareMetricsResponseMessage.getBootloaderBuildRevision());
    }
}
