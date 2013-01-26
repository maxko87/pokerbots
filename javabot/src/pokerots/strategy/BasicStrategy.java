package pokerots.strategy;

public class BasicStrategy implements BettingStrategy {
	public float probRaise( float w, float t, int street ) {
		return w-0.4f;
	}
	
	public float probBet( float w, float t, int street ) {
		return w-0.4f;
	}
	
	public float probCheck( float w, float t, int street ) {
		return 0.7f-w;
	}
	
	public float probCall( float w, float t, int street ) {
		return w-0.3f;
	}
	
	public float probFold( float w, float t, int street ) {
		return t;
	}
	
	public float valueRaise( float w, float t, int street ) {
		return w*0.3f;
	}
	
	public float valueBet( float w, float t, int street ) {
		return w*0.2f;
	}
}
