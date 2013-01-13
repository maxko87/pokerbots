package pokerbots.player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import pokerbots.packets.ActionObject;
import pokerbots.packets.GameObject;
import pokerbots.packets.GetActionObject;
import pokerbots.packets.HandObject;
import pokerbots.utils.BettingBrain;
import pokerbots.utils.HandEvaluator;
import pokerbots.utils.PreflopTableGen;
import pokerbots.utils.StatAggregator;
import pokerbots.utils.StatAggregator.OpponentStats;
import pokerbots.utils.StochasticSimulator;
import pokerbots.utils.Utils;


/**
 * Simple example pokerbot, written in Java.
 * 
 * This is an example of a bare bones, pokerbot. It only sets up the socket
 * necessary to connect with the engine and then always returns the same action.
 * It is meant as an example of how a pokerbot should communicate with the
 * engine.
 * 
 */
public class LearningPlayer_3 {
	
	//number of iterations for our simulator to calculate probabilities before deciding which card to toss.
	private final int DISCARD_SIM_ITERS = 500;
	//number of iterations for calculating probabilities after each other street 
	private final int FLOP_SIM_ITERS = 500;
	private final int TURN_SIM_ITERS = 300;
	private final int RIVER_SIM_ITERS = 200;
	//minimum default percentage range of winning to play each street.
	private final float[][] MIN_WIN_TO_PLAY = new float[][] {{0.3f, 0.6f}, {0.3f, 0.6f}, {0.3f, 0.6f}, {0.3f, 0.6f}};
	//scaling for larger bets on later streets
	private final float[] CONTINUATION_FACTORS = new float[] {1.0f, 1.0f, 1.5f, 2.0f};
	
	private final PrintWriter outStream;
	private final BufferedReader inStream;
	private GameObject myGame;
	private HandObject myHand;
	private BettingBrain brain;
	private StatAggregator aggregator;
	private OpponentStats opponent;

	public LearningPlayer_3(PrintWriter output, BufferedReader input) {
		this.outStream = output;
		this.inStream = input;
		brain = new BettingBrain();
		aggregator = new StatAggregator(); // TODO: initialize?
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
					opponent = aggregator.getOrCreateOpponent(myGame.oppName);
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
	
	public String playerLogic( GetActionObject getActionObject) {
		int numBoardCards = getActionObject.boardCards.length;

		switch ( numBoardCards ) {
			//PREFLOP
			case 0:
				int street_num = 0;
				float winChance0 = PreflopTableGen.getPreflopWinRate(myHand.cards3[1],myHand.cards3[2]);
				float winChance1 = PreflopTableGen.getPreflopWinRate(myHand.cards3[0],myHand.cards3[2]);
				float winChance2 = PreflopTableGen.getPreflopWinRate(myHand.cards3[0],myHand.cards3[1]);
				float winChance = Utils.getMax(winChance0, winChance1, winChance2);
				if ( winChance > getMinWinChance(street_num) )
					return betRaiseCall(getActionObject,winChance);
				else
					return foldOrCheck(getActionObject);
				
			//FLOP
			case 3:
				street_num = 1;
				for ( int i = 0; i < getActionObject.legalActions.length; i++ ) {
					ActionObject action = getActionObject.legalActions[i];
					if ( action.actionType.equalsIgnoreCase("discard") ) {
						return discardHelper(getActionObject);
					}
				}
				
				winChance2 = StochasticSimulator.computeRates(new int[] {myHand.cards3[0], myHand.cards3[1]}, getActionObject.boardCards, FLOP_SIM_ITERS)[10];
				winChance1 = StochasticSimulator.computeRates(new int[] {myHand.cards3[0], myHand.cards3[2]}, getActionObject.boardCards, FLOP_SIM_ITERS)[10];
				winChance0 = StochasticSimulator.computeRates(new int[] {myHand.cards3[1], myHand.cards3[2]}, getActionObject.boardCards, FLOP_SIM_ITERS)[10];
				float maxChance = Utils.getMax(winChance0, winChance1, winChance2);
				if ( maxChance > getMinWinChance(street_num) )
					return betRaiseCall(getActionObject, maxChance);
				else
					return foldOrCheck(getActionObject);
				
			//TURN
			case 4:
				street_num = 2;
				winChance = StochasticSimulator.computeRates(myHand.cards2, getActionObject.boardCards, TURN_SIM_ITERS)[10];
				if ( winChance > getMinWinChance(street_num) )
					return betRaiseCall(getActionObject, winChance);
				else
					return foldOrCheck(getActionObject);
			
			//RIVER
			case 5:
				street_num = 3;
				winChance = StochasticSimulator.computeRates(myHand.cards2, getActionObject.boardCards, RIVER_SIM_ITERS)[10];
				if ( winChance > getMinWinChance(street_num) )
					return betRaiseCall(getActionObject, winChance);
				else
					return foldOrCheck(getActionObject);
			default:
				break;
		}
		
		return "FOLD-BAD";
	}
	
	//decides which card to discard, and populates new cards2 in myHand
	private String discardHelper(GetActionObject getActionObject) {
		
		float p01 = StochasticSimulator.computeRates(new int[] {myHand.cards3[0], myHand.cards3[1]}, getActionObject.boardCards, DISCARD_SIM_ITERS)[10];
		float p12 = StochasticSimulator.computeRates(new int[] {myHand.cards3[1], myHand.cards3[2]}, getActionObject.boardCards, DISCARD_SIM_ITERS)[10];
		float p02 = StochasticSimulator.computeRates(new int[] {myHand.cards3[0], myHand.cards3[2]}, getActionObject.boardCards, DISCARD_SIM_ITERS)[10];
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

	// uses opponent's aggression to scale our looseness -- higher opp aggression = we play tighter
	public float getMinWinChance(int street){
		return Utils.scale(opponent.getAggression(street, myGame.stackSize), 0, 100, MIN_WIN_TO_PLAY[street][0], MIN_WIN_TO_PLAY[street][1]);
	}
	
	// TODO: uses opponent's looseness to scale our bets -- higher opp looseness = we play more aggressively
	public String betRaiseCall( GetActionObject getActionObject, float winChance ) {
		for ( int i = 0; i < getActionObject.legalActions.length; i++ ) {
			ActionObject action = getActionObject.legalActions[i];
		
			if ( action.actionType.equalsIgnoreCase("bet") ) {
				int min = action.minBet;
				int max = action.maxBet;
				int bet = (int)(brain.makeProportionalBet(winChance,min,max,getActionObject.potSize/2));
				return "BET:"+bet;
			}
			else if ( action.actionType.equalsIgnoreCase("call") ) {
				return "CALL";
			}
		}
		return "FOLD";
	}
	
	public String foldOrCheck( GetActionObject getActionObject ) {
		for ( int i = 0; i < getActionObject.legalActions.length; i++ ) {
			ActionObject action = getActionObject.legalActions[i];
			if ( action.actionType.equalsIgnoreCase("check") ) {
				return "CHECK";
			}
		}
		return "FOLD";
	}
}