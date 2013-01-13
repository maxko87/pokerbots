package pokerbots.utils;

import java.util.HashMap;

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
	
	//Deserialize TODO
	public StatAggregator(String keyValuePairs){
		
	}
	
	//Serialize TODO
	public String Serializer(){
		return "";
	}
	
	
	private class OpponentStats{

		public final int[] THRESHOLD_FOR_GENERALIZING = new int[] {1, 1, 1, 1}; // must be at least 1
		public final float DEFAULT_PERCENT = 0.5f;
		public final int NUM_OPP_WIN_PERCENTAGE_BUCKETS = 5;
		
		public int[] timesFoldsToBet;
		public int[] timesCallsToBet;
		public int[] timesRaisesToBet;
		
		public int[] timesFoldsToRaise;
		public int[] timesCallsToRaise;
		public int[] timesRaisesToRaise;

		public int[] timesChecksOnAction;
		public int[] timesBetsOnAction;

		public int[] totalTimesWeBet;
		public int[] totalTimesWeRaise;
		public int[] totalTimesOnAction;


		public int[] totalAmountWeBetForFold;
		public int[] totalAmountWeRaiseForFold;
		
		
		public int[][] totalValueOfBetsPerBucketPerStreet;
		public int[][] totalCountOfBetsPerBucketPerStreet;
		
		public OpponentStats(){
			
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
			totalTimesOnAction = new int[] {0,0,0,0};
			totalAmountWeBetForFold = new int[] {0,0,0,0};
			totalAmountWeRaiseForFold = new int[] {0,0,0,0};
			
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

		/* Parses the lines between every "HAND #" and "__ wins the pot" in the match.txt file to update this class accordingly. 
		 * TODO
		 */
		public void parseHand(String[] linesOfDump){
			
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

		public float getOurAverageBetForFold(int street){
			return fractionOrGeneralize(totalAmountWeBetForFold, timesFoldsToBet, street);
		}

		public float getOurAverageRaiseForFold(int street){
			return fractionOrGeneralize(totalAmountWeRaiseForFold, timesFoldsToRaise, street);
		}
		
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

	}

}





















