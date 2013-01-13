package pokerbots.packets;

import pokerbots.utils.HandEvaluator;

public class GetActionObject{

	// GETACTION potSize numBoardCards [boardCards] numLastActions [lastActions] numLegalActions [legalActions] timebank

	public int potSize;
	public int[] boardCards;
	public PerformedActionObject[] lastActions;
	public LegalActionObject[] legalActions;
	public float timebank;

	
	public GetActionObject(String input){
		String[] values = input.split(" ");
		
		potSize = Integer.parseInt(values[1]);

		int i = 2;
		boardCards = new int[Integer.parseInt(values[i])];
		for (int j=0; j<boardCards.length; j++){
			boardCards[j] = HandEvaluator.stringToCard(values[i+j+1]);
		}

		//TODO: parse first/second player actions?
		i += boardCards.length + 1;
		lastActions = new PerformedActionObject[Integer.parseInt(values[i])];
		for (int j=0; j<lastActions.length; j++){
			lastActions[j] = new PerformedActionObject(values[i+j+1]);
		}

		i += lastActions.length + 1;
		legalActions = new LegalActionObject[Integer.parseInt(values[i])];
		for (int j=0; j<legalActions.length; j++){
			legalActions[j] = new LegalActionObject(values[i+j+1]);
		}

		timebank = Float.parseFloat(values[i + legalActions.length + 1]);
	}
}
