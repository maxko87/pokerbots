package pokerbots.regression;

public interface Model {
	public void addData( float x, float y );
	public float getEstimate( float x );
	public void print();
}
