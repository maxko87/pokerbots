package pokerbots.player;

import pokerbots.packets.*;

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
public class BasicSafeProportionalPlayer_1 {
	
	private final PrintWriter outStream;
	private final BufferedReader inStream;
	private GameObject myGame;
	private HandObject myHand;

	public BasicSafeProportionalPlayer_1(PrintWriter output, BufferedReader input) {
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
					String action = playerLogic(msg);
					outStream.println(action);
				} else if ("NEWGAME".compareToIgnoreCase(packetType) == 0) {

					myGame = new GameObject(input);

				} else if ("NEWHAND".compareToIgnoreCase(packetType) == 0) {
					
					myHand = new HandObject(input);

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

	private final float betStrength = 2.0f;
	public int makeProportionalBet(float expectedWinPercentage, int minBet, int maxBet, int currStackSize ){
		//return (int) ((expectedWinPercentage - .5) * (maxBet - minBet) * (myGame.stackSize / currStackSize) * betStrength);
		return (int) ( 2 * (expectedWinPercentage - .5) * (maxBet - minBet) + minBet);
	}
	
	public String playerLogic( GetActionObject curr ) {
		int numBoardCards = curr.boardCards.length;

		switch ( numBoardCards ) {
			//PREFLOP
			case 0:
				float winChance0 = PreflopTableGen.getPreflopWinRate(myHand.cards3[1],myHand.cards3[2]);
				float winChance1 = PreflopTableGen.getPreflopWinRate(myHand.cards3[0],myHand.cards3[2]);
				float winChance2 = PreflopTableGen.getPreflopWinRate(myHand.cards3[0],myHand.cards3[1]);
				float p = winChance0;
				if ( winChance1 > p )
					p = winChance1;
				else if ( winChance2 > p )
					p = winChance2;
				if ( p > 0.4 )
					return betRaiseCall(curr,p);
				else
					return foldOrCheck(curr);
				
			//FLOP
			case 3:
				for ( int i = 0; i < curr.legalActions.length; i++ ) {
					ActionObject action = curr.legalActions[i];
					if ( action.actionType.equalsIgnoreCase("discard") ) {
						return "DISCARD:"+HandEvaluator.cardToString(myHand.cards3[0]);
					}
				}
				winChance0 = PreflopTableGen.getPreflopWinRate(myHand.cards3[1],myHand.cards3[2]);
				winChance1 = PreflopTableGen.getPreflopWinRate(myHand.cards3[0],myHand.cards3[2]);
				winChance2 = PreflopTableGen.getPreflopWinRate(myHand.cards3[0],myHand.cards3[1]);
				p = winChance0;
				if ( winChance1 > p )
					p = winChance1;
				else if ( winChance2 > p )
					p = winChance2;
				if ( p > 0.4 )
					return betRaiseCall(curr,p);
				else
					return foldOrCheck(curr);
				
			//TURN
			case 4:
				winChance0 = PreflopTableGen.getPreflopWinRate(myHand.cards3[1],myHand.cards3[2]);
				winChance1 = PreflopTableGen.getPreflopWinRate(myHand.cards3[0],myHand.cards3[2]);
				winChance2 = PreflopTableGen.getPreflopWinRate(myHand.cards3[0],myHand.cards3[1]);
				p = winChance0;
				if ( winChance1 > p )
					p = winChance1;
				else if ( winChance2 > p )
					p = winChance2;
				if ( p > 0.4 )
					return betRaiseCall(curr,p);
				else
					return foldOrCheck(curr);
			
			//RIVER
			case 5:
				winChance0 = PreflopTableGen.getPreflopWinRate(myHand.cards3[1],myHand.cards3[2]);
				winChance1 = PreflopTableGen.getPreflopWinRate(myHand.cards3[0],myHand.cards3[2]);
				winChance2 = PreflopTableGen.getPreflopWinRate(myHand.cards3[0],myHand.cards3[1]);
				p = winChance0;
				if ( winChance1 > p )
					p = winChance1;
				else if ( winChance2 > p )
					p = winChance2;
				if ( p > 0.4 )
					return betRaiseCall(curr,p);
				else
					return foldOrCheck(curr);
			default:
				break;
		}
		
		return "FOLD-BAD";
	}
	
	public String betRaiseCall( GetActionObject curr, float winChance ) {
		for ( int i = 0; i < curr.legalActions.length; i++ ) {
			ActionObject action = curr.legalActions[i];
		
			if ( action.actionType.equalsIgnoreCase("bet") ) {
				int min = action.minBet;
				int max = action.maxBet;
				int bet = makeProportionalBet(winChance,min,max,curr.potSize/2);
				return "BET:"+bet;
			}
			else if ( action.actionType.equalsIgnoreCase("call") ) {
				return "CALL";
			}
		}
		return "FOLD";
	}
	
	public String foldOrCheck( GetActionObject curr ) {
		for ( int i = 0; i < curr.legalActions.length; i++ ) {
			ActionObject action = curr.legalActions[i];
			if ( action.actionType.equalsIgnoreCase("check") ) {
				return "CHECK";
			}
		}
		return "FOLD";
	}
}