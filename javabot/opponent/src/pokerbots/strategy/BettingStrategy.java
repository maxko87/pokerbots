package pokerbots.strategy;

public interface BettingStrategy {
	
	public float probRaise( float w, float t, int street );
	public float probBet( float w, float t, int street );
	public float probCheck( float w, float t, int street );
	public float probCall( float w, float t, int street );
	public float probFold( float w, float t, int street );
	
	public float valueRaise( float w, float t, int street );
	public float valueBet( float w, float t, int street );
}
