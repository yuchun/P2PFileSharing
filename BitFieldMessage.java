
public class BitFieldMessage extends NormalMessage{
	
	public BitFieldMessage(byte[] bitstream){
		super(bitstream);		
	}
	
	/*
	 * Build from a payload
	 * @param msgType is useless, just differentiate from another constructor
	 * */
	public BitFieldMessage(int msgType, byte[] bitfield){
		super(Message.BITFIELD, bitfield);	
	}
		
	public byte[] getBitFieldFromPayload(){
		return payload;
	}
	

}
