package pokerbots.fourstyle;

import pokerbots.brains.GenericBrain;
import pokerbots.utils.StatAggregator.OpponentStats;

// * Against a Calling Station you should only bet if you have
//	 good hands, do not try to bluff a Calling Station as he
//	 will call your bluffs anyway
// * With strong hands, try to make higher than usual pre-flop
//	 raises if the Calling Station is already in the hand or
//	 sitting in the blinds
// * If a loose-passive player bets, you should fold your
//	 medium strength hands

public class CallStation implements Style {
	public String getName() {
		return "Call Station";
	}
	
	public String actionInitiator( float hisPredictedWinChance, GenericBrain b ) {
		int street = b.getStreet();
		float w = b.getWinChance();
		OpponentStats opp = b.getOpponent();
		int bb = b.getGame().bigBlind;
		int ss = b.getGame().stackSize;
		int pot = b.getActionObject().potSize;
		
		//If I have a good hand, get him invested during the pre-flop
		if ( street==0 ) {
			if ( w>0.65 ) {
				int bet = bb;
				float EV = -10000;
				for ( int betSize = bb; betSize < ss; betSize += bb ) {
					float PheCalls = opp.P_Call_given_Bet[street].getEstimate(betSize);
					float PheRaises = opp.P_Raise_given_Bet[street].getEstimate(betSize);
					float PheFolds = opp.P_Fold_given_Bet[street].getEstimate(betSize);
					float ev = PheCalls * (pot + betSize*2) + PheRaises * (pot + betSize*2) + PheFolds * (pot);
					if ( ev>EV ) {
						bet = betSize;
						EV = ev;
					}
				}
				return b.validateAndReturn("bet", bet);
			}
		} else {
			int[] last = b.getHistory().getCurrentRound().getOppLastBetOrRaise();
			int value = last[1];
			float t = opp.value_Bet_given_their_winChance[street].getInverseModel(value);
			
			//If I predict that my win chance is less than his (based on bets), then fold
			if ( w < t && value>0 || w<0.65 ) {
				//If I predict that I'm going to lose against him, then check if re-raising is viable first
				for ( int betSize = value+bb; betSize < value+bb*20; betSize += bb*2 ) {
					float P = opp.P_Fold_given_Bet[street].getEstimate(betSize/(float)ss);
					//I better be darn sure he's going to fold before I bet a call station...
					if ( P>0.9 ) {
						return b.validateAndReturn("bet", betSize);
					}
				}
				if ( w>t-0.1 && Math.random()>0.4 )
					return b.validateAndReturn("call", 0);	
				return b.validateAndReturn("fold", 0);
			} else {
				//I think I have a better hand than him! Let's go to town if confident.
				if ( w>0.85 ) {
					int betSize = bb*3;
					if ( Math.random()>0.5 )
						betSize = bb*7;
					return b.validateAndReturn("bet",betSize);
				} else {
					//No sure what he has, so play it safe here
					if ( w>0.7 )
						return b.validateAndReturn("bet",bb*2);
					else
						return b.validateAndReturn("call", 0);
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
		
		//If I have a good hand, get him invested during the pre-flop
		if ( street==0 ) {
			if ( w>0.75 ) {
				int raise = bb;
				float EV = -10000;
				for ( int raiseSize = bb; raiseSize < ss; raiseSize += bb ) {
					float PheCalls = opp.P_Call_given_Raise[street].getEstimate(raiseSize);
					float PheRaises = opp.P_Raise_given_Raise[street].getEstimate(raiseSize);
					float PheFolds = opp.P_Fold_given_Raise[street].getEstimate(raiseSize);
					float ev = PheCalls * (pot + raiseSize*2) + PheRaises * (pot + raiseSize*2) + PheFolds * (pot);
					if ( ev>EV ) {
						raise = raiseSize;
						EV = ev;
					}
				}
				return b.validateAndReturn("raise", raise);
			} else if ( w>opp.value_Bet_given_their_winChance[street].getInverseModel(value) ) {
				return b.validateAndReturn("call",0);
			} else if ( w>0.4 && value==bb ) {
				return b.validateAndReturn("call",0);
			} else {
				return b.validateAndReturn("fold",0);
			}
		} else {
			
			//If I predict that my win chance is less than his (based on bets), then fold
			if ( w < opp.value_Raise_given_their_winChance[street].getInverseModel(value) && value>0 || w<0.65) {
				//If I predict that I'm going to lose against him, then check if re-raising is viable first
				for ( int reRaise = value+bb; reRaise < value+bb*20; reRaise += bb*2 ) {
					float P = opp.P_Fold_given_Raise[street].getEstimate(reRaise/(float)ss);
					//I better be darn sure he's going to fold before I re-raise a call station...
					if ( P>0.9 ) {
						return b.validateAndReturn("raise", reRaise);
					}
				}
				return b.validateAndReturn("fold", 0);
			} else {
				//I think I have a better hand than him! Let's go to town if confident.
				if ( w>0.85 ) {
					int raiseSize = value+bb*2;
					if ( Math.random()>0.5 )
						raiseSize = value+bb*4;
					return b.validateAndReturn("raise",raiseSize);
				} else {
					//Not sure what he has, so play it safe here
					return b.validateAndReturn("call", 0);
				}
			}
		}
	}
}
