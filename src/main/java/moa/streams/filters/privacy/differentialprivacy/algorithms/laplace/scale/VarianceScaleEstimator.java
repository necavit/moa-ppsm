package moa.streams.filters.privacy.differentialprivacy.algorithms.laplace.scale;

public class VarianceScaleEstimator extends LaplacianNoiseScaleEstimator {

	private long n;
	
	private double oldMean, newMean, oldVariance, newVariance;
	
	public VarianceScaleEstimator(double epsilon) {
		super(epsilon);
		this.n = 0;
	}
	
	private void updateStatistics(double value) {
		n++;
		if (n == 1) {
			oldMean = newMean = value;
		}
		else {
			newMean = oldMean + (value - oldMean) / n;
			newVariance = oldVariance + (value - oldMean) * (value - newMean);
			
			oldMean = newMean;
			oldVariance = newVariance;
		}
	}
	
	private double getVariance() {
		return (n > 1 ? newVariance / ((double)(n - 1)) : 0.0);
	}
	
	@Override
	public double estimateScale(double value) {
		updateStatistics(value);
		return getVariance() / epsilon;
	}
}