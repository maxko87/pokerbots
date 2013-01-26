package pokerbots.regression;

import pokerbots.utils.Utils;

public class LinearModel implements Model{
	//[ n		S(x)  ]^-1	[ S(y)   ] = [ a ]
	//[ S(x)	S(x*x)]		[ S(x*y) ] = [ b ]
	float N;
	float SX;
	float SXX;
	float SY;
	float SYY;
	float SXY;
	
	//Regression equations for y = a+bx;
	//det = N*SXX-SX*SX
	//a = 1/det * (SY*SXX-SX*SXY)
	//b = 1/det * (N*SXY-SX*SY)
	float REG_A;
	float REG_B;
	
	String name = "Untitled";
	String xAxis = "x";
	String yAxis = "y";
	public LinearModel( String name, String xAxis, String yAxis ) {
		this.name = name;
		this.xAxis = xAxis;
		this.yAxis = yAxis;
	}
	
	public int getN(){
		return (int)N;
	}
			
	public void addData( float x, float y ) {
		N += 1;
		SX += x;
		SXX += x*x;
		SY += y;
		SYY += y*y;
		SXY += x*y;
		
		System.out.println("&&& Train Data &&& " + name + " ("+xAxis +"="+x+", "+yAxis+"="+y+")");
		
		if ( N<2 ) {
			REG_A = 0.5f;
			REG_B = 0;
		}
		else {
			float det = N*SXX-SX*SX;
			if ( det == 0 ) {
				REG_A = 0.5f;
				REG_B = 0;
			} else {
				REG_A = 1.0f/det*(SY*SXX-SX*SXY);
				REG_B = 1.0f/det*(N*SXY-SX*SY);
			}
		}
	}
	
	public float getEstimate( float x ) {
		return REG_A+REG_B*x;
	}
	
	public void print() {
		System.out.println(name+" (Linear Model) : " + yAxis + " = " + REG_A + " + " + REG_B + " * " + xAxis);
	}
}
