package pokerbots.regression;

public interface Model2D {
	public void addData( float x, float y );
	public float getEstimate( float x );
	public int getN();
	public void print();
}
