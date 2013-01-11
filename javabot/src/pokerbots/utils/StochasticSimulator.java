package pokerbots.utils;

public class StochasticSimulator {
	private static HandEvaluator he = new HandEvaluator();
	
	public static void main ( String[] args ) {
		StochasticSimulator SS = new StochasticSimulator();
		long time = System.currentTimeMillis();
		float[] rates = SS.computeRates(
				new int[]{
						0x1a,
						0x0a
				},
				new int[]{
				},
				300);
		
		for ( int i = 0; i < 9; i++ ) {
			System.out.println("Hand Type ("+i+"): " + rates[i] );
		}
		
		System.out.println("Win rate: " + rates[10] );
		System.out.println("RUNTIME: " + (System.currentTimeMillis()-time) +" ms");
	}
	
	public StochasticSimulator() {
	}
	
	public static float[] computeRates( int[] myHand, int[] table, int iters ) {
		float[] handTypes = new float[11];
		int rounds = 0;
		int win = 0;
		
		//Determine what needs to be simulated
		int[] hisHand = new int[2];
		int[] simTable = new int[5];
		for ( int i = 0; i < table.length; i++ )
			simTable[i] = table[i];
		
		for ( int trials = 0; trials < iters; trials++ ) {
			//fill out the table and opponent hand
			for ( int i = table.length; i < 5; i++ ) {
				int suit = (int)(Math.random()*3.99);
				int rank = (int)(Math.random()*12.99);
				int card = suit<<4 | rank;
				simTable[i] = card;
			}
			for ( int i = 0; i < 2; i++ ) {
				int suit = (int)(Math.random()*3.99);
				int rank = (int)(Math.random()*12.99);
				int card = suit<<4 | rank;
				hisHand[i] = card;
			}
			
			//find my best hand.
			long myBestHand = getBestHand(myHand,simTable);
			long hisBestHand = getBestHand(hisHand,simTable);
			
			int type = (int)(myBestHand>>39);
			handTypes[type]++;
			
			if ( myBestHand > hisBestHand )
				win++;
			rounds++;
		}
		
		for ( int i = 0; i < handTypes.length; i++ )
			handTypes[i] /= rounds;
		
		handTypes[10] = ((float)win)/rounds;
		return handTypes;
	}
	
	public static long getBestHand( int[] hand, int[] table  ) {
		if ( table.length==3 ) {
			return he.evaluate( new int[]{hand[0],hand[1],table[0],table[1],table[2]} );
		} else if ( table.length==4 ) {
			return he.evaluate( new int[]{hand[0],hand[1],table[0],table[1],table[2],table[3]} );
		} else if ( table.length==5 ) {
			return he.evaluate( new int[]{hand[0],hand[1],table[0],table[1],table[2],table[3],table[4]} );
		}
		return 0;
		/*
		else if ( table.length==4 ) {
			long best = 0;
			int[] select = new int[]{hand[0],hand[1],table[0],table[1],table[2],table[3]};
			int[] setup = new int[5];
			for ( int i = 0; i < select.length; i++ ) {
				int idx = 0;
				for ( int q = 0; q < select.length; q++ ) {
					if ( q==i )
						continue;
					setup[idx] = select[q];
					idx++;
				}
				long test = he.evaluate( setup );
				if ( test>best )
					best = test;
			}
			return best;
		}
		else if ( table.length==5 ) {
			long best = 0;
			int[] select = new int[]{hand[0],hand[1],table[0],table[1],table[2],table[3],table[4]};
			int[] setup = new int[5];
			for ( int i = 0; i < select.length; i++ ) {
			for ( int j = i+1; j < select.length; j++ ) {
				int idx = 0;
				for ( int q = 0; q < select.length; q++ ) {
					if ( q==i | q==j )
						continue;
					setup[idx] = select[q];
					idx++;
				}
				long test = he.evaluate( setup );
				if ( test>best )
					best = test;
			}
			}
			return best;
		}
		return 0;*/
	}
}
