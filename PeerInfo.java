import java.net.Socket;

public class PeerInfo {
	public final String peerId;
	public final String peerAddress;
	public final int peerPort;
	public final boolean has;
	
	private volatile Socket socket = null;
	private volatile int status;
	private BitFields bitfields = null;
	
	public PeerInfo(String pId, String pAddress, String pPort, boolean has, int fileSize, int pieceSize) {
		this.peerId = pId;
		peerAddress = pAddress;
		this.peerPort = Integer.parseInt(pPort);
		this.has = has;
		if(has)
			this.status = PeerInfo.STATUS_COMPLETED;
		int num = fileSize / pieceSize;
		if((fileSize%pieceSize) > 0)
			num++;
		this.bitfields = new BitFields(num, false);
	}
	
	public void setSocket(Socket socket){this.socket = socket;}
	
	public Socket getSocket(){return socket;}

	public void setStatus(int status){this.status = status;}
	
	public int getStatus(){return status;}
	
	public synchronized void setBitfields(BitFields bitfields){
		this.bitfields = bitfields;
	}
	
	public synchronized void setBitfields(byte[] bitfields, int size){
		this.bitfields = new BitFields(bitfields, size);
	}
	
	public synchronized BitFields getBitfields(){
		return bitfields;
	}
	
	public static final int STATUS_NOTCONNECTED = 0;
	public static final int STATUS_TCPCONNECTED = (1<<0);
	public static final int STATUS_HANDSHAKETO = (1<<1);
	public static final int STATUS_HANDSHAKEFROM = (1<<2);
	public static final int STATUS_HANDSHAKEDBOTH = (1<<3);
	//public static final int STATUS_CHOKED = (0<<4);
	public static final int STATUS_UNCHOKED = (1<<4);
	//public static final int STATUS_NOT_INTERESTED = (0<<5);
	public static final int STATUS_INTERESTED = (1<<5);
	public static final int STATUS_OPT_UNCHOKED = (1<<6);
	public static final int STATUS_COMPLETED = (1<<7);
}