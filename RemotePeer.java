import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;


public class RemotePeer {
	private PeerInfo peerInfo = null;
	private Socket socket = null;
	private OutputStream output = null;
	private InputStream input = null;
	private Peer localPeer;
	private Vector<ReceiveLog> rlog = new Vector<ReceiveLog>(10);
	private ReentrantLock lock = new ReentrantLock(); 
	private ReentrantLock logLock = new ReentrantLock();
	
	public RemotePeer(PeerInfo peerInfo, Peer localPeer){
		this.peerInfo = peerInfo;
		this.localPeer = localPeer;
	}
	
	public final PeerInfo getPeerInfo(){
		return peerInfo;
	}
	
	public int getStatus(){
		return peerInfo.getStatus();
	}
	
	public void setStatus(int st){
		peerInfo.setStatus(st);
	}
	
	public void addNewlyReceived(long time, int size){
		logLock.lock();
		try{
			if(rlog.size() >= 10){
				rlog.remove(0);
			}
			rlog.add(new ReceiveLog(time, size));
		}finally{
			logLock.unlock();
		}
	}
	
	public int getSpeed(){
		logLock.lock();
		try{
			if(rlog.size() == 0)
				return 0;
			int count = 0;
			for(ReceiveLog rl: rlog)
				count += rl.size;
			long time = System.currentTimeMillis() - rlog.get(0).time;
			return (int) (count / time);
		}finally{
			logLock.unlock();
		}
	}
	
	public void setSocketAndInputOutput(Socket socket, OutputStream output, InputStream input){
		lock.lock();
		try{
			this.socket = socket;
			this.output = output;
			this.input = input;
		}finally{
			lock.unlock();
		}
	}
	
	public final Socket getSocket(){
		return socket;
	}
	
	public void sendUnchokeMessage() throws IOException{
		UnchokeMessage umsg = new UnchokeMessage();
		lock.lock();
		try {
			output.write(umsg.getBytes());
			output.flush();
		}finally{
			lock.unlock();
		}			
	}
	
	public void sendChokeMessage() throws IOException{
		ChokeMessage cmsg = new ChokeMessage();
		lock.lock();
		try {
			output.write(cmsg.getBytes());
			output.flush();
		
		}finally{
			lock.unlock();
		}				
		
	}
	
	public boolean sendPieceMessage(int pieceIndex) throws IOException, InterruptedException{
		byte[] b = localPeer.getFile().getChunk(pieceIndex);
		PieceMessage pmsg = new PieceMessage(pieceIndex, b);
		lock.lock();
		try {
			output.write(pmsg.getBytes());		
			output.flush();
		}finally{
			lock.unlock();	
		}
		return true;
	}
	
	public void sendHaveMessage(int pieceIndex) throws IOException{		
		HaveMessage hmsg = new HaveMessage(pieceIndex);
		lock.lock();
		try {
			output.write(hmsg.getBytes());
			output.flush();			
		}finally{
			lock.unlock();
		}
	}
	
	public int sendHandShakeMessage() throws IOException{
		Message hsmsg = new HandShakeMessage(localPeer.getThisPeer().peerId);
		lock.lock();
		try {
			output.write(hsmsg.getBytes());
			output.flush();
			
		}finally{
			lock.unlock();
		}
		return hsmsg.getBytes().length;
	}
	
	public void sendBitFieldMessage() throws IOException{
		Message bitfieldMsg = new BitFieldMessage(Message.BITFIELD, localPeer.getThisPeer().getBitfields().getBitfields());
		lock.lock();
		try {
			output.write(bitfieldMsg.getBytes());
			output.flush();
			
		}finally{
			lock.unlock();
		}
	}
	
	public void sendRequestMessage() throws IOException, InterruptedException{
		int index = localPeer.getFile().getNextChunk(this);
		if(index >= 0){
			RequestMessage rqmsg = new RequestMessage(index);
			lock.lock();
			try {
				output.write(rqmsg.getBytes());
				output.flush();
				
			}finally{
				lock.unlock();
			}
		}		
	}
	
	public void sendInterestedMessage() throws IOException{
		Message interestedMsg = new InterestedMessage();
		lock.lock();
		try {
			output.write(interestedMsg.getBytes());
			output.flush();
			
		}finally{
			lock.unlock();
		}
	}
	
	public void readSize(byte[] rmsg, int start, int size) throws IOException{
		lock.lock();
		try{
			int rbytes = 0;
			while(rbytes < size){
				rbytes += input.read(rmsg, start + rbytes, size - rbytes);
			}		
		}finally{
			lock.unlock();
		}
		return;
	}
	
	public void sendNotInterestedMessage() throws IOException{
		Message notInterestedMsg = new NotInterestedMessage();
		lock.lock();
		try {
			output.write(notInterestedMsg.getBytes());
			output.flush();			
		}finally{
			lock.unlock();
		}	
	}
	
	public String toString(){
		return this.peerInfo.peerId+"";
	}
	
	public static class ReceiveLog{
		long time;
		int size;
		public ReceiveLog(long time, int size){
			this.time = time;
			this.size = size;
		}
	}
}
