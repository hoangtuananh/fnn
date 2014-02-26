
public class HistoryKey {

	private int[] listKey;
	private int intKey;
	
	private int size;
	
	public HistoryKey (int[] list) {
		listKey = list;
		size = list.length;
	}
	
	public HistoryKey (int k) {
		intKey = k;
		size = -1;
	}
	
	public boolean is(HistoryKey k ) {
		return distanceFrom(k) == 0;
	}
	
	public int getSize() {
		return size;
	}
	
	public int[] getListContent() {
		return listKey;
	}
	
	public int getIntContent() {
		return intKey;
	}

	public int distanceFrom(HistoryKey k) {
		if (size == -1 && k.getSize() == -1) return k.getIntContent() - intKey;
		else if (size != -1 && k.getSize() != -1 && size == k.getSize()){
			int res = 0;
			int[] targetList = k.getListContent();
			for (int i = 0; i < size; i++) {
				if (targetList[i] != listKey[i]) res += 1;
			}
			return res;
		} 
		return -1;
	}
	
}
