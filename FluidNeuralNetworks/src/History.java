import java.util.ArrayList;
import java.util.List;


public class History {

	private ArrayList<HistoryKey> keys;
	
	private int size;
	
	public History() {
		keys = new ArrayList<HistoryKey>();
		size = 0;
	}

	public Integer get(HistoryKey key, double neighborhoodSize) {
		int res = 0;
		double tempDistance = 0;
		for (HistoryKey k : keys) {
			tempDistance = k.distanceFrom(key);
			if (tempDistance < neighborhoodSize)  {
				res ++;
			}
		}
		return res;
	}
	
	
	public void put(HistoryKey key) {
		keys.add(key);
		size ++;
	}
	
	public int getSize() {
		return size;
	}
	
	public void printHistory() {
		for (HistoryKey k : keys) {
			System.out.println(" " + k.printKey());
		}
	}
}
