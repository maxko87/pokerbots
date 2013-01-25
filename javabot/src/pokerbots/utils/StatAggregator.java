package pokerbots.utils;

import java.util.HashMap;

import pokerbots.packets.GameObject;
import pokerbots.packets.HandObject;
import pokerbots.packets.PerformedActionObject;
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
	
	public OpponentStats getOrCreateOpponent(String oppName, int startingStackSize) {
		if (m.get(oppName) == null){
			m.put(oppName, new OpponentStats(oppName, startingStackSize));
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

		String name = "Undefined";
		int startingStackSize = 400;
		public final int[] THRESHOLD_FOR_GENERALIZING = new int[] {3, 3, 3, 3}; // must be at least 1
		public final float DEFAULT_PERCENT = 0.3f;
		public final int NUM_OPP_WIN_PERCENTAGE_BUCKETS = 5;
		
		//these two parameters should be learned later
		public final float DEFAULT_LOOSENESS = 0.3f;
		public final float DEFAULT_AGGRESSION = 0.25f;
		
		
		public int[] timesChecksToCheck;
		public int[] timesBetsToCheck;

		public int[] timesFoldsToBet;
		public int[] timesCallsToBet;
		public int[] timesRaisesToBet;
		
		public int[] timesFoldsToRaise;
		public int[] timesCallsToRaise;
		public int[] timesRaisesToRaise;

		public int[] timesChecksOnAction;
		public int[] timesBetsOnAction;

		public int[] totalTimesWeCheck;
		public int[] totalTimesWeBet;
		public int[] totalTimesWeRaise;
		public int[] totalTimesOnAction;
		
		public int[] totalValueOfBets;
		public int[] totalValueOfRaises;


		public int[] totalAmountWeBetForFold;
		public int[] totalAmountWeRaiseForFold;
		
		
		public int[][] totalValueOfBetsPerBucketPerStreet;
		public int[][] totalCountOfBetsPerBucketPerStreet;
		
		public OpponentStats( String name, int startingStackSize ){
			
			this.name = name;
			this.startingStackSize = startingStackSize;
			
			//times they ___ to our ___
			timesChecksToCheck = new int[] {0,0,0,0};
			timesBetsToCheck = new int[] {0,0,0,0};
			timesFoldsToBet = new int[] {0,0,0,0};
			timesCallsToBet = new int[] {0,0,0,0};
			timesRaisesToBet = new int[] {0,0,0,0};
			timesFoldsToRaise = new int[] {0,0,0,0};
			timesCallsToRaise = new int[] {0,0,0,0};
			timesRaisesToRaise = new int[] {0,0,0,0};
			timesChecksOnAction = new int[] {0,0,0,0};
			timesBetsOnAction = new int[] {0,0,0,0};
			totalTimesWeBet = new int[] {0,0,0,0};
			totalTimesWeRaise = new int[] {0,0,0,0};
			totalTimesWeCheck = new int[] {0,0,0,0};
			totalTimesOnAction = new int[] {0,0,0,0};
			
			totalAmountWeBetForFold = new int[] {0,0,0,0};
			totalAmountWeRaiseForFold = new int[] {0,0,0,0};
			
			totalValueOfBets = new int[] {0,0,0,0};
			totalValueOfRaises = new int[] {0,0,0,0};
			
			totalValueOfBetsPerBucketPerStreet = new int[NUM_OPP_WIN_PERCENTAGE_BUCKETS][4];
			for (int i=0; i<totalValueOfBetsPerBucketPerStreet.length; i++){
				for (int j=0; j<totalValueOfBetsPerBucketPerStreet[0].length; j++){
					totalValueOfBetsPerBucketPerStreet[i][j] = 0;
				}
			}
			totalCountOfBetsPerBucketPerStreet = new int[NUM_OPP_WIN_PERCENTAGE_BUCKETS][4];
			for (int i=0; i<totalCountOfBetsPerBucketPerStreet.length; i++){
				for (int j=0; j<totalCountOfBetsPerBucketPerStreet[0].length; j++){
					totalCountOfBetsPerBucketPerStreet[i][j] = 0;
				}
			}

		}
		
		//Estimate win-rate data analysis with linear regression
		//[ n		S(x)  ]^-1	[ S(y)   ] = [ a ]
		//[ S(x)	S(x*x)]		[ S(x*y) ] = [ b ]
		float[] N = new float[4];
		float[] SX = new float[4];
		float[] SXX = new float[4];
		float[] SY = new float[4];
		float[] SXY = new float[4];
		
		//Regression equations for y = a+bx;
		//det = N*SXX-SX*SX
		//a = 1/det * (SY*SXX-SX*SXY)
		//b = 1/det * (N*SXY-SX*SY)
		float[] REG_A = new float[4];
		float[] REG_B = new float[4];
		
		public void addEWRdata( Round r ) {
			System.out.println("EWR data added");
			for ( int i = 0; i < 4; i++ ) {
				float y = r.oppWinRates[i];
				int x = r.oppAmounts[i];
				if ( x>0 ) {
					N[i] += 1;
					SX[i] += x;
					SXX[i] += x*x;
					SY[i] += y;
					SXY[i] += x*y;
				}
				
				if ( N[i]>1 ) {
					float det = N[i]*SXX[i]-SX[i]*SX[i];
					REG_A[i] = 1.0f/det * (SY[i]*SXX[i]-SX[i]*SXY[i]);
					REG_B[i] = 1.0f/det * (N[i]*SXY[i]-SX[i]*SY[i]);
					System.out.println("Street: " + i + ", eq: win = a+b*bet, a=" + REG_A[i] + ", b=" + REG_B[i]);
				}
			}
		}
		
		public float getEstimatedWinRate(int street,int value) {
			if ( N[street]<2 )
				return 0.5f;
			return REG_A[street]+REG_B[street]*value;
		}
		

		/* Parses the lines between every "HAND #" and "__ wins the pot" in the match.txt file to update this class accordingly. 
		 * TODO
		 */
		public void analyzeRoundData( GameObject game, HandObject hand, Round data, int stackSize ){
			//NOTE: scale all amounts by the quantity (potSize/startingStackSize)!!!!
			
			int street = 0;
			for ( int i = 0; i < data.actions.size() - 1; i++ ) {
				PerformedActionObject prev = data.actions.get(i);
				PerformedActionObject curr = data.actions.get(i+1);

				String prevA = prev.actionType;
				String currA = curr.actionType;
				
				//Increment street
				if ( currA.equalsIgnoreCase("deal") ) {
					street++;
					prev = curr;
					continue;
				}
				
				//
				if ( curr.actor.equals(game.myName) && curr.actionType.equalsIgnoreCase("refund") ) {
					timesFoldsToBet[street]++;
					totalTimesWeBet[street]++;
					
					System.out.println("INCREMENTED timesFoldsToBet: " + street + "\t\t" + timesFoldsToBet[street]);
					System.out.println("INCREMENTED totalTimesWeBet: " + street + "\t\t" + totalTimesWeBet[street]);
				}
				
				//IF I perform an action THEN OPP performs action
				if ( prev.actor.equalsIgnoreCase(game.myName) && curr.actor.equals(game.oppName) ) {
					if ( prevA.equalsIgnoreCase("bet") ) {
						if ( currA.equalsIgnoreCase("fold") ) {
							timesFoldsToBet[street]++;
							totalTimesWeBet[street]++;
							totalAmountWeBetForFold[street] += curr.amount;
							
							System.out.println("INCREMENTED timesFoldsToBet: " + street + "\t\t" + timesFoldsToBet[street]);
							System.out.println("INCREMENTED totalTimesWeBet: " + street + "\t\t" + totalTimesWeBet[street]);
						}
						else if ( currA.equalsIgnoreCase("raise") ) {
							timesRaisesToBet[street]++;
							totalTimesWeBet[street]++;
							totalValueOfRaises[street] += curr.amount;
							
							System.out.println("INCREMENTED timesRaisesToBet: " + street + "\t\t" + timesRaisesToBet[street]);
							System.out.println("INCREMENTED totalTimesWeBet: " + street + "\t\t" + totalTimesWeBet[street]);
							System.out.println("INCREMENTED totalValueOfRaises: " + street + "\t\t" + totalValueOfRaises[street]);
						}
						else if ( currA.equalsIgnoreCase("call") ) {
							timesCallsToBet[street]++;
							totalTimesWeBet[street]++;
							
							System.out.println("INCREMENTED timesCallsToBet: " + street + "\t\t" + timesCallsToBet[street]);
							System.out.println("INCREMENTED totalTimesWeBet: " + street + "\t\t" + totalTimesWeBet[street]);
						}
					}
					else if ( prevA.equalsIgnoreCase("raise") ) {
						if ( currA.equalsIgnoreCase("fold") ) {
							timesFoldsToRaise[street]++;
							totalTimesWeRaise[street]++;
							totalAmountWeRaiseForFold[street] += curr.amount;

							System.out.println("INCREMENTED timesFoldsToRaise: " + street + "\t\t" + timesFoldsToRaise[street]);
							System.out.println("INCREMENTED totalTimesWeRaise: " + street + "\t\t" + totalTimesWeRaise[street]);
						}
						else if ( currA.equalsIgnoreCase("raise") ) {
							timesRaisesToRaise[street]++;
							totalTimesWeRaise[street]++;
							totalValueOfRaises[street] += curr.amount;
							
							System.out.println("INCREMENTED timesRaisesToRaise: " + street + "\t\t" + timesRaisesToRaise[street]);
							System.out.println("INCREMENTED totalTimesWeRaise: " + street + "\t\t" + totalTimesWeRaise[street]);
							System.out.println("INCREMENTED totalValueOfRaises: " + street + "\t\t" + totalValueOfRaises[street]);
						}
						else if ( currA.equalsIgnoreCase("call") ) {
							timesCallsToRaise[street]++;
							totalTimesWeRaise[street]++;
							
							System.out.println("INCREMENTED timesCallsToRaise: " + street + "\t\t" + timesCallsToRaise[street]);
							System.out.println("INCREMENTED totalTimesWeRaise: " + street + "\t\t" + totalTimesWeRaise[street]);
						}
					}
					else if ( prevA.equalsIgnoreCase("check") ) {
						if ( currA.equalsIgnoreCase("bet") || currA.equalsIgnoreCase("raise") ) { //can't really raise unless preflop

							timesBetsToCheck[street]++;
							totalTimesWeCheck[street]++;
							totalValueOfBets[street] += curr.amount;
							
							System.out.println("INCREMENTED timesBetsToCheck: " + street + "\t\t" + timesBetsToCheck[street]);
							System.out.println("INCREMENTED totalTimesWeCheck: " + street + "\t\t" + totalTimesWeCheck[street]);
							System.out.println("INCREMENTED totalValueOfBets: " + street + "\t\t" + totalValueOfBets[street]);

						}
						else if ( currA.equalsIgnoreCase("check") ) {

							timesChecksToCheck[street]++;
							totalTimesWeCheck[street]++;
							
							System.out.println("INCREMENTED timesChecksToCheck: " + street + "\t\t" + timesChecksToCheck[street]);
							System.out.println("INCREMENTED totalTimesWeCheck: " + street + "\t\t" + totalTimesWeCheck[street]);

						}
					}
				}
				
				//IF On-Action behavior is seen
				else if ( (prevA.equalsIgnoreCase("DEAL") || prevA.equalsIgnoreCase("POST")) && curr.actor.equalsIgnoreCase(game.oppName) ) {
					if ( currA.equalsIgnoreCase("check") ) {
						timesChecksOnAction[street]++;
						totalTimesOnAction[street]++;
						
						System.out.println("INCREMENTED timesChecksOnAction: " + street + "\t\t" + timesChecksOnAction[street]);
						System.out.println("INCREMENTED totalTimesOnAction: " + street + "\t\t" + totalTimesOnAction[street]);
					}
					if ( currA.equalsIgnoreCase("bet") ) {
						timesBetsOnAction[street]++;
						totalTimesOnAction[street]++;
						totalValueOfBets[street] += curr.amount;
						
						System.out.println("INCREMENTED timesBetsOnAction: " + street + "\t\t" + timesBetsOnAction[street]);
						System.out.println("INCREMENTED totalTimesOnAction: " + street + "\t\t" + totalTimesOnAction[street]);
						System.out.println("INCREMENTED totalValueOfBets: " + street + "\t\t" + totalValueOfBets[street]);
					}
				}
				prev = curr;
				
			}
			
			//Add Showdown data!
			if (data.showdown) {
				addEWRdata(data);
			}
		}	

		/* Helper that either returns the percentage of action over total for a specific street,
		 * or generalizes to all streets, or returns a default value.
		 *
		 * Street can be > 4 so that aggregating over streets is the default.
		 */
		private float fractionOrGeneralize(int[] action, int[] total, int street){
			if (total[street] < THRESHOLD_FOR_GENERALIZING[street] || street > 4){
				float sumActions = 0.0f;
				float sumTotals = 0.0f;
				for (int i=0; i<4; i++){
					sumActions += action[i];
					sumTotals += total[i];
				}
				return (sumTotals > 0.0f) ? sumActions/sumTotals : DEFAULT_PERCENT;
			}
			else {
				return action[street] / (float)total[street];
			}
		}

		public float getPercentFoldToBet(int street){
			return fractionOrGeneralize(timesFoldsToBet, totalTimesWeBet, street);
		}

		public float getPercentCallToBet(int street){
			return fractionOrGeneralize(timesCallsToBet, totalTimesWeBet, street);
		}

		public float getPercentRaiseToBet(int street){
			return fractionOrGeneralize(timesRaisesToBet, totalTimesWeBet, street);
		}

		public float getPercentFoldToRaise(int street){
			return fractionOrGeneralize(timesFoldsToRaise, totalTimesWeRaise, street);
		}

		public float getPercentCallToRaise(int street){
			return fractionOrGeneralize(timesCallsToRaise, totalTimesWeRaise, street);
		}

		public float getPercentRaiseToRaise(int street){
			return fractionOrGeneralize(timesRaisesToRaise, totalTimesWeRaise, street);
		}

		public float getPercentChecksOnAction(int street){
			return fractionOrGeneralize(timesChecksOnAction, totalTimesOnAction, street);
		}

		public float getPercentBetsOnAction(int street){
			return fractionOrGeneralize(timesBetsOnAction, totalTimesOnAction, street);
		}
		
		public float getPercentChecksToCheck(int street){
			return fractionOrGeneralize(timesChecksToCheck, totalTimesWeCheck, street);
		}
		
		public float getPercentBetsToCheck(int street){
			return fractionOrGeneralize(timesBetsToCheck, totalTimesWeCheck, street);
		}

		public float getOurAverageBetForFold(int street){
			return fractionOrGeneralize(totalAmountWeBetForFold, timesFoldsToBet, street);
		}

		public float getOurAverageRaiseForFold(int street){
			return fractionOrGeneralize(totalAmountWeRaiseForFold, timesFoldsToRaise, street);
		}
		
		public int[] getTimesRaises(){
			int[] res = new int[4];
			for (int i=0; i<4; i++){
				res[i] = timesRaisesToBet[i] + timesRaisesToRaise[i];
			}
			return res;
		}
		
		public float getAverageRaise(int street){
			return fractionOrGeneralize(totalValueOfRaises, getTimesRaises(), street);
		}
		
		public float getAverageBet(int street){
			return fractionOrGeneralize(totalValueOfBets, timesBetsOnAction, street);
		}
		
		/*
		// showdowns only: retroactively determine opponent's bet size given a win percentage on specific street
		// we can tell how much an opponent would bet on a street if he had a certain win percentage.
		public float getBetAmount(int street, float oppWinPercentage){
			int bucket = (int)((oppWinPercentage*NUM_OPP_WIN_PERCENTAGE_BUCKETS) / 100);
			return fractionOrGeneralize(totalValueOfBetsPerBucketPerStreet[bucket], totalCountOfBetsPerBucketPerStreet[bucket], street);
		}
		
		// gives middle of estimated opponent's win percentage for a street given a bet
		public float getOppWinPercentage(int street, int bet){
			int bucket = 0;
			float diff = Float.MAX_VALUE;
			float bucketWidth = 100.0f / NUM_OPP_WIN_PERCENTAGE_BUCKETS;
			for (int i=0; i<NUM_OPP_WIN_PERCENTAGE_BUCKETS; i++){
				float winPercent = (i*bucketWidth) + (bucketWidth/2);
				if (Math.abs(getBetAmount(street, winPercent) - bet) < diff){
					bucket = i;
					diff = Math.abs(getBetAmount(street, winPercent) - bet);
				}
			}
			return (bucket*bucketWidth) + (bucketWidth/2);
		}
		*/
		
		// (0,1) rating of this opponent's aggression (size of bet when he does bet)
		public float getAggression(int street){
			/*
			int totalValueOfBets = 0;
			int totalNumberOfBets = 0;
			for (int i=0; i<NUM_OPP_WIN_PERCENTAGE_BUCKETS; i++){
				totalValueOfBets += totalValueOfBetsPerBucketPerStreet[i][street];
				totalNumberOfBets += totalCountOfBetsPerBucketPerStreet[i][street];
			}
			float averageBet = (float)(totalValueOfBets)/totalNumberOfBets;
			return (maxBet > 0.0f) ? averageBet/maxBet : DEFAULT_AGGRESSION;
			*/
			float totalBetCount = (timesRaisesToBet[street] + timesRaisesToRaise[street] + timesBetsOnAction[street]);
			return (totalBetCount > THRESHOLD_FOR_GENERALIZING[street]) ? (float)(totalValueOfBets[street] + totalValueOfRaises[street] ) / (totalBetCount * startingStackSize) : getTotalAggression();
		}
		
		public float getTotalAggression(){
			float totalBetCount = 0;
			float totalValueOfAllBets = 0;
			for (int street=0; street<4; street++){
				totalBetCount += (timesRaisesToBet[street] + timesRaisesToRaise[street] + timesBetsOnAction[street]);
				totalValueOfAllBets += (float)(totalValueOfBets[street] + totalValueOfRaises[street] );
			}
			return (totalBetCount > 0.0f) ? totalValueOfAllBets / (totalBetCount * startingStackSize) : DEFAULT_AGGRESSION;
		}
		
		// (0,1) rating of this opponent's looseness (number of calls+raises over number of calls+raises+folds)
		public float getLooseness(int street){
			int calls = timesCallsToBet[street] + timesCallsToRaise[street];
			int raises = timesRaisesToBet[street] + timesRaisesToRaise[street];
			int folds = timesFoldsToBet[street] + timesFoldsToRaise[street];
			return (calls+raises+folds > THRESHOLD_FOR_GENERALIZING[street]) ? (float)(calls+raises)/(calls+raises+folds) : getTotalLooseness();
		}
		
		public float getTotalLooseness(){
			int calls = 0;
			int raises = 0;
			int folds = 0;
			for (int street=0; street<4; street++){
				calls += timesCallsToBet[street] + timesCallsToRaise[street];
				raises += timesRaisesToBet[street] + timesRaisesToRaise[street];
				folds += timesFoldsToBet[street] + timesFoldsToRaise[street];
			}
			return (calls+raises+folds > 0) ? (float)(calls+raises)/(calls+raises+folds) : DEFAULT_LOOSENESS;
		}
		
		public void printStats(GameObject myGame) {
			String[] streets = new String[]{"Preflop","Flop","Turn","River"};
			for ( int i = 0; i < 4; i++ ) {
				System.out.println("<<< STREET : " + streets[i]+" >>>");
				System.out.println("opp \\ us\tBet\t\tRaise\t\tCheck");
				System.out.println("Fold\t\t" + f(getPercentFoldToBet(i)) +"\t\t"+f(getPercentFoldToRaise(i))+"\t\tX");
				System.out.println("Raise\t\t" + f(getPercentRaiseToBet(i)) +"\t\t"+f(getPercentRaiseToRaise(i))+"\t\tX");
				System.out.println("Call\t\t" + f(getPercentCallToBet(i)) +"\t\t"+f(getPercentCallToRaise(i))+"\t\tX");
				System.out.println("Bet\t\t" + "X\t\tX\t\t" + f(getPercentBetsOnAction(i)) );
				System.out.println("Checks on action: " + f(getPercentChecksOnAction(i)) );
				System.out.println("Aggression: " + f(getAggression(i)));
				System.out.println("Looseness: " + f(getLooseness(i)));
				System.out.println("Average bet, raise: " + (int)getAverageBet(i) + ", " + (int)getAverageRaise(i));
			}
			System.out.println("Aggregate Aggression: " + f(getTotalAggression()) );
			System.out.println("Aggregate Looseness: " + f(getTotalLooseness()) );
			System.out.println("END\n\n\n\n\n\n\n\n\n");
		}

	}
	
	public String f(float f){
		return String.format("%.2f", f);
	}

}





















