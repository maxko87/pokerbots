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
		return k*(yMax - yMin) + yMin;
	}
	
	public static void main(String[] args){
		System.out.println(scale(11,10,20,20,80));
	}
	
}
