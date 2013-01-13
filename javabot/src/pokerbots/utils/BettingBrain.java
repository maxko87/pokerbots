package pokerbots.utils;

public class BettingBrain {

	public enum State {GOOD, BLUFF, ETC}
	
	public BettingBrain(){
		
	}
	
	//given odds and stack size, decides how much to bet
	public int makeProportionalBet(float expectedWinPercentage, int minBet, int maxBet, int currStackSize ){
		return (int) ( 2 * (expectedWinPercentage - .5) * (maxBet - minBet) + minBet);
	}
	
	

}
