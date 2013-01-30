package pokerbots.utils;

import java.util.ArrayList;
import java.util.List;

import pokerbots.packets.GetActionObject;
import pokerbots.packets.PerformedActionObject;

public class MatchHistory {
	public List<Round> rounds = new ArrayList<Round>();
	private Round current = new Round();
	private int roundId = 0;
	
	public class Round {
		public String opponentName = "NOT SET!";
		public int id = 0;
		public List<PerformedActionObject> actions = new ArrayList<PerformedActionObject>();
		public int[][] boardCards = new int[][]{
				{},{0,0,0},{0,0,0,0},{0,0,0,0,0}	
		};
		public int[] potSizes = new int[4];
		public int[] oppCards = null;
		
		public boolean showdown = false;
		/*
		public float[] ratesPF;
		public float[] ratesFLOP;
		public float[] ratesTURN;
		public float[] ratesRIVER;
		*/
		public float[] oppWinRates;
		public int[] oppAmounts;
		
		public void printRound() {
			System.out.println("-------\n");
			System.out.println("NEW ROUND (Hand #" + roundId + ")");
			
			for ( int i = 0; i < actions.size(); i++ ) {
				PerformedActionObject action = actions.get(i);
				System.out.println( action.actionType + " (" + action.amount+") : " + action.actor);
			}
		}
				
		public void analyzeRound() {
			showdown = true;
			float[] ratesPF = PreflopTableGen.getPreflopWinRates(oppCards[0],oppCards[1]);
			float[] ratesFLOP = StochasticSimulator.computeRates(oppCards, boardCards[1], 800);
			float[] ratesTURN = StochasticSimulator.computeRates(oppCards, boardCards[2], 500);
			float[] ratesRIVER = StochasticSimulator.computeRates(oppCards, boardCards[3], 500);
			oppWinRates = new float[]{ratesPF[10],ratesFLOP[10],ratesTURN[10],ratesRIVER[10]};
			oppAmounts = new int[4];
			
			//System.out.println("Round Analysis:");
			
			int prevPot = 0;
			int street = 0;
			int wager = 0;
			for ( int i = 0; i < actions.size(); i++ ) {
				PerformedActionObject action = actions.get(i);
				if ( action.actionType.equalsIgnoreCase("deal") ) {
					wager = potSizes[street]-prevPot;
					oppAmounts[street] = wager;
					prevPot = potSizes[street];
					street++;
					wager = 0;
				}
			}
			
			oppAmounts[street] = wager;
			
			for ( int i = 0; i < 4; i++ ) {
				String cards = "";
				cards += HandEvaluator.cardToString(oppCards[0])+" ";
				cards += HandEvaluator.cardToString(oppCards[1]);
				String board = "";
				for ( int j = 0; j < boardCards[i].length; j++ ) {
					board += HandEvaluator.cardToString(boardCards[i][j])+" ";
				}
				//System.out.println("Hand["+i+"] cards: { " + cards +"}, boards: { " + board + "}, wager = " + oppAmounts[i] +", winChance = " + oppWinRates[i] + ", potSize = " + potSizes[i]);
			}
		}
		
		public int[] getOppLastBetOrRaise() {
			List<PerformedActionObject> actions = current.actions;
			int street = 0;
			int value = -1;
			for ( int i = 0; i<actions.size(); i++ ) {
				PerformedActionObject ACTION = actions.get(i);
				if ( ACTION.actionType.equals("DEAL") )
					street++;
				if ( ACTION.actor.equalsIgnoreCase(current.opponentName) ) {
					if ( ACTION.actionType.equals("BET") || ACTION.actionType.equals("RAISE") ) {
						value = ACTION.amount;
					}
				}
			}
			return new int[]{street,value};
		}
	}
	
	public MatchHistory() {
	}
	
	public void newRound(int r,String opp) {
		current = new Round();
		current.opponentName = opp;
		current.id = r;
		roundId = r;
	}
	
	public Round getCurrentRound() {
		return current;
	}
	
	public void appendRoundData( PerformedActionObject[] actions ) {
		for ( int i = 0; i < actions.length; i++ ) {
			current.actions.add(actions[i]);
			if ( actions[i].actionType.equalsIgnoreCase("show") ) {
				if ( actions[i].actor.equalsIgnoreCase(current.opponentName) ) {
					current.oppCards = new int[]{actions[i].card1,actions[i].card2};
					current.analyzeRound();
				}
			}
		}
	}
	
	public void setStreetData( GetActionObject msg ) {
		//System.out.println("SET STREET SESAME: " + msg.boardCards.length );
		int idx = msg.boardCards.length-2;
		if ( idx<0 ) idx = 0;
		current.boardCards[idx] = msg.boardCards;
		current.potSizes[idx] = msg.potSize;
	}
	
	public void saveRoundData() {
		rounds.add(current);
	}
}
