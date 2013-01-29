package pokerbots.brains;

import pokerbots.packets.GameObject;
import pokerbots.packets.GetActionObject;
import pokerbots.packets.LegalActionObject;
import pokerbots.packets.PerformedActionObject;
import pokerbots.utils.MatchHistory;
import pokerbots.utils.StatAggregator.OpponentStats;
import pokerbots.utils.Utils;


public class AdvancedBrain extends GenericBrain{
	
	public boolean playAnywaysFlag = true;
	
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
	

	
	public AdvancedBrain(GameObject game, MatchHistory history){
		myGame = game;
		this.history = history;
	}

	
	
	//DELEGATES ALL ACTIONS 
	public String takeAction(OpponentStats o, GetActionObject g, float w, int s) {
		
		super.setVars(o, g, w, s);
		
		//PREFLOP ADJUSTMENTS
		if ( street == 0 ) {
			float raise_size = winChance * Utils.scale(opponent.getLooseness(0), .2f, .8f, 0f, 1f) * (myGame.stackSize / 10) + 2;
			if (winChance > getMinWinChanceForRaisePreflop() && raise_size < 30 && opponent.totalHandCount > 20){
				return validateAndReturn("raise",(int)(raise_size));
			}
			return validateAndReturn("call",0); //don't continuously reraise preflop
		}
		
		if (weBeatThem() || weBluffThem() || (playAnywaysFlag && playAnyways())){
			return betRaiseCall(false);
		}
		else{
			return foldOrCheck();
		}		
	}
	
	//returns true if we have a higher estimated winChance than them
	private boolean weBeatThem() {
		
		int streetOfTheirLastAction = street;
		float oppWinChanceEstimate = Utils.inverseScale(opponent.getLooseness(street), 0.0f, 1.0f, MIN_WIN_TO_PLAY[street][0], MIN_WIN_TO_PLAY[street][1]);
		
		for ( int i = getActionObject.lastActions.length-1; i >= 0; i-- ) {
			PerformedActionObject performedAction = getActionObject.lastActions[i];
			if ( performedAction.actionType.equalsIgnoreCase("deal") ) {
				streetOfTheirLastAction -= 1;
			}
			else if ( performedAction.actionType.equalsIgnoreCase("bet") && performedAction.actor.equalsIgnoreCase(myGame.oppName)  ) {
				oppWinChanceEstimate = opponent.value_Bet_given_their_winChance[streetOfTheirLastAction].getInverseModel(performedAction.amount);
				if (winChance > oppWinChanceEstimate){
					System.out.println("estimate we win: " + winChance + " > " +oppWinChanceEstimate);
					return true;
				}
				else{
					System.out.println("estimate we lose: " + winChance + " < " +oppWinChanceEstimate);
					return true;
				}
					
			}
			else if ( performedAction.actionType.equalsIgnoreCase("raise") && performedAction.actor.equalsIgnoreCase(myGame.oppName)  ) {
				oppWinChanceEstimate = opponent.value_Raise_given_their_winChance[streetOfTheirLastAction].getInverseModel(performedAction.amount);
				if (winChance > oppWinChanceEstimate){
					System.out.println("estimate we win: " + winChance + " > " +oppWinChanceEstimate);
					return true;
				}
				else{
					System.out.println("estimate we lose: " + winChance + " < " +oppWinChanceEstimate);
					return true;
				}
			}
			
		}
		System.out.println("estimate fail");
		return false;
	}
	
	private boolean weBluffThem() {
		return false;
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
	
	public float getMinWinChanceForRaisePreflop(){
		float winChance = Utils.inverseScale(opponent.getLooseness(street), 0.0f, 1.0f, .7f, .85f);
		System.out.println("WINCHANCE FOR RERAISE PREFLOP: " + winChance);
		return winChance;
	}
		
	
	
	//given odds and stack size, decides how much to bet
	public int makeBet(int maxBet, int potSize){
		float bet = (winChance - MIN_WIN_TO_PLAY[street][0]) * maxBet * Utils.randomBetween(.8f, 1.2f);
		bet = bet * opponent.getLooseness(street); 
		return (int)bet;
	}
	
	//given odds and stack size, decides how much to raise
	public int makeRaise(int maxBet, int potSize){
		int raise;
		raise = (int) (potSize * winChance * Utils.randomBetween(.8f, 1.2f));		
		return raise;
	}
	
	public String betRaiseCall(boolean bluff) {
		
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
	
	public String toString(){
		return "S+EV_Brain";
	}	
	
	
}
