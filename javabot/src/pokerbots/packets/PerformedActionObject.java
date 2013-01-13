package pokerbots.packets;

import pokerbots.utils.HandEvaluator;

public class PerformedActionObject {
	public String actionType;
	public int amount;
	public int card1,card2;
	public String actor;

	public PerformedActionObject(String input){
		String[] values = input.split(":");
		actionType = values[0];

		//if fold,discard,deal,check,call
		if ( actionType.equalsIgnoreCase("fold") | actionType.equalsIgnoreCase("call")
			| actionType.equalsIgnoreCase("deal") | actionType.equalsIgnoreCase("deal")
			| actionType.equalsIgnoreCase("check") ) {
			if ( values.length==2 )
				actor = values[1];
		}
		else if ( actionType.equalsIgnoreCase("show") ) {
			card1 = HandEvaluator.stringToCard(values[1]);
			card2 = HandEvaluator.stringToCard(values[2]);
			if ( values.length==4 )
				actor = values[3];
		}
		else {
			amount = Integer.parseInt(values[1]);
			if ( values.length==3 )
				actor = values[2];
		}
	}
}
