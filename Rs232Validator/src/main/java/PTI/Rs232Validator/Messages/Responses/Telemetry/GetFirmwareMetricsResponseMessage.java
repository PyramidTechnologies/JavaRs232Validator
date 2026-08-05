package PTI.Rs232Validator.Messages.Responses.Telemetry;

import PTI.Rs232Validator.Utility.ByteUtils;

import java.util.List;

/**
 * An RS-232 message from an acceptor to a host for {@link PTI.Rs232Validator.Messages.Commands.TelemetryCommand#GetFirmwareMetrics GetFirmwareMetrics}
 */
public class GetFirmwareMetricsResponseMessage extends TelemetryResponseMessage{

    private final byte PayloadByteSize = 69;

    /**
     * Initializes a new instance of {@link GetFirmwareMetricsResponseMessage}
     */
    public GetFirmwareMetricsResponseMessage(List<Byte> payload) {
        super(payload);

        if(!PayloadIssues.isEmpty()){
            return;
        }

        if(payload.size() != PayloadByteSize){
            PayloadIssues.add(String.format("The payload size is %d bytes, but %d bytes are expected", payload.size(), PayloadByteSize));
            return;
        }

        FlashUpdateCount = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(0, 8));
        UsbFlashDriveFirmwareUpdateCount = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(8, 16));
        TotalFlashDriveInsertCount = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(16, 24));
        FirmwareCountryRevision = ByteUtils.ConvertToUint16Via4BitEncoding(Data.subList(24, 28));
        FirmwareCoreRevision = ByteUtils.ConvertToUint16Via4BitEncoding(Data.subList(28, 32));
        FirmwareBuildRevision = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(32, 40));
        FirmwareCrc = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(40, 48));
        BootloaderMajorRevision = ByteUtils.ConvertToUint16Via4BitEncoding(Data.subList(48, 52));
        BootloaderMinorRevision = ByteUtils.ConvertToUint16Via4BitEncoding(Data.subList(52, 56));
        BootloaderBuildRevision = ByteUtils.ConvertToUint32Via4BitEncoding(Data.subList(56, 64));
    }

    private long FlashUpdateCount;

    /**
     * The total times an acceptor has had a firmware update.
     */
    public long getFlashUpdateCount() {
        return FlashUpdateCount;
    }

    private long UsbFlashDriveFirmwareUpdateCount;

    /**
     * The total times an acceptor has had a firmware update via a flash drive.
     */
    public long getUsbFlashDriveFirmwareUpdateCount() {
        return UsbFlashDriveFirmwareUpdateCount;
    }

    private long TotalFlashDriveInsertCount;

    /**
     * The total times an acceptor has detected a flash drive insert.
     */
    public long getTotalFlashDriveInsertCount() {
        return TotalFlashDriveInsertCount;
    }

    private int FirmwareCountryRevision;

    /**
     * The country revision of the firmware.
     */
    public int getFirmwareCountryRevision() {
        return FirmwareCountryRevision;
    }

    private int FirmwareCoreRevision;

    /**
     * The core revision of the firmware.
     */
    public int getFirmwareCoreRevision() {
        return FirmwareCoreRevision;
    }

    private long FirmwareBuildRevision;

    /**
     * The build revision of the firmware.
     */
    public long getFirmwareBuildRevision() {
        return FirmwareBuildRevision;
    }

    private long FirmwareCrc;

    /**
     * The CRC of the firmware.
     */
    public long getFirmwareCrc() {
        return FirmwareCrc;
    }

    private int BootloaderMajorRevision;

    /**
     * The major revision of the bootloader.
     */
    public int getBootloaderMajorRevision() {
        return BootloaderMajorRevision;
    }

    public int BootloaderMinorRevision;

    /**
     * The minor revision of the bootloader.
     */
    public int getBootloaderMinorRevision() {
        return BootloaderMinorRevision;
    }

    private long BootloaderBuildRevision;

    /**
     * The build revision of the bootloader.
     */
    public long getBootloaderBuildRevision() {
        return BootloaderBuildRevision;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return IsValid.get()
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
