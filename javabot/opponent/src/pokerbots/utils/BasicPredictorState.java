package pokerbots.utils;

import pokerbots.packets.GameObject;
import pokerbots.packets.GetActionObject;
import pokerbots.packets.HandObject;
import pokerbots.states.DecisionState;
import pokerbots.states.ResponseObject;

public class BasicPredictorState extends DecisionState {
	
	public ResponseObject preflop( GameObject myGame, HandObject myHand, GetActionObject actions ) {
		return CHECK_FOLD(actions);
	}
	
	public ResponseObject flop( GameObject myGame, HandObject myHand, GetActionObject actions ) {
		return preflop(myGame,myHand,actions);
	}
	
	public ResponseObject discard( GameObject myGame, HandObject myHand, GetActionObject actions ) {
		float[] r0 = StochasticSimulator.computeRates( 
				new int[]{
					myHand.cards3[1],myHand.cards3[2]
				}, actions.boardCards, 3000);
		float[] r1 = StochasticSimulator.computeRates( 
				new int[]{
					myHand.cards3[0],myHand.cards3[2]
				}, actions.boardCards, 3000);
		float[] r2 = StochasticSimulator.computeRates( 
				new int[]{
					myHand.cards3[0],myHand.cards3[1]
				}, actions.boardCards, 3000);
		
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
	
	public ResponseObject turn( GameObject myGame, HandObject myHand, GetActionObject actions ) {
		return preflop(myGame,myHand,actions);
	}
	
	public ResponseObject river( GameObject myGame, HandObject myHand, GetActionObject actions ) {
		return preflop(myGame,myHand,actions);
	}
	
	public ResponseObject CHECK_FOLD( GetActionObject actions ) {
		for ( int i = 0; i < actions.legalActions.length; i++ ) {
			String type = actions.legalActions[i].actionType;
			if ( type.equalsIgnoreCase("CHECK") )
				return new ResponseObject("CHECK","",this);
		}
		return new ResponseObject("FOLD","",this);
	}
}
