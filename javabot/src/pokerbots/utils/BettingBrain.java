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
 *  
 *  
 *  For the bluffs, we should make use of getOurAverageBetForFold and getOurAverageRaiseForFold, and maybe a comparable 
 *  getOurAverageBetForCall/Raise -- that way, we may be able to identify a value threshold to which the opponent reliably folds.
 * 
 * 
 * 
 * 
 * 
 */

public class BettingBrain {
	
	float val1 = 0.25f;
	float val2 = 0.45f;
	
	float val3 = 0.7f;
	float val4 = 0.1f;
	
	float val5 = 0.4f;
	float val6 = 0.7f;
	
	float val7 = 0.3f;
	float val8 = 0.4f;
	
	float val9 = 0.23f;
	float val10 = 0.35f;
	float val11 = 0.33f;
	float val12 = 0.47f;
	
	float val13 = 0.6f;
	float val14 = 0.9f;
	float val15 = 1.0f;
	float val16 = 1.0f;
	
	float val17 = 0.05f;
	
	//minimum default percentage range of winning to play/raise each street.
	//private final float[][] MIN_WIN_TO_PLAY = new float[][] {{.2f, .5f}, {.2f, .5f}, {.4f, .5f}, {.4f, .5f}};
	private final float[][] MIN_WIN_TO_PLAY = new float[][] {{val1, val2}, {val3, val4}, {val5, val6}, {val7, val8}};
	private final float[] MIN_WIN_TO_RAISE = new float[] {.8f, .8f, .9f, .9f};
	
	//scaling for larger bets on later streets.
	//private final float[] CONTINUATION_FACTORS = new float[] {1.0f, 1.0f, 1.5f, 2.0f};
	
	//minimum/maximum percentages of pot we should be betting during value bets
	//private final float[] MIN_OF_POT = new float[] {.2f, .2f, .3f, .4f};
	private final float[] MIN_OF_POT = new float[] {val9, val10, val11, val12};
	//private final float[] MAX_OF_POT = new float[] {1.0f, 1.0f, .8f, .5f};
	
	//minimum/maximum percentages of our stack we should be betting during value bets
	//private final float[] MIN_OF_POT = new float[] {.2f, .2f, .2f, .3f};
	//private final float[] MIN_OF_STACK = new float[] {.4f, .7f, .8f, 1.0f};
	private final float[] MAX_OF_STACK = new float[] {val13, val14, val15, val16};
	
	//maxmimum reduction in winChance based on a strong bet. TODO: this should be lower when we play against bluffing bots
	private final float MAX_WIN_CHANCE_REDUCTION = val17;


	public enum State {VALUE_BET, BLUFF, ETC}
	
	public GameObject myGame;
	
	public BettingBrain(GameObject game){
		myGame = game;
	}
	
	
	
	
	
	
	//DELEGATES ALL ACTIONS 
	public String takeAction(OpponentStats opponent, GetActionObject getActionObject, float winChance, int street) {
		if ( winChance > getMinWinChance(getActionObject, street, opponent) )
			return betRaiseCall(opponent, getActionObject,winChance,street);
		else
			return foldOrCheck(getActionObject);
	}
	
	
	
	// minimum chance of winning we need to play
	public float getMinWinChance( GetActionObject getActionObject, int street, OpponentStats opponent){
		// use opponent's aggression to scale our looseness -- higher opp aggression = we play tighter
		float winChance = Utils.scale(opponent.getTotalAggression(myGame.stackSize), 0.0f, 1.0f, MIN_WIN_TO_PLAY[street][0], MIN_WIN_TO_PLAY[street][1]);
		
		// if the opponent bets a lot compared to the pot, our win chance goes down
		float winChanceIncreaseNeeded = 0f;
		if (getActionObject.lastActions.length > 0){
			PerformedActionObject performedAction = getActionObject.lastActions[getActionObject.lastActions.length-1];
			if ( performedAction.actor.equalsIgnoreCase(myGame.oppName) && performedAction.actionType.equalsIgnoreCase("bet")){
				winChanceIncreaseNeeded = Utils.scale(performedAction.amount, 0, getActionObject.potSize, 0, MAX_WIN_CHANCE_REDUCTION);
				winChance += winChanceIncreaseNeeded;
			}		
		}
		return winChance;
	}
	
	
	
	
	
	//given odds and stack size, decides how much to bet
	public int makeProportionalBet(OpponentStats opponent, float expectedWinPercentage, int minBet, int maxBet, int potSize, int street){
		int myRemainingStack = (myGame.stackSize - potSize/2); //TODO: this is an approximation

		//first, create a reasonable bet based on our winning expectation and stack size
		int bet = (int) ((expectedWinPercentage - MIN_WIN_TO_PLAY[street][0]) * (maxBet - minBet));
		
		//next, make sure this is a reasonable bet for the pot size and stack size
		bet = Utils.boundInt(bet, (int)(MIN_OF_POT[street]*potSize), (int)(MAX_OF_STACK[street]*myRemainingStack));
		
		//factor in opponent looseness
		bet = (int)(bet*opponent.getTotalLooseness());
		
		//finally, bound to make sure it's valid
		bet = Utils.boundInt(bet, minBet, maxBet);
		
		return bet;
	}
	
	// uses opponent's looseness to scale our bets -- higher opp looseness = we play more aggressively
	public String betRaiseCall( OpponentStats opponent, GetActionObject getActionObject, float winChance, int street ) {
		
		for ( int i = 0; i < getActionObject.legalActions.length; i++ ) {
			LegalActionObject legalAction = getActionObject.legalActions[i];
		
			if ( legalAction.actionType.equalsIgnoreCase("bet") ) {
				int bet = (int)(makeProportionalBet(opponent, winChance, legalAction.minBet, legalAction.maxBet, getActionObject.potSize, street));
				return "BET:"+bet;
			}
			else if ( legalAction.actionType.equalsIgnoreCase("raise") ) {
				//raise or call? TODO: don't use same formula as betting...
				if (winChance > MIN_WIN_TO_RAISE[street]){
					int raise = (int)(makeProportionalBet(opponent, winChance, legalAction.minBet, legalAction.maxBet, getActionObject.potSize, street));
					return "RAISE:"+raise;
				}
				else{
					return "CALL";
				}
			}
		}
		return "FOLD";
	}
	

	public String foldOrCheck( GetActionObject curr ) {
		for ( int i = 0; i < curr.legalActions.length; i++ ) {
			LegalActionObject action = curr.legalActions[i];
			if ( action.actionType.equalsIgnoreCase("check") ) {
				return "CHECK";
			}
		}
		return "FOLD";
	}

}
