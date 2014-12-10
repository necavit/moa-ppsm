package moa.streams.filters.privacy.noiseaddition;

import moa.streams.InstanceStream;
import moa.streams.filters.privacy.PrivacyFilter;

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
	
	/**
	 * Builds a {@link NoiseAdditionFilter} with the given input stream and a
	 * default built {@link NoiseAdder} as the anonymization algorithm.
	 * 
	 * @param inputStream the {@link InstanceStream} to be anonymized
	 */
	public NoiseAdditionFilter(InstanceStream inputStream) {
		super(inputStream,
				new NoiseAdder());
	}
	
	/**
	 * Builds a {@link NoiseAdditionFilter} with the given input stream and a
	 * {@link NoiseAdder} as the anonymization algorithm, which parameters
	 * can be tuned using the {@code classNoiseFraction} and {@code attributeNoiseFraction}
	 * arguments (please see {@link NoiseAdder#GaussianDistorter(double, double)}).
	 * 
	 * @param inputStream the {@link InstanceStream} to be anonymized
	 * @param classNoiseFraction the noise fraction being added to all class attributes.
	 * It <b>must</b> be in the [0.0 - 1.0] range.
	 * @param attributeNoiseFraction the noise fraction being added to all other attributes.
	 * It <b>must</b> be in the [0.0 - 1.0] range.
	 */
	public NoiseAdditionFilter(InstanceStream inputStream, 
			double classNoiseFraction, double attributeNoiseFraction) {
		super(inputStream,
				new NoiseAdder(classNoiseFraction, 
									  attributeNoiseFraction));
	}
	
	/**
	 * Builds a {@link NoiseAdditionFilter} with the given input stream and a
	 * {@link NoiseAdder} as the anonymization algorithm, which parameters
	 * can be tuned using the {@code randomSeed}, the
	 * {@code classNoiseFraction} and {@code attributeNoiseFraction}
	 * arguments (see {@link NoiseAdder#GaussianDistorter(long, double, double)}).
	 * 
	 * @param inputStream the {@link InstanceStream} to be anonymized
	 * @param randomSeed the random generator seed being used
	 * @param classNoiseFraction the noise fraction being added to all class attributes.
	 * It <b>must</b> be in the [0.0 - 1.0] range.
	 * @param attributeNoiseFraction the noise fraction being added to all other attributes.
	 * It <b>must</b> be in the [0.0 - 1.0] range.
	 */
	public NoiseAdditionFilter(InstanceStream inputStream, 
			long randomSeed, double classNoiseFraction, double attributeNoiseFraction) {
		super(inputStream,
				new NoiseAdder(randomSeed,
									  classNoiseFraction, 
									  attributeNoiseFraction));
	}
	
	/**
	 * Builds a {@link NoiseAdditionFilter} with the given input stream and
	 * {@link NoiseAdder} as the anonymization algorithm.
	 * 
	 * @param inputStream the {@link InstanceStream} to be anonymized
	 * @param noiseAdder the anonymization algorithm being used
	 */
	public NoiseAdditionFilter(InstanceStream inputStream, NoiseAdder noiseAdder) {
		super(inputStream, noiseAdder);
	}

	@Override
	public void getDescription(StringBuilder arg0, int arg1) {
		// TODO Auto-generated method stub
	}

	
	
}
