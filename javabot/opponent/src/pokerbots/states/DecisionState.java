package pokerbots.states;

import pokerbots.packets.GameObject;
import pokerbots.packets.GetActionObject;
import pokerbots.packets.HandObject;
import pokerbots.player.StatePlayer;
import pokerbots.utils.HandEvaluator;
import pokerbots.utils.StochasticSimulator;

public abstract class DecisionState {
	protected StatePlayer controller;
	public void initialize( StatePlayer sp ) { controller = sp; }
	public abstract ResponseObject preflop( GameObject myGame, HandObject myHand, GetActionObject actions );
	public abstract ResponseObject flop( GameObject myGame, HandObject myHand, GetActionObject action );
	public ResponseObject discard( GameObject myGame, HandObject myHand, GetActionObject actions ) {
		float[] r0 = StochasticSimulator.computeRates( 
				new int[]{
					myHand.cards3[1],myHand.cards3[2]
				}, actions.boardCards, 400);
		float[] r1 = StochasticSimulator.computeRates( 
				new int[]{
					myHand.cards3[0],myHand.cards3[2]
				}, actions.boardCards, 400);
		float[] r2 = StochasticSimulator.computeRates( 
				new int[]{
					myHand.cards3[0],myHand.cards3[1]
				}, actions.boardCards, 400);
		
		int remove = 0;
		if ( r0[10]>r1[10] && r0[10]>r2[10] )
			remove = 0;
		if ( r1[10]>r0[10] && r1[10]>r2[10] )
			remove = 1;
		else
			remove = 2;
		
		if ( remove == 0 ) {
			myHand.cards2[0] = myHand.cards3[1];
			myHand.cards2[1] = myHand.cards3[2];
		} else if ( remove == 1 ) {
			myHand.cards2[0] = myHand.cards3[0];
			myHand.cards2[1] = myHand.cards3[2];
		} else if ( remove == 2 ) {
			myHand.cards2[0] = myHand.cards3[0];
			myHand.cards2[1] = myHand.cards3[1];
		}
		
		return new ResponseObject("DISCARD",HandEvaluator.cardToString(myHand.cards3[remove]), this);
	}
	public abstract ResponseObject turn( GameObject myGame, HandObject myHand, GetActionObject actions );
	public abstract ResponseObject river( GameObject myGame, HandObject myHand, GetActionObject actions );
}
