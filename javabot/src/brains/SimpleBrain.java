package brains;

import pokerbots.packets.GameObject;
import pokerbots.packets.GetActionObject;
import pokerbots.packets.LegalActionObject;
import pokerbots.packets.PerformedActionObject;
import pokerbots.utils.MatchHistory;
import pokerbots.utils.StatAggregator.OpponentStats;
import pokerbots.utils.Utils;


public class SimpleBrain {
	
	public boolean playAnywaysFlag = true;
	public boolean winChanceReductionFlag = true;
	
	float val1 = 0.5f;
	float val2 = 0.8f;
	
	float val3 = 0.6f;
	float val4 = 0.8f;
	
	float val5 = 0.5f;
	float val6 = 0.8f;
	
	float val7 = 0.5f;
	float val8 = 0.8f;
	

	float val17 = 0.05f;
	
	//minimum default percentage range of winning to play/raise each street.
	private final float[][] MIN_WIN_TO_PLAY = new float[][] {{val1, val2}, {val3, val4}, {val5, val6}, {val7, val8}};
	private final float[] MIN_WIN_TO_RAISE = new float[] {.8f, .8f, .7f, .7f};
	
	//maxmimum reduction in winChance based on a strong bet. TODO: this should be lower when we play against bluffing bots
	private final float MAX_WIN_CHANCE_REDUCTION = val17;
	
	public GameObject myGame;
	
	//these are updated by takeAction
	MatchHistory history;
	OpponentStats opponent;
	GetActionObject getActionObject;
	float winChance;
	int street;
	
	public SimpleBrain(GameObject game, MatchHistory history){
		myGame = game;
		this.history = history;
	}

	
	
	//DELEGATES ALL ACTIONS 
	public String takeAction(OpponentStats o, GetActionObject g, float w, int s) {
		
		opponent = o;
		getActionObject = g;
		winChance = w;
		street = s;
		
		//PREFLOP ADJUSTMENTS
		if ( street == 0 ) {
			float raise_size = winChance * Utils.scale(opponent.getLooseness(0), .2f, .8f, 0f, 1f) * (myGame.stackSize / 10) + 2;
			if ( winChance > getMinWinChance() ) {
				if (winChance > getMinWinChanceForRaisePreflop() && raise_size < 30 && opponent.totalHandCount > 20){
					return validateAndReturn("raise",(int)(raise_size));
				}
			}
			return validateAndReturn("call",0); //don't continuously reraise preflop
		}
		
		//original, basic strategy
		if ( winChance > getMinWinChance() || (playAnywaysFlag && playAnyways()) ){
			return betRaiseCall();
		}
		else{
			return foldOrCheck();
		}		
	}
	
	//if the opp made a tiny bet, cover it regardless of percentage of winning
	private final float[] MAX_PORTION_OF_POT_TO_PLAY = new float[] {.3f, .2f, .2f, .2f};
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
	
	// minimum chance of winning we need to play
	public float getMinWinChance(){
		// use opponent's aggression to scale our looseness -- higher opp aggression = we play tighter
		//float winChance = Utils.scale(opponent.getAggression(street), 0.0f, 1.0f, MIN_WIN_TO_PLAY[street][0], MIN_WIN_TO_PLAY[street][1]);
		float winChance = Utils.inverseScale(opponent.getLooseness(street), 0.0f, 1.0f, MIN_WIN_TO_PLAY[street][0], MIN_WIN_TO_PLAY[street][1]);
			
		System.out.println("WINCHANCE: " + winChance);
		
		if (winChanceReductionFlag){
			// if the opponent bets a lot compared to the pot, our win chance goes down
			float winChanceIncreaseNeeded = 0f;
			if (getActionObject.lastActions.length > 0){
				PerformedActionObject performedAction = getActionObject.lastActions[getActionObject.lastActions.length-1];
				if ( performedAction.actor.equalsIgnoreCase(myGame.oppName) && (performedAction.actionType.equalsIgnoreCase("bet") || performedAction.actionType.equalsIgnoreCase("raise")) ) {
					winChanceIncreaseNeeded = Utils.scale(performedAction.amount, 0, getActionObject.potSize, 0, MAX_WIN_CHANCE_REDUCTION);
					winChance += winChanceIncreaseNeeded;
				}
			}
		}
		
		return winChance;
		
	}
	
	public float getMinWinChanceForRaisePreflop(){
		float winChance = Utils.inverseScale(opponent.getLooseness(street), 0.0f, 1.0f, .7f, .85f);
		System.out.println("WINCHANCE FOR RERAISE PREFLOP: " + winChance);
		return winChance;
	}
		
	
	
	//given odds and stack size, decides how much to bet
	public int makeBet(int maxBet, int potSize){
		float bet = (winChance - MIN_WIN_TO_PLAY[street][0]) * maxBet;
		bet = bet * opponent.getLooseness(street); 
		return (int)bet;
	}
	
	//given odds and stack size, decides how much to raise
	public int makeRaise(int maxBet, int potSize){
		int raise;
		raise = (int) (potSize * winChance);		
		return raise;
	}
	
	public String betRaiseCall() {
		
		for ( int i = 0; i < getActionObject.legalActions.length; i++ ) {
			LegalActionObject legalAction = getActionObject.legalActions[i];
		
			if ( legalAction.actionType.equalsIgnoreCase("bet") ) {
				int bet = (int)(makeBet(legalAction.maxBet, getActionObject.potSize));
				return validateAndReturn("bet", bet);
			}
			else if ( legalAction.actionType.equalsIgnoreCase("raise") ) {
				if (winChance > MIN_WIN_TO_RAISE[street]){
					int raise = (int)(makeRaise(legalAction.maxBet, getActionObject.potSize));
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
		for ( int i = 0; i < getActionObject.legalActions.length; i++ ) {
			LegalActionObject legalAction = getActionObject.legalActions[i];
			if (legalAction.actionType.equalsIgnoreCase(action)){
				if (action.equalsIgnoreCase("raise")){
					return "CALL";
				}
				else if (action.equalsIgnoreCase("check")){
					return "CHECK";
				}
				else if (action.equalsIgnoreCase("fold")){
					return "FOLD";
				}
			}
		}
		if (action.equalsIgnoreCase("raise")){
			System.out.println("SOMETHING FUCKED UP");
			return "CALL";
		}
		// 2) instead of calling, just check
		return "CHECK";
	}

	
	
	
	
	
	
}
