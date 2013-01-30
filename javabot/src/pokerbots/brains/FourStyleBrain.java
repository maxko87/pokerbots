package pokerbots.brains;

import pokerbots.fourstyle.CallStation;
import pokerbots.fourstyle.Style;
import pokerbots.packets.GameObject;
import pokerbots.packets.GetActionObject;
import pokerbots.packets.LegalActionObject;
import pokerbots.strategy.BasicStrategy;
import pokerbots.utils.MatchHistory;
import pokerbots.utils.StatAggregator.OpponentStats;

public class FourStyleBrain extends GenericBrain {
	
	Style himCallStation = new CallStation();
	
	public FourStyleBrain( MatchHistory history, GameObject game ) {
		this.history = history;
		this.game = game;
	}
	
	public String takeAction(OpponentStats o, GetActionObject g, float w, int s) {
		this.setVars(o, g, w, s);
		
		//Predict his win chances based on anything we can
		float t = 0;
		
		//PlayStyle Consensus
		Style hisPlayStyle = himCallStation;
		
		//Decide which turn we're taking
		boolean onAction = getOnAction();
		if ( onAction )
			return hisPlayStyle.actionInitiator(t, this);
		else
			return hisPlayStyle.actionReceiver(t, this);
	}
	
	public boolean getOnAction() {
		for ( int i = 0; i < getActionObject.legalActions.length; i++ ) {
			LegalActionObject g = getActionObject.legalActions[i];
			if ( g.actionType.equalsIgnoreCase("bet") ){
				return true;
			}
		}
		return false;
	}
}