package pokerbots.regression;

public class MemoryModel implements Model1D {
	int N = 0;
	float value = 0;
	float avg = 0;
	
	public void addData( float x ) {
		value += x;
		N+=1;
		avg = value/N;
	}
	public float getHigh(){return 0;}
	public float getAverage(){return 0;}
	public float getLow(){return 0;}
	public int getN(){return 0;}
	public void print(){return;}
}
