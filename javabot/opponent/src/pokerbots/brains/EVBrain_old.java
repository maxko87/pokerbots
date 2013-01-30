package pokerbots.brains;

import pokerbots.packets.GameObject;
import pokerbots.packets.GetActionObject;
import pokerbots.packets.LegalActionObject;
import pokerbots.utils.EVCalculator;
import pokerbots.utils.EVCalculator.EVObj;
import pokerbots.utils.MatchHistory;
import pokerbots.utils.StatAggregator.OpponentStats;
import pokerbots.utils.Utils;

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

public class EVBrain_old extends GenericBrain{
	
	//which players are in use?
	boolean EV_Player = true; 
	
	
	
	
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
	
	//threshold for learning amounts in order to use heuristics
	private final int[] THRESHOLD_FOR_GENERALIZING = new int[] {3, 3, 3, 3};
	
	//number of hands to start using expected value and generalizing stuff
	private final int ENOUGH_HANDS = 100;
	
	EVCalculator ev;
	
	public EVBrain_old(GameObject game,MatchHistory history){
		this.game = game;
		ev = new EVCalculator(history);
		this.history = history;
	}

	
	
	//DELEGATES ALL ACTIONS 
	public String takeAction(OpponentStats o, GetActionObject g, float w, int s) {
		
		super.setVars(o, g, w, s);
		
		//turn on EV player after enough data is collected
		if (opponent.totalHandCount > ENOUGH_HANDS)
			EV_Player = true;
		
		//PREFLOP ADJUSTMENTS
		if ( street == 0 ) {
			float raise_size = winChance * Utils.scale(opponent.getLooseness(0), .2f, .8f, 0f, 1f) * (this.game.stackSize / 10) + 2;
			if ( winChance > getMinWinChance() ) {
				if (winChance > getMinWinChanceForRaisePreflop() && raise_size < 30 && opponent.totalHandCount > ENOUGH_HANDS){
					return validateAndReturn("raise",(int)(raise_size));
				}
			}
			return validateAndReturn("call",0); //don't continuously reraise preflop
		}
		
		//USING EV CALCULATOR
		if (EV_Player && street == 3){
			System.out.println("using EV Calc");
			EVObj evObj = ev.getRiverEVandAction(opponent, winChance, getActionObject);
			int maxBet = Utils.boundInt(this.game.stackSize - (getActionObject.potSize / 2), 1, this.game.stackSize);
			if (evObj.action.equalsIgnoreCase("bet")){
				//return validateAndReturn("bet", makeBet(maxBet, getActionObject.potSize));
				return validateAndReturn("bet", (int)evObj.EV);
			}
			else if (evObj.action.equalsIgnoreCase("raise")){
				//return validateAndReturn("raise", makeRaise(maxBet, getActionObject.potSize));
				return validateAndReturn("bet", (int)evObj.EV);
			}
			else if (evObj.action.equalsIgnoreCase("call")){
				return validateAndReturn("call", 0);
			}
			return validateAndReturn("check", 0);
		}
		
		//original, basic strategy
		if ( winChance > getMinWinChance() ){
			return betRaiseCall();
		}
		else{
			return foldOrCheck();
		}		
	}
	
	// minimum chance of winning we need to play
	public float getMinWinChance(){
		// use opponent's aggression to scale our looseness -- higher opp aggression = we play tighter
		//float winChance = Utils.scale(opponent.getAggression(street), 0.0f, 1.0f, MIN_WIN_TO_PLAY[street][0], MIN_WIN_TO_PLAY[street][1]);
		float winChance = Utils.inverseScale(opponent.getLooseness(street), 0.0f, 1.0f, MIN_WIN_TO_PLAY[street][0], MIN_WIN_TO_PLAY[street][1]);
		if (opponent.totalHandCount < ENOUGH_HANDS){
			winChance = MIN_WIN_TO_PLAY[street][0];
		}
			
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
	
	public float getMinWinChanceForRaisePreflop(){
		float winChance = Utils.inverseScale(opponent.getLooseness(street), 0.0f, 1.0f, .75f, .9f);
		//winChance = Utils.scale(winChance, 0f, 1f, .65f, 1f);
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
	
	public String toString(){
		return "EVBrain_old";
	}
	
	
	
	
}
