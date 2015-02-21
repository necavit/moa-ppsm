package moa.streams.filters.privacy.noiseaddition;

import java.util.Random;

import moa.core.AutoExpandVector;
import moa.core.DoubleVector;
import moa.core.GaussianEstimator;
import moa.options.FloatOption;
import moa.options.IntOption;
import moa.streams.InstanceStream;
import moa.streams.filters.privacy.InstancePair;
import moa.streams.filters.privacy.PrivacyFilter;
import weka.core.Instance;

/**
 * Filter for adding random noise to examples in a stream.
 * <p>
 * Noise can be added to attribute values or to class labels, by customizing
 * the {@link NoiseAdder} used by this filter. This is the algorithm
 * performing the actual noise addition process. Please see 
 * {@link #NoiseAdditionFilter(InstanceStream, double, double)}, 
 * {@link #NoiseAdditionFilter(InstanceStream, long, double, double)} and
 * {@link #NoiseAdditionFilter(InstanceStream, NoiseAdder)}. 
 */
public class NoiseAdditionFilter extends PrivacyFilter {
	
	private static final long serialVersionUID = 94085693669724686L;
	
	/** The pseudo-random generator */
	private Random randomGenerator;
	
	/** Random generator seed */
	public IntOption randomSeedOption = new IntOption("randomSeed", 'r', 
			"The pseudo-random generator seed", 3141592, Integer.MIN_VALUE, Integer.MAX_VALUE);
	
	/** The fraction of class labels to disturb */
	public FloatOption classNoiseFractionOption =  new FloatOption("classNoiseFraction", 'c', 
			"The fraction of class values to distort.", 0.1f, 0.0f, 1.0f);
	
	/** The fraction of attribute values to disturb */
	public FloatOption attributeNoiseFractionOption =  new FloatOption("attributeNoiseFraction", 'a', 
			"The fraction of attribute values to distort.", 0.1f, 0.0f, 1.0f);
	
	/** Observers of attributes values */
	private AutoExpandVector<Object> attValueObservers;
	
	
	public NoiseAdditionFilter() {
		this(31415921);
	}
	
	public NoiseAdditionFilter(int randomSeed) {
		this(randomSeed, 0.1, 0.1); //default fraction parameters
	}
	
	public NoiseAdditionFilter(double classNoiseFraction, double attributeNoiseFraction) {
		this(31415921, classNoiseFraction, attributeNoiseFraction); //default seed
	}
	
	/**
	 * Builds a {@link NoiseAdditionFilter}, with tuning parameters.
	 * 
	 * @param randomSeed the random generator seed being used
	 * @param classNoiseFraction the noise fraction being added to all class attributes.
	 * It <b>must</b> be in the [0.0 - 1.0] range.
	 * @param attributeNoiseFraction the noise fraction being added to all other attributes.
	 * It <b>must</b> be in the [0.0 - 1.0] range.
	 */
	public NoiseAdditionFilter(int randomSeed, double classNoiseFraction, double attributeNoiseFraction) {
		this.classNoiseFractionOption.setValue(classNoiseFraction);
		this.attributeNoiseFractionOption.setValue(attributeNoiseFraction);
		this.randomSeedOption.setValue(randomSeed);
		this.randomGenerator = new Random(randomSeed);
		this.attValueObservers = new AutoExpandVector<Object>();
	}

	@Override
	public void getDescription(StringBuilder arg0, int arg1) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void restartFilter() {
		this.randomGenerator = new Random(randomSeedOption.getValue());
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
            			classNoiseFractionOption.getValue() : 
            				attributeNoiseFractionOption.getValue();
			
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
