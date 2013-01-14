package pokerbots.packets;

import pokerbots.utils.HandEvaluator;

public class HandObject {
	//NEWHAND handId button holeCard1 holeCard2 holeCard3 yourBank oppBank timeBank
	//NEWHAND 10 true Ah Ac Ad 0 0 20.000000
	
	public int handId = 0;
	public boolean button = false;
	public int[] cards3 = new int[3]; //before discard
	public int[] cards2 = new int[2]; //after discard
	public int myBank = 0;
	public int oppBank = 0;
	public double timeBank = 0;
	
	public HandObject( String input ) {
		String[] tokens = input.split(" ");
		handId = Integer.parseInt(tokens[1]);
		button = Boolean.parseBoolean(tokens[2]);
		cards3[0] = HandEvaluator.stringToCard(tokens[3]);
		cards3[1] = HandEvaluator.stringToCard(tokens[4]);
		cards3[2] = HandEvaluator.stringToCard(tokens[5]);
		myBank = Integer.parseInt(tokens[6]);
		oppBank = Integer.parseInt(tokens[7]);
		timeBank = Double.parseDouble(tokens[8]);
	}
	
	//populates cards2 after the toss
	public void remove(int tossCard){
		if (tossCard == 0){
			cards2[0] = cards3[1];
			cards2[1] = cards3[2];
		}
		else if (tossCard == 1){
			cards2[0] = cards3[0];
			cards2[1] = cards3[2];
		}
		else{ // if (tossCard == 2)
			cards2[0] = cards3[0];
			cards2[1] = cards3[1];
		}
	}
}