package pokerbots.regression;

public class StddevModel implements Model1D {
	int N = 0;
	float SX = 0;
	float SXX = 0;
	float avg = 0;
	float stddev = 0;
	
	String name;
	public StddevModel( String name ) {
		this.name = name;
	}
	public void addData( float x ) {
		N +=1;
		SX += x;
		SXX += x*x;
		avg = SX/N;
		stddev = (float)(Math.sqrt(SXX/N - (SX*SX)/(N*N)));
	}
	public int getN() { return N; }
	public float getHigh() {
		return getAverage()+stddev;
	}
	public float getAverage() {
		return avg;
	}
	public float getLow() {
		return getAverage()-stddev;
	}
	
	public void print() {
		System.out.println( name + " (Stddev Model). Avg = " + avg +  ", stddev = " + stddev );
	}
}
