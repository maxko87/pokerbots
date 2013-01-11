
public class HandEvaluator {
	public static void main( String[] args ) {
		
		long start = System.currentTimeMillis();
		HandEvaluator he = new HandEvaluator();
		
		for ( int trials = 0; trials < 1000000; trials++ ) {
			int[] cards = new int[7];
			for ( int i = 0; i < cards.length; i++ ) {
				int suit = (int)(Math.random()*3.99);
				int rank = (int)(Math.random()*12.99);
				int card = suit<<4 | rank;
				cards[i] = card;
				//System.out.println(Integer.toHexString(card));
			}
			long value = he.evaluate(cards);
			//System.out.println("Hand: " + (value>>39));
		}
		
		System.out.println("Runtime: " + (System.currentTimeMillis()-start) + " ms");
	}
	
	public HandEvaluator() {
	}
	
	public long evaluate( int[] cards ) {
		long handType = 0; //type of hand
		long highCard = 0; //rank of hand for tie breakers
		long shift = 1;
		
		//High card counter
		for ( int c = 0; c < cards.length; c++ ) {
			int rank = cards[c]&0xf;
			highCard += (shift<<(rank*3));
		}
		
		//Rank counters
		int[] ranks = new int[13];
		for ( int c = 0; c < cards.length; c++ ) {
			int rank = cards[c]&0xf;
			ranks[rank]++;
			if ( ranks[rank]==2 & handType<1 )
				handType = 1;
			else if ( ranks[rank]==2 & handType==1 )
				handType = 2;
			else if ( ranks[rank]==3 & handType==1 )
				handType = 3;
			else if ( ranks[rank]==2 & handType==3 )
				handType = 6;
			else if ( ranks[rank]==3 & (handType==1 | handType==2) )
				handType = 6;
			else if ( ranks[rank]==4 )
				handType = 7;
		}
		
		//Straight checkers
		boolean hasStraight = false;
		int consecutive = 0;
		for ( int c = 0; c < ranks.length; c++ ) {
			if ( ranks[c]==1 )
				consecutive++;
			else
				consecutive=0;
			if ( consecutive==5 )
				hasStraight = true;
		}
		
		//Suit counters
		int[] suits = new int[4];
		boolean hasFlush = false;
		for ( int c = 0; c < cards.length; c++ ) {
			int suit = (cards[c]&0x30)>>4;
			suits[suit]++;
			if ( suits[suit]==5 )
				hasFlush = true;
		}
		
		if ( hasFlush & handType<5 )
			handType = 5;
		if ( hasStraight & handType<4 )
			handType = 4;	
		if ( hasFlush & hasStraight )
			handType = 8;
		
		return highCard | handType<<39;
	}
	
	/*
	public long evaluate( int[] cards ) {
		long handType = 0; //type of hand
		long highCard = 0; //rank of hand for tie breakers
		long shift = 1;
		
		//High card counter
		for ( int c = 0; c < cards.length; c++ ) {
			int rank = cards[c]&0xf;
			highCard += (shift<<(rank*3));
		}
		
		//Rank counters
		int[] ranks = new int[13];
		for ( int c = 0; c < cards.length; c++ ) {
			int rank = cards[c]&0xf;
			ranks[rank]++;
			if ( ranks[rank]==2 & handType<1 )
				handType = 1;
			else if ( ranks[rank]==2 & handType==1 )
				handType = 2;
			else if ( ranks[rank]==3 & handType==1 )
				handType = 3;
			else if ( ranks[rank]==2 & handType==3 )
				handType = 6;
			else if ( ranks[rank]==3 & (handType==1 | handType==2) )
				handType = 6;
			else if ( ranks[rank]==4 )
				handType = 7;
		}
		
		//Straight checkers
		boolean hasStraight = false;
		int consecutive = 0;
		for ( int c = 0; c < ranks.length; c++ ) {
			if ( ranks[c]==1 )
				consecutive++;
			else
				consecutive=0;
			if ( consecutive==5 )
				hasStraight = true;
		}
		
		//Suit counters
		int[] suits = new int[4];
		boolean hasFlush = false;
		for ( int c = 0; c < cards.length; c++ ) {
			int suit = (cards[c]&0x30)>>4;
			suits[suit]++;
			if ( suits[suit]==5 )
				hasFlush = true;
		}
		
		if ( hasFlush & handType<5 )
			handType = 5;
		if ( hasStraight & handType<4 )
			handType = 4;	
		if ( hasFlush & hasStraight )
			handType = 8;
		
		return highCard | handType<<39;
	}
	*/
}
