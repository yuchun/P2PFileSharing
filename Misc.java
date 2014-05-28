import java.util.Arrays;


public class Misc {
	public static void main(String[] args){
		int ival = 1234;
		byte[] bval = int2Bytes(ival);
		ival = bytes2Int(bval);
		System.out.println(ival);
		for(int i=0; i<bval.length; i++)
			System.out.printf("%02x", bval[i]&0xff);
		System.out.println();
		
		String str = "1003";
		bval = string2Bytes(str);
		System.out.println(Arrays.toString(bval));
		str = bytes2String(bval);
		System.out.println(str);
		return;
	}
	/*convert a 4 byte array to integer*/
	public static int bytes2Int(byte[] bytes){
		int rval = 0;
		for(int i=0; i<4; i++){
			rval = rval<<8;
			rval += bytes[i]&0xff;
		}
		return rval;
	}
	
	/*convert a integer to a 4 byte array*/
	public static byte[] int2Bytes(int ival){
		byte[] bytes = new byte[4];
		for(int i=3; i>=0; i--){
			bytes[i] = (byte) (ival&0xff);
			ival = ival>>8;
		}
		return bytes;
	}
	
	public static byte[] string2Bytes(String str){
		byte[] bytes = new byte[str.length()];
		for(int i=0; i<bytes.length; i++)
			bytes[i] = (byte) Integer.parseInt(""+str.charAt(i), 10);
		return bytes;
	}
	
	public static String bytes2String(byte[] bval){
		//Arrays.
		String str="";
		for(int i=0; i<bval.length; i++)
			str += String.valueOf(bval[i]);
		return str;
	}
}
