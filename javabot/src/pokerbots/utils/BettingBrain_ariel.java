package pokerbots.utils;

import pokerbots.packets.GetActionObject;
import pokerbots.packets.LegalActionObject;
import pokerbots.utils.StatAggregator_old.OpponentStats;

public class BettingBrain_ariel {
	
	float val1 = 0.3f;
	float val2 = 0.6f;
	
	float val3 = 0.7f;
	float val4 = 1.0f;
	
	float val5 = 0.4f;
	float val6 = 0.6f;
	
	float val7 = 0.4f;
	float val8 = 0.6f;
	

	float val17 = 0.05f;
	
	//minimum default percentage range of winning to play/raise each street.
	private final float[][] MIN_WIN_TO_PLAY = new float[][] {{val1, val2}, {val3, val4}, {val5, val6}, {val7, val8}};
	private final float[] MIN_WIN_TO_RAISE = new float[] {.6f, .8f, .7f, .7f};
	
	//scaling for larger bets on later streets.
	private final float[] CONTINUATION_FACTORS = new float[] {1.0f, 1.0f, 1.2f, 1.5f};	
	
	//maxmimum reduction in winChance based on a strong bet. TODO: this should be lower when we play against bluffing bots
	private final float MAX_WIN_CHANCE_REDUCTION = val17;
	
	//threshold for learning amounts in order to use heuristics
	private final int[] THRESHOLD_FOR_GENERALIZING = new int[] {3, 3, 3, 3};
	
	

	//DELEGATES ALL ACTIONS 
	public String takeAction(OpponentStats o, GetActionObject g, float w, int s) {

		//check heuristics
		for ( int i = 0; i < g.legalActions.length; i++ ) {
			LegalActionObject legalAction = g.legalActions[i];

			if (legalAction.actionType.equalsIgnoreCase("bet")){

				//betting heuristics
				if (oppNeverFoldsToBet(o,g,w,s)){
					System.out.println("BET ALL IN");
					return validateAndReturn("bet", legalAction.maxBet,g);
				}

			}
			else if (legalAction.actionType.equalsIgnoreCase("raise")){

				//raising heuristics
				if (oppNeverFoldsToRaise(o,g,w,s)){
					System.out.println("RAISE ALL IN");
					return validateAndReturn("raise", legalAction.maxBet,g);
				}

			}
		}
		if ( w > getMinWinChance(o,g,w,s) || playAnyways(o,g,w,s) ){
			return betRaiseCall(o,g,w,s);
		}
		else{
			return foldOrCheck(o,g,w,s);
		}		
	}

	//if the opp made a tiny bet, cover it regardless of percentage of winning
	private final float[] MAX_PORTION_OF_POT_TO_PLAY = new float[] {.1f, .15f, .2f, .2f};
	private int MAX_BET_TO_STILL_PLAY = 6;
	public boolean playAnyways(OpponentStats o, GetActionObject g, float w, int s){
		if (g.lastActions.length > 0 && g.lastActions[g.lastActions.length-1].amount > 0){
			int amount  = g.lastActions[g.lastActions.length-1].amount;
			boolean playAnyways =  (amount < MAX_PORTION_OF_POT_TO_PLAY[s] * g.potSize) || (amount < MAX_BET_TO_STILL_PLAY);
			System.out.println("PLAY ANYWAYS: " + playAnyways);
			return playAnyways;
		}
		return false;
	}


	//HEURISTIC: if they never fold and we have a great hand, bet/raise all
	private final float[] MIN_HAND_FOR_ALL_IN = new float[] {.7f, .8f, .9f, .9f};
	public boolean oppNeverFoldsToBet(OpponentStats o, GetActionObject g, float w, int s){
		return (o.getPercentFoldToBet(s) < .05f && w > MIN_HAND_FOR_ALL_IN[s] && o.timesFoldsToBet[s] > THRESHOLD_FOR_GENERALIZING[s]);
	}
	public boolean oppNeverFoldsToRaise(OpponentStats o, GetActionObject g, float w, int s){
		return (o.getPercentFoldToBet(s) < .05f && w > MIN_HAND_FOR_ALL_IN[s] && o.timesFoldsToBet[s] > THRESHOLD_FOR_GENERALIZING[s]);
	}



	// minimum chance of winning we need to play
	public float getMinWinChance(OpponentStats o, GetActionObject g, float w, int s){
		// use opponent's aggression to scale our looseness -- higher opp aggression = we play tighter
		float winChance = Utils.scale(o.getAggression(s), 0.0f, 1.0f, MIN_WIN_TO_PLAY[s][0], MIN_WIN_TO_PLAY[s][1]);

		// if the opponent bets a lot compared to the pot, our win chance goes down
		/*
float winChanceIncreaseNeeded = 0f;
if (getActionObject.lastActions.length > 0){
PerformedActionObject performedAction = getActionObject.lastActions[getActionObject.lastActions.length-1];
if ( performedAction.actor.equalsIgnoreCase(myGame.oppName) && (performedAction.actionType.equalsIgnoreCase("bet") || performedAction.actionType.equalsIgnoreCase("raise")) ) {
winChanceIncreaseNeeded = Utils.scale(performedAction.amount, 0, getActionObject.potSize, 0, MAX_WIN_CHANCE_REDUCTION);
winChance += winChanceIncreaseNeeded;
}		
}
		 */
		return winChance;
	}


	//given odds and stack size, decides how much to bet
	public int makeBet(int minBet, int maxBet, int potSize,OpponentStats o, GetActionObject g, float w, int s){
		float bet = (w - MIN_WIN_TO_PLAY[s][0]) * maxBet;
		bet = bet * o.getLooseness(s); 
		return (int)bet;
	}

	//given odds and stack size, decides how much to raise
	public int makeRaise(int minBet, int maxBet, int potSize,OpponentStats o, GetActionObject g, float w, int s){
		int raise;
		if (s == 0){
			raise = (int) (Utils.randomBetween(5, 10) * potSize * w);
		}
		else{
			raise = (int) (potSize * w);
		}		
		return raise;
	}

	public String betRaiseCall(OpponentStats o, GetActionObject g, float w, int s) {

		for ( int i = 0; i < g.legalActions.length; i++ ) {
			LegalActionObject legalAction = g.legalActions[i];

			if ( legalAction.actionType.equalsIgnoreCase("bet") ) {
				int bet = (int)(makeBet(legalAction.minBet, legalAction.maxBet, g.potSize,o,g,w,s));
				return validateAndReturn("bet", bet,g);
			}
			else if ( legalAction.actionType.equalsIgnoreCase("raise") ) {
				if (w > MIN_WIN_TO_RAISE[s]){
					int raise = (int)(makeRaise(legalAction.minBet, legalAction.maxBet, g.potSize,o,g,w,s));
					return validateAndReturn("raise", raise,g);
				}
				else{
					return validateAndReturn("call",0,g);
				}
			}
		}
		return validateAndReturn("call",0,g);
	}


	public String foldOrCheck(OpponentStats o, GetActionObject g, float w, int s) {
		for ( int i = 0; i < g.legalActions.length; i++ ) {
			LegalActionObject action = g.legalActions[i];
			if ( action.actionType.equalsIgnoreCase("check") ) {
				return "CHECK";
			}
		}
		return "FOLD";
	}

	//makes sure that the move we are making is legal, and fixes it automatically if not
	public String validateAndReturn(String action, int amount, GetActionObject g){
		for ( int i = 0; i < g.legalActions.length; i++ ) {
			LegalActionObject legalAction = g.legalActions[i];
			if (legalAction.actionType.equalsIgnoreCase(action)){
				if (action.equalsIgnoreCase("bet") || action.equalsIgnoreCase("raise")){
					amount = Utils.boundInt(amount, legalAction.minBet, legalAction.maxBet);
					return action.toUpperCase()+":"+amount;
				}
				else{
					return action.toUpperCase();
				}
			}
		}
		// if we get here, we tried to make an erroneous move
		// 1) if they raise us all in, and we want to raise, we just call instead
		if (action.equalsIgnoreCase("raise")){
			System.out.println("Tried to raise and all in!  Calling.");
			return "CALL";
		}
		// 2) instead of calling, just check
		return "CHECK";
	}
}
