package pokerbots.packets;

public class NewGameObject {
	//NEWGAME yourName oppName stackSize bb numHands timeBank
	//NEWGAME player1 player2 200 2 100 20.000000
	
	public String myName = "";
	public String oppName = "";
	public int stackSize = 0;
	public int bigBlind = 0;
	public int numHands = 0;
	public double timeBank = 0;
	
	public NewGameObject( String input ) {
		String[] tokens = input.split(" ");
		myName = tokens[1];
		oppName = tokens[2];
		stackSize = Integer.parseInt(tokens[3]);
		bigBlind = Integer.parseInt(tokens[4]);
		numHands = Integer.parseInt(tokens[5]);
		timeBank = Double.parseDouble(tokens[6]);
	}
}