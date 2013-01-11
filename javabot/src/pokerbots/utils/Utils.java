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
	
}
