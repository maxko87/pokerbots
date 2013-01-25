package pokerbots.utils;

import java.util.ArrayList;
import java.util.List;

import pokerbots.packets.PerformedActionObject;

public class MatchHistory {
	public List<Round> rounds = new ArrayList<Round>();
	private Round current = new Round();
	private int roundId = 0;
	
	public class Round {
		public List<PerformedActionObject> actions = new ArrayList<PerformedActionObject>();
		
		public void printRound() {
			System.out.println("-------\n--------");
			System.out.println("NEW ROUND (Hand #" + roundId + ")");
			
			for ( int i = 0; i < actions.size(); i++ ) {
				PerformedActionObject action = actions.get(i);
				System.out.println( action.actionType + " (" + action.amount+") : " + action.actor);
			}
		}
	}
	
	public MatchHistory() {
	}
	
	public void newRound(int r) {
		current = new Round();
		roundId = r;
	}
	
	public Round getCurrentRound() {
		return current;
	}
	
	public void appendRoundData( PerformedActionObject[] actions ) {
		for ( int i = 0; i < actions.length; i++ ) {
			current.actions.add(actions[i]);
		}
	}
	
	public void saveRoundData() {
		rounds.add(current);
	}
}
