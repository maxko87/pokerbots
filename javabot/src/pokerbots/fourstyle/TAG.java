package pokerbots.fourstyle;

import pokerbots.brains.GenericBrain;
import pokerbots.utils.StatAggregator.OpponentStats;

// * As he plays tight, try stealing his blinds if you
//   are sitting in late position
// * If a TAG bets or raises pre-flop, you should fold
//   all but the very best starting hands. Play these 
//   hands aggressively and always re-raise with AA, KK,
//   QQ (and sometimes JJ and AKs)
// * If you play out of position against a TAG and hold
//   a strong hand, then check to him on the flop: TAGs
//   usually make continuation bets, regardless whether
//   they hit the flop or not

public class TAG implements Style{
	public String getName() {
		return "TAG";
	}
	
	public String actionInitiator( float hisPredictedWinChance, GenericBrain b ) {
		int street = b.getStreet();
		float w = b.getWinChance();
		OpponentStats opp = b.getOpponent();
		int bb = b.getGame().bigBlind;
		int ss = b.getGame().stackSize;
		int pot = b.getActionObject().potSize;
		
		//Attempt to steal big blinds if I have an above garbage hand
		if ( street==0 ) {
			if ( w>0.4 ) {
				int bet = bb;
				float EV = 0;
				for ( int betSize = bb; betSize < 5*bb; betSize += bb ) {
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
			
			//If he has already placed a bet once, I should probably peace out
			if ( value>0 ) {
				if ( w > t+0.15 ) {
					float EV = 0;
					int raise = 0;
					for ( int raiseSize = value+bb; raiseSize < ss; raiseSize += bb ) {
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
				else if ( w > (1.0f+opp.getAggression(street))/2.0f )
					return b.validateAndReturn("call", 0);
				return b.validateAndReturn("fold", 0);
			}
			
			//If I'm not confident yet, throw in a bet.
			if ( w < 0.65 ) {
				return b.validateAndReturn("bet", (int)(8*bb*Math.random()));
			} else {
				//I have a decent hand...
				if ( w>0.45+street/10.0 ) {
					int betSize = bb*(int)(Math.random()*20+1);
					return b.validateAndReturn("bet",betSize);
				} else {
					//Not sure what he has, so play it safe here
					if ( Math.random()>0.4 )
						return b.validateAndReturn("bet",bb*(int)(Math.random()*20+1));
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
		float t = opp.value_Bet_given_their_winChance[street].getInverseModel(value);
		
		//If he has raised me here, he is either bluffing or loaded
		if ( w>0.85 ) {
			int raiseSize = value*2 + bb*(int)(Math.random()*30);
			return b.validateAndReturn("raise", raiseSize); 
		} else if ( w > 0.65 ) {
			float EV = 0;
			int raise = 0;
			for ( int raiseSize = value+bb; raiseSize < ss; raiseSize += bb ) {
				float PheCalls = opp.P_Call_given_Bet[street].getEstimate(raiseSize);
				float PheRaises = opp.P_Raise_given_Bet[street].getEstimate(raiseSize);
				float PheFolds = opp.P_Fold_given_Bet[street].getEstimate(raiseSize);
				float ev = PheCalls * (pot + raiseSize*2)*(w-t) + PheRaises * (pot + raiseSize*2)*(w-t) + PheFolds * (pot);
				if ( ev>EV ) {
					raise = raiseSize;
					EV = ev;
				}
			}
			if ( EV==0 )
				return b.validateAndReturn("fold",0); 
			if ( pot*(w-t) > EV )
				return b.validateAndReturn("call",0);
			return b.validateAndReturn("raise",raise);
		} else {
			if ( value<3*bb && w>0.4 )
				return b.validateAndReturn("call", 0);
			return b.validateAndReturn("fold",0);
		}
	}
}
