package PTI.Rs232Validator.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * A container of utility methods for {@code byte}
 */
public class ByteUtils {

    /**
     * Indicates whether the specified bit is set (i.e. 1).
     * @param b The byte to observe
     * @param bitIndex The 0-based index of the bit (e.g. 0 -> 2^0).
     */
    public static boolean IsBitSet(byte b, byte bitIndex){
        return  (b & (1 << bitIndex)) != 0;
    }

    /**
     * Sets the specified bit
     * @param b The byte to mutate
     * @param bitIndex The 0-based index of the bit to set (e.g. 0 -> 2^0)
     * @return The mutated byte
     */
    public static byte SetBit(byte b, byte bitIndex){
        return (byte) (b | (1 << bitIndex));
    }

    /**
     * Clears the specified bit
     * @param b The byte to mutate
     * @param bitIndex The 0-index of the bit to clear (e.g. 0 -> 2^0).
     * @return The mutated byte
     */
    public static byte ClearBit(byte b, byte bitIndex){
        return (byte) (b & ~(1 << bitIndex));
    }

    /**
     * Converts the specified byte to a String representation of its binary value
     * @param b The byte to convert
     * @param shouldIncludePrefix {@code true} to indicated the binary prefix "0b"; otherwise, {@code false}.
     * @return The binary String
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     *     byte b = 0b00000001;
     *     System.out.println(ByteUtils.ConvertToBinaryString(b, true)); // Output: 0b00000001
     *     System.out.println(ByteUtils.ConvertToBinaryString(b, false)); // Output: 00000001
     * }</pre>
     */
    public static String ConvertToBinaryString(byte b, boolean shouldIncludePrefix){
        String prefix = shouldIncludePrefix ? "0b" : "";
        return prefix + String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
    }

    /**
     * Converts the specified 4-byte collection to a 16-bit unsign integer via 4-bit encoding under big-endian order.
     * @param bytes The 4-byte collection to convert
     * @return The 16-bit unsigned integer
     */
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

    /**
     * Converts the specified 8-byte collection to a 32-bit unsigned integer via 4-bit encoding under big-endian order
     * @param bytes The 8-byte collection to convert
     * @return The 32-bit unsigned integer
     */
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

    /**
     * Clears the 8th bit of each byte in the specified collection
     * @param bytes The byte collection to mutate
     * @return The mutated byte collection
     */
    public static List<Byte> ClearEighthBits(List<Byte> bytes){
        List<Byte> result = new ArrayList<>(bytes);
        for(int i = 0; i < bytes.size(); i++){
            result.set(i, ClearBit(bytes.get(i), (byte)7));
        }
        return result;
    }

    /**
     * Converts the specified byte collection to a hexadecimal string
     * @param bytes The byte collection to convert
     * @param shouldIncludeHexPrefix {@code true} to include the hex prefix "0x"; otherwise {@code false}
     * @param shouldIncludeSpaces {@code true} to include spaces between each byte; otherwise {@code false}
     * @return The hexadecimal string
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     *     List<Byte> bytes = new ArrayList<>()
     *     {
     *          {
     *               add((byte) 0x01);
     *               add((byte) 0x02);
     *               add((byte) 0x03);
     *               add((byte) 0x04);
     *          }
     *     };
     *     System.out.println(ByteUtils.ConvertToHexString(bytes, true, true); // Output: 0x01 0x02 0x03 0x04
     *     System.out.println(ByteUtils.ConvertToHexString(bytes, true, false); // Output: 0x01020304
     *     System.out.println(ByteUtils.ConvertToHexString(bytes, false, true); // Output: 01 02 03 04
     *     System.out.println(ByteUtils.ConvertToHexString(bytes, false, false); // Output: 01020304
     * }</pre>
     */
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
