
public class RequestMessage extends NormalMessage{
	public RequestMessage(byte[] message){
		super(message);
	}
	
	public RequestMessage(int pieceIndex){
		super(Message.REQUEST, Misc.int2Bytes(pieceIndex));		
	}
	
	public int getPieceIndexFromPayload(){
		return Misc.bytes2Int(payload);
	}
	

}
