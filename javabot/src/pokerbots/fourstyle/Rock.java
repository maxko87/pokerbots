package pokerbots.fourstyle;

import pokerbots.brains.GenericBrain;
import pokerbots.utils.StatAggregator.OpponentStats;

// * If he is sitting in the blinds, try to steal the blinds
//   with a 3BB raise from late position
// * If a rock bets or even raises preflop you should only
//   play the very best starting hands and fold the rest of the hole cards
// * If you have a drawing hand on the flop, checking
//   is usually the best strategy. As a Rock rarely bets, you will often
//	 get to see the next board card for free

public class Rock implements Style {
	public String getName() {
		return "Rock";
	}
	
	public String actionInitiator( float hisPredictedWinChance, GenericBrain b ) {
		int street = b.getStreet();
		float w = b.getWinChance();
		OpponentStats opp = b.getOpponent();
		int bb = b.getGame().bigBlind;
		int ss = b.getGame().stackSize;
		int pot = b.getActionObject().potSize;
		
		//Attempt to steam big blinds if I have an above garbage hand
		if ( street==0 ) {
			if ( w>0.4 ) {
				for ( int betSize = bb; betSize < 8*bb; betSize += bb ) {
					float PheFolds = opp.P_Fold_given_Bet[street].getEstimate(betSize);
					if ( PheFolds > 0.8 ) {
						return b.validateAndReturn("bet", betSize);
					}
				}
				return b.validateAndReturn("check", 0);
			}
		} else {
			int[] last = b.getHistory().getCurrentRound().getOppLastBetOrRaise();
			int value = last[1];
			float t = opp.value_Bet_given_their_winChance[street].getInverseModel(value);
			
			//If he bets, I should probably just fold.
			//Call him on my solid hands.
			if ( value>0 ) {
				if ( w > t+0.15 )
					return b.validateAndReturn("call", 0);
				else if ( w > 0.5 && value <= 2*bb )
					return b.validateAndReturn("call", 0);
				return b.validateAndReturn("fold", 0);
			}
			
			//If I'm not confident yet, throw in a small bet.
			if ( w < 0.65 ) {
				return b.validateAndReturn("bet", (int)(4*bb*Math.random()));
			} else {
				//I have a decent hand...
				if ( w>0.45+street/10.0 ) {
					int betSize = bb*3;
					if ( Math.random()>0.5 )
						betSize = bb*7;
					return b.validateAndReturn("bet",betSize);
				} else {
					//Not sure what he has, so play it safe here
					if ( Math.random()>0.4 )
						return b.validateAndReturn("bet",4*bb);
					return b.validateAndReturn("check",0);
				}
			}
		}
		
		return b.validateAndReturn("check", 0);
	}
	
	public String actionReceiver( float hisPredictedWinChance, GenericBrain b ) {
		int street = b.getStreet();
		float w = b.getWinChance();
		OpponentStats opp = b.getOpponent();
		int bb = b.getGame().bigBlind;
		int ss = b.getGame().stackSize;
		int pot = b.getActionObject().potSize;
		int[] last = b.getHistory().getCurrentRound().getOppLastBetOrRaise();
		int value = last[1];
		
		//I he has raised me here, I need to get out
		if ( street==0 ) {
			if ( value<=2*bb && w>0.5 )
				return b.validateAndReturn("call", 0);
			else if ( w>0.8 )
				return b.validateAndReturn("call", 0);
		} else {
			float t = opp.value_Bet_given_their_winChance[street].getInverseModel(value);
			
			//If he raises, I should probably just fold.
			//Call him on my solid hands.
			if ( value>0 ) {
				if ( w > t+0.15 ) {
					if ( Math.random()>0.3 )
						return b.validateAndReturn("call", 0);
					int raise = value+bb;
					float EV = 0;
					for ( int raiseSize = raise; raiseSize < ss; raiseSize += bb ) {
						float PheCalls = opp.P_Call_given_Bet[street].getEstimate(raiseSize);
						float PheRaises = opp.P_Raise_given_Bet[street].getEstimate(raiseSize);
						float PheFolds = opp.P_Fold_given_Bet[street].getEstimate(raiseSize);
						float ev = PheCalls * (pot + raiseSize*2) + PheRaises * (pot + raiseSize*2) + PheFolds * (pot);
						if ( ev>EV ) {
							raise = raiseSize;
							EV = ev;
						}
					}
					return b.validateAndReturn("raise", raise);					
				}
				else if ( w > 0.5 && value <= 2*bb )
					return b.validateAndReturn("call", 0);
				return b.validateAndReturn("fold", 0);
			}
		}
		
		return b.validateAndReturn("check", 0);
	}
}
