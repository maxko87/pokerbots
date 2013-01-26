package brains;

import pokerbots.packets.GameObject;
import pokerbots.packets.GetActionObject;
import pokerbots.utils.MatchHistory;
import pokerbots.utils.StatAggregator.OpponentStats;
import pokerbots.utils.Utils;
import pokerots.strategy.BasicStrategy;
import pokerots.strategy.BettingStrategy;

public class EV_template_Brain extends GenericBrain {
	
	MatchHistory history;
	GameObject game;
	BettingStrategy strategy;
	
	public EV_template_Brain( MatchHistory history, GameObject game ) {
		this.history = history;
		this.game = game;
		this.strategy = new BasicStrategy();
	}
	
	public String takeAction(OpponentStats o, GetActionObject g, float w, int s) {
		this.opponent = o;
		this.getActionObject = g;
		this.street = s;
		
		float myWinChance = w;
		float theirWinChance = 0.8f-o.getLooseness(s)*0.5f;
		
		return EV(g.potSize,myWinChance,theirWinChance,street);
	}
	
	public float WF( float w, float t ) {
		//return (w-t);
		return w-t;//((w + (1-t))*0.5f) - 0.5f;
	}
	
	public String EV( int potSize, float w, float t, int s ) {
		//Compute EV trees
		System.out.println("EV CALCULATOR");
		System.out.println("*****************************");
		
		if ( s==0 )
			return validateAndReturn("call", 0);
		
		//EVs when on action
		String bestAction = validateAndReturn("check", 0);
		float EV_BEST = -100000;
		for ( int i = 0; i < getActionObject.legalActions.length; i++ ) {
		
			String action = getActionObject.legalActions[i].actionType;
			
			if ( action.equalsIgnoreCase("check") ) {
				float EV_CHECK = EV_myCheck(potSize, s, w, t);
				if ( EV_CHECK > EV_BEST ) {
					EV_BEST = EV_CHECK;
					bestAction = validateAndReturn("check", 0);
				}
			}
			
			if ( action.equalsIgnoreCase("bet") ) {
				int minBet = getActionObject.legalActions[i].minBet;
				int maxBet = getActionObject.legalActions[i].maxBet;
				for ( int myBetSize = minBet; myBetSize <= maxBet; myBetSize+=5 ) {
					float ev = EV_myBet(potSize, myBetSize, s, w, t);
					if ( ev > EV_BEST ) {
						EV_BEST = ev;
						bestAction = validateAndReturn("bet", myBetSize);
					}
				}
			}
			
			if ( action.equalsIgnoreCase("raise") ) {
				int minRaise = getActionObject.legalActions[i].minBet;
				int maxRaise = getActionObject.legalActions[i].maxBet;
				for ( int myRaiseSize = minRaise; myRaiseSize <= maxRaise; myRaiseSize+=5 ) {
					float ev = EV_myRaise(potSize, myRaiseSize, s, w, t);
					if ( ev > EV_BEST ) {
						EV_BEST = ev;
						bestAction = validateAndReturn("raise", myRaiseSize);
					}
				}
			}
			
			if ( action.equalsIgnoreCase("call") ) {
				int theirLastWager= history.getCurrentRound().getOppLastBetOrRaise()[1];
				float EV_CALL = EV_myCall(potSize, theirLastWager, s, w, t); //can also dual as check with theirBet = 0
				if ( EV_CALL > EV_BEST ) {
					EV_BEST = EV_CALL;
					bestAction = validateAndReturn("call", 0);
				}
			}
			
			if ( action.equalsIgnoreCase("fold") ) {
				if ( 0 > EV_BEST ) {
					bestAction = validateAndReturn("fold", 0);
					EV_BEST = 0;
				}
			}
			
			System.out.println("BEST EV: " + EV_BEST);
			System.out.println("BEST ACTION: " + bestAction);
		}
		
		return bestAction;
	}
	
	public float EV_myCheck( int potSize, int s, float w, float t ) {
		float P_he_checks = Utils.boundFloat(opponent.P_Check_given_Check[s].getEstimate(t),0,1);
		float P_he_bets = Utils.boundFloat(opponent.P_Bet_given_Check[s].getEstimate(t),0,1);
				
		//Compute EV cumulative
		float EV = 	P_he_checks * ( potSize ) * WF(w,t) +
					P_he_bets * ( EV_myResponseToHisBet(potSize,s,w,t) );
		
		return EV;
	}
	
	public float EV_myCall( int potSize, int theirBet, int s, float w, float t ) {
		float EV = 	(potSize + theirBet*2) * WF(w,t);
		return EV;
	}
	
	public float EV_myBet( int potSize, float betSize, int s, float w, float t ) {
		float P_he_folds = Utils.boundFloat(opponent.P_Fold_given_Bet[s].getEstimate(betSize),0,1);
		float P_he_calls = Utils.boundFloat(opponent.P_Call_given_Bet[s].getEstimate(betSize),0,1);
		float P_he_raises = Utils.boundFloat(opponent.P_Raise_given_Bet[s].getEstimate(betSize),0,1);

		// TUNE P_call and P_raise based on P_fold
		// P_fold + P_call + P_raise = 1
		P_he_calls = (P_he_calls/2 + (1-P_he_folds)/4);
		P_he_raises = (P_he_raises/2 + (1-P_he_folds)/4);
				
		//Compute EV cumulative
		float EV = 	P_he_calls * (potSize + betSize*2) * WF(w,t) +
					P_he_raises * ( EV_myResponseToHisRaise(potSize,s,w,t) );
		
		return EV;
	}
	
	public float EV_myRaise( int potSize, float raiseSize, int s, float w, float t ) {
		float P_he_folds = Utils.boundFloat(opponent.P_Fold_given_Raise[s].getEstimate(raiseSize),0,1);
		float P_he_calls = Utils.boundFloat(opponent.P_Call_given_Raise[s].getEstimate(raiseSize),0,1);
		float P_he_raises = Utils.boundFloat(opponent.P_Raise_given_Raise[s].getEstimate(raiseSize),0,1);

		// TUNE P_call and P_raise based on P_fold
		// P_fold + P_call + P_raise = 1
		P_he_calls = (P_he_calls/2 + (1-P_he_folds)/4);
		P_he_raises = (P_he_raises/2 + (1-P_he_folds)/4);
				
		//Compute EV cumulative
		float EV = 	P_he_calls * ( potSize + raiseSize*2) * WF(w,t) +
					P_he_raises * ( EV_myResponseToHisRaise(potSize,s,w,t) );
		
		return EV;
	}
	
	public float EV_myResponseToHisRaise( int potSize, int s, float w, float t ) {
		float P_fold = Utils.boundFloat(strategy.probFold(w, t, s),0,1);
		float P_call = Utils.boundFloat(strategy.probCall(w, t, s),0,1);
		float P_raise = Utils.boundFloat(strategy.probRaise(w, t, s),0,1);
		float VALUE_raise = Utils.boundFloat(strategy.valueRaise(w, t, s),0,1)*game.stackSize;
		
		P_call = (P_call/2 + (1-P_fold)/4);
		P_raise = (P_raise/2 + (1-P_fold)/4);
		
		float hisPredictedRaise = opponent.value_Raise_given_their_winChance[s].getEstimate(t);
		
		float EV = 	P_call*(potSize + hisPredictedRaise*2)*WF(w,t) +
					P_raise*(potSize + VALUE_raise*2)*WF(w,t);
		
		return EV;
	}
	
	public float EV_myResponseToHisBet( int potSize, int s, float w, float t ) {
		float P_fold = Utils.boundFloat(strategy.probFold(w, t, s),0,1);
		float P_call = Utils.boundFloat(strategy.probCall(w, t, s),0,1);
		float P_raise = Utils.boundFloat(strategy.probRaise(w, t, s),0,1);
		float VALUE_raise = Utils.boundFloat(strategy.valueRaise(w, t, s),0,1)*game.stackSize;
		
		P_call = (P_call/2 + (1-P_fold)/4);
		P_raise = (P_raise/2 + (1-P_fold)/4);
		
		float hisPredictedBet = opponent.value_Bet_given_their_winChance[s].getEstimate(t);
		
		float EV = 	P_call*(potSize + hisPredictedBet*2)*WF(w,t) +
					P_raise*(potSize + VALUE_raise*2)*WF(w,t);
		
		return EV;
	}
}
