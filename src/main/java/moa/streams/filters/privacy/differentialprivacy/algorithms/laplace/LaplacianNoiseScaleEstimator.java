package moa.streams.filters.privacy.differentialprivacy.algorithms.laplace;

public abstract class LaplacianNoiseScaleEstimator {

	protected final double epsilon;
	
	public LaplacianNoiseScaleEstimator(double epsilon) {
		this.epsilon = epsilon;
	}
	
	public abstract double estimateScale(double value);
	
}
