package pokerbots.regression;


public class PlanarModel implements Model3D{
	//[ n		S(X)	S(Y)	][ A ] = [ S(Z)  ]
	//[ S(X)	S(XX)	S(XY)	][ B ] = [ S(XZ) ]
	//[ S(Y)	S(XY)	S(YY)	][ C ] = [ s(YZ) ]
	float N;
	float SX;
	float SXX;
	float SY;
	float SYY;
	float SXY;
	float SZ;
	float SZZ;
	float SXZ;
	float SYZ;
	
	//Regression equations for z = a+bx+c*y
	float REG_A;
	float REG_B;
	float REG_C;
	
	String name = "Untitled";
	String xAxis = "x";
	String yAxis = "y";
	String zAxis = "z";
	public PlanarModel( String name, String xAxis, String yAxis, String zAxis ) {
		this.name = name;
		this.xAxis = xAxis;
		this.yAxis = yAxis;
		this.zAxis = zAxis;
	}
	
	public int getN(){
		return (int)N;
	}
			
	public void addData( float x, float y, float z ) {
		N += 1;
		SX += x;
		SXX += x*x;
		SY += y;
		SYY += y*y;
		SXY += x*y;
		SZ += z;
		SZZ += z*z;
		SXZ += x*z;
		SYZ += y*z;
		
		System.out.println("&&& Train Data &&& " + name + " ("+xAxis +"="+x+", "+yAxis+"="+y+", " + zAxis +"=" + z + ")");
		
		if ( N<2 ) {
			REG_A = 0.5f;
			REG_B = 0;
			REG_C = 0;
		}
		else {
			float det = N*(SXX*SYY - SXY*SXY) + SX*(SXY*SY-SX*SYY) + SY*(SX*SXY-SXX*SY);
			if ( det == 0 ) {
				REG_A = 0.5f;
				REG_B = 0;
				REG_C = 0;
			} else {
				float a = N;
				float b = SX;
				float c = SY;
				float d = SX;
				float e = SXX;
				float f = SXY;
				float g = SY;
				float h = SXY;
				float i = SYY;
				REG_A = 1.0f/det*((e*i-f*h)*SZ + (c*h-b*i)*SXZ + (b*f-c*e)*SYZ);
				REG_B = 1.0f/det*((f*g-d*i)*SZ + (a*i-c*g)*SXZ + (c*d-a*f)*SYZ);
				REG_C = 1.0f/det*((d*h-e*g)*SZ + (b*g-a*h)*SXZ + (a*e-b*d)*SYZ);
			}
		}
	}
	
	public float getEstimate( float x, float y ) {
		return REG_A+REG_B*x+REG_C*y;
	}
	
	public void print() {
		System.out.println(name+" (Linear Model) : " + zAxis + " = " + REG_A + " + " + REG_B + " * " + xAxis +" + " + REG_C +" * " + yAxis);
	}
}
