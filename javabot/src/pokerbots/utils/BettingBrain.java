package pokerbots.utils;

public class BettingBrain {

	public enum State {GOOD, BLUFF, ETC}
	StatAggregator aggregator;
	
	public BettingBrain(){
		aggregator = new StatAggregator();
	}
	
	//given odds and stack size, decides how much to bet
	public int makeProportionalBet(float expectedWinPercentage, int minBet, int maxBet, int currStackSize ){
		return (int) ( 2 * (expectedWinPercentage - .5) * (maxBet - minBet) + minBet);
	}
	
	

}
