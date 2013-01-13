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

		private final int[] THRESHOLD_FOR_GENERALIZING = new int[] {1, 1, 1, 1}; // must be at least 1
		private final float DEFAULT_PERCENT = 0.5f;
		private final int NUM_BUCKETS = 5;
		
		private int[] timesFoldsToBet;
		private int[] timesCallsToBet;
		private int[] timesRaisesToBet;
		
		private int[] timesFoldsToRaise;
		private int[] timesCallsToRaise;
		private int[] timesRaisesToRaise;

		private int[] timesChecksOnAction;
		private int[] timesBetsOnAction;

		private int[] totalTimesToBet;
		private int[] totalTimesToRaise;
		private int[] totalTimesOnAction;


		private int[] totalAmountWeBetForFold;
		private int[] totalAmountWeRaiseForFold;
		
		
		private int[][] totalBetsPerBucketPerStreet;
		private int[][] timesBetsPerBucketPerStreet;
		
		public OpponentStats(){
			
			timesFoldsToBet = new int[] {0,0,0,0};
			timesCallsToBet = new int[] {0,0,0,0};
			timesRaisesToBet = new int[] {0,0,0,0};
			timesFoldsToRaise = new int[] {0,0,0,0};
			timesCallsToRaise = new int[] {0,0,0,0};
			timesRaisesToRaise = new int[] {0,0,0,0};
			timesChecksOnAction = new int[] {0,0,0,0};
			timesBetsOnAction = new int[] {0,0,0,0};
			totalTimesToBet = new int[] {0,0,0,0};
			totalTimesToRaise = new int[] {0,0,0,0};
			totalTimesOnAction = new int[] {0,0,0,0};
			totalAmountWeBetForFold = new int[] {0,0,0,0};
			totalAmountWeRaiseForFold = new int[] {0,0,0,0};
			
			totalBetsPerBucketPerStreet = new int[NUM_BUCKETS][4];
			for (int i=0; i<totalBetsPerBucketPerStreet.length; i++){
				for (int j=0; j<totalBetsPerBucketPerStreet[0].length; j++){
					totalBetsPerBucketPerStreet[i][j] = 0;
				}
			}
			timesBetsPerBucketPerStreet = new int[NUM_BUCKETS][4];
			for (int i=0; i<timesBetsPerBucketPerStreet.length; i++){
				for (int j=0; j<timesBetsPerBucketPerStreet[0].length; j++){
					timesBetsPerBucketPerStreet[i][j] = 0;
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
			return fractionOrGeneralize(timesFoldsToBet, totalTimesToBet, street);
		}

		public float getPercentCallToBet(int street){
			return fractionOrGeneralize(timesCallsToBet, totalTimesToBet, street);
		}

		public float getPercentRaiseToBet(int street){
			return fractionOrGeneralize(timesRaisesToBet, totalTimesToBet, street);
		}

		public float getPercentFoldToRaise(int street){
			return fractionOrGeneralize(timesFoldsToRaise, totalTimesToRaise, street);
		}

		public float getPercentCallToRaise(int street){
			return fractionOrGeneralize(timesCallsToRaise, totalTimesToRaise, street);
		}

		public float getPercentRaiseToRaise(int street){
			return fractionOrGeneralize(timesRaisesToRaise, totalTimesToRaise, street);
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
		
		public float getBetAmount(int street, float oppWinPercentage){
			int bucket = (int)((oppWinPercentage*NUM_BUCKETS) / 100);
			return fractionOrGeneralize(totalBetsPerBucketPerStreet[bucket], timesBetsPerBucketPerStreet[bucket], street);
		}

	}

}





















