
public class Stats {

	int size;
	double sum;
	double sumSquare;
	double standardDeviation;
	
	public Stats() {
		size = 0;
		sum = 0;
		sumSquare = 0;
		standardDeviation = 0;
	}
	
	public double standardDeviation() {
		return Math.sqrt(Math.abs(sumSquare/size - Math.pow(sum/size, 2)));
	}

	public double size() {
		return size;
	}

	public void add(double dataPoint) {
		sum += dataPoint;
		sumSquare += dataPoint*dataPoint;
		size ++;
	}
	

}
