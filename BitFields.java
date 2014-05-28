import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

public class BitFields {
	private Vector<Integer> bitfields = null;
	private final int size;
	public BitFields(int size, boolean fill){
		this.size = size;
		bitfields = new Vector<Integer>();
		for(int i=0; i<size; i++)
			bitfields.add(new Integer(fill?1:0));
	}
	
	public BitFields(byte[] bits, int size){
		this.size = size;
		bitfields = new Vector<Integer>();
		for(int i=0; i<size; i++){
			int ival = getElemAt(bits, i);
			bitfields.add(new Integer(ival));
		}
	}
	
	public boolean isComplete(){
		for(int i=0; i<bitfields.size(); i++)
			if(getBitfieldAt(i) == 0)
				return false;
		return true;
	}
	
	public boolean hasPiece(int index){
		return (bitfields.get(index) == 1)?true:false;
	}
	
	public int size(){
		return size;
	}
	
	public int getBitfieldAt(int index){
		if(index > size)
			throw new ArrayIndexOutOfBoundsException();
		return bitfields.get(index);
	}
	
	public void setBitfieldAt(int index){
		if(index > size)
			throw new ArrayIndexOutOfBoundsException();
		bitfields.set(index, new Integer(1));			
	}
	
	public boolean existsNotHavedPieces(BitFields other){
		Iterator<Integer> iter1 = bitfields.iterator();
		Iterator<Integer> iter2 = other.bitfields.iterator();
		while(iter1.hasNext() && iter2.hasNext()){
			Integer i1 = iter1.next();
			Integer i2 = iter2.next();
			if(i1 == 0 && i2 == 1)
				return true;
		}
		while(iter2.hasNext()){
			return true;
		}
		return false;
	} 
	
	public ArrayList<Integer> getNotHavedPieces(BitFields other){
		ArrayList<Integer> list = new ArrayList<Integer>();
		Iterator<Integer> iter1 = bitfields.iterator();
		Iterator<Integer> iter2 = other.bitfields.iterator();
		int i = 0;
		while(iter1.hasNext() && iter2.hasNext()){
			Integer i1 = iter1.next();
			Integer i2 = iter2.next();
			if(i1 == 0 && i2 == 1)
				list.add(new Integer(i));
			i++;
		}
		while(iter2.hasNext()){
			list.add(new Integer(i));
			i++;
		}
		return list;	
	} 
	
	public byte[] getBitfields(){
		int arraySize = size/8;
		if(size%8 != 0)
			arraySize++;
		byte[] bfs = new byte[arraySize];
		for(int i=0; i<size; i++)
			setBitfield(bfs, i);
		return bfs;
	}
	
	private static int getElemAt(byte[] bits, int index){
		int id = index/8;
		int off = index%8;
		return (bits[id]>>off)&1;
	}
	
	private static void setBitfield(byte[] bits, int index){
		int id = index/8;
		int off = index%8;
		bits[id] |= 1<<off;
	}
}
