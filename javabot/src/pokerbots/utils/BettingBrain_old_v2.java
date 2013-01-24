package pokerbots.utils;

import pokerbots.packets.GameObject;
import pokerbots.packets.GetActionObject;
import pokerbots.packets.LegalActionObject;
import pokerbots.packets.PerformedActionObject;
import pokerbots.utils.StatAggregator.OpponentStats;

/*
 * High-level strategies:
 * 
 * 1) Value betting. This is what we are doing normally -- bet based on our own chance of winning, thus playing good hands harder. This will be enough
 * to beat at least 75% of the bots, and we should stick to it as much as we can (the rest of the strategies rely increasingly on psychology, which
 * bots may or may not have).
 * 
 * turn and river (maybe even check/call on the turn and hold off the betting until the river). 
 * 
 * 3) Holding out bluff. Similar to above, but instead of actually having a strong hand, we wait for a board that might make us seem like we have a 
 * better hand than the opponent (e.g. three queens on the board after the turn) and we bet hard. This is essentially hoping that our opponent's bot
 * is factoring in our bet sizes in its decisions of whether or not to play -- this might backfire against worse bots that don't do this.
 * 
 * 4) Check raise. If we have a very strong hand on any street and we are on action, we may want to check, let the opponent bet, and then raise them
 * hard. This works well if the opponent is tight and aggressive -- if we can be confident they will bet after our check, we can be sure to take at
 * least some money from them (as opposed to coming out with a strong bet that they might fold to).
 * 
 * 5) Check raise bluff. Same as 4 but without the strong hand.
 *  
 * 
 *  For the bluffs, we should make use of getOurAverageBetForFold and getOurAverageRaiseForFold, and maybe a comparable 
 *  getOurAverageBetForCall/Raise -- that way, we may be able to identify a value threshold to which the opponent reliably folds.
 * 
 * 
 */

public class BettingBrain_old_v2 {
	
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
	private final float[] MIN_WIN_TO_RAISE = new float[] {.6f, .8f, .7f, .7f};
	
	//maxmimum reduction in winChance based on a strong bet. TODO: this should be lower when we play against bluffing bots
	private final float MAX_WIN_CHANCE_REDUCTION = val17;
	
	//threshold for learning amounts in order to use heuristics
	private final int[] THRESHOLD_FOR_GENERALIZING = new int[] {3, 3, 3, 3};


	public enum State {VALUE_BET, BLUFF, ETC}
	
	public GameObject myGame;
	
	//these are updated by takeAction
	OpponentStats opponent;
	GetActionObject getActionObject;
	float winChance;
	int street;
	
	public BettingBrain_old_v2(GameObject game){
		myGame = game;
	}

	
	
	//DELEGATES ALL ACTIONS 
	public String takeAction(OpponentStats o, GetActionObject g, float w, int s) {
		opponent = o;
		getActionObject = g;
		winChance = w;
		street = s;
		
		
		/*
		//check heuristics
		for ( int i = 0; i < getActionObject.legalActions.length; i++ ) {
			LegalActionObject legalAction = getActionObject.legalActions[i];
			
			if (legalAction.actionType.equalsIgnoreCase("bet")){
				
				//betting heuristics
				if (oppNeverFoldsToBet()){
					System.out.println("BET ALL IN");
					return validateAndReturn("bet", legalAction.maxBet);
				}
				
			}
			else if (legalAction.actionType.equalsIgnoreCase("raise")){
				
				//raising heuristics
				if (oppNeverFoldsToRaise()){
					System.out.println("RAISE ALL IN");
					return validateAndReturn("raise", legalAction.maxBet);
				}
				
			}
		}
		*/
		
		//PREFLOP ADJUSTMENTS
		if ( street == 0 ) {
			float raise_size = winChance * Utils.scale(opponent.getLooseness(0), .2f, .8f, 0f, 1f) * (myGame.stackSize / 10) + 2;
			if ( winChance > getMinWinChance() ) {
				if (getActionObject.potSize > raise_size){
					return validateAndReturn("call",0);
				}
				return validateAndReturn("raise",(int)(raise_size));
			}
		}
		
		if ( winChance > getMinWinChance() || playAnyways() ){
			return betRaiseCall();
		}
		else{
			return foldOrCheck();
		}		
	}
	
	//if the opp made a tiny bet, cover it regardless of percentage of winning
	private final float[] MAX_PORTION_OF_POT_TO_PLAY = new float[] {.1f, .15f, .2f, .2f};
	private int MAX_BET_TO_STILL_PLAY = 6;
	public boolean playAnyways(){
		if (getActionObject.lastActions.length > 0 && getActionObject.lastActions[getActionObject.lastActions.length-1].amount > 0){
			int amount  = getActionObject.lastActions[getActionObject.lastActions.length-1].amount;
			boolean playAnyways =  (amount < MAX_PORTION_OF_POT_TO_PLAY[street] * getActionObject.potSize) || (amount < MAX_BET_TO_STILL_PLAY);
			System.out.println("PLAY ANYWAYS: " + playAnyways);
			return playAnyways;
		}
		return false;
	}
	
	/*
	//HEURISTIC: if they never fold and we have a great hand, bet/raise all
	private final float[] MIN_HAND_FOR_ALL_IN = new float[] {.7f, .8f, .9f, .9f};
	public boolean oppNeverFoldsToBet(){
		return (opponent.getPercentFoldToBet(street) < .05f && winChance > MIN_HAND_FOR_ALL_IN[street] && opponent.timesFoldsToBet[street] > THRESHOLD_FOR_GENERALIZING[street]);
	}
	public boolean oppNeverFoldsToRaise(){
		return (opponent.getPercentFoldToBet(street) < .05f && winChance > MIN_HAND_FOR_ALL_IN[street] && opponent.timesFoldsToBet[street] > THRESHOLD_FOR_GENERALIZING[street]);
	}
	*/
	
	
	
	// minimum chance of winning we need to play
	public float getMinWinChance(){
		// use opponent's aggression to scale our looseness -- higher opp aggression = we play tighter
		//float winChance = Utils.scale(opponent.getAggression(street), 0.0f, 1.0f, MIN_WIN_TO_PLAY[street][0], MIN_WIN_TO_PLAY[street][1]);
		float winChance = Utils.inverseScale(opponent.getLooseness(street), 0.0f, 1.0f, MIN_WIN_TO_PLAY[street][0], MIN_WIN_TO_PLAY[street][1]);
		System.out.println("WINCHANCE: " + winChance);
		
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
	public int makeBet(int minBet, int maxBet, int potSize){
		float bet = (winChance - MIN_WIN_TO_PLAY[street][0]) * maxBet;
		bet = bet * opponent.getLooseness(street); 
		return (int)bet;
	}
	
	//given odds and stack size, decides how much to raise
	public int makeRaise(int minBet, int maxBet, int potSize){
		int raise;
		raise = (int) (potSize * winChance);		
		return raise;
	}
	
	public String betRaiseCall() {
		
		for ( int i = 0; i < getActionObject.legalActions.length; i++ ) {
			LegalActionObject legalAction = getActionObject.legalActions[i];
		
			if ( legalAction.actionType.equalsIgnoreCase("bet") ) {
				int bet = (int)(makeBet(legalAction.minBet, legalAction.maxBet, getActionObject.potSize));
				return validateAndReturn("bet", bet);
			}
			else if ( legalAction.actionType.equalsIgnoreCase("raise") ) {
				if (winChance > MIN_WIN_TO_RAISE[street]){
					int raise = (int)(makeRaise(legalAction.minBet, legalAction.maxBet, getActionObject.potSize));
					return validateAndReturn("raise", raise);
				}
				else{
					return validateAndReturn("call",0);
				}
			}
		}
		return validateAndReturn("call",0);
	}
	

	public String foldOrCheck( ) {
		for ( int i = 0; i < getActionObject.legalActions.length; i++ ) {
			LegalActionObject action = getActionObject.legalActions[i];
			if ( action.actionType.equalsIgnoreCase("check") ) {
				return "CHECK";
			}
		}
		return "FOLD";
	}
	
	//makes sure that the move we are making is legal, and fixes it automatically if not
	public String validateAndReturn(String action, int amount){
		for ( int i = 0; i < getActionObject.legalActions.length; i++ ) {
			LegalActionObject legalAction = getActionObject.legalActions[i];
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
			System.out.println("SOMETHING FUCKED UP");
			return "CALL";
		}
		// 2) instead of calling, just check
		return "CHECK";
	}

	
	
	
	
	
	
}
