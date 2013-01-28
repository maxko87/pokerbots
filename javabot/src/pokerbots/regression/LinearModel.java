package pokerbots.regression;


public class LinearModel implements Model2D{
	//[ n		S(x)  ]^-1	[ S(y)   ] = [ a ]
	//[ S(x)	S(x*x)]		[ S(x*y) ] = [ b ]
	float N;
	float SX;
	float SXX;
	float SY;
	float SYY;
	float SXY;
	int counter;
	
	//Regression equations for y = a+bx;
	//det = N*SXX-SX*SX
	//a = 1/det * (SY*SXX-SX*SXY)
	//b = 1/det * (N*SXY-SX*SY)
	float REG_A;
	float REG_B;
	float default_A=0.5f, default_B=0;
	
	String name = "Untitled";
	String xAxis = "x";
	String yAxis = "y";
	public LinearModel( String name, String xAxis, String yAxis ) {
		this.name = name;
		this.xAxis = xAxis;
		this.yAxis = yAxis;
	}
	
	public LinearModel( String name, String xAxis, String yAxis, float default_A, float default_B ) {
		this.name = name;
		this.xAxis = xAxis;
		this.yAxis = yAxis;
		this.default_A = default_A;
		this.default_B = default_B;
		this.REG_A = default_A;
		this.REG_B = default_B;
	}
	
	public int getN(){
		return (int)counter;
	}
			
	public void addData( float x, float y ) {
		if (y > 0)
			counter += 1;
		N += 1;
		SX += x;
		SXX += x*x;
		SY += y;
		SYY += y*y;
		SXY += x*y;
		
		System.out.println("&&& Train Data &&& " + name + " ("+xAxis +"="+x+", "+yAxis+"="+y+")");
		
		if ( N<2 ) {
			REG_A = default_A;
			REG_B = default_B;
		}
		else {
			float det = N*SXX-SX*SX;
			if ( det == 0 ) {
				REG_A = default_A;
				REG_B = default_B;
			} else {
				REG_A = 1.0f/det*(SY*SXX-SX*SXY);
				REG_B = 1.0f/det*(N*SXY-SX*SY);
			}
		}
	}
	
	public float getEstimate( float x ) {
		return REG_A+REG_B*x;
	}
	
	public float getInverseModel( float y ) {
		return (y-REG_A)/REG_B;
	}
	
	public void print() {
		System.out.println(name+" (Linear Model) : " + yAxis + " = " + REG_A + " + " + REG_B + " * " + xAxis);
	}
}
