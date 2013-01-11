package pokerbots.packets;

public class GetActionObject{

	// GETACTION potSize numBoardCards [boardCards] numLastActions [lastActions] numLegalActions [legalActions] timebank

	public int potSize;
	public int[] boardCards;
	public String[] lastActions;
	public Action[] legalActions;
	public float timebank;

	
	public GetActionObject(String input){
		String[] values = input.split(" ");
		
		potSize = Integer.parseInt(values[1]);

		int i = 2;
		boardCards = new int[i];
		for (int j=0; j<boardCards.length; j++){
			boardCards[j] = Integer.parseInt(values[i+j]);
		}

		//TODO: parse first/second player actions?
		i += boardCards.length + 1;
		lastActions = new String[i];
		for (int j=0; j<lastActions.length; j++){
			lastActions[j] = values[i+j];
		}

		i += lastActions.length + 1;
		legalActions = new String[i];
		for (int j=0; j<legalActions.length; j++){
			legalActions[j] = new Action(values[i+j]);
		}

		timebank = Float.parseFloat(values[i + legalActions.length + 1]);
	}

	public class Action(){

		public String actionType;
		public int minBet; //optional
		public int maxBet; //optional
		public int cardToDiscard; //optional

		public Action(String input){
			String[] values = input.split(" ");
			String actionType = values[0];

			// bet or raise
			if (values.length > 1){
				String[] bets = values[1].split(":");
				minBet = Integer.parseInt(bets[0]);
				maxBet = Integer.parseInt(bets[1]);
			}

			//discard
			else if (actionType.contains(":")){
				String words = actionType.split(":");
				actionType = words[0];
				cardToDiscard = HandEvaluator.stringToCard(words[1]);
			}

		}
	}
}	
