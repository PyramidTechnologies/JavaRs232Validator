package PTI.Rs232Validator.SerialProviders;

import PTI.Rs232Validator.Loggers.ILogger;

/**
 * A provider of serial communication to an external device
 */
public interface ISerialProvider{

    /**
     * Sends the config command to the FTDI chip, allowing for communication
     * @param baud The baud rate
     * @param dataBits How number of data bits
     * @param stopBits The stop bit of each byte
     * @param parity The parity
     * @param flowControl The flow control
     */
    void SetConfig(int baud, byte dataBits, byte stopBits,
                   byte parity, byte flowControl);



    /**
     * Resumes the connection after app resumes from pause or is re-opened
     */
    int ResumeAccessory();

    void OnPause();

    /**
     * Reads data from the external device
     * @return If successful, an array with the requested count of bytes;
     * otherwise, an array with less then the requested count of bytes.
     */
    byte Read(int numBytes, byte[] buffer, int[] actualNumBytes);

    /**
     * Writes data to the external device
     * @param data The data to write
     */
    byte Write(int numBytes, byte[] data);

    /**
     * Closes the connection to the external device
     * @implNote Closing the connection also closes the application
     */
    void close();

    void SetLogger(ILogger logger);

}
