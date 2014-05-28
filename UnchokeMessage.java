
public class UnchokeMessage extends NormalMessage{
	UnchokeMessage(byte[] message){
		super(message);
	}
	
	UnchokeMessage() {
		super(Message.UNCHOKE, null);
		// TODO Auto-generated constructor stub
	}

}
