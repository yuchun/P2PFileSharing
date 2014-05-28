import java.io.*;
import java.util.Calendar;
import java.util.Vector;


public class Log {
	public static final int TCP_CONNTO = 0;
	public static final int TCP_CONNFROM = 1;
	public static final int CHANGE_PREFER_NEIGH = 2;
	public static final int CHANGE_OPT_NEIGH = 3;
	public static final int UNCHOKING = 4;
	public static final int CHOKING = 5;
	public static final int REC_HAVING = 6;
	public static final int REC_INTERESTED = 7;
	public static final int REC_NOTINTERESTED = 8;
	public static final int DOWN_PIECE = 9;
	public static final int COMP_DOWNLOAD = 10;
	
	public static final int LOG_NONE = 0;
	public static final int LOG_VERBOSE = 1;
	
	public Log(String peerId, int logLevel){
		this.peerId = peerId;
		File logFile = new File("log_peer_" + peerId + Config.LogFileSuffix);
		//path = Paths.get(/*Config.WorkingDirectory,Config.LogFileName + */"log_peer_" + peerId + Config.LogFileSuffix);
		calendar = Calendar.getInstance();
		try {
			writer = new PrintWriter(logFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void logInfo(int msgType){
		logInfo(msgType, "", 0);
	}
	
	public void logInfo(int msgType, Vector<RemotePeer> vec){
		logInfo(msgType, vec.toString(), 0, 0);
	}
	
	public void logInfo(int msgType, String remotePeerId){
		logInfo(msgType, remotePeerId, 0, 0);
	}
	
	public void logInfo(int msgType, String remotePeerId, int pieceIndex){
		logInfo(msgType, remotePeerId, pieceIndex, 0);
	}
	
	public void logInfo(int msgType, String remotePeerId, int pieceIndex, int numPieces){
		String info = "";
		switch(msgType){
		case TCP_CONNTO:
			info = "[" + calendar.getTime().toString() + "] Peer " + "[" + peerId + "] makes a connection to Peer [" + remotePeerId + "].";
			break;
		case TCP_CONNFROM:
			info = "[" + calendar.getTime().toString() + "] Peer " + "[" + peerId + "] is connected from Peer [" + remotePeerId + "].";		
			break;
		case CHANGE_PREFER_NEIGH:
			info = "[" + calendar.getTime().toString() + "] Peer " + "[" + peerId + "] has the preferred neighbors [" + remotePeerId + "].";		
			break;
		case CHANGE_OPT_NEIGH:
			info = "[" + calendar.getTime().toString() + "] Peer " + "[" + peerId + "] has the optimistically-unchoked neighbor [" + remotePeerId + "].";		
			break;
		case UNCHOKING:
			info = "[" + calendar.getTime().toString() + "] Peer " + "[" + peerId + "] is unchoked by [" + remotePeerId + "].";	
			break;
		case CHOKING:
			info = "[" + calendar.getTime().toString() + "] Peer " + "[" + peerId + "] is choked by [" + remotePeerId + "].";	
			break;
		case REC_HAVING:
			info = "[" + calendar.getTime().toString() + "] Peer " + "[" + peerId + "] received a have message from [" + remotePeerId + "]for the piece [" + pieceIndex +"].";	
			break;
		case REC_INTERESTED:
			info = "[" + calendar.getTime().toString() + "] Peer " + "[" + peerId + "] received an interested message from [" + remotePeerId + "].";	
			break;
		case REC_NOTINTERESTED:
			info = "[" + calendar.getTime().toString() + "] Peer " + "[" + peerId + "] received a not interested message from [" + remotePeerId + "].";	
			break;
		case DOWN_PIECE:
			info = "[" + calendar.getTime().toString() + "] Peer " + "[" + peerId + "] has downloaded the piece [" + pieceIndex + "] from ["+ remotePeerId + "]. Now the number of pieces it has is [" + numPieces + "].";	
			//[Time]: Peer [peer_ID 1] has downloaded the piece [piece index] from [peer_ID 2]. 
			//Now the number of pieces it has is [number of pieces]. 
			break;
		case COMP_DOWNLOAD:
			info = "[" + calendar.getTime().toString() + "] Peer " + "[" + peerId + "] has downloaded the complete file."; 
			break;
		default:
			System.out.println("Unsupported Message Type");
			break;
		}
	
		synchronized(writer){
			writer.println(info);
			System.out.println(info);
			writer.flush();
		}//synchronized
	}
	
	/*
	public boolean open(){
		if(!Files.exists(path)){
			try {
				Files.createFile(path);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			System.out.println(path.toString());
			
			logFile = new PrintWriter(Files.newBufferedWriter(path, Charset.defaultCharset(), StandardOpenOption.APPEND));
		}catch(NoSuchFileException e){
			System.out.println("["+calendar.getTime().toString()+"] Peer "+"["+peerId+"] Cannot open Log file");
			return false;
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		return true;
	}
	*/
	
	public void finalize(){
		writer.close();
	}

	private final String peerId;
	private PrintWriter writer = null;
	private Calendar calendar = null;
}
