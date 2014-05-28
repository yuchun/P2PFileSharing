
public class ChunkInfo {
	private volatile int index;
	private volatile int offset;
	private volatile int length;
	private volatile boolean has;
	ChunkInfo(){
		
	}
	ChunkInfo(int index, int offset, int length, boolean has){
		this.index = index;
		this.offset = offset;
		this.length = length;
		this.has = has;
	}
	
	public void setInfo(int index, int location, int length, boolean has){
		this.index = index;
		this.offset = offset;
		this.length = length;
		this.has = has;
	}
	
	public int getIndex(){
		return index;
	}
	
	public void setIndex(int index){
		this.index = index;
	}
	
	public int getOffset(){
		return offset;
	}
	
	public void setOffset(int offset){
		this.offset = offset;
	}
	
	public int getLength(){
		return length;
	}
	
	public void setLength(){
		this.length = length;
	}
	
	public boolean getHas(){
		return has;
	}
	
	public void setHas(boolean has){
		this.has = has;
	}
}
