package PTI.Rs232Validator.SerialProviders;

import java.util.List;

public interface ISerialProvider {

    boolean IsOpen = false;

    boolean TryOpen();


    void Close();


    List<Byte> Read(int count);


    void Write(List<Byte> data);
}
