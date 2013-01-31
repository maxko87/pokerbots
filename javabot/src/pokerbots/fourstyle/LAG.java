package pokerbots.fourstyle;

import pokerbots.brains.GenericBrain;
import pokerbots.utils.StatAggregator.OpponentStats;

// * If you’re a good player yourself, you can play more hands
//   than usual against a LAG, as you will usually be a pre-flop
//   favorite against his wide range of starting hands
// * When a LAG bets before the flop, you can raise with your
//   strong hands in order to isolate him
// * If the LAG has position on you, you can often check and
//   wait to raise his bets

public class LAG implements Style {
	public String getName() {
		return "LAG";
	}
	
	public String actionInitiator( float hisPredictedWinChance, GenericBrain b ) {
		int street = b.getStreet();
		float w = b.getWinChance();
		OpponentStats opp = b.getOpponent();
		int bb = b.getGame().bigBlind;
		
		//Play mildly conservative
		if ( street==0 ) {
			if ( w>0.5 ) {
				int bet = bb * (int)(Math.random()*5+1);
				return b.validateAndReturn("bet", bet);
			}
		} else {
			int[] last = b.getHistory().getCurrentRound().getOppLastBetOrRaise();
			int value = last[1];
			float t = opp.value_Bet_given_their_winChance[street].getInverseModel(value);
			
			//If I predict that my win chance is less than his (based on bets), then fold
			if ( w < t && value>0 || w<0.65 ) {
				if ( w>t-0.1 && Math.random()>0.4 )
					return b.validateAndReturn("call", 0);	
				return b.validateAndReturn("fold", 0);
			} else {
				//I think I have a better hand than him! Let's get him invested because he's aggressive!
				if ( w>1.0-street/5.0 ) {
					int betSize = bb*(int)(Math.random()*10+1);
					return b.validateAndReturn("bet",betSize);
				} else {
					//Not sure what he has, so play it safe here
					return b.validateAndReturn("check", 0);
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
		int[] last = b.getHistory().getCurrentRound().getOppLastBetOrRaise();
		int value = last[1];
		
		//If I have a good hand, get him invested during the pre-flop
		if ( street==0 ) {
			if ( w>opp.value_Bet_given_their_winChance[street].getInverseModel(value) ) {
				if ( w > opp.value_Bet_given_their_winChance[street].getInverseModel(value)+0.15 )
					return b.validateAndReturn("raise",value+bb*(int)(Math.random()*20));	
				return b.validateAndReturn("call",0);
			} else if ( w>0.4 && value==bb ) {
				return b.validateAndReturn("call",0);
			} else {
				return b.validateAndReturn("fold",0);
			}
		} else {
			
			//If I predict that my win chance is less than his (based on bets), then fold
			if ( w < opp.value_Raise_given_their_winChance[street].getInverseModel(value) && value>0 ) {
				//If I predict that I'm going to lose against him, then check if re-raising is viable first
				if ( w>0.7 ) {
					for ( int reRaise = value+bb; reRaise < value+bb*20; reRaise += bb*2 ) {
						float P = opp.P_Fold_given_Raise[street].getEstimate(reRaise/(float)ss);
						if ( P>1.0-w/10.0 ) {
							return b.validateAndReturn("raise", reRaise);
						}
					}
				}
				return b.validateAndReturn("fold", 0);
			} else {
				//I think I have a better hand than him! Let's go to town if confident.
				if ( w>0.65 ) {
					int raiseSize = value+bb*(int)(Math.random()*30*w);
					return b.validateAndReturn("raise",raiseSize);
				} else {
					//Not sure what he has, so play it safe here
					return b.validateAndReturn("call", 0);
				}
			}
		}
	}
}
