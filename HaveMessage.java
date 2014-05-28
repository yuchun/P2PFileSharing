
public class HaveMessage extends NormalMessage{
	public HaveMessage(byte[] message){
		super(message);
	}
	
	public HaveMessage(int pieceIndex){
		super(Message.HAVE, Misc.int2Bytes(pieceIndex));		
	}
	
	public int getPieceIndexFromPayload(){
		int rval = Misc.bytes2Int(payload);
		return rval;
	}
}
