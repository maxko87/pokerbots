package pokerbots.utils;

public class StochasticSimulator {
	private static HandEvaluator he = new HandEvaluator();
	private static String[] handNames = new String[]{
		"High Card ",
		"Pair      ",
		"Two Pair  ",
		"Triple    ",
		"Straight  ",
		"Flush     ",
		"Full House",
		"Four      ",
		"Str. Flush"
	};
	
	public static void main ( String[] args ) {
		StochasticSimulator SS = new StochasticSimulator();
		long time = System.currentTimeMillis();
		//
		// [3h 6h 5h 4d] [6s]
		// [9h 7h]
		//
		float[] rates = StochasticSimulator.computeRates(
				new int[]{
						HandEvaluator.stringToCard("8s"),
						HandEvaluator.stringToCard("9h")
				},
				new int[]{
						HandEvaluator.stringToCard("9d"),
						HandEvaluator.stringToCard("9s"),
						HandEvaluator.stringToCard("Td"),
						HandEvaluator.stringToCard("3d")
				},
				5000);
		time = System.currentTimeMillis();

		rates = StochasticSimulator.computeRates(
				new int[]{
						HandEvaluator.stringToCard("As"),
						HandEvaluator.stringToCard("Ac")
				},
				new int[]{
						HandEvaluator.stringToCard("8d"),
						HandEvaluator.stringToCard("4c"),
						HandEvaluator.stringToCard("4d"),
						HandEvaluator.stringToCard("Ks"),
						HandEvaluator.stringToCard("Ad")
				},
				5000);

		for ( int i = 0; i < 9; i++ ) {
			System.out.println("Hand Type ("+handNames[i]+"): [" + rates[i] +", " + rates[i+21] +"],["+rates[i+11]+","+rates[i+31]+"]");
		}
		
		System.out.println("Win rate: " + rates[10] );
		System.out.println("RUNTIME: " + (System.currentTimeMillis()-time) +" ms");
	}
	
	public StochasticSimulator() {
	}
	
	public static float[] computeRates( int[] myTrueHand, int[] table, int iters ) {
		float[] handTypes = new float[41];
		int rounds = 0;
		int win = 0;
		
		//Determine what needs to be simulated
		int[] myHand = new int[2];
		int[] hisHand = new int[2];
		int[] simTable = new int[5];
		for ( int i = 0; i < table.length; i++ )
			simTable[i] = table[i];
		
		//use only existing cards
		int[] restricted = new int[9];
		int restrict_ptr = 0;
		for ( int i = 0; i < myTrueHand.length; i++ ) {
			myHand[i] = myTrueHand[i];
			restricted[restrict_ptr] = myTrueHand[i];
			restrict_ptr++;
		}
		for ( int i = 0; i < table.length; i++ ) {
			restricted[restrict_ptr] = table[i];
			restrict_ptr++;
		}
		int reset_ptr = restrict_ptr;
		
		for ( int trials = 0; trials < iters; trials++ ) {
			restrict_ptr = reset_ptr;
			//fill out my hand
			for ( int i = myTrueHand.length; i < 2; i++ ) {
				int suit = (int)(Math.random()*3.99);
				int rank = (int)(Math.random()*12.99);
				int card = suit<<4 | rank;
				boolean used = false;
				for( int e = 0; e < restrict_ptr; e++ ) {
					if ( restricted[e]==card ) {
						used=true;
						break;
					}
				}
				if ( used ) {
					i--;
					continue;
				}
				myHand[i] = card;
				restricted[restrict_ptr] = card;
				restrict_ptr++;
			}
			//fill out the table
			for ( int i = table.length; i < 5; i++ ) {
				int suit = (int)(Math.random()*3.99);
				int rank = (int)(Math.random()*12.99);
				int card = suit<<4 | rank;
				boolean used = false;
				for( int e = 0; e < restrict_ptr; e++ ) {
					if ( restricted[e]==card ) {
						used=true;
						break;
					}
				}
				if ( used ) {
					i--;
					continue;
				}
				simTable[i] = card;
				restricted[restrict_ptr] = card;
				restrict_ptr++;
			}
			//fill out the opponent hand
			for ( int i = 0; i < 2; i++ ) {
				int suit = (int)(Math.random()*3.99);
				int rank = (int)(Math.random()*12.99);
				int card = suit<<4 | rank;
				boolean used = false;
				for( int e = 0; e < restrict_ptr; e++ ) {
					if ( restricted[e]==card ) {
						used=true;
						break;
					}
				}
				if ( used ) {
					i--;
					continue;
				}
				hisHand[i] = card;
				restricted[restrict_ptr] = card;
				restrict_ptr++;
			}
			
			//find my best hand.
			long myBestHand = getBestHand(myHand,simTable);
			long hisBestHand = getBestHand(hisHand,simTable);
			
			int type = (int)(myBestHand>>39);
			handTypes[type]++;
			int type2 = (int)(hisBestHand>>39);
			handTypes[11+type2]++;
			
			if ( myBestHand > hisBestHand ) {
				win++;
				handTypes[type+21]++;
			} else {
				handTypes[type2+31]++;
			}
		
			rounds++;
		}		
		
		for ( int i = 0; i < 10; i++ ) {
			if ( handTypes[i]>0 )
				handTypes[21+i] = handTypes[i+21]/handTypes[i];
			else
				handTypes[21+i] = 0;
		}
		
		for ( int i = 0; i < 10; i++ ) {
			if ( handTypes[i+11]>0 )
				handTypes[31+i] = handTypes[i+31]/handTypes[i+11];
			else
				handTypes[31+i] = 0;
		}
		
		for ( int i = 0; i < 20; i++ )
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
