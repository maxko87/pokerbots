package pokerbots.utils;

public class PredictedCard {
	//card properties
	float range = 0.5f;				//approximate range of card
	float pair = 0.3f;				//probability that this card is paired
	float suited = 0.1f;			//probability that this card is part of a flush
	float straight = 1.5f/13.0f;	//probability that this card is part of a straight
	float trip = 0.05f;				//probability that this card is trip'd
	
	public float tune( float v, float x ) {
		float w = (float)(v+x*1.2f);
		if ( w<0 ) w = 0;
		if ( w>1 ) w = 1;
		return w;
	}
	
	public void range( float Q ) {
		range = tune(range,Q);
	}
	
	public void pair( float Q ) {
		pair = tune(pair,Q);
	}
	
	public void suited( float Q ) {
		suited = tune(suited,Q);
	}
	
	public void straight( float Q ) {
		straight = tune(straight,Q);
	}
	
	public void trip( float Q ) {
		trip = tune(trip,Q);
	}
	
	public void print() {
		System.out.println("range: "+range);
		System.out.println("pair: "+pair);
		System.out.println("trip: "+trip);
		System.out.println("suited: "+suited);
		System.out.println("straight: "+straight);
	}
}
