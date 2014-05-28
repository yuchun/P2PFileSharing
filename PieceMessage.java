
public class PieceMessage extends NormalMessage{
	public PieceMessage(byte[] message){
		super(message);
	}
	
	public PieceMessage(int pieceIndex, byte[]payload){
		super(Message.PIECE, combine(pieceIndex, payload));		
	}
	
	public int getPieceIndexFromPayload(){
		return Misc.bytes2Int(payload);
	}
	
	public byte[] getPayload(){
		byte[] ret = new byte[this.payload.length - 4];
		System.arraycopy(payload, 4, ret, 0, ret.length);
		return ret;
	}
	
	static byte[] combine(int pieceIndex, byte[]payload){
		byte[] ret = new byte[4 + payload.length];
		System.arraycopy(Misc.int2Bytes(pieceIndex), 0, ret, 0, 4);
		System.arraycopy(payload, 0, ret, 4, payload.length);
		return ret;
	}
}
