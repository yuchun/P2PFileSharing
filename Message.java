
public interface Message {
	public static byte CHOKE = 0;
	public static byte UNCHOKE = 1; 
	public static byte INTERESTED = 2;
	public static byte NOT_INTERESTED = 3; 
	public static byte HAVE = 4; 
	public static byte BITFIELD = 5; 
	public static byte REQUEST = 6;
	public static byte PIECE = 7;
	public static byte HANDSHAKE = 8;
	
	public byte[] getBytes();
	public int getMessageType();
}
