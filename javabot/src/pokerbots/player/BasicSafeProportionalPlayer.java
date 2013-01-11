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
public class BasicSafeProportionalPlayer {
	
	private final PrintWriter outStream;
	private final BufferedReader inStream;
	private NewGameObject myGame;
	private NewHandObject myHand;

	public BasicSafeProportionalPlayer(PrintWriter output, BufferedReader input) {
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

					myGame = new NewGameObject(input);

				} else if ("NEWHAND".compareToIgnoreCase(packetType) == 0) {
					
					myHand = new NewHandObject(input);

				} else if ("HANDOVER".compareToIgnoreCase(packetType) == 0) {
					//no learning yet
				}else if ("KEYVALUE".compareToIgnoreCase(packetType) == 0) {
					//none
				} else if ("REQUESTKEYVALUES".compareToIgnoreCase(packetType) == 0) {
					//none
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

	//given odds and stack size, decides how much to bet
	public int makeProportionalBet(float percentage, int minBet, int maxBet){
		
	}

	
	
}