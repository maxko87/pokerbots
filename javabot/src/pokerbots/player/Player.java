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

			// Block until engine sends us a packet; read it into input.
			while ((input = inStream.readLine()) != null) {
				System.out.println(input);
				String packetType = input.split(" ")[0];
				if ("GETACTION".compareToIgnoreCase(packetType) == 0) {
					// When appropriate, reply to the engine with a legal action.
					// The engine will ignore all spurious packets you send.
					// The engine will also check/fold for you if you return an illegal action.
					// GETACTION potSize numBoardCards [boardCards] numLastActions [lastActions] numLegalActions [legalActions] timebank
					Parser.parseGETACTION(input);
					outStream.println("CHECK");
				} else if ("NEWGAME".compareToIgnoreCase(packetType) == 0) {

				} else if ("HANDOVER".compareToIgnoreCase(packetType) == 0) {

				} else if ("KEYVALUE".compareToIgnoreCase(packetType) == 0) {

				} else if ("REQUESTKEYVALUES".compareToIgnoreCase(packetType) == 0) {
					// At the end, engine will allow bot to send key/value pairs to store.
					// FINISH indicates no more to store.
					outStream.println("FINISH");
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
