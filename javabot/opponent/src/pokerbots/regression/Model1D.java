package pokerbots.regression;

public interface Model1D {
	public void addData( float x );
	public float getHigh();
	public float getAverage();
	public float getLow();
	public int getN();
	public void print();
}
