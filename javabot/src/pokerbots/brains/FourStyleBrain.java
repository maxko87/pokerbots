package pokerbots.brains;

import pokerbots.fourstyle.CallStation;
import pokerbots.fourstyle.LAG;
import pokerbots.fourstyle.Rock;
import pokerbots.fourstyle.Style;
import pokerbots.fourstyle.TAG;
import pokerbots.packets.GameObject;
import pokerbots.packets.GetActionObject;
import pokerbots.packets.LegalActionObject;
import pokerbots.utils.MatchHistory;
import pokerbots.utils.StatAggregator.OpponentStats;

public class FourStyleBrain extends GenericBrain {
	
	Style himCallStation = new CallStation();
	Style himLAG = new LAG();
	Style himRock = new Rock();
	Style himTAG = new TAG();
	
	public FourStyleBrain( MatchHistory history, GameObject game ) {
		this.history = history;
		this.game = game;
	}
	
	public String takeAction(OpponentStats o, GetActionObject g, float w, int s) {
		this.setVars(o, g, w, s);
		
		//Predict his win chances based on anything we can
		float t = 0;
		
		//PlayStyle Consensus
		float looseness = o.getLooseness(s);
		float aggression = o.getAggression(s);
		Style hisPlayStyle = himTAG;

		
		//Check if loose playstyle
		if ( looseness>0.45f ) {
			if ( aggression>0.45f )
				hisPlayStyle = himLAG;
			else
				hisPlayStyle = himCallStation;
		} else {
			if ( aggression>0.45f )
				hisPlayStyle = himTAG;
			else
				hisPlayStyle = himRock;
		}
		System.out.println("<<<< Opponent Playstyle: [L="+looseness+",A="+aggression+"] -> " + hisPlayStyle.getName() +" >>>>");
		
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