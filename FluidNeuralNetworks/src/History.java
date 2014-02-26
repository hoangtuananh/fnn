import java.util.ArrayList;
import java.util.List;


public class History {

	private ArrayList<HistoryKey> keys;
	private ArrayList<Integer> values;
	
	private int size;
	
	public History() {
		keys = new ArrayList<HistoryKey>();
		values = new ArrayList<Integer>();
		
		size = 0;
	}
	
	public boolean contains(HistoryKey key) {
		return this.indexOf(key) != -1;
	}
	
	private int indexOf(HistoryKey key) {
		int index = 0;
		for (HistoryKey k : keys) {
			if (k.is(key)) return index;
			index++;
		}
		return -1;
	}
	
	public Integer getExact(HistoryKey key) {
		int keyIndex = this.indexOf(key);
		if (keyIndex != -1) return values.get(keyIndex);
		return null;
	}
	
	public Integer getCloset(HistoryKey key) {
		int d = Integer.MAX_VALUE;
		HistoryKey res = null;
		int tempDistance = 0;
		for (HistoryKey k : keys) {
			tempDistance = k.distanceFrom(key);
			if (tempDistance < d)  {
				d = tempDistance;
				res = k;
			}
		}
		return values.get(indexOf(res));
	}
	
	
	public void put(HistoryKey key) {
		int keyIndex = this.indexOf(key);
		if (keyIndex != -1) {
			values.set(keyIndex, values.get(keyIndex) + 1) ;
		} else {
			keys.add(key);
			values.add(1);
		}
		size++;
	}

	public int sumValues() {
		
		return size;
	}
	
}
