package pokerbots.player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import pokerbots.utils.*;

/**
 * Simple example pokerbot, written in Java.
 * 
 * This is an example of a bare bones, pokerbot. It only sets up the socket
 * necessary to connect with the engine and then always returns the same action.
 * It is meant as an example of how a pokerbot should communicate with the
 * engine.
 * 
 */
public class Player {
	
	private final PrintWriter outStream;
	private final BufferedReader inStream;

	public Player(PrintWriter output, BufferedReader input) {
		this.outStream = output;
		this.inStream = input;
	}
	
	public void run() {
		String input;
		try {
			while ((input = inStream.readLine()) != null) {
				System.out.println(input);
				String packetType = input.split(" ")[0];
				if ("GETACTION".compareToIgnoreCase(packetType) == 0) {
					
					//outStream.println("CHECK");
				} else if ("NEWGAME".compareToIgnoreCase(packetType) == 0) {

				} else if ("HANDOVER".compareToIgnoreCase(packetType) == 0) {

				} else if ("KEYVALUE".compareToIgnoreCase(packetType) == 0) {

				} else if ("REQUESTKEYVALUES".compareToIgnoreCase(packetType) == 0) {
					outStream.println("FINISH");
				}
			}
		} catch (IOException e) {
			System.out.println("IOException: " + e.getMessage());
		}

		System.out.println("Gameover, engine disconnected");
		
		// Once the server disconnects from us, close our streams and sockets.
		try {
			outStream.close();
			inStream.close();
		} catch (IOException e) {
			System.out.println("Encounterd problem shutting down connections");
			e.printStackTrace();
		}
	}
	
}

class NewGameObject {
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

class GetActionObject{

// GETACTION potSize numBoardCards [boardCards] numLastActions [lastActions] numLegalActions [legalActions] timebank

	int potSize;
	int[] boardCards;
	String[] lastActions;
	String[] legalActions;
	float timebank;

	
	public GetActionObject(String input){
		String[] values = data.split(" ");
		
		potSize = Integer.parseInt(values[1]);

		int i = 2;
		boardCards = new int[i];
		while (int j=0; j<boardCards.length; j++){
			boardCards[j] = Integer.parseInt(values[i+j]);
		}

		//TODO: parse first/second player actions?
		i += boardCards.length + 1;
		lastActions = new String[i];
		while (int j=0; j<lastActions.length; j++){
			lastActions[j] = values[i+j];
		}

		i += lastActions.length + 1;
		legalActions = new String[i];
		while (int j=0; j<legalActions.length; j++){
			legalActions[j] = values[i+j];
		}

		timebank = Float.parseFloat(values[i + legalActions.length + 1]);

	}

}
