import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileManagement {
	private ArrayList<Integer> neededChunks = new ArrayList<Integer>();
	private ArrayList<Integer> requestingChunks = new ArrayList<Integer>();
	private ArrayList<ChunkInfo> chunkList = new ArrayList<ChunkInfo>();
	private int requestedTimes = 0;

	private RandomAccessFile rafile = null;
	private PeerInfo thisPeer = null;
	
	private ReentrantLock listLock = new ReentrantLock();
	private ReentrantLock fileLock = new ReentrantLock();

	
	
	public FileManagement(PeerInfo thisPeer, String fileName, int fileSize, int pieceSize) throws FileNotFoundException{
		int i;
		int offset;
		
		this.thisPeer = thisPeer;
		for(i=0; i<fileSize/pieceSize; i++){
			offset = i*pieceSize;
			chunkList.add(new ChunkInfo(i, offset, pieceSize, thisPeer.has));
		}
		if(fileSize%pieceSize != 0){
			offset = i*pieceSize;
			chunkList.add(new ChunkInfo(i, offset, fileSize%pieceSize, thisPeer.has));
		}
		
		thisPeer.setBitfields(new BitFields(chunkList.size(), thisPeer.has));
		if(!thisPeer.has){
				for(i=0; i<chunkList.size(); i++)
					neededChunks.add(i);
		}
		
		//Path path = Paths.get("peer" + thisPeer.peerId, fileName);
		File newFile = new File("peer_" + thisPeer.peerId);
		if(!newFile.exists())
			newFile.mkdirs();
		newFile = new File("peer_" + thisPeer.peerId + "/" + fileName);

		//BufferedReader in 
		if(thisPeer.has){	
			rafile = new RandomAccessFile(newFile, "r");
		}else{
			try {
				rafile = new RandomAccessFile(newFile, "rw");
				rafile.setLength(fileSize);
			} catch (FileNotFoundException e) {
				throw e;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public int size(){
		listLock.lock();
		try{
			return chunkList.size();
		}finally{
			listLock.unlock();
		}
	}
	
	public boolean isCompleted(){
		listLock.lock();
		try{
			return (neededChunks.size() == 0 && requestingChunks.size() == 0);
		}finally{
			listLock.unlock();
		}
	}
	
	public void close() throws IOException{
		fileLock.lock();
		try{
			if(rafile != null)
				rafile.close();
		}finally{
			fileLock.unlock();
		}
	}
	
	/*
	 * Get next chunk index to request from remotePeer, the chunk should be in the neededChunk and remotePeer should
	 * have it, after requesting, move it to requestingChunks list in case duplicate request; after 10 times, move
	 * the first chunk in the requestingChunks back to neededChunks in case of not receiving requested data;
	 *  */
	public int getNextChunk(RemotePeer remotePeer){
		listLock.lock();
		try{
			requestedTimes++;
			if(requestedTimes > 100){
				requestedTimes = 0;
				if(requestingChunks.size()>0)
					neededChunks.add(requestingChunks.remove(0));
			}
			if(neededChunks.size() == 0){
					return -1;			
			}
		}finally{
			listLock.unlock();
		}
		
		ArrayList<Integer> minus = thisPeer.getBitfields().getNotHavedPieces(remotePeer.getPeerInfo().getBitfields());
		
		listLock.lock();
		try{
			for(Integer ci: requestingChunks){
				if(minus.contains(ci))
					minus.remove(ci);
			}
		}finally{
			listLock.unlock();
		}
			
		if(minus.size() == 0)
			return -1;
			
		Random random = new Random();
		int index = random.nextInt(minus.size());
		
		listLock.lock();
		try{
			neededChunks.remove(new Integer(minus.get(index)));
			requestingChunks.add(new Integer(minus.get(index)));
			return minus.get(index);
		}finally{
			listLock.unlock();
		}
	}

	public int neededChunkSize(){
		listLock.lock();
		try{
			return neededChunks.size();
		}finally{
			listLock.unlock();
		}
	}
	
	public boolean containsChunk(int index){
		listLock.lock();
		try{
			return chunkList.get(index).getHas();
		}finally{
			listLock.unlock();
		}
	}
	
	public int containsChunkNumber(){
		listLock.lock();
		try{
			return chunkList.size() - neededChunks.size() - requestingChunks.size();
		}finally{
			listLock.unlock();
		}
	}
	
	public byte[] getChunk(int index){
		byte[] content = null;
		int offset = 0, length = 0;
		listLock.lock();
		try{
			if(!containsChunk(index))
				return null;
			content = new byte[chunkList.get(index).getLength()];
			offset = chunkList.get(index).getOffset();
			length = chunkList.get(index).getLength();			
		}finally{
			listLock.unlock();
		}			
		
		fileLock.lock();
		try{
			try {
				rafile.seek(offset);
				rafile.read(content, 0, length);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return content;
		}finally{
			fileLock.unlock();
		}
	}
	
	public void fillChunk(int index, byte[] content){
		int offset = 0, length = 0;
		listLock.lock();
		try{
			requestingChunks.remove(new Integer(index));
			neededChunks.remove(new Integer(index));
			thisPeer.getBitfields().setBitfieldAt(index);	
			if(neededChunks.size() == 0 && requestingChunks.size() == 0)
				thisPeer.setStatus(PeerInfo.STATUS_COMPLETED);
			chunkList.get(index).setHas(true);
			offset = chunkList.get(index).getOffset();
			length = chunkList.get(index).getLength();
		}finally{
			listLock.unlock();
		}			
		
		fileLock.lock();
		try{
			try {
				rafile.seek(offset);
				rafile.write(content, 0, length);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}finally{
			fileLock.unlock();
		}
	}

}
