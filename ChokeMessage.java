
public class ChokeMessage extends NormalMessage{
	ChokeMessage(byte[] message){
		super(message);
	}
	
	ChokeMessage() {
		super(Message.CHOKE, null);
		// TODO Auto-generated constructor stub
	}

}
