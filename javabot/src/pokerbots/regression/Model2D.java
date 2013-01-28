package pokerbots.regression;

public interface Model2D {
	public void addData( float x, float y );
	public float getEstimate( float x );
	public float getInverseModel( float y );
	public int getN();
	public void print();
}
