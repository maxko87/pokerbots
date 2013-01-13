package pokerbots.player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import pokerbots.packets.LegalActionObject;
import pokerbots.packets.GetActionObject;
import pokerbots.packets.GameObject;
import pokerbots.packets.HandObject;
import pokerbots.utils.HandEvaluator;
import pokerbots.utils.Utils;
import pokerbots.utils.PreflopTableGen;
import pokerbots.utils.StochasticSimulator;


/**
 * Simple example pokerbot, written in Java.
 * 
 * This is an example of a bare bones, pokerbot. It only sets up the socket
 * necessary to connect with the engine and then always returns the same action.
 * It is meant as an example of how a pokerbot should communicate with the
 * engine.
 * 
 */
public class ProbabilityCalculatingPlayer_2 {
	
	//number of iterations for our simulator to calculate probabilities before deciding which card to toss.
	private final int DISCARD_SIM_ITERS = 500;
	//number of iterations for calculating probabilities after each other street 
	private final int FLOP_SIM_ITERS = 500;
	private final int TURN_SIM_ITERS = 300;
	private final int RIVER_SIM_ITERS = 200;
	//minimum estimated percentage of winning to play each street.
	private final float[] MIN_WIN_TO_PLAY = new float[] {0.3f, 0.3f, 0.4f, 0.4f};
	//scaling for larger bets on later streets
	private final float[] CONTINUATION_FACTORS = new float[] {1.0f, 1.0f, 1.5f, 2.0f};
	
	private final PrintWriter outStream;
	private final BufferedReader inStream;
	private GameObject myGame;
	private HandObject myHand;

	public ProbabilityCalculatingPlayer_2(PrintWriter output, BufferedReader input) {
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
					//TODO: no learning yet
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
				int street_num = 0;
				float winChance0 = PreflopTableGen.getPreflopWinRate(myHand.cards3[1],myHand.cards3[2]);
				float winChance1 = PreflopTableGen.getPreflopWinRate(myHand.cards3[0],myHand.cards3[2]);
				float winChance2 = PreflopTableGen.getPreflopWinRate(myHand.cards3[0],myHand.cards3[1]);
				float winChance = Utils.getMax(winChance0, winChance1, winChance2);
				if ( winChance > MIN_WIN_TO_PLAY[street_num] )
					return betRaiseCall(curr,winChance);
				else
					return foldOrCheck(curr);
				
			//FLOP
			case 3:
				street_num = 1;
				for ( int i = 0; i < curr.legalActions.length; i++ ) {
					LegalActionObject action = curr.legalActions[i];
					if ( action.actionType.equalsIgnoreCase("discard") ) {
						return discardHelper(curr);
					}
				}
				
				winChance2 = StochasticSimulator.computeRates(new int[] {myHand.cards3[0], myHand.cards3[1]}, curr.boardCards, FLOP_SIM_ITERS)[10];
				winChance1 = StochasticSimulator.computeRates(new int[] {myHand.cards3[0], myHand.cards3[2]}, curr.boardCards, FLOP_SIM_ITERS)[10];
				winChance0 = StochasticSimulator.computeRates(new int[] {myHand.cards3[1], myHand.cards3[2]}, curr.boardCards, FLOP_SIM_ITERS)[10];
				float maxChance = Utils.getMax(winChance0, winChance1, winChance2);
				if ( maxChance > MIN_WIN_TO_PLAY[street_num] )
					return betRaiseCall(curr, maxChance);
				else
					return foldOrCheck(curr);
				
			//TURN
			case 4:
				street_num = 2;
				winChance = StochasticSimulator.computeRates(myHand.cards2, curr.boardCards, TURN_SIM_ITERS)[10];
				if ( winChance > MIN_WIN_TO_PLAY[street_num] )
					return betRaiseCall(curr, winChance);
				else
					return foldOrCheck(curr);
			
			//RIVER
			case 5:
				street_num = 3;
				winChance = StochasticSimulator.computeRates(myHand.cards2, curr.boardCards, RIVER_SIM_ITERS)[10];
				if ( winChance > MIN_WIN_TO_PLAY[street_num] )
					return betRaiseCall(curr, winChance);
				else
					return foldOrCheck(curr);
			default:
				break;
		}
		
		return "FOLD-BAD";
	}
	
	//decides which card to discard, and populates new cards2 in myHand
	private String discardHelper(GetActionObject curr) {
		
		float p01 = StochasticSimulator.computeRates(new int[] {myHand.cards3[0], myHand.cards3[1]}, curr.boardCards, DISCARD_SIM_ITERS)[10];
		float p12 = StochasticSimulator.computeRates(new int[] {myHand.cards3[1], myHand.cards3[2]}, curr.boardCards, DISCARD_SIM_ITERS)[10];
		float p02 = StochasticSimulator.computeRates(new int[] {myHand.cards3[0], myHand.cards3[2]}, curr.boardCards, DISCARD_SIM_ITERS)[10];
		float max = p01;
		int toss = 2;
		if (p12 > max){
			max = p12;
			toss = 0;
		}
		if (p02 > max){
			max = p02;
			toss = 1;
		}
		myHand.remove(toss);
		return "DISCARD:"+HandEvaluator.cardToString(myHand.cards3[toss]);
		
	}

	public String betRaiseCall( GetActionObject curr, float winChance ) {
		for ( int i = 0; i < curr.legalActions.length; i++ ) {
			LegalActionObject action = curr.legalActions[i];
		
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
			LegalActionObject action = curr.legalActions[i];
			if ( action.actionType.equalsIgnoreCase("check") ) {
				return "CHECK";
			}
		}
		return "FOLD";
	}
}