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
	 * TODO: scale getAverageBet and Raise accordingly based on aggression on the previous streets
	 */
	
	int startingStack = 400;
	String[] options = new String[] {"check", "call", "bet", "raise"};
	
	//returns what action to take on the river. 
	public String getRiverAction(OpponentStats opponent, float winChance, GetActionObject getActionObject){
		
		System.out.println("\n--------\nEXPECTED VALUEs FOR RIVER\n");
		
		float ev_check = -9000;
		float ev_call = -9000;
		float ev_bet = -9000;
		float ev_raise = -9000;
		float[] results = new float[] {ev_check, ev_call, ev_bet, ev_raise};
		
		LegalActionObject[] legalActions = getActionObject.legalActions;
		int potSize = getActionObject.potSize;
		int street = 3;
		
		//calculate opponent's perceived chance of winning this hand: TODO: subtract bluffing factor
		float loseChance = .85f - (opponent.getLooseness(street) / 2); //max = .85, min = .35
		
		for ( int i = 0; i < legalActions.length; i++ ) {
			LegalActionObject legalAction = legalActions[i];
			
			if (legalAction.actionType.equalsIgnoreCase("check")){
				results[0] = 0; //TODO: is this right?
			}
			
			else if (legalAction.actionType.equalsIgnoreCase("call")){
				//(%win - %lose)(pot+2*oppBet)
				results[1] =  (winChance - loseChance) * (potSize + 2 * opponent.getAverageBet(street));
			}
			else if (legalAction.actionType.equalsIgnoreCase("bet")){	
				//(%oppfolds)(Pot)+(%oppcalls)[(%win)(Pot+Bet)-(%lose)(Bet)]
				int ourEstimatedBet = makeBet(Utils.boundInt(startingStack - (potSize / 2), 1, startingStack), potSize, winChance, street, opponent);
				results[2] = (opponent.getPercentFoldToBet(street) * potSize) + opponent.getPercentCallToBet(street) * (winChance * (potSize + ourEstimatedBet) - (loseChance) * ourEstimatedBet);
			}
			else if (legalAction.actionType.equalsIgnoreCase("raise")){
				//assuming we call will always just call a reraise
				//(%oppfolds)(Pot) + (%oppcalls)[(%win)(Pot+raise)-(%lose)(raise)] + (%oppraises)[(%win - %lose)(Pot+(2*hisRaise))]
				int ourEstimatedRaise = makeRaise(potSize, winChance);
				results[3] = (opponent.getPercentFoldToRaise(street) * potSize) 
						+ opponent.getPercentCallToRaise(street) * ( winChance * (potSize + ourEstimatedRaise) - (loseChance) * ourEstimatedRaise )
						+ opponent.getPercentRaiseToRaise(street) * ( (winChance - loseChance) * (potSize + (2 * opponent.getAverageRaise(street))));
			}
		}
		
		
		return selectActionWithHighestEV(results);
	}
	
	private String selectActionWithHighestEV(float[] results) {
		String action = "fold"; 
		float ev_action = 0; 
		for (int i=0; i<results.length; i++){
			System.out.println(options[i] + ": " + results[i]); 
			if (results[i] > ev_action){
				ev_action = results[i];
				action = options[i];
			}
		}
		System.out.println("EV ACTION: " + action + "\n");
		return action;
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
