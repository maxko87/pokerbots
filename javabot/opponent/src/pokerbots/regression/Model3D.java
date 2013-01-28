package pokerbots.regression;

public interface Model3D {
	public void addData( float x, float y, float z );
	public float getEstimate( float x, float y );
	public int getN();
	public void print();
}
