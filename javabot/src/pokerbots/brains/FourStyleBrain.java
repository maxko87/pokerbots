package pokerbots.brains;

import pokerbots.packets.GameObject;
import pokerbots.packets.GetActionObject;
import pokerbots.strategy.BasicStrategy;
import pokerbots.strategy.BettingStrategy;
import pokerbots.utils.MatchHistory;
import pokerbots.utils.StatAggregator.OpponentStats;

public class FourStyleBrain extends GenericBrain {
	
	MatchHistory history;
	GameObject game;
	BettingStrategy strategy;
	
	public FourStyleBrain( MatchHistory history, GameObject game ) {
		this.history = history;
		this.game = game;
		this.strategy = new BasicStrategy();
	}
	
	public String takeAction(OpponentStats o, GetActionObject g, float w, int s) {
		this.setVars(o, g, w, s);
		//overriden in children classes
		if ( w>0.72 )
			return validateAndReturn("call",0);
		return validateAndReturn("check", 0);
	}
}