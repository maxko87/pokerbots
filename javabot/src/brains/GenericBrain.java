package brains;

import pokerbots.packets.GameObject;
import pokerbots.packets.GetActionObject;
import pokerbots.packets.LegalActionObject;
import pokerbots.utils.MatchHistory;
import pokerbots.utils.Utils;
import pokerbots.utils.StatAggregator.OpponentStats;

public class GenericBrain {
	
	public GameObject myGame;
	
	//these are updated by takeAction
	MatchHistory history;
	OpponentStats opponent;
	float winChance;
	int street;
	GetActionObject getActionObject;
	
	public void setVars (OpponentStats o, GetActionObject g, float w, int s){
		opponent = o;
		getActionObject = g;
		winChance = w;
		street = s;
	}
	
	public String takeAction(OpponentStats o, GetActionObject g, float w, int s) {
		//overriden in children classes
		return null;
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

	public String toString(){
		return "GenericBrain";
	}
	
}
