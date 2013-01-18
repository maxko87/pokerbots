package pokerbots.utils;

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
	
	public static int stringToCard( String str ) {
		// club, spade, diamond, heart
		char rank = str.charAt(0);
		char suit = str.charAt(1);
		
		int R = (int)(rank-'2');
		if ( rank=='T' )
			R = 8;
		else if ( rank=='J' )
			R = 9;
		else if ( rank=='Q' )
			R = 10;
		else if ( rank=='K' )
			R = 11;
		else if ( rank=='A' )
			R = 12;
		
		int S = 0;
		if ( suit=='s' )
			S = 1;
		else if ( suit=='d' )
			S = 2;
		else if ( suit=='h' )
			S = 3;
		
		return (S<<4) | R;
	}
	
	private static char[] ranks = new char[]{
		'2','3','4','5','6','7','8','9','T','J','Q','K','A'
	};
	
	private static char[] suits = new char[]{
		'c','s','d','h'	
	};
	
	public static String cardToString( int card ) {
		// club, spade, diamond, heart
		int rank = card&0xf;
		int suit = card>>4;
		
		return ranks[rank] + "" + suits[suit];
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
			if ( ranks[c]>0 )
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
