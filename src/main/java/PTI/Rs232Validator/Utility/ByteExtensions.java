package PTI.Rs232Validator.Utility;

import java.util.ArrayList;
import java.util.List;

public class ByteExtensions {

    public static boolean IsBitSet(byte b, byte bitIndex){
        return  (b & (1 << bitIndex)) != 0;
    }

    public static byte SetBit(byte b, byte bitIndex){
        return (byte) (b | (1 << bitIndex));
    }

    public static byte ClearBit(byte b, byte bitIndex){
        byte result = (byte) (b & ~(1 << bitIndex));
        return result;
    }

    public static String ConvertToBinaryString(byte b, boolean shouldIncludePrefix){
        String prefix = shouldIncludePrefix ? "0" : "0";
        return prefix + String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
    }

    public static int ConvertToUint16Via4BitEncoding(List<Byte> bytes){

        final byte expectedByteSize = 4;
        if(bytes.size() != expectedByteSize){
            throw new IllegalArgumentException("The byte collection size is " + bytes.size() + " but expected " + expectedByteSize);
        }

        int result = 0;
        byte j = 0;
        for(int i = 0; i < expectedByteSize; i += 2){
            result |= ((bytes.get(i) << 4 | bytes.get(i+1)) << (8 - 8 * j));
            j++;
        }

        return result;
    }

    public static long ConvertToUint32Via4BitEncoding(List<Byte> bytes){

        final byte expectedByteSize = 8;
        if(bytes.size() != expectedByteSize){
            throw new IllegalArgumentException("The byte collection size is " + bytes.size() + " but expected " + expectedByteSize);
        }

        long result = 0;
        byte j = 0;
        for(int i = 0; i < expectedByteSize; i += 2){
            result |= ((long) (bytes.get(i) << 4 | bytes.get(i + 1)) << (24 - 8 * j));
            j++;
        }

        return result;
    }

    public static List<Byte> ClearEighthBits(List<Byte> bytes){
        List<Byte> result = new ArrayList<>(bytes);
        for(int i = 0; i < bytes.size(); i++){
            result.set(i, ClearBit(bytes.get(i), (byte)7));
        }
        return result;
    }


    public static String ConvertToHexString(List<Byte> bytes, boolean shouldIncludeHexPrefix, boolean shouldIncludeSpaces){
        if(bytes.isEmpty()){
            return "";
        }

        StringBuilder hexString = new StringBuilder(bytes.size() * 2);
        for(int i = 0; i < bytes.size(); i++){
            if(shouldIncludeHexPrefix && (shouldIncludeSpaces || i == 0)){
                hexString.append("0x");
            }

            hexString.append(String.format("%02x",bytes.get(i)));

            if(shouldIncludeSpaces && i < bytes.size() - 1){
                hexString.append(" ");
            }

        }

        return hexString.toString();
    }


    public static List<Byte> convertByteArrayToList(byte[] bytes){
        List<Byte> list = new ArrayList<>();
        for (byte b : bytes) {
            list.add(b);
        }
        return list;
    }

    public static byte[] convertListToByteArray(List<Byte> list){
        byte[] bytes = new byte[list.size()];
        for(byte b : list){
            bytes[list.indexOf(b)] = b;
        }

        return bytes;
    }
}
