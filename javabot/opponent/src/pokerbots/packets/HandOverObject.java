package pokerbots.packets;

import pokerbots.utils.HandEvaluator;

public class HandOverObject{

	// GETACTION potSize numBoardCards [boardCards] numLastActions [lastActions] numLegalActions [legalActions] timebank
	
	// HANDOVER yourBank oppBank numBoardCards [boardCards] numLastActions [lastActions] timeBank
		
	public int myBank;
	public int oppBank;
	public int[] boardCards;
	public PerformedActionObject[] lastActions;
	public float timebank;

	
	public HandOverObject(String input){
		String[] values = input.split(" ");
		
		myBank = Integer.parseInt(values[1]);
		oppBank = Integer.parseInt(values[2]);

		int i = 3;
		boardCards = new int[Integer.parseInt(values[i])];
		for (int j=0; j<boardCards.length; j++){
			boardCards[j] = HandEvaluator.stringToCard(values[i+j+1]);
		}

		i += boardCards.length + 1;
		lastActions = new PerformedActionObject[Integer.parseInt(values[i])];
		for (int j=0; j<lastActions.length; j++){
			lastActions[j] = new PerformedActionObject(values[i+j+1]);
		}

		timebank = Float.parseFloat(values[values.length-1]);
	}
}
