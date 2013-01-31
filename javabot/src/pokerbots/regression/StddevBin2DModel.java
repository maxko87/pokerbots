package pokerbots.regression;

public class StddevBin2DModel implements Model2D {
	
	int counter = 0;
	StddevModel[] bins;
	
	public static void main( String[] args ) {
		System.out.println("Testing StddevBin2DModel");
		StddevBin2DModel model = new StddevBin2DModel("P(win|betSize)","win%","betSize",10);
		for ( int i = 0; i < 100; i++ ) {
			double winChance = Math.random();
			int betSize = 0;
			if ( winChance>0.9 )
				betSize = 15+(int)(Math.random()*20);
			if ( winChance>0.6 && winChance<0.8 )
				betSize = 30;
			if ( betSize==0 ) {
				i--;
				continue;
			}
			model.addData((float)winChance,betSize);
		}
		
		model.print();
		for ( int betSize = 0; betSize < 100; betSize+=5 ) {
			float winChance = model.getInverseModel(betSize);
			System.out.println("My guess of win% given his bet = " + betSize +", Win% = " + winChance);
			//System.out.println(model.percentile(betSize/100.0f, 0.5f, 0.1f));
		}
	}
	
	String xAxis, yAxis, name;
	public StddevBin2DModel( String name, String xAxis, String yAxis, int binNum ) {
		this.xAxis = xAxis;
		this.yAxis = yAxis;
		this.name = name;
		bins = new StddevModel[binNum];
		for ( int i = 0; i < binNum; i++ ) {
			bins[i] = new StddevModel("["+i+"]-"+yAxis);
		}
	}
	
	//assumes x data is in [0,1]
	public void addData( float x, float y ) {
		if (y!=0)
			counter++;
		int bin = (int)(x*(bins.length));
		if ( bin > bins.length-1 )
			bin = bins.length-1;
		bins[bin].addData(y);
	}
	
	public float getEstimate( float x ) {
		int bin = (int)(x*(bins.length-0.001f));
		if ( bin > bins.length-1 )
			bin = bins.length-1;
		return bins[bin].getAverage();
	}
	
	public float getInverseModel( float y ) {
		float xPred = 0;
		float weight = 0;
		for ( int i = 0; i < bins.length; i++ ) {
			float avg = bins[i].avg;
			float dev = bins[i].stddev;
			if ( dev==0 ) dev = 0.01f;			//If the trained data is exact, then spike here!
			float P = percentile(y, avg, dev);
			if ( bins[i].N<3 )
				P = 0.001f;
			weight+=P;
			float xData = ((i+0.5f)/(float)bins.length);
			//System.out.println( "y = " + y +", avg = " + avg + ", stddev = " + dev +", % = " + P +", xData = " + xData);
			xPred += P*xData;
		}
		if ( weight<0.2 )
			return 0;
		return xPred / weight;
	}
	
	public float percentile( float x, float avg, float stddev ) {
		float x_mu = (x-avg);
		float dx = x_mu/stddev;
		float percentile = (float)/*(1.0f/(stddev*2.5066) * */(Math.exp(-0.5*dx*dx));
		return percentile;
	}
	
	public int getN() {
		return counter;
	}
	
	public void print(){
		System.out.println(name + " (Stddev2DModel)");
		for ( int i = 0; i < bins.length; i++ ) {
			System.out.print("   ");
			bins[i].print();
		}
	}
}
