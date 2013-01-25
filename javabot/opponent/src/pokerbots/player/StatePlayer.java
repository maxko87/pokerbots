package pokerbots.player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import pokerbots.packets.GameObject;
import pokerbots.packets.GetActionObject;
import pokerbots.packets.HandObject;
import pokerbots.packets.HandOverObject;
import pokerbots.packets.LegalActionObject;
import pokerbots.states.DecisionState;
import pokerbots.states.ResponseObject;
import pokerbots.utils.BasicPredictorState;
import pokerbots.utils.BettingBrain_old_v2;
import pokerbots.utils.HandEvaluator;
import pokerbots.utils.MatchHistory;
import pokerbots.utils.PreflopTableGen;
import pokerbots.utils.StatAggregator;
import pokerbots.utils.StatAggregator.OpponentStats;
import pokerbots.utils.StochasticSimulator;
import pokerbots.utils.Utils;


public class StatePlayer {
	
	private final PrintWriter outStream;
	private final BufferedReader inStream;
	
	private int potSize;
	private DecisionState decisionState;
	private GameObject myGame;
	private HandObject myHand;
	private MatchHistory history;
	private StatAggregator aggregator;
	private OpponentStats opponentStats;

	public StatePlayer(PrintWriter output, BufferedReader input, DecisionState init) {
		this.outStream = output;
		this.inStream = input;
		this.decisionState = init;
		
		potSize = 0;
		aggregator = new StatAggregator();
		history = new MatchHistory();
	}
	
	public void run() {
		String input;
		try {
			while ((input = inStream.readLine()) != null) {
				System.out.println("Input: " + input);
				String packetType = input.split(" ")[0];
				
				if ("GETACTION".compareToIgnoreCase(packetType) == 0) {
					GetActionObject msg = new GetActionObject(input);
					potSize = msg.potSize;
					
					//Store match history
					history.appendRoundData(msg.lastActions);
										
					//Call state decision makers
					int boardCards = msg.boardCards.length;
					ResponseObject RObj = null;
					if ( boardCards==0 )
						RObj = decisionState.preflop(myGame, myHand, msg);
					else if ( boardCards==3 ) {
						if ( msg.legalActions[0].actionType.equalsIgnoreCase("discard") ) {
							RObj = decisionState.discard(myGame, myHand, msg);
						} else {
							RObj = decisionState.flop(myGame, myHand, msg);
						}
					}
					else if ( boardCards==4 )
						RObj = decisionState.turn(myGame, myHand, msg);
					else if ( boardCards==5 )
						RObj = decisionState.river(myGame, myHand, msg);
					
					//Send response and advance state
					System.out.println("RESPOND: " + RObj.toString() );
					decisionState = RObj.getNextState();
					outStream.println(RObj.toString());
				} else if ("NEWGAME".compareToIgnoreCase(packetType) == 0) {
					myGame = new GameObject(input);
					opponentStats = aggregator.getOrCreateOpponent(myGame.oppName, myGame.stackSize);
					decisionState.initialize(this);
				} else if ("NEWHAND".compareToIgnoreCase(packetType) == 0) {
					myHand = new HandObject(input);
					history.newRound(myGame.stackSize);
					potSize = 0;
				} else if ("HANDOVER".compareToIgnoreCase(packetType) == 0) {
					HandOverObject HOobj = new HandOverObject(input);
					history.appendRoundData(HOobj.lastActions);
					opponentStats.analyzeRoundData(myGame, myHand, history.getCurrentRound(), potSize);
					history.saveRoundData();
					history.getCurrentRound().printRound();
					opponentStats.printStats(myGame);
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
	
	public OpponentStats getOpponentStats() { return opponentStats; }
	
}