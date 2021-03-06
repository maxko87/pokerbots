package pokerbots.player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import pokerbots.brains.FourStyleBrain;
import pokerbots.brains.GenericBrain;
import pokerbots.brains.SimpleBrain;
import pokerbots.packets.GameObject;
import pokerbots.packets.GetActionObject;
import pokerbots.packets.HandObject;
import pokerbots.packets.HandOverObject;
import pokerbots.packets.LegalActionObject;
import pokerbots.utils.HandEvaluator;
import pokerbots.utils.MatchHistory;
import pokerbots.utils.PreflopTableGen;
import pokerbots.utils.StatAggregator;
import pokerbots.utils.StatAggregator.OpponentStats;
import pokerbots.utils.StochasticSimulator;
import pokerbots.utils.Utils;


/**
 * Improvements made:
 * - switches between brains
 * 
 * Todo:
 * - ROUND ANALYSIS WTF
 * - 
 * 
 */
public class FourStylePlayer_6 {
	
	//number of iterations for our simulator to calculate probabilities before deciding which card to toss.
	private final int DISCARD_SIM_ITERS = 1000;
	//number of iterations for calculating probabilities after each other street.
	private final int FLOP_SIM_ITERS = 1000;
	private final int TURN_SIM_ITERS = 700;
	private final int RIVER_SIM_ITERS = 700;
	
	private final PrintWriter outStream;
	private final BufferedReader inStream;
	private GameObject myGame;
	private HandObject myHand;
	private StatAggregator aggregator;
	private OpponentStats opponent;
	private MatchHistory history;
	private int potSize;

	private GenericBrain brain;

	public FourStylePlayer_6(PrintWriter output, BufferedReader input) {
		this.outStream = output;
		this.inStream = input;
		aggregator = new StatAggregator(); // TODO: initialize with past data?
		history = new MatchHistory();
		potSize = 0;
	}
	
	public void run() {
		String input;
		try {
			while ((input = inStream.readLine()) != null) {
				System.out.println(input);
				String packetType = input.split(" ")[0];
				
				if ("GETACTION".compareToIgnoreCase(packetType) == 0) {
					GetActionObject msg = new GetActionObject(input);
					potSize = msg.potSize;
					history.setStreetData(msg);
					history.appendRoundData(msg.lastActions);
					String action = respondToGetAction(msg);
					outStream.println(action);
					
				} else if ("NEWGAME".compareToIgnoreCase(packetType) == 0) {
					myGame = new GameObject(input);
					//instantiate opponent
					opponent = aggregator.getOrCreateOpponent(myGame);
					//choose initial brain
					brain = new FourStyleBrain(history, myGame);
				} else if ("NEWHAND".compareToIgnoreCase(packetType) == 0) {
					myHand = new HandObject(input);
					history.newRound(myHand.handId,myGame.oppName);
					potSize = 0;
					
				} else if ("HANDOVER".compareToIgnoreCase(packetType) == 0) {
					//store analytics from current round
					HandOverObject HOobj = new HandOverObject(input);
					history.appendRoundData(HOobj.lastActions);
					opponent.analyzeRoundData(myHand, history.getCurrentRound());
					history.saveRoundData();
					history.getCurrentRound().printRound();
					//opponent.printStats(myGame);
					
				}else if ("KEYVALUE".compareToIgnoreCase(packetType) == 0) {
					//none
					
				} else if ("REQUESTKEYVALUES".compareToIgnoreCase(packetType) == 0) {

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
	
	public String respondToGetAction( GetActionObject getActionObject) {
		int numBoardCards = getActionObject.boardCards.length;
		float winChance0;
		float winChance1;
		float winChance2;
		float winChance;

		switch ( numBoardCards ) {
			//PREFLOP
			case 0:
				int street = 0;
				winChance0 = PreflopTableGen.getPreflopWinRate(myHand.cards3[1],myHand.cards3[2]);
				winChance1 = PreflopTableGen.getPreflopWinRate(myHand.cards3[0],myHand.cards3[2]);
				winChance2 = PreflopTableGen.getPreflopWinRate(myHand.cards3[0],myHand.cards3[1]);
				winChance = Utils.getMax(winChance0, winChance1, winChance2);
				
				return brain.takeAction(opponent, getActionObject, winChance, street);
				
			//FLOP
			case 3:
				street = 1;
				for ( int i = 0; i < getActionObject.legalActions.length; i++ ) {
					LegalActionObject action = getActionObject.legalActions[i];
					if ( action.actionType.equalsIgnoreCase("discard") ) {
						return discardHelper(getActionObject);
					}
				}
				
				winChance2 = StochasticSimulator.computeRates(new int[] {myHand.cards3[0], myHand.cards3[1]}, getActionObject.boardCards, FLOP_SIM_ITERS)[10];
				winChance1 = StochasticSimulator.computeRates(new int[] {myHand.cards3[0], myHand.cards3[2]}, getActionObject.boardCards, FLOP_SIM_ITERS)[10];
				winChance0 = StochasticSimulator.computeRates(new int[] {myHand.cards3[1], myHand.cards3[2]}, getActionObject.boardCards, FLOP_SIM_ITERS)[10];
				winChance = Utils.getMax(winChance0, winChance1, winChance2);
				
				return brain.takeAction(opponent, getActionObject, winChance, street);
			
				
			//TURN
			case 4:
				street = 2;
				winChance = StochasticSimulator.computeRates(myHand.cards2, getActionObject.boardCards, TURN_SIM_ITERS)[10];
				
				return brain.takeAction(opponent, getActionObject, winChance, street);
				
			//RIVER
			case 5:
				street = 3;
				winChance = StochasticSimulator.computeRates(myHand.cards2, getActionObject.boardCards, RIVER_SIM_ITERS)[10];
				
				return brain.takeAction(opponent, getActionObject, winChance, street);
				
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
	
}