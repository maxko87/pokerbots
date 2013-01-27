package pokerbots.regression;


public class CrossSectionModel implements Model3D{
	Model2D[] sections;
	int N = 0;
	
	String name = "Untitled";
	String xAxis = "x";
	String yAxis = "y";
	String zAxis = "z";
	public CrossSectionModel( String name, String xAxis, String yAxis, String zAxis ) {
		this.name = name;
		this.xAxis = xAxis;
		this.yAxis = yAxis;
		this.zAxis = zAxis;
		sections = new Model2D[2];
		for ( int i = 0; i < sections.length; i++ ) {
			sections[i] = new LinearModel(name+":"+xAxis+"["+i+"]", yAxis, zAxis);
		}
	}
	
	public int getN(){
		return (int)N;
	}
			
	public void addData( float x, float y, float z ) {

		int section = 0;
		if ( x >= 0.5 )
			section = 1;
		
		//System.out.println("&&& Train Data &&& " + name + " ("+xAxis +"="+x+", "+yAxis+"="+y+", " + zAxis +"=" + z + ") -- section = " + section);
		
		sections[section].addData(y,z);
	}
	
	public float getEstimate( float x, float y ) {
		int section1 = 0;
		int section2 = 1;
		
		float E1 = sections[section1].getEstimate(y);
		float E2 = sections[section2].getEstimate(y);
		float E = (1-x)*E1 + (x)*E2;
		
		return E;
	}
	
	public void print() {
		System.out.println(name+" (CrossSectional Model) : ");
		for ( int i = 0; i < sections.length; i++ ) {
			System.out.print("     ");
			sections[i].print();
		}
	}
}
