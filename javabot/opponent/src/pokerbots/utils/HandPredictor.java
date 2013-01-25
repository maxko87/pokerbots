package pokerbots.utils;

import pokerbots.packets.GameObject;
import pokerbots.packets.GetActionObject;
import pokerbots.packets.PerformedActionObject;
import pokerbots.utils.MatchHistory.Round;

public class HandPredictor {
	float[] myHandChances;	//[HQ,P,2P,T,ST,FL,FH,F,SF]
	float[] myHandWinRates; //[5%,30%,50%,70%,...,100%]
	
	float[] hisHandChances; //factual information
	float[] hisHandWinRates;//more facts
	
	float[] hisLastChances; 	//his hand chances before this next hand
	float[] adjustmentPattern;	//how to adjust expected win rates
	
	float _myFactualChance = 0;
	float _hisPredictedChance = 0;
	
	public static void main( String[] args ) {
		HandPredictor hp = new HandPredictor();
		float[] rates = StochasticSimulator.computeRates(
				new int[]{
						HandEvaluator.stringToCard("Ac"),
						HandEvaluator.stringToCard("Qd")
				},
				new int[]{
				},
				30000);
		hp.populate(rates);
		
		int street = 0;
		GameObject game = new GameObject("NEWGAME yourName oppName 400 2 1000 20.000");
		GetActionObject msg = new GetActionObject("GETACTION 400 0 1 BET:100:"+game.oppName+" 0 20.000");
		MatchHistory history = new MatchHistory();
		history.newRound(game.stackSize,game.oppName);
		history.appendRoundData( new PerformedActionObject[]{new PerformedActionObject("BET:100:"+game.oppName)} );
		hp.updatePredictions( history, msg, game );
		
		
		rates = StochasticSimulator.computeRates(
				new int[]{
						HandEvaluator.stringToCard("Ac"),
						HandEvaluator.stringToCard("Qd")
				},
				new int[]{
						HandEvaluator.stringToCard("As"),
						HandEvaluator.stringToCard("Qc"),
						HandEvaluator.stringToCard("3c")
				},
				30000);
		hp.populate(rates);
		
		msg = new GetActionObject("GETACTION 50 0 1 BET:30:"+game.oppName+" 0 20.000");
		history.newRound(game.stackSize,game.oppName);
		history.appendRoundData( new PerformedActionObject[]{new PerformedActionObject("BET:1:"+game.oppName)} );
		hp.updatePredictions( history, msg, game );
	}
	
	public HandPredictor() {
		hisLastChances = new float[10];
		adjustmentPattern = new float[10];
	}
	
	public void populate( float[] a ) {
		hisLastChances = new float[10];
		myHandChances = new float[10];
		myHandWinRates = new float[10];
		hisHandChances = new float[10];
		hisHandWinRates = new float[10];
		for ( int i = 0; i < 10; i++ )
			myHandChances[i] = a[i];
		for ( int i = 0; i < 9; i++ )
			myHandWinRates[i] = a[i+21];
		for ( int i = 0; i < 9; i++ )
			hisHandChances[i] = a[i+11];
		for ( int i = 0; i < 9; i++ )
			hisHandWinRates[i] = a[i+31];
		
		float myFactualChances = 0;
		for ( int i = 0; i < myHandChances.length; i++ ) {
			myFactualChances += myHandChances[i]*myHandWinRates[i];
		}
		float hisPredictedChances = 0;
		for ( int i = 0; i < hisHandChances.length; i++ ) {
			hisPredictedChances += hisHandChances[i]*hisHandWinRates[i];
		}
		_myFactualChance = myFactualChances;
		_hisPredictedChance = hisPredictedChances;
	}
	
	public void setMyHandChances( float[] a ) { this.myHandChances = a; }
	public void setMyHandWinRates( float[] a ) { this.myHandWinRates = a; }
	public void setHisHandChances( float[] a ) { this.hisHandChances = a; }
	public void setHisHandWinRates( float[] a ) { this.hisHandWinRates = a; }
	
	public void updatePredictions( MatchHistory history, GetActionObject msg, GameObject game ) {
		int HIS_ACTION = 3;
		float amount = 0;
		Round r = history.getCurrentRound();
		for ( int i = r.actions.size()-1; i >= 0; i-- ) {
			PerformedActionObject action = r.actions.get(i);
			if ( action.actor.equalsIgnoreCase(game.oppName) ) {
				if ( action.actionType.equalsIgnoreCase("bet") ) {
					HIS_ACTION = 1;
					amount = action.amount/(float)game.stackSize;
					break;
				}
				else if ( action.actionType.equalsIgnoreCase("raise") ) {
					HIS_ACTION = 2;
					amount = action.amount/(float)game.stackSize;
					break;
				}
				else if ( action.actionType.equalsIgnoreCase("check") ) {
					HIS_ACTION = 3;
					amount = action.amount/(float)game.stackSize;
					break;
				}
				else if ( action.actionType.equalsIgnoreCase("call") ) {
					HIS_ACTION = 4;
					amount = msg.potSize/(float)game.stackSize;
					break;
				}
			}
		}
		
		// Iterate through all hand types and their chance of occurring
		// If his bets: He has a reasonable-good hand OR a bluff.
		// If he raises: He has an average-reasonable hand OR a bluff.
		// If he checks: He is hiding a strong hand OR has a weak hand.
		// If he calls: He has an average hand OR nothing.
		
		float alpha = 0.5f/0.3f;
		float beta = 0.10f/0.3f;
		float gamma = 0.005f/0.3f;
		float delta = 0.4f/0.3f;
		//if ( street==0 )
		//	return;
		
		switch ( HIS_ACTION ) {
			case 1:	//BET
				for ( int i = 0; i < hisLastChances.length; i++ ) {
					float diff = hisHandChances[i]-hisLastChances[i];
					adjustmentPattern[i] += diff*alpha*amount;
				}
				break;
			case 2: //RAISE
				for ( int i = 0; i < hisLastChances.length; i++ ) {
					float diff = hisHandChances[i]-hisLastChances[i];
					adjustmentPattern[i] += diff*alpha*amount+gamma;
				}
				break;
			case 3: //CHECK
				for ( int i = 0; i < hisLastChances.length; i++ ) {
					float diff = hisHandChances[i]-hisLastChances[i];
					adjustmentPattern[i] -= diff*beta+gamma;
				}
				break;
			case 4: //CALL
				for ( int i = 0; i < hisLastChances.length; i++ ) {
					float diff = hisHandChances[i]-hisLastChances[i];
					adjustmentPattern[i] += diff*amount*delta+gamma;
				}
				break;
		}
		
		
		//Adjusted hand chances
		float[] adjusted = new float[9];
		for ( int i = 0; i < adjusted.length; i++ ) {
			adjusted[i] = adjustmentPattern[i]+hisHandChances[i];
		}
		
		//His new predicted chance of winning
		float myFactualChances = 0;
		for ( int i = 0; i < myHandChances.length; i++ ) {
			myFactualChances += myHandChances[i]*myHandWinRates[i];
		}
		float hisPredictedChances = 0;
		for ( int i = 0; i < adjusted.length; i++ ) {
			hisPredictedChances += adjusted[i]*hisHandWinRates[i];
		}
		
		//System.out.println("True chances:      " + (1.0f-myFactualChances) );
		//System.out.println("Predicted chances: " + hisPredictedChances );
		
		_hisPredictedChance = hisPredictedChances;
		_myFactualChance    = myFactualChances;
		
		System.out.println("True chances:      " + (1.0f-_myFactualChance) );
		System.out.println("Predicted chances: " + _hisPredictedChance );
	}
	
	public float adjust( float value, float motion ) {
		value = value*0.8f + 0.5f*(motion*motion);
		if ( value>1 ) value = 1;
		if ( value<0 ) value = 0;
		return value;
	}
}
