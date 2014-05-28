
public abstract class NormalMessage implements Message{
	protected int length;
	protected byte type;
	protected byte[] payload;
	NormalMessage(byte type, byte[] payload){
		 this.length = 1;
		 if(payload != null && payload.length > 0)
			 this.length += payload.length;
		 this.type = type;
		 this.payload = payload;
	}

	NormalMessage(byte[]bitstream){
		this.type = bitstream[4];
		length = Misc.bytes2Int(bitstream);
		payload = new byte[length - 1];
		if(payload != null)
			System.arraycopy(bitstream, MessageHeaderSize, payload, 0, length - 1);
	}

	@Override
	public byte[] getBytes() {
		// TODO Auto-generated method stub
		byte[] ret = null;
		if(payload == null || payload.length == 0)
			ret = new byte[5];
		else
			ret = new byte[5 + payload.length];
		System.arraycopy(Misc.int2Bytes(length), 0, ret, 0, 4);
		ret[4] = type;
		if(payload != null && payload.length > 0)
			System.arraycopy(payload, 0, ret, 5, payload.length);
		return ret;
	}
	
	public int getMessageType(){
		return type;
	}
	
	public static int getMessageType(byte[] msg){
		byte ret = msg[4];
		return ret;
	}
	
	public static int getMessageLength(byte[]msg){
		return Misc.bytes2Int(msg);
	}
	
	public static final int MessageHeaderSize = 5;
}
