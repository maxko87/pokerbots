package pokerbots.botv5;

import pokerbots.packets.GameObject;
import pokerbots.packets.GetActionObject;
import pokerbots.packets.HandObject;
import pokerbots.states.DecisionState;
import pokerbots.states.ResponseObject;
import pokerbots.utils.BettingBrain;
import pokerbots.utils.HandEvaluator;

public class ValueBettingState extends DecisionState {
	
	BettingBrain bettingBrain;
	
	public ValueBettingState() {
		bettingBrain = new BettingBrain();
	}
	
	public ResponseObject preflop( GameObject myGame, HandObject myHand, GetActionObject actions ) {
		float[] rates = HandEvaluator.handOdds(myHand, actions, 500);
		String actionParam = bettingBrain.takeAction(controller.getOpponentStats(), actions, rates[10], 0);
		return new ResponseObject(actionParam, this);
	}
	
	public ResponseObject flop( GameObject myGame, HandObject myHand, GetActionObject actions ) {
		float[] rates = HandEvaluator.handOdds(myHand, actions, 500);
		String actionParam = bettingBrain.takeAction(controller.getOpponentStats(), actions, rates[10], 1);
		return new ResponseObject(actionParam, this);
	}
	
	public ResponseObject turn( GameObject myGame, HandObject myHand, GetActionObject actions ) {
		float[] rates = HandEvaluator.handOdds(myHand, actions, 500);
		String actionParam = bettingBrain.takeAction(controller.getOpponentStats(), actions, rates[10], 2);
		return new ResponseObject(actionParam, this);
	}
	
	public ResponseObject river( GameObject myGame, HandObject myHand, GetActionObject actions ) {
		float[] rates = HandEvaluator.handOdds(myHand, actions, 500);
		String actionParam = bettingBrain.takeAction(controller.getOpponentStats(), actions, rates[10], 3);
		return new ResponseObject(actionParam, this);
	}
}
