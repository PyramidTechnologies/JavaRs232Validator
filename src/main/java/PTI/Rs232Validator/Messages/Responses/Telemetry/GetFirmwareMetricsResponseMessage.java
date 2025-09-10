package PTI.Rs232Validator.Messages.Responses.Telemetry;

import PTI.Rs232Validator.Utility.ByteExtensions;

import java.util.List;

public class GetFirmwareMetricsResponseMessage extends TelemetryResponseMessage{

    private final byte PayloadByteSize = 69;

    public GetFirmwareMetricsResponseMessage(List<Byte> payload) {
        super(payload);

        if(!PayloadIssues.isEmpty()){
            return;
        }

        if(payload.size() != PayloadByteSize){
            PayloadIssues.add(String.format("The payload size is %d bytes, but %d bytes are expected", payload.size(), PayloadByteSize));
            return;
        }

        FlashUpdateCount = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(0, 8));
        UsbFlashDriveFirmwareUpdateCount = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(8, 16));
        TotalFlashDriveInsertCount = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(16, 24));
        FirmwareCountryRevision = ByteExtensions.ConvertToUint16Via4BitEncoding(Data.subList(24, 28));
        FirmwareCoreRevision = ByteExtensions.ConvertToUint16Via4BitEncoding(Data.subList(28, 32));
        FirmwareBuildRevision = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(32, 40));
        FirmwareCrc = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(40, 48));
        BootloaderMajorRevision = ByteExtensions.ConvertToUint16Via4BitEncoding(Data.subList(48, 52));
        BootloaderMinorRevision = ByteExtensions.ConvertToUint16Via4BitEncoding(Data.subList(52, 56));
        BootloaderBuildRevision = ByteExtensions.ConvertToUint32Via4BitEncoding(Data.subList(56, 64));
    }

    private long FlashUpdateCount;
    public long getFlashUpdateCount() {
        return FlashUpdateCount;
    }

    private long UsbFlashDriveFirmwareUpdateCount;
    public long getUsbFlashDriveFirmwareUpdateCount() {
        return UsbFlashDriveFirmwareUpdateCount;
    }

    private long TotalFlashDriveInsertCount;
    public long getTotalFlashDriveInsertCount() {
        return TotalFlashDriveInsertCount;
    }

    private int FirmwareCountryRevision;
    public int getFirmwareCountryRevision() {
        return FirmwareCountryRevision;
    }

    private int FirmwareCoreRevision;
    public int getFirmwareCoreRevision() {
        return FirmwareCoreRevision;
    }

    private long FirmwareBuildRevision;
    public long getFirmwareBuildRevision() {
        return FirmwareBuildRevision;
    }

    private long FirmwareCrc;
    public long getFirmwareCrc() {
        return FirmwareCrc;
    }

    private int BootloaderMajorRevision;
    public int getBootloaderMajorRevision() {
        return BootloaderMajorRevision;
    }

    public int BootloaderMinorRevision;
    public int getBootloaderMinorRevision() {
        return BootloaderMinorRevision;
    }

    private long BootloaderBuildRevision;
    public long getBootloaderBuildRevision() {
        return BootloaderBuildRevision;
    }

    @Override
    public String toString() {
        return IsValid
                ? String.format("Flash Update Count: %d | " +
                    "Usb Flash Drive Firmware Update Count: %d | " +
                    "Total Flash Drive Insert Count: %d | " +
                    "Firmware Country Revision: %d | " +
                    "Firmware Core Revision: %d | " +
                    "Firmware Build Revision: %d | " +
                    "Firmware Crc: %d | " +
                    "Bootloader Major Revision: %d | " +
                    "Bootloader Minor Revision: %d | " +
                    "Bootloader Build Revision: %d",
                    FlashUpdateCount,
                    UsbFlashDriveFirmwareUpdateCount,
                    TotalFlashDriveInsertCount,
                    FirmwareCountryRevision,
                    FirmwareCoreRevision,
                    FirmwareBuildRevision,
                    FirmwareCrc,
                    BootloaderMajorRevision,
                    BootloaderMinorRevision,
                    BootloaderBuildRevision)
                : super.toString();
    }
}
