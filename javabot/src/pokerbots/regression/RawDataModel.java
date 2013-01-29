package pokerbots.regression;

import java.util.HashMap;

public class RawDataModel implements Model2D {
	
	HashMap<Integer, Float> map;
	int N;
	float REG_A;
	float REG_B;
	float default_A=0.5f, default_B=0;
	String name = "Untitled";
	String xAxis = "x";
	String yAxis = "y";
	
	public RawDataModel(String name, String xAxis, String yAxis){
		map = new HashMap<Integer, Float>();
		N = 0;
		this.name = name;
		this.xAxis = xAxis;
		this.yAxis = yAxis;
		this.REG_A = default_A;
		this.REG_B = default_B;
	}

	@Override
	public void addData(float winChance, float b) {
		int bet = (int)b;
		if (map.containsKey(bet)){
			map.put(bet, (float)(map.get(bet) * .25 + winChance * .75));
		}
		else{
			map.put(bet, winChance);
		}
	}

	@Override //given a bet, returns a win chance
	public float getEstimate(float d) {
		int desiredBet = (int)d;
		if (map.size() == 0){
			return .5f;
		}
		int closestBet = 0;
		int distanceOfBet = 100000;
		for (int bet : map.keySet()){
			if (Math.abs(bet - desiredBet) < distanceOfBet){
				closestBet = bet;
				distanceOfBet = Math.abs(bet - desiredBet);
			}
		}
		return map.get(closestBet);
	}

	@Override //given a win chance, returns a bet
	public float getInverseModel(float desiredWinChance) {
		if (map.size() == 0){
			return 0;
		}
		float closestWinChance = 0;
		float distanceOfWinChance = 100000;
		for (float winChance : map.values()){
			if (Math.abs(winChance - desiredWinChance) < distanceOfWinChance){
				closestWinChance = winChance;
				distanceOfWinChance = Math.abs(winChance - desiredWinChance);
			}
		}
		for (int key : map.keySet()){
			if (map.get(key) == closestWinChance)
				return key;
		}
		return 0;
	}

	@Override
	public int getN() {
		return N;
	}

	@Override
	public void print() {
		System.out.println(name+" (Linear Model) : " + yAxis + " = " + REG_A + " + " + REG_B + " * " + xAxis);
	}

}
