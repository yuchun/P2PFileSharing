
public class InterestedMessage extends NormalMessage{

	InterestedMessage(byte[] bitstream) {
		super(bitstream);
		// TODO Auto-generated constructor stub
	}

	InterestedMessage() {
		super((byte)Message.INTERESTED, null);
		// TODO Auto-generated constructor stub
	}

}
