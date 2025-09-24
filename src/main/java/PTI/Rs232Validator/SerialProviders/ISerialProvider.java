package PTI.Rs232Validator.SerialProviders;

public interface ISerialProvider {

    boolean IsOpen();

    boolean TryOpen();

    void Close();

    byte[] Read(long count);

    void Write(byte[] data);
}
