package pokerbots.utils;

public class Utils {
	
	public static float getMin(float a, float b, float c){
		float min = a;
		if (b < min)
			min = b;
		if (c < min)
			min = c;
		return min;
	}
	
	public static float getMax(float a, float b, float c){
		float max = a;
		if (b > max)
			max = b;
		if (c > max)
			max = c;
		return max;
	}
	
	public static float scale(float val, float xMin, float xMax, float yMin, float yMax){
		float k = (val - xMin) / (xMax - xMin);
		return boundFloat( (k*(yMax - yMin) + yMin), yMin, yMax);
	}
	
	public static int boundInt(int num, int minNum, int maxNum) {
		if (num < minNum)
			return minNum;
		else if (num > maxNum)
			return maxNum;
		return num;
	}
	
	public static float boundFloat(float num, float minNum, float maxNum) {
		if (num < minNum)
			return minNum;
		else if (num > maxNum)
			return maxNum;
		return num;
	}
	
	public static float randomBetween(float min, float max){
		return min + (int)(Math.random() * ((max - min) + 1));
	}
	
	
	
	
	public static void main(String[] args){
		System.out.println(scale(100,10,20,20,80));
	}
	
}
