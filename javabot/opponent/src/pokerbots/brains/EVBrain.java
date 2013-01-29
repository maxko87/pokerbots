package pokerbots.brains;

import pokerbots.packets.GameObject;
import pokerbots.packets.GetActionObject;
import pokerbots.strategy.BasicStrategy;
import pokerbots.strategy.BettingStrategy;
import pokerbots.utils.MatchHistory;
import pokerbots.utils.StatAggregator.OpponentStats;
import pokerbots.utils.Utils;

public class EVBrain extends GenericBrain {
	
	MatchHistory history;
	GameObject game;
	BettingStrategy strategy;
	
	public EVBrain( MatchHistory history, GameObject game ) {
		this.history = history;
		this.game = game;
		this.strategy = new BasicStrategy();
	}
	
	public String takeAction(OpponentStats o, GetActionObject g, float w, int s) {
		this.opponent = o;
		this.getActionObject = g;
		this.street = s;
		
		float myWinChance = w;
		int[] last = history.getCurrentRound().getOppLastBetOrRaise();
		float theirWinChance = 0.8f-o.getLooseness(s)*0.5f;
		if ( last[1]!=-1 ) {
			if ( last[2]==1 ) {
				theirWinChance = opponent.value_Bet_given_their_winChance[last[0]].getInverseModel(last[1]);
			} else if ( last[2]==2 ) {
				theirWinChance = opponent.value_Raise_given_their_winChance[last[0]].getInverseModel(last[1]);
			}
		}
		
		return (String)EV(g.potSize,myWinChance,theirWinChance,street)[0];
	}
	
	public float WF( float w, float t ) {
		return (w-t);
		//return ((w + (1-t))*0.5f) - 0.5f;
	}
	
	public Object[] EV( int potSize, float w, float t, int s ) {
		//Compute EV trees
		System.out.println("EV CALCULATOR - " + s);
		//System.out.println("*****************************");
		
		//PREFLOP ADJUSTMENTS
		if ( street == 0 ) {
			float raise_size = Utils.scale(w, .6f, .9f, 0f, 1f) * Utils.scale(opponent.getLooseness(0), .2f, .8f, 0f, 1f) * (game.stackSize / 10) + 2;
			if (winChance > Utils.inverseScale(opponent.getLooseness(0), 0.0f, 1.0f, .6f, .8f) && raise_size < 30){
				System.out.println("preflop winchance: " + w + ", min win required: " + Utils.inverseScale(opponent.getLooseness(0), 0.0f, 1.0f, .6f, .8f) + ", looseness: " + opponent.getLooseness(0) + ", raise size: " + raise_size);
				return new Object[]{validateAndReturn("raise",(int)(raise_size))};
			}
			return new Object[]{validateAndReturn("call",0)}; //don't continuously reraise preflop
		}
		
		//EVs when on action
		String bestAction = validateAndReturn("check", 0);
		float EV_BEST = -100000;
		for ( int i = 0; i < getActionObject.legalActions.length; i++ ) {
		
			String action = getActionObject.legalActions[i].actionType;
			
			if ( action.equalsIgnoreCase("check") ) {
				float EV_CHECK = EV_myCheck(potSize, s, w, t, 0);
				if ( EV_CHECK > EV_BEST ) {
					EV_BEST = EV_CHECK;
					bestAction = validateAndReturn("check", 0);
				}
			}
			
			if ( action.equalsIgnoreCase("bet") ) {
				int minBet = getActionObject.legalActions[i].minBet;
				int maxBet = getActionObject.legalActions[i].maxBet;
				for ( int myBetSize = minBet; myBetSize <= maxBet; myBetSize+=15 ) {
					float ev = EV_myBet(potSize, myBetSize, s, w, t, 0);
					//System.out.println("Bet EV ("+myBetSize+") = " + ev);
					if ( ev > EV_BEST ) {
						EV_BEST = ev;
						bestAction = validateAndReturn("bet", myBetSize);
					}
				}
			}
			
			if ( action.equalsIgnoreCase("raise") ) {
				int minRaise = getActionObject.legalActions[i].minBet;
				int maxRaise = getActionObject.legalActions[i].maxBet;
				for ( int myRaiseSize = minRaise; myRaiseSize <= maxRaise; myRaiseSize+=15 ) {
					float ev = EV_myRaise(potSize, myRaiseSize, s, w, t,0);
					//System.out.println("Raise EV ("+myRaiseSize+") = " + ev);
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
		}
		
		System.out.println("---> BEST EV: " + EV_BEST);
		System.out.println("---> BEST ACTION: " + bestAction);
		
		return new Object[]{bestAction,EV_BEST};
	}
	
	public float EV_myCheck( int potSize, int s, float w, float t, int depth ) {
		float P_he_checks = Utils.boundFloat(opponent.P_Check_given_Check[s].getEstimate(t),0,1);
		float P_he_bets = Utils.boundFloat(opponent.P_Bet_given_Check[s].getEstimate(t),0,1);
		int hisBet = (int)Utils.boundFloat(opponent.value_Bet_given_their_winChance[s].getEstimate(t),game.bigBlind,game.stackSize);
		
		float norm = P_he_checks+P_he_bets;
		P_he_checks = P_he_checks/norm;
		P_he_bets = P_he_bets/norm;
		
		float EV_future = potSize;
		if ( s<3 )
			EV_future = (Float)(EV_Street(potSize,s+1,w,t));
		
		//Compute EV cumulative
		float EV = 	P_he_checks * ( EV_future ) * WF(w,t) +
					P_he_bets * ( EV_myResponseToHisRaise(potSize,hisBet,s,w,t,depth+1) );
		
		return EV;
	}
	
	public float EV_myCall( int potSize, int theirBet, int s, float w, float t ) {
		float EV_future = potSize + theirBet*2;
		//if ( s<3 )
		//	EV_future = (Float)(EV(potSize+theirBet*2,w,t,s+1)[1]);
		
		float EV = 	(EV_future) * WF(w,t);
		return EV;
	}
	
	public float EV_myBet( int potSize, float betSize, int s, float w, float t, int depth ) {
		float P_he_folds = Utils.boundFloat(opponent.P_Fold_given_Bet[s].getEstimate(betSize/game.stackSize,t),0,1);
		float P_he_calls = Utils.boundFloat(opponent.P_Call_given_Bet[s].getEstimate(betSize/game.stackSize,t),0,1);
		float P_he_raises = Utils.boundFloat(opponent.P_Raise_given_Bet[s].getEstimate(betSize/game.stackSize,t),0,1);
		int hisRaise = (int)Utils.boundFloat(opponent.value_Raise_given_their_winChance[s].getEstimate(t),betSize+game.bigBlind,game.stackSize);
		
		float norm = P_he_folds+P_he_calls+P_he_raises;
		P_he_calls = P_he_calls/norm;
		P_he_raises = P_he_raises/norm;
		
		float EV_future = potSize + betSize*2;
		//if ( s<3 )
		//	EV_future = (Float)(EV(potSize + (int)(betSize*2),w,t,s+1)[1]);
		
		//Compute EV cumulative
		float EV = 	P_he_calls * (EV_future) * WF(w,t) +
					P_he_raises * ( EV_myResponseToHisRaise(potSize,hisRaise,s,w,t,depth+1) );
		
		return EV;
	}
	
	public float EV_myRaise( int potSize, float raiseSize, int s, float w, float t, int depth ) {
		float P_he_folds = Utils.boundFloat(opponent.P_Fold_given_Raise[s].getEstimate(raiseSize/game.stackSize,t),0,1);
		float P_he_calls = Utils.boundFloat(opponent.P_Call_given_Raise[s].getEstimate(raiseSize/game.stackSize,t),0,1);
		float P_he_raises = Utils.boundFloat(opponent.P_Raise_given_Raise[s].getEstimate(raiseSize/game.stackSize,t),0,1);
		int hisRaise = (int)Utils.boundFloat(opponent.value_Raise_given_their_winChance[s].getEstimate(t),raiseSize+game.bigBlind,game.stackSize);
		
		float norm = P_he_folds+P_he_calls+P_he_raises;
		P_he_calls = P_he_calls/norm;
		P_he_raises = P_he_raises/norm;
		
		float EV_future = potSize + raiseSize*2;
		//if ( s<3 )
		//	EV_future = (Float)(EV(potSize + (int)(raiseSize*2),w,t,s+1)[1]);
		
		//Compute EV cumulative
		float EV = 	P_he_calls * ( EV_future ) * WF(w,t) +
					P_he_raises * ( EV_myResponseToHisRaise(potSize,hisRaise,s,w,t,depth+1)  );
		
		return EV;
	}
	
	public float EV_myResponseToHisRaise( int potSize, int hisRaise, int s, float w, float t, int depth ) {
		
		if ( depth>3 )
			return 0;
		
		//if fold
		float EV = 0;
		
		//if call
		float EVcall = ( potSize + hisRaise*2 ) * WF(w,t);
		if ( EVcall > EV )
			EV = EVcall;
		
		//if re-raise
		for ( int r = hisRaise+game.bigBlind; r < game.stackSize; r+=15 ) {
			float EVraise = EV_myRaise(potSize,r,s,w,t,depth+1);
			if ( EVraise > EV )
				EV = EVraise;
		}
		
		return EV;
	}
	
	public float EV_Street( int potSize, int s, float w, float t ) {
		if ( s>=3 )
			return 0;
		
		//if fold
		float EV = 0;
		
		//if check
		EV = EV_myCheck(potSize, s, w, t, 0);
		
		//if bet
		for ( int r = game.bigBlind; r < game.stackSize; r+=15 ) {
			float EVbet = EV_myBet(potSize,r,s,w,t,0);
			if ( EVbet > EV )
				EV = EVbet;
		}
		return EV;
	}
	
	public String toString(){
		return "ExpValBrain";
	}
}
