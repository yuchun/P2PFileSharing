
public class HandShakeMessage implements Message{
	private byte[] handshakeHeader = {'H','E','L','L','O'};
	//private final char[] zeroBits = new char[23];
	private byte[] zeroBits = new byte[23];
	private String peerId;

	public HandShakeMessage(String peerId){
		this.peerId = peerId;
	}
	
	public HandShakeMessage(byte[] message){
		System.arraycopy(message, 0, handshakeHeader, 0, 5);
		byte[] bytePeerId = new byte[4];
		System.arraycopy(message, 28, bytePeerId, 0, 4);
		this.peerId = Misc.bytes2String(bytePeerId);
	}
	
	public boolean isHandShakeMessage(){
		return new String(handshakeHeader).equals("HELLO");
	}
	
	public String getPeerId(){
		return peerId;
	}
	
	public String toString(){
		String rstr = "";
		for(byte b: handshakeHeader)
			rstr += (char)b;
		for(int i=0; i<zeroBits.length; i++)
			rstr += "0";
		rstr += peerId;
		return rstr;
	}
	public byte[] getBytes(){
		byte[] rval = new byte[handshakeHeader.length + zeroBits.length + 4];
		System.arraycopy(handshakeHeader, 0, rval, 0, handshakeHeader.length);
		System.arraycopy(zeroBits, 0, rval, handshakeHeader.length, zeroBits.length);
		System.arraycopy(Misc.string2Bytes(peerId), 0, rval, handshakeHeader.length + zeroBits.length, 4);
		return rval;
	}
	@Override
	public int getMessageType() {
		// TODO Auto-generated method stub
		return Message.HANDSHAKE;
	}

}
