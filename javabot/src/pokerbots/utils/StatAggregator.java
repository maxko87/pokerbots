package pokerbots.utils;

import java.util.HashMap;

import pokerbots.brains.GenericBrain;
import pokerbots.packets.GameObject;
import pokerbots.packets.HandObject;
import pokerbots.packets.PerformedActionObject;
import pokerbots.regression.CrossSectionModel;
import pokerbots.regression.LinearModel;
import pokerbots.regression.Model1D;
import pokerbots.regression.Model2D;
import pokerbots.regression.Model3D;
import pokerbots.regression.PlanarModel;
import pokerbots.regression.StddevBin2DModel;
import pokerbots.regression.StddevModel;
import pokerbots.utils.MatchHistory.Round;

public class StatAggregator {
	
	/*
	 * This class collects data per hand and aggregates it across matches.
	 * 
	 * Per street:	(% fold, call, raise) to a bet
	 * 				(% fold, call, raise) to a raise
	 * 				(% check, bet) on action
	 * 				In addition to frequencies, we also have values of the bets/raises.
	 * 
	 * Every time there is a showdown:
	 * 		we bucket probabilities of hand success at each street, and calculate 
	 * 			- average bets per street
	 * 			- bet to call ratio (later)
	 * 		
	 * ALL VALUES SHOULD BE NORMALIZED TO POT SIZE
	 * 
	 * Map from opponent names to MatchStats objects
	 * 
	 * Implement serializer and deserializer into Key Value pairs
	 */
	
	private HashMap<String, OpponentStats> m;
	
	//Generic constructor
	public StatAggregator(){
		m = new HashMap<String, OpponentStats>();
	}
	
	public OpponentStats getOrCreateOpponent(GameObject game) {
		String oppName = game.oppName;
		if (m.get(oppName) == null){
			m.put(oppName, new OpponentStats(oppName, game));
		}
		return m.get(oppName);
	}
	
	//Deserialize TODO
	public StatAggregator(String keyValuePairs){
		
	}
	
	//Serialize TODO
	public String Serializer(){
		return "";
	}
	
	
	public class OpponentStats{
		
		//store data about how different brains perform against different opponents
		public HashMap<String, Integer> brainScores;
		public HashMap<String, Integer> brainHands;
		
		public float getAvgBrainScore(String brainName){
			return (float)(brainScores.get(brainName)) / brainHands.get(brainName);
		}
		
		String name;
		int startingStackSize;
		public int totalHandCount;
		
		public Model1D[] N_Check_given_Check;  	// How often they check?
		public Model1D[] N_Bet_given_Check;		// How often they bet?
		
		public Model2D[] P_Fold_given_Bet;		// Chance of folding given my bet size
		public Model2D[] P_Call_given_Bet;		// Chance of calling given my bet size
		public Model2D[] P_Raise_given_Bet;		// Chance of raising given my bet size
		
		public Model2D[] P_Fold_given_Raise;	// Chance of folding given my raise size
		public Model2D[] P_Call_given_Raise;	// Chance of calling given my raise size
		public Model2D[] P_Raise_given_Raise;	// Chance of raising given my raise size
		
		public Model2D[] value_Raise_given_their_winChance;		//How much do they raise given their win chance
		public Model2D[] value_Bet_given_their_winChance;		//How much do they bet given their win chance
		
		/*
		float[] Looseness;
		float[] Aggression;
		public final float DEFAULT_LOOSENESS = 0.3f;
		public final float DEFAULT_AGGRESSION = 0.25f;
		*/
		
		GameObject game;
		
		//TODO: make sure this constructor doesn't get run when pulling from KeyValues!!
		public OpponentStats( String name, GameObject game ){
			
			this.startingStackSize = game.stackSize;
			this.name = name;
			this.game = game;
			this.totalHandCount = 0;
			this.brainScores = new HashMap<String, Integer>(); //total score of each brain
			this.brainHands = new HashMap<String, Integer>(); //total hands played by each brain
			
			N_Check_given_Check = new Model1D[4];
			N_Bet_given_Check = new Model1D[4];
			P_Fold_given_Bet = new Model2D[4];
			P_Call_given_Bet = new Model2D[4];
			P_Raise_given_Bet = new Model2D[4];
			P_Fold_given_Raise = new Model2D[4];
			P_Call_given_Raise = new Model2D[4];
			P_Raise_given_Raise = new Model2D[4];
			value_Raise_given_their_winChance = new Model2D[4];
			value_Bet_given_their_winChance = new Model2D[4];
			
			//initialize all regression lines
			int BINS = 8;
			for ( int i = 0; i < 4; i++ ) {
				N_Check_given_Check[i] = new StddevModel("N(Check|Check)");
				N_Bet_given_Check[i] = new StddevModel("N(Bet|Check)");
				
				P_Fold_given_Bet[i] = new StddevBin2DModel("P(Fold|Bet)","bet","P",BINS);
				P_Call_given_Bet[i]  = new StddevBin2DModel("P(Call|Bet)","bet","P",BINS);
				P_Raise_given_Bet[i] = new StddevBin2DModel("P(Raise|Bet)","bet","P",BINS);
				
				P_Fold_given_Raise[i] = new StddevBin2DModel("P(Fold|Raise)","wager","P",BINS);
				P_Call_given_Raise[i] = new StddevBin2DModel("P(Call|Raise)","wager","P",BINS);
				P_Raise_given_Raise[i] = new StddevBin2DModel("P(Raise|Raise)","wager","P",BINS);
			
				value_Raise_given_their_winChance[i] = new StddevBin2DModel("$ Raise per % win","%win","$",BINS);
				value_Bet_given_their_winChance[i] = new StddevBin2DModel("$ Bet per % win","%win","$",BINS);
			}
		}
		
		//helper for storing data into values
		public void updateBrain(String brainName, int amount){
			if (brainScores.containsKey(brainName)){
				brainScores.put(brainName, brainScores.get(brainName) + amount);
				brainHands.put(brainName, brainHands.get(brainName) + 1); 
			}
			else{
				brainScores.put(brainName, amount);
				brainHands.put(brainName, 1);
			}
		}

		public void analyzeRoundData( HandObject hand, Round data ){
			
			int street = 0;
			float myWagerSize = 0; 	//Size of MY wager
			float hisEstimatedWinChance = 0;
			
			// Set the opponents win chance on fact or prediction (FOR PREFLOP)
			if ( data.showdown ) {
				hisEstimatedWinChance = data.oppWinRates[street];
			} else {
				hisEstimatedWinChance = -1;
				//hisEstimatedWinChance = 0.5f-HandEvaluator.handOdds(hand, data.boardCards[street],400)[10];
			}
			
			for ( int i = 0; i < data.actions.size() - 1; i++ ) {
				
				PerformedActionObject prev = data.actions.get(i);
				PerformedActionObject curr = data.actions.get(i+1);

				String prevA = prev.actionType;
				String currA = curr.actionType;
				
				//Increment street
				if ( currA.equalsIgnoreCase("deal") ) {
					street++;
					//System.out.println("######### NEXT STREET ##########");
					prev = curr;
					
					// Set the opponents win chance on fact or prediction (FOR ALL STREETS)
					if ( data.showdown ) {
						hisEstimatedWinChance = data.oppWinRates[street];
					} else {
						hisEstimatedWinChance = -1;
						//hisEstimatedWinChance = 1.0f-HandEvaluator.handOdds(hand, data.boardCards[street],400)[10];
					}
					continue;
				}
				
				//Get my most recent wager
				if ( prev.actor.equalsIgnoreCase(game.myName) ) {
					if ( prev.actionType.equals("BET") || prev.actionType.equals("RAISE") ) {
						myWagerSize = prev.amount/(float)game.stackSize;
					}
				}
				
				//Check if I got refunded AKA he folded
				if ( curr.actor.equals(game.myName) && curr.actionType.equalsIgnoreCase("refund") ) {
					P_Fold_given_Bet[street].addData(myWagerSize, 1);
					P_Fold_given_Raise[street].addData(myWagerSize, 1);
				}
				
				//IF I perform an action THEN OPP performs action
				if ( prev.actor.equalsIgnoreCase(game.myName) && curr.actor.equals(game.oppName) ) {
					if ( prevA.equalsIgnoreCase("bet") || prevA.equalsIgnoreCase("post")) {
						if ( currA.equalsIgnoreCase("fold") ) {
							P_Fold_given_Bet[street].addData(myWagerSize, 1);
							P_Raise_given_Bet[street].addData(myWagerSize, 0);
							P_Call_given_Bet[street].addData(myWagerSize, 0);
						}
						else if ( currA.equalsIgnoreCase("raise") ) {
							P_Fold_given_Bet[street].addData(myWagerSize, 0);
							P_Raise_given_Bet[street].addData(myWagerSize, 1);
							P_Call_given_Bet[street].addData(myWagerSize, 0);
							if ( hisEstimatedWinChance >=0 )
								value_Raise_given_their_winChance[street].addData(hisEstimatedWinChance, curr.amount);
						}
						else if ( currA.equalsIgnoreCase("call") ) {
							P_Fold_given_Bet[street].addData(myWagerSize, 0);
							P_Raise_given_Bet[street].addData(myWagerSize, 0);
							P_Call_given_Bet[street].addData(myWagerSize, 1);
						}
					}
					else if ( prevA.equalsIgnoreCase("raise") ) {
						if ( currA.equalsIgnoreCase("fold") ) {
							P_Fold_given_Raise[street].addData(myWagerSize, 1);
							P_Raise_given_Raise[street].addData(myWagerSize, 0);
							P_Call_given_Raise[street].addData(myWagerSize, 0);
						}
						else if ( currA.equalsIgnoreCase("raise") ) {
							P_Fold_given_Raise[street].addData(myWagerSize, 0);
							P_Raise_given_Raise[street].addData(myWagerSize, 1);
							P_Call_given_Raise[street].addData(myWagerSize, 0);
							if ( hisEstimatedWinChance >=0 )
								value_Raise_given_their_winChance[street].addData(hisEstimatedWinChance, curr.amount);
						}
						else if ( currA.equalsIgnoreCase("call") ) {
							P_Fold_given_Raise[street].addData(myWagerSize, 0);
							P_Raise_given_Raise[street].addData(myWagerSize, 0);
							P_Call_given_Raise[street].addData(myWagerSize, 1);
						}
					}
					else if ( prevA.equalsIgnoreCase("check") ) {
						if ( currA.equalsIgnoreCase("bet") || currA.equalsIgnoreCase("raise") ) { //can't really raise unless preflop
							N_Bet_given_Check[street].addData(1);
							N_Check_given_Check[street].addData(0);
							if ( hisEstimatedWinChance >=0 )
								value_Bet_given_their_winChance[street].addData(hisEstimatedWinChance, curr.amount);
						}
						else if ( currA.equalsIgnoreCase("check") ) {
							N_Bet_given_Check[street].addData(0);
							N_Check_given_Check[street].addData(1);
						}
					}
				}
				
				//IF On-Action behavior is seen
				else if ( (prevA.equalsIgnoreCase("DEAL") || prevA.equalsIgnoreCase("POST")) && curr.actor.equalsIgnoreCase(game.oppName) ) {
					if ( currA.equalsIgnoreCase("check") ) {
						N_Bet_given_Check[street].addData(0);
						N_Check_given_Check[street].addData(1);
					}
					if ( currA.equalsIgnoreCase("bet") ) {
						N_Bet_given_Check[street].addData(1);
						N_Check_given_Check[street].addData(0);
						if ( hisEstimatedWinChance >=0 )
							value_Bet_given_their_winChance[street].addData(hisEstimatedWinChance, curr.amount);
					}
				}
				prev = curr;
				
			}
			
			totalHandCount++;
		}
		
		public final int[] THRESHOLD_FOR_GENERALIZING = new int[] {5,5,5,5};
		
		// (0,1) rating of this opponent's looseness (number of calls+raises over number of calls+raises+folds)
		// because check implies tight more than it implies loose
		final float DEFAULT_LOOSENESS= 0.4f;
		public float getLooseness(int street){
			int calls = P_Call_given_Bet[street].getN() + P_Call_given_Raise[street].getN();
			int raises = P_Raise_given_Bet[street].getN() + P_Raise_given_Raise[street].getN();
			int folds = P_Fold_given_Bet[street].getN() + P_Fold_given_Raise[street].getN();
			System.out.println("Looseness calculator: " + calls + " calls, " + raises + " raises, " + folds + " folds");
			return (calls+raises+folds > THRESHOLD_FOR_GENERALIZING[street]) ? (float)(calls+raises)/(calls+raises+folds) : DEFAULT_LOOSENESS;
		}
		
		// (0,1) rating of this opponent's aggression (number of bets+raises over number of bets+raises+calls+folds+checks
		final float AGGRESSION= 0.4f;
		public float getAggression(int street) {
			int checks = N_Check_given_Check[street].getN();
			int bets = N_Bet_given_Check[street].getN();
			int calls = P_Call_given_Bet[street].getN() + P_Call_given_Raise[street].getN();
			int raises = P_Raise_given_Bet[street].getN() + P_Raise_given_Raise[street].getN();
			int folds = P_Fold_given_Bet[street].getN() + P_Fold_given_Raise[street].getN();
			return (bets+calls+raises+folds+checks > THRESHOLD_FOR_GENERALIZING[street]) ? (float)(bets+raises)/(bets+calls+raises+folds+checks) : DEFAULT_LOOSENESS;
		}
		
		
		public void printStats(GameObject myGame) {
			String[] streets = new String[]{"Preflop","Flop","Turn","River"};
			for ( int i = 0; i < 4; i++ ) {
				System.out.println("<<< STREET : " + streets[i]+" >>>");
				
				System.out.println("Aggression: " + getAggression(i) );
				System.out.println("Looseness:  " + getLooseness(i) );
				
				P_Fold_given_Bet[i].print();
				P_Call_given_Bet[i].print();
				P_Raise_given_Bet[i].print();
				
				P_Fold_given_Raise[i].print();
				P_Call_given_Raise[i].print();
				P_Raise_given_Raise[i].print();
				
				N_Check_given_Check[i].print();
				N_Bet_given_Check[i].print();
				
				value_Raise_given_their_winChance[i].print();
				value_Bet_given_their_winChance[i].print();
				System.out.println("\n\n\n\n\n\n");
			}
		}

	}
	
	public String f(float f){
		return String.format("%.2f", f);
	}

}
