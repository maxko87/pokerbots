package pokerbots.utils;

import pokerbots.packets.GameObject;
import pokerbots.packets.GetActionObject;
import pokerbots.packets.LegalActionObject;
import pokerbots.utils.StatAggregator.OpponentStats;


public class BettingBrain_original {
	
	float val1 = 0.2f;
	float val2 = 0.5f;
	float val3 = 0.2f;
	float val4 = 0.5f;
	float val5 = 0.4f;
	float val6 = 0.5f;
	float val7 = 0.4f;
	float val8 = 0.5f;
	
	float val9 = 0.2f;
	float val10 = 0.2f;
	float val11 = 0.3f;
	float val12 = 0.4f;
	
	float val13 = 0.4f;
	float val14 = 0.7f;
	float val15 = 0.8f;
	float val16 = 1.0f;
	
	//minimum default percentage range of winning to play/raise each street.
	//private final float[][] MIN_WIN_TO_PLAY = new float[][] {{.2f, .5f}, {.2f, .5f}, {.4f, .5f}, {.4f, .5f}};
	private final float[][] MIN_WIN_TO_PLAY = new float[][] {{val1, val2}, {val3, val4}, {val5, val6}, {val7, val8}};
	private final float[] MIN_WIN_TO_RAISE = new float[] {.9f, .9f, .9f, .7f};
	
	//scaling for larger bets on later streets.
	//private final float[] CONTINUATION_FACTORS = new float[] {1.0f, 1.0f, 1.5f, 2.0f};
	
	//minimum/maximum percentages of pot we should be betting during value bets
	//private final float[] MIN_OF_POT = new float[] {.2f, .2f, .3f, .4f};
	private final float[] MIN_OF_POT = new float[] {val9, val10, val11, val12};
	//private final float[] MAX_OF_POT = new float[] {1.0f, 1.0f, .8f, .5f};
	
	//minimum/maximum percentages of our stack we should be betting during value bets
	//private final float[] MIN_OF_POT = new float[] {.2f, .2f, .2f, .3f};
	//private final float[] MAX_OF_STACK = new float[] {.4f, .7f, .8f, 1.0f};
	private final float[] MAX_OF_STACK = new float[] {val13, val14, val15, val16};



}
