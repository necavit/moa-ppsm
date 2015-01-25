package moa.streams.filters.privacy.differentialprivacy.algorithms.laplace;

import moa.core.AutoExpandVector;
import moa.streams.filters.privacy.AnonymizationAlgorithm;
import moa.streams.filters.privacy.InstancePair;
import weka.core.Instance;

public class LaplaceMechanism extends AnonymizationAlgorithm {

	//TODO add more members, possibly
	public static final long DEFAULT_RANDOM_SEED_VALUE = 1235711;
	private long randomSeed;
	private LaplacianNoiseGenerator laplacianNoiseGenerator;
	
	public static final double DEFAULT_EPSILON_VALUE = 0.1;
	private double epsilon;
	private AutoExpandVector<LaplacianNoiseScaleEstimator> attributeScaleEstimators;
	
	public LaplaceMechanism() {
		this(DEFAULT_EPSILON_VALUE);
	}
	
	public LaplaceMechanism(double epsilon) {
		this(DEFAULT_RANDOM_SEED_VALUE, epsilon);
	}
	
	public LaplaceMechanism(long randomSeed, double epsilon) {
		this.randomSeed = randomSeed;
		this.epsilon = epsilon;
		this.laplacianNoiseGenerator = new LaplacianNoiseGenerator(randomSeed);
		this.attributeScaleEstimators = new AutoExpandVector<LaplacianNoiseScaleEstimator>();
	}

	@Override
	public void restart() {
		this.laplacianNoiseGenerator = new LaplacianNoiseGenerator(randomSeed);
		this.attributeScaleEstimators = new AutoExpandVector<LaplacianNoiseScaleEstimator>();
	}
	
	public double getEpsilon() {
		return epsilon;
	}
	
	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
	}

	@Override
	public InstancePair nextAnonymizedInstancePair() {
		Instance originalInstance = (Instance) inputStream.nextInstance().copy();
		Instance anonymizedInstance = addLaplaceNoise(originalInstance);
		
		InstancePair instancePair = new InstancePair(originalInstance, anonymizedInstance);
		return instancePair;
	}

	private Instance addLaplaceNoise(final Instance originalInstance) {
		//copy the instance
		Instance anonymizedInstance = (Instance) originalInstance.copy();
		
		//for each attribute, add its corresponding noise
		for (int i = 0; i < anonymizedInstance.numAttributes(); i++) {
			//do not distort if it is a class attribute
			if (i != anonymizedInstance.classIndex()) {
				LaplacianNoiseScaleEstimator scaleEstimator = attributeScaleEstimators.get(i);
				if (anonymizedInstance.attribute(i).isNominal()) {
					//TODO
				}
				else { //numerical attribute
					if (scaleEstimator == null) {
						scaleEstimator = new NumericalAttributeScaleEstimator(epsilon);
						attributeScaleEstimators.set(i, scaleEstimator);
					}
					double value = anonymizedInstance.value(i);
					double scale = scaleEstimator.estimateScale(value);
					anonymizedInstance.setValue(i, 
												(value + 
													laplacianNoiseGenerator.nextLaplacian(0.0, scale)));
				}
			}
        }
		return anonymizedInstance;
	}

	@Override
	public boolean hasMoreInstances() {
		return inputStream.hasMoreInstances();
	}

}
