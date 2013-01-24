package pokerbots.utils;

import pokerbots.packets.GetActionObject;
import pokerbots.packets.LegalActionObject;
import pokerbots.utils.StatAggregator.OpponentStats;

public class EVCalculator {

	//as a reference::
	//
	//EV folding is 0
	//
	//EV calling is (equity)*(size of pot after call)*TIO - (amount to call) - (chance we will have to fold this round*amount to call)
	//ev_call =   
	//
	//EV raising is (equity when called)*(size of pot when called)*TIO + (% chance all fold)* (size of the pot with our raise) - (amount costs us to raise) - (EV loss when we have to fold after we raise)
	//
	
	
	
	/*
	 * we want to build an expected value calculator that we use at every decision. 
	 * 
	 * inputs -> StatAggregator, street, % winChance, getActionObject (potsize, legalActions) 
	 * output -> an action to take (only *which* action, not the amount. for now.)
	 * 
	 * street # will only be 1, 2 or 3. we can stick with our simple preflop shit for now.
	 * 
	 * assume that our % chance of winning doesn't change between the flop, the turn, and the river (e.g. use whatever is passed in for all future % winChances) 
	 * 
	 * you may need to recreate the getActionObject that you will pass on to future streets, which might be tough. we only need an accurate potSize and legalAction types though, no need for actually doing min and max values.
	 * 
	 */
	
	int startingStack = 400;
	
	//returns what action to take on the river. 
	public String getRiverAction(OpponentStats opponent, int street, float winChance, GetActionObject getActionObject){
		
		float ev_fold = 0;
		float ev_check = 0;
		float ev_call = 0;
		float ev_bet = 0;
		float ev_raise = 0;
		
		LegalActionObject[] legalActions = getActionObject.legalActions;
		int potSize = getActionObject.potSize;
		
		for ( int i = 0; i < legalActions.length; i++ ) {
			LegalActionObject legalAction = legalActions[i];
			
			if (legalAction.actionType.equalsIgnoreCase("bet")){	
				//(%oppfolds)(Pot)+(%oppcalls)[(%win)(Pot+Bet)-(%lose)(Bet)]
				int ourEstimatedBet = makeBet(Utils.boundInt(startingStack - (potSize / 2), 1, startingStack), potSize, winChance, street, opponent);
				ev_bet = (opponent.getPercentFoldToBet(street) * potSize) + opponent.getPercentCallToBet(street) * (winChance * (potSize + ourEstimatedBet) - (1 - winChance) * ourEstimatedBet);
			}
			else if (legalAction.actionType.equalsIgnoreCase("raise")){
				//same as bet
				int ourEstimatedRaise = makeRaise(potSize, winChance);
				ev_raise = (opponent.getPercentFoldToRaise(street) * potSize) + opponent.getPercentCallToRaise(street) * (winChance * (potSize + ourEstimatedRaise) - (1 - winChance) * ourEstimatedRaise);
			}
			else if (legalAction.actionType.equalsIgnoreCase("call")){
				
			}
		}
		
		
		return "";
	}
	
	
	
	
	//RUDIMENTARY BET/RAISE ESTIMATES FOR CALCULATIONS. THIS IS MESSY BUT FUCK IT
	
	public int makeBet(int maxBet, int potSize, float winChance, int street, OpponentStats opponent){
		float bet = (winChance - MIN_WIN_TO_PLAY[street][0]) * maxBet;
		bet = bet * opponent.getLooseness(street); 
		return (int)bet;
	}
	
	public int makeRaise(int potSize, float winChance){
		int raise;
		raise = (int) (potSize * winChance);		
		return raise;
	}
	
	float val1 = 0.5f;
	float val2 = 0.8f;
	
	float val3 = 0.6f;
	float val4 = 0.85f;
	
	float val5 = 0.5f;
	float val6 = 0.7f;
	
	float val7 = 0.5f;
	float val8 = 0.7f;
	

	float val17 = 0.05f;
	
	//minimum default percentage range of winning to play/raise each street.
	private final float[][] MIN_WIN_TO_PLAY = new float[][] {{val1, val2}, {val3, val4}, {val5, val6}, {val7, val8}};
}
