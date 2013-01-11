package pokerbots.player;

import pokerbots.packets.*;
import pokerbots.packets.GetActionObject.Action;

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
					GetActionObject msg = new GetActionObject(input);
					playerLogic(msg);
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
		return 0;
	}

	
	
	public void playerLogic( GetActionObject curr ) {
		int numBoardCards = curr.boardCards.length;
		
		switch ( numBoardCards ) {
			//PREFLOP
			case 0:
				float winChance0 = PreflopTableGen.getPreflopWinRate(myHand.cards[1],myHand.cards[2]);
				float winChance1 = PreflopTableGen.getPreflopWinRate(myHand.cards[0],myHand.cards[2]);
				float winChance2 = PreflopTableGen.getPreflopWinRate(myHand.cards[0],myHand.cards[1]);
				float p = winChance0;
				if ( winChance1 > p )
					p = winChance1;
				else if ( winChance2 > p )
					p = winChance2;
				if ( p > 0.5 )
					betRaiseCall(curr,p);
				else
					foldOrCheck(curr);
				break;
				
			//FLOP
			case 3:
				break;
				
			//TURN
			case 4:
				break;
			
			//RIVER
			case 5:
				break;
			default:
				break;
		}
	}
	
	public void betRaiseCall( GetActionObject curr, float winChance ) {
		for ( int i = 0; i < curr.legalActions.length; i++ ) {
			Action action = curr.legalActions[i];
			if ( action.actionType.equalsIgnoreCase("bet") ) {
				int min = action.minBet;
				int max = action.maxBet;
				int bet = makeProportionalBet(winChance,min,max);
				outStream.println("BET:"+bet);
				return;
			}
			if ( action.actionType.equalsIgnoreCase("call") ) {
				outStream.println("CALL");
				return;
			}
		}
	}
}