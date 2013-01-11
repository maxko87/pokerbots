package pokerbots.packets;

import pokerbots.utils.HandEvaluator;

class NewHandObject {
	//NEWHAND handId button holeCard1 holeCard2 holeCard3 yourBank oppBank timeBank
	//NEWHAND 10 true Ah Ac Ad 0 0 20.000000
	
	public int handId = 0;
	public boolean button = false;
	public int[] cards = new int[3];
	public int myBank = 0;
	public int oppBank = 0;
	public double timeBank = 0;
	
	public NewHandObject( String input ) {
		String[] tokens = input.split(" ");
		handId = Integer.parseInt(tokens[1]);
		button = Boolean.parseBoolean(tokens[2]);
		cards[0] = HandEvaluator.stringToCard(tokens[3]);
		cards[1] = HandEvaluator.stringToCard(tokens[4]);
		cards[2] = HandEvaluator.stringToCard(tokens[5]);
		myBank = Integer.parseInt(tokens[6]);
		oppBank = Integer.parseInt(tokens[7]);
		timeBank = Double.parseDouble(tokens[8]);
	}
}