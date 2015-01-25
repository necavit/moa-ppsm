package moa.streams.filters.privacy.differentialprivacy.algorithms.laplace;

public class NumericalAttributeScaleEstimator extends LaplacianNoiseScaleEstimator {

	private double minimum;
	
	private double maximum;
	
	private boolean initialized;
	
	public NumericalAttributeScaleEstimator(double epsilon) {
		super(epsilon);
		initialized = false;
	}
	
	@Override
	public double estimateScale(double value) {
		if (!initialized) {
			initialized = true;
			minimum = value;
			maximum = value;
		}
		else {
			if (value > maximum) {
				maximum = value;
			}
			if (value < minimum) {
				minimum = value;
			}
		}
		
		return maximum - minimum;
	}

}
