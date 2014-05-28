import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Vector;


public class DataTransThread implements Runnable {
	private Socket socket = null;
	private boolean to = false;
	private OutputStream output= null;
	private InputStream input = null;
	private Peer peer = null;
	private RemotePeer remotePeer = null;
	
	public DataTransThread(Socket socket, boolean to, Peer peer){
		this.socket = socket;
		this.to = to;
		this.peer = peer;
		try {
			output = socket.getOutputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			input = socket.getInputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean connect() throws IOException, InterruptedException{
		// TODO Auto-generated method stub
		/*Send HandShake Message*/
		
		Message hsmsg = new HandShakeMessage(peer.getThisPeer().peerId);
		byte[] hsbyte = hsmsg.getBytes();
		int size = hsbyte.length;
		try {
			output.write(hsbyte);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		byte[] hsret = null;
		/*Try to receive HandShake Message sent from other peers*/
		try {
			/*also anticipating a HandShake Message*/	
			hsret = new byte[size];
			input.read(hsret);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
		
		/*Check whether valid HandShake Message or not*/
		HandShakeMessage rhsmsg = new HandShakeMessage(hsret);
		if(!rhsmsg.isHandShakeMessage())
			return false;
		
		/*Check valid peerId or not*/
		Vector<RemotePeer> vector = null;
		if(to){
			vector = peer.getConnectingPeers();
		}else
			vector = peer.getWaitingPeers();
		
		String peerId = rhsmsg.getPeerId();
		for(RemotePeer rPeer: vector){
			if(rPeer.getPeerInfo().peerId.equals(peerId)){
				remotePeer = rPeer;
				break;
			}				
		}
		
		/*We should suppose invalid HandShake Message and peerId is impossible for now*/
		if(remotePeer == null){
			System.out.println("Cannot find corresponding peer, it's impossible");
			return false;
		}
		
		if(!to){
			peer.log.logInfo(Log.TCP_CONNFROM, remotePeer.toString());
		}
		vector.remove(remotePeer);
		peer.getRemotePeers().add(remotePeer);
		remotePeer.getPeerInfo().setStatus(PeerInfo.STATUS_HANDSHAKEDBOTH);
		
		remotePeer.setSocketAndInputOutput(socket, output, input);
		

		/*Transmit "bitfield" message only when peers have chunk pieces*/
		if(peer.getFile().containsChunkNumber() > 0){
			remotePeer.sendBitFieldMessage();
		}
		return true;
		
	}
	
	@Override
	public void run(){
		//long when = 0;// = System.currentTimeMillis();
		int size = 0;
		
		/*maximum size of a message is 5 + 4 + peer.getPieceSize()*/
		byte[] rmsg = new byte[NormalMessage.MessageHeaderSize + 4 + peer.getPieceSize()];
		try{
			while(!Thread.currentThread().isInterrupted()){			
				try {
					while((size = input.available()) > 0){			
						if(size >= NormalMessage.MessageHeaderSize){
	
							remotePeer.readSize(rmsg, 0, NormalMessage.MessageHeaderSize);
							int msgType = NormalMessage.getMessageType(rmsg);
							/*message size includes length of type field and payload, so minus 1 to get data size*/
							int dataSize = NormalMessage.getMessageLength(rmsg) - 1;
							remotePeer.readSize(rmsg, NormalMessage.MessageHeaderSize, dataSize);
	
							NormalMessage newMsg = null;
							int pieceIndex = 0;
							byte[] payload = null;
							switch(msgType){
							case Message.BITFIELD:					
								/*set bitfields of Remote Peer*/
								newMsg = new BitFieldMessage(rmsg);
								remotePeer.getPeerInfo().setBitfields(((BitFieldMessage)newMsg).getBitFieldFromPayload(), peer.getThisPeer().getBitfields().size());
								/*Remote peer contains chunk pieces that the peer has not, send interested message*/
								if(remotePeer.getPeerInfo().getBitfields().isComplete()){
									remotePeer.getPeerInfo().setStatus(PeerInfo.STATUS_COMPLETED);
								}
								if(peer.getThisPeer().getBitfields().existsNotHavedPieces(remotePeer.getPeerInfo().getBitfields())){
									remotePeer.sendInterestedMessage();
								}else{
									remotePeer.sendNotInterestedMessage();
								}							
								break;
							case Message.CHOKE:
								/*This peer is choked by remote peer, what should it do?*/
								if(peer.getUnchokeMeLists().contains(remotePeer))
									peer.getUnchokeMeLists().remove(remotePeer);
								peer.log.logInfo(Log.CHOKING, remotePeer.toString());
								break;
							case Message.HAVE:
								/*update bitfield of Remote Peer*/
								newMsg = new HaveMessage(rmsg);
								pieceIndex = ((HaveMessage)newMsg).getPieceIndexFromPayload();
								remotePeer.getPeerInfo().getBitfields().setBitfieldAt(pieceIndex);
								if(remotePeer.getPeerInfo().getBitfields().isComplete()){
									remotePeer.getPeerInfo().setStatus(PeerInfo.STATUS_COMPLETED);
								}
								/*If the newly got piece of remote peer is I interested, send Interested Message*/
								if(!peer.getThisPeer().getBitfields().hasPiece(pieceIndex)){
									remotePeer.sendInterestedMessage();
								}
								peer.log.logInfo(Log.REC_HAVING, remotePeer.toString(), pieceIndex);
								break;
							case Message.INTERESTED:
								remotePeer.getPeerInfo().setStatus(remotePeer.getPeerInfo().getStatus() | PeerInfo.STATUS_INTERESTED);
								peer.log.logInfo(Log.REC_INTERESTED, remotePeer.toString());
								break;
							case Message.NOT_INTERESTED:
								remotePeer.getPeerInfo().setStatus(remotePeer.getPeerInfo().getStatus() & (~PeerInfo.STATUS_INTERESTED));
								peer.log.logInfo(Log.REC_NOTINTERESTED, remotePeer.toString());
								break;
							case Message.PIECE:
								if((peer.getThisPeer().getStatus() & PeerInfo.STATUS_COMPLETED) == 0){
									newMsg = new PieceMessage(rmsg);
									pieceIndex = ((PieceMessage)newMsg).getPieceIndexFromPayload();
									payload = ((PieceMessage)newMsg).getPayload();
									peer.getFile().fillChunk(pieceIndex, payload);
									broadcastHaveMessage(pieceIndex);
									peer.log.logInfo(Log.DOWN_PIECE, remotePeer.toString(), pieceIndex, peer.getFile().containsChunkNumber());
									if((peer.getThisPeer().getStatus() & PeerInfo.STATUS_COMPLETED) != 0){
										peer.log.logInfo(Log.COMP_DOWNLOAD);
									}
								}
								break;
							case Message.REQUEST:
								newMsg = new RequestMessage(rmsg);
								if((remotePeer.getPeerInfo().getStatus() & PeerInfo.STATUS_UNCHOKED) != 0){
									pieceIndex = ((RequestMessage)newMsg).getPieceIndexFromPayload();
									remotePeer.sendPieceMessage(pieceIndex);
								}
								break;
							case Message.UNCHOKE:
								if(!peer.getUnchokeMeLists().contains(remotePeer)){
									peer.getUnchokeMeLists().add(remotePeer);
									//remotePeer.sendInterestedMessage();
								}
								peer.log.logInfo(Log.UNCHOKING, remotePeer.toString());
								break;
							case Message.HANDSHAKE:
								remotePeer.sendBitFieldMessage();
								break;
							default:
								System.out.println("Error: unrecognized message type");
							}
						}
					}
				}catch(IOException e){
					return;
				}
				if((peer.getThisPeer().getStatus() & PeerInfo.STATUS_COMPLETED) == 0 && peer.getUnchokeMeLists().size() > 0 && peer.getUnchokeMeLists().contains(remotePeer)){
					try{
						remotePeer.sendRequestMessage();
					}catch(IOException e){
						return;
					}
				}
				Thread.sleep(1);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return;
		}
	}
	
	private void broadcastHaveMessage(int pieceIndex) throws IOException{
		for(RemotePeer rpeer: peer.getRemotePeers())
			rpeer.sendHaveMessage(pieceIndex);
	}
}
