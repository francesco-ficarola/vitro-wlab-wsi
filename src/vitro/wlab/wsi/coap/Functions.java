package vitro.wlab.wsi.coap;

import java.util.Map;
import java.util.Map.Entry;

public class Functions {
	
	public static short byteArraytoShort(byte[] data) {
		return (short) (data[0] & 0xFF | data[1] << 8);
	}
	
	public static byte[] shortToByteArray(short data) {
		return new byte[] { (byte) data, (byte) (data >> 8) };
	}
	
	public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
	    for (Entry<T, E> entry : map.entrySet()) {
	        if (value.equals(entry.getValue())) {
	            return entry.getKey();
	        }
	    }
	    return null;
	}
}
