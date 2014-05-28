import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Peer {	
	private DataTransThread[] dtts;

	public Peer(String peerId){
		//thisPeer = new PeerInfo();
		/*initialization used only, peers that listed in front of this peer and will be connected by this peer*/
		connectingPeers = new Vector<RemotePeer>();
		/*initialization used only, peers that listed after this peer and will connect this peer*/
		waitingPeers = new Vector<RemotePeer>();
		
		unchokeMe = new Vector<RemotePeer>();
		
		/*all the remote peers*/
		remotePeers = new Vector<RemotePeer>();
		
		/*peers that selected to be unchoked and can be sent data*/
		unchokedPeers = new Vector<RemotePeer>();

		log = new Log(peerId, Log.LOG_VERBOSE);
		
		//threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());	
		threadPool = Executors.newFixedThreadPool(10);	
		try {
			getConfiguration(peerId);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void start() throws InterruptedException{
		int size = connectingPeers.size();
		int dttsIndex = 0;
		
		for(int i=size-1; i>=0; i--){
			RemotePeer remote = connectingPeers.get(i);
			
			while(((thisPeer.getStatus() & PeerInfo.STATUS_COMPLETED) == 0)){
				Socket client = new Socket();
				try {
					client.connect(new InetSocketAddress(remote.getPeerInfo().peerAddress, remote.getPeerInfo().peerPort), 100000);
					log.logInfo(Log.TCP_CONNTO, remote.getPeerInfo().peerId);
					if(client.isConnected()){
						dtts[dttsIndex] = new DataTransThread(client, true, Peer.this);
						if(dtts[dttsIndex].connect())
							break;
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			dttsIndex++;		
		}

		if(waitingPeers.size() > 0){
			try {
				server = new ServerSocket(thisPeer.peerPort);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			while(!waitingPeers.isEmpty()){
				try {
					Socket incoming = server.accept();
					if(incoming != null){
						dtts[dttsIndex] = new DataTransThread(incoming, false, Peer.this);
						dtts[dttsIndex].connect();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
				dttsIndex++;
			}
		}
		
		for(DataTransThread dtt: dtts)
			threadPool.submit(dtt);
		
		//long unchokingTime = System.currentTimeMillis();
		//long optimisticUnchokingTime = unchokingTime;
		//Start first unchoking right now
		long unchokingTime = System.currentTimeMillis() - unchokingInterval;
		long optimisticUnchokingTime = System.currentTimeMillis() - optimisticUnchokingInteval;
		while(!isAllPeersFinished()){
			//System.out.println("xxxxxx");
			long cur = System.currentTimeMillis();
			if((cur - unchokingTime) > unchokingInterval){
				try{
					unchokingPeers();
				}catch(IOException e){
					break;
				}
				unchokingTime = cur;
				if(unchokedPeers != null && unchokedPeers.size() > 0)
					log.logInfo(Log.CHANGE_PREFER_NEIGH, unchokedPeers);
			}
			
			if((cur - optimisticUnchokingTime) > optimisticUnchokingInteval){
				try{
					optimisticUnchokingPeers();
				}catch(IOException e){
					break;
				}
				optimisticUnchokingTime = cur;
				if(optUnchokedPeer != null)
					log.logInfo(Log.CHANGE_OPT_NEIGH, optUnchokedPeer.toString());
			}			
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		threadPool.shutdownNow();
		try {
			if(server != null){
				server.close();
				while(!server.isClosed())
					System.out.println("hurry up");
			}
				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.finalize();
		while(!threadPool.isShutdown())
			System.out.println("hurry up");
		
		System.out.println("Finished");
	}
	
	public void finalize() throws InterruptedException, IOException{		
		server.close();
		file.close();
		log.finalize();
	}
	
	@SuppressWarnings("unchecked")
	private void unchokingPeers() throws IOException{
		ArrayList<PeerSpeed> speeds = new ArrayList<PeerSpeed>();
		for(RemotePeer rp: remotePeers){
			if((rp.getStatus() & PeerInfo.STATUS_COMPLETED) == 0)
				speeds.add(new PeerSpeed(rp.getSpeed(), rp));
		}
		Collections.sort(speeds);
		for(int i=0; i<((numberOfPreferredNeighbors>(speeds.size()))?(speeds.size()):numberOfPreferredNeighbors); i++){
			if((!unchokedPeers.contains(speeds.get(i).rp)) && (optUnchokedPeer != speeds.get(i).rp)){
				speeds.get(i).rp.sendUnchokeMessage();
				speeds.get(i).rp.getPeerInfo().setStatus(speeds.get(i).rp.getPeerInfo().getStatus() | PeerInfo.STATUS_UNCHOKED);
			}
		}
		unchokedPeers.clear();
		for(int i=0; i<((numberOfPreferredNeighbors>(speeds.size()))?(speeds.size()):numberOfPreferredNeighbors); i++){
			unchokedPeers.add(speeds.get(i).rp);
		}
		//System.out.println("unchokedPeers: " + unchokedPeers);
	}
	
	private static class PeerSpeed implements Comparable{
		int speed;
		RemotePeer rp;
		public PeerSpeed(int speed, RemotePeer rp){
			this.speed = speed;
			this.rp = rp;
		}
		@Override
		public int compareTo(Object arg0) {
			// TODO Auto-generated method stub
			return ((PeerSpeed)arg0).speed - speed;
		}
		
	}
	
	private void optimisticUnchokingPeers() throws IOException{
		Random random = new Random();
		ArrayList<RemotePeer> uncomplete = new ArrayList<RemotePeer>();
		for(RemotePeer rp: remotePeers){
			if((rp.getStatus() & PeerInfo.STATUS_COMPLETED) == 0)
				uncomplete.add(rp);
		}	
		
		if(uncomplete.size() > 0){
			int index = random.nextInt(uncomplete.size());
			if((!unchokedPeers.contains(uncomplete.get(index))) && (optUnchokedPeer != uncomplete.get(index))){
				uncomplete.get(index).sendUnchokeMessage();
				uncomplete.get(index).setStatus(uncomplete.get(index).getStatus() | PeerInfo.STATUS_UNCHOKED);
			}
			optUnchokedPeer = uncomplete.get(index);
		}
	}
	
	private boolean isAllPeersFinished(){
		int completeNum = 0;
		for(RemotePeer rp: remotePeers){
			if((rp.getPeerInfo().getStatus() & PeerInfo.STATUS_COMPLETED) == 0){
				return false;
			}
			completeNum++;
		}
		if((thisPeer.getStatus() & PeerInfo.STATUS_COMPLETED) == 0){
			return false;
		}
		completeNum++;
		if(completeNum >= (this.remotePeers.size() + this.waitingPeers.size() + this.connectingPeers.size() + 1))
			return true;
		return false;
	}
	
	public void getConfiguration(String thisId) throws IOException
	{
		String st;
		BufferedReader in;
		
		remotePeers = new Vector<RemotePeer>();
	
		in = new BufferedReader(new FileReader(Config.FILE_COMMONINFO));
		while((st = in.readLine()) != null){
			String[] tokens = st.split("\\s+");
			if(tokens[0].equals("NumberOfPreferredNeighbors"))
				this.numberOfPreferredNeighbors = Integer.parseInt(tokens[1]);
			else if(tokens[0].equals("UnchokingInterval"))
				this.unchokingInterval = Integer.parseInt(tokens[1])*1000;
			else if(tokens[0].equals("OptimisticUnchokingInterval"))
				this.optimisticUnchokingInteval = Integer.parseInt(tokens[1])*1000;
			else if(tokens[0].equals("FileName"))
				this.fileName = tokens[1];
			else if(tokens[0].equals("FileSize"))
				this.fileSize = Integer.parseInt(tokens[1]);
			else if(tokens[0].equals("PieceSize"))
				this.pieceSize = Integer.parseInt(tokens[1]);
			else{
				System.out.println("Error, unrecognized item. st=" + st);
				in.close();
				return;
			}
		}
		
		in.close();
		
		in = new BufferedReader(new FileReader(Config.FILE_PEERINFO));
		while((st = in.readLine()) != null) {
				
			String[] tokens = st.split("\\s+"); 
			 if(tokens[0].equals(thisId)){	
				 thisPeer = new PeerInfo(tokens[0], tokens[1], tokens[2], tokens[3].equals("1"), this.fileSize, this.pieceSize);
			 }else{
				 if(thisPeer == null){
					 connectingPeers.addElement(new RemotePeer(new PeerInfo(tokens[0], tokens[1], tokens[2], tokens[3].equals("1"), this.fileSize, this.pieceSize), this));
				 }else{
					 waitingPeers.addElement(new RemotePeer(new PeerInfo(tokens[0], tokens[1], tokens[2], tokens[3].equals("1"), this.fileSize, this.pieceSize), this));
				 }
			 }
		}
		
		in.close();
		dtts = new DataTransThread[connectingPeers.size() + waitingPeers.size()];

		
		file = new FileManagement(thisPeer, fileName, fileSize, pieceSize);

		System.out.println(thisPeer.peerId + " " + thisPeer.peerAddress + " " + thisPeer.peerPort);
		System.out.println("connectingPeers:");
		for(RemotePeer pi : connectingPeers)
			System.out.println(pi.getPeerInfo().peerId + " " + pi.getPeerInfo().peerAddress + " " + pi.getPeerInfo().peerPort);
		System.out.println("waitingPeers:");
		for(RemotePeer pi : waitingPeers)
			System.out.println(pi.getPeerInfo().peerId + " " + pi.getPeerInfo().peerAddress + " " + pi.getPeerInfo().peerPort);
	}
	
	public final PeerInfo getThisPeer(){
		return thisPeer;
	}
	
	public final Vector<RemotePeer> getUnchokeMeLists(){
		return unchokeMe;
	}
	
	public void addUnchokeMe(RemotePeer pInfo){
		if(unchokeMe.contains(pInfo))
			return;
		unchokeMe.add(pInfo);
	}
	
	public void removeUnchokeMe(RemotePeer pInfo){
		unchokeMe.remove(pInfo);
	}
	
	public final Vector<RemotePeer> getRemotePeers(){
		return remotePeers;
	}
	
	public final Vector<RemotePeer> getConnectingPeers(){
		return connectingPeers;
	}
	
	public final Vector<RemotePeer> getWaitingPeers(){
		return waitingPeers;
	}
	
	public void addRemotePeer(RemotePeer pInfo){
		remotePeers.add(pInfo);
	}
	
	public void removeConnectingPeer(RemotePeer pInfo){
		connectingPeers.remove(pInfo);
	}
	
	public void removeWaitingPeer(RemotePeer pInfo){
		waitingPeers.remove(pInfo);
	}
	
	/*
	private class TcpServer implements Runnable{	

		@Override
		public void run() {
			// TODO Auto-generated method stub

			try {
				server = new ServerSocket(thisPeer.peerPort);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			while(!waitingPeers.isEmpty()){
				try {
					Socket incoming = server.accept();
					if(incoming != null)
						threadPool.submit(new DataTransThread(incoming, false, Peer.this));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
				if(Thread.currentThread().isInterrupted())
					break;
			}
		}

	}
	
	private class TcpClient implements Runnable{
		RemotePeer remote;
		public TcpClient(RemotePeer remote){
			this.remote = remote;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			while(((thisPeer.getStatus() & PeerInfo.STATUS_COMPLETED) == 0) && !Thread.currentThread().isInterrupted()){
				Socket client = new Socket();
				try {
					client.connect(new InetSocketAddress(remote.getPeerInfo().peerAddress, remote.getPeerInfo().peerPort), 100000);
					log.logInfo(Log.TCP_CONNTO, remote.getPeerInfo().peerId);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(client.isConnected()){
					new DataTransThread(client, true, Peer.this).run();
				}
				if(Thread.currentThread().isInterrupted())
					break;
			}
			System.out.println("Client thread finished");
			return;
		}
	}
	*/
	
	public final FileManagement getFile(){
		return file;
	}
	
	public int getPieceSize(){
		return pieceSize;
	}
	
	private int numberOfPreferredNeighbors;
	private int unchokingInterval;
	private int optimisticUnchokingInteval;
	private String fileName;
	private int fileSize;
	private int pieceSize;
	
	private FileManagement file = null;
	private ExecutorService threadPool = null;
	public Log log;
	private PeerInfo thisPeer = null;
	private Vector<RemotePeer> remotePeers = null;

	private Vector<RemotePeer> connectingPeers = null;
	private Vector<RemotePeer> waitingPeers = null;
	
	private Vector<RemotePeer> unchokedPeers = null;
	private volatile RemotePeer optUnchokedPeer = null;
	
	private ServerSocket server = null;
	
	/*The list of remote peers that currently unchoke this peer, means I can request chunks from them*/
	private Vector<RemotePeer> unchokeMe = null;
}
