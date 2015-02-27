package moa.streams.filters.privacy.differentialprivacy.algorithms.laplace;

import moa.core.AutoExpandVector;
import moa.streams.filters.privacy.differentialprivacy.algorithms.laplace.scale.DomainRangeScaleEstimator;
import moa.streams.filters.privacy.differentialprivacy.algorithms.laplace.scale.LaplacianNoiseScaleEstimator;
import weka.core.Instance;

public class LaplaceMechanism {
	
	private LaplacianNoiseGenerator laplacianNoiseGenerator;
	private double epsilon;
	private AutoExpandVector<LaplacianNoiseScaleEstimator> attributeScaleEstimators;
	
	public LaplaceMechanism(long randomSeed, double epsilon) {
		this.epsilon = epsilon;
		this.laplacianNoiseGenerator = new LaplacianNoiseGenerator(randomSeed);
		this.attributeScaleEstimators = new AutoExpandVector<LaplacianNoiseScaleEstimator>();
	}
	
	public Instance addLaplaceNoise(final Instance originalInstance) {
		//copy the instance
		Instance anonymizedInstance = (Instance) originalInstance.copy();
		
		//for each attribute, add its corresponding noise
		for (int i = 0; i < anonymizedInstance.numAttributes(); i++) {
			//do not distort if it is a class attribute
			if (i != anonymizedInstance.classIndex()) {
				LaplacianNoiseScaleEstimator scaleEstimator = attributeScaleEstimators.get(i);
				if (anonymizedInstance.attribute(i).isNominal()) {
					//TODO add laplacian noise to nominal attributes
				}
				else { //numerical attribute
					if (scaleEstimator == null) {
						scaleEstimator = 
							new DomainRangeScaleEstimator(epsilon);
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
	
	
}
