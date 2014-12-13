package moa.streams.filters.privacy.noiseaddition;

import java.util.Random;

import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.GaussianEstimator;
import moa.streams.filters.privacy.AnonymizationAlgorithm;
import moa.streams.filters.privacy.InstancePair;
import weka.core.Instance;

/**
 * Anonymization algorithm that adds noise to instances of a stream.
 * <p>
 * Three parameters can be used to tune this method:
 * <ul>
 *   <li>{@link #randomSeed}</li>
 *   <li>{@link #classNoiseFraction}</li>
 *   <li>{@link #attributeNoiseFraction}</li>
 * </ul>
 */
class NoiseAdder extends AnonymizationAlgorithm {

	/** The pseudo-random generator */
	private Random randomGenerator;
	
	/** Random generator seed */
	private long randomSeed;
	
	/** The fraction of class labels to disturb */
	private double classNoiseFraction;
	
	/** The fraction of attribute values to disturb */
	private double attributeNoiseFraction;
	
	/** Observers of attributes values */
	private AutoExpandVector<Object> attValueObservers;
	
	public NoiseAdder() {
		this(31415921); //just a magic number! =)
	}
	
	public NoiseAdder(long randomSeed) {
		this(randomSeed, 0.1, 0.1); //default fraction parameters
	}
	
	public NoiseAdder(double classNoiseFraction, double attributeNoiseFraction) {
		this(31415921, classNoiseFraction, attributeNoiseFraction); //default seed
	}
	
	public NoiseAdder(long randomSeed, double classNoiseFraction, double attributeNoiseFraction) {
		this.classNoiseFraction = classNoiseFraction;
		this.attributeNoiseFraction = attributeNoiseFraction;
		this.randomSeed = randomSeed;
		this.randomGenerator = new Random(randomSeed);
		this.attValueObservers = new AutoExpandVector<Object>();
	}
	
	
	@Override
	public void restart() {
		this.randomGenerator = new Random(randomSeed);
		this.attValueObservers = new AutoExpandVector<Object>();
	}

	@Override
	public InstancePair nextAnonymizedInstancePair() {
		Instance originalInstance = (Instance) inputStream.nextInstance().copy();
		Instance anonymizedInstance = distortInstance(originalInstance);
		
		InstancePair instancePair = new InstancePair(originalInstance, anonymizedInstance);
		return instancePair;
	}

	private Instance distortInstance(final Instance originalInstance) {
		//copy the instance
		Instance anonymizedInstance = (Instance) originalInstance.copy();
		
		//for each attribute, add its corresponding noise
		for (int i = 0; i < anonymizedInstance.numAttributes(); i++) {
			
			//depending on whether it is a class attribute
			double noiseFraction = 
            		(i == anonymizedInstance.classIndex()) ? 
            			classNoiseFraction : 
            				attributeNoiseFraction;
			
			//and depending on whether it is numeric or nominal
            if (anonymizedInstance.attribute(i).isNominal()) {
                DoubleVector observer = (DoubleVector) attValueObservers.get(i);
                if (observer == null) {
                    observer = new DoubleVector();
                    attValueObservers.set(i, observer);
                }
                int originalValue = (int) anonymizedInstance.value(i);
                if (!anonymizedInstance.isMissing(i)) {
                    observer.addToValue(originalValue, anonymizedInstance.weight());
                }
                if ((randomGenerator.nextDouble() < noiseFraction)
                        && (observer.numNonZeroEntries() > 1)) {
                    do {
                        anonymizedInstance.setValue(i, randomGenerator.nextInt(observer.numValues()));
                    }
                    while (((int) anonymizedInstance.value(i) == originalValue)
                            || (observer.getValue((int) anonymizedInstance.value(i)) == 0.0));
                }
            }
            else {
                GaussianEstimator observer = (GaussianEstimator) attValueObservers.get(i);
                if (observer == null) {
                    observer = new GaussianEstimator();
                    attValueObservers.set(i, observer);
                }
                observer.addObservation(anonymizedInstance.value(i), anonymizedInstance.weight());
                anonymizedInstance
                	.setValue(i, 
                			  anonymizedInstance.value(i) 
                			  	+ randomGenerator.nextGaussian() * observer.getStdDev() * noiseFraction);
            }
        }
		return anonymizedInstance;
	}

	@Override
	public boolean hasMoreInstances() {
		return inputStream.hasMoreInstances();
	}

}
