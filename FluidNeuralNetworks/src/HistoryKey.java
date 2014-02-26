
public class HistoryKey {

	private double[] listKey;
	private double doubleKey;
	
	private int size;
	
	public HistoryKey (double[] list) {
		listKey = list;
		size = list.length;
	}
	
	public HistoryKey (double k) {
		doubleKey = k;
		size = -1;
	}
	
	public boolean is(HistoryKey k ) {
		return distanceFrom(k) == 0;
	}
	
	public int getSize() {
		return size;
	}
	
	public double[] getListContent() {
		return listKey;
	}
	
	public double getIntContent() {
		return doubleKey;
	}

	public double distanceFrom(HistoryKey k) {
		if (size == -1 && k.getSize() == -1) return Math.abs(k.getIntContent() - doubleKey);
		else if (size != -1 && k.getSize() != -1 && size == k.getSize()){
			double res = 0;
			double[] targetList = k.getListContent();
			for (int i = 0; i < size; i++) {
				if (targetList[i] != listKey[i]) res += Math.abs(targetList[i] - listKey[i]);
			}
			return res;
		} 
		return -1;
	}
	
	public String printKey() {
		if (size == -1) return Double.valueOf(doubleKey).toString();
		else {
			String res = "";
			for (int i = 0; i < size; i++) {
				res += Double.valueOf(listKey[i]).toString();
			}
			return res;
		}
	}
	
}
