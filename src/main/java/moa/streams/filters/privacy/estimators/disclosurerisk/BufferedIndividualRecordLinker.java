package moa.streams.filters.privacy.estimators.disclosurerisk;

import java.util.Vector;

import moa.streams.filters.privacy.InstancePair;
import moa.streams.filters.privacy.utils.Metrics;
import weka.core.Instance;

public class BufferedIndividualRecordLinker implements DisclosureRiskEstimator {

	/** The current re-identification buffer (original instances) */
	private Vector<Instance> originalInstancesBuffer;
	
	/** The size of the re-identification buffer */
	private final int bufferSize;
		
	/** The number of re-identification hits */
	private int recordLinkageHits;
	
	/** The number of already procesed instances */
	private int processedInstances;
	
	/**
	 * Builds an instance of this estimator with the given instance buffer size.
	 * 
	 * @param bufferSize the size of the buffer of original instances (the re-identification buffer)
	 */
	public BufferedIndividualRecordLinker(final int bufferSize) {
		this.recordLinkageHits = 0;
		this.processedInstances = 0;
		this.bufferSize = bufferSize;
		this.originalInstancesBuffer = new Vector<Instance>(bufferSize);
	}
	
	/**
	 * Builds this estimator with a default {@link #bufferSize} of 100 instances.
	 */
	public BufferedIndividualRecordLinker() {
		this(100);
	}
	
	/**
	 * @return the size of the re-identification buffer
	 */
	public int getBufferSize() {
		return bufferSize;
	}
	
	@Override
	public void restart() {
		this.recordLinkageHits = 0;
		this.processedInstances = 0;
		this.originalInstancesBuffer = new Vector<Instance>(bufferSize);
	}
	
	@Override
	public double getCurrentDisclosureRisk() {
		return (double)recordLinkageHits / (double)processedInstances ;
	}

	@Override
	public void estimateDisclosureRiskForInstancePair(InstancePair instancePair) {
		//adds the instance, keeping the buffer with a maximum fixed size
		addInstanceToBuffer(instancePair.originalInstance);
		
		//get nearest instances (indexes) to the anonymized one
		int guess = guessNearestInstance(instancePair.originalInstance);
		
		//check if the nearest guesses contain the target one
		if (guess == (originalInstancesBuffer.size() - 1)) {
			++recordLinkageHits;
		}
	}
	
	/**
	 * @param targetInstance the target against which the re-identification is performed
	 * @return the index of the instance in the buffer that is considered to be the target one
	 */
	private int guessNearestInstance(final Instance targetInstance) {
		double minimum = 0.0;
		int index = 0;
		for (int i = 0; i < originalInstancesBuffer.size(); ++i) {
			double distance = Metrics.distance(targetInstance, originalInstancesBuffer.get(i));
			if (i == 0) {
				minimum = distance;
			}
			else {
				if (distance < minimum) {
					minimum = distance;
					index = i;
				}
			}
		}
		
		return index;
	}
	
	/**
	 * Adds the given instance in the re-identification buffer and discards older instances if necessary.
	 */
	private void addInstanceToBuffer(Instance originalInstance) {
		if (originalInstancesBuffer.size() >= bufferSize) {
			originalInstancesBuffer.remove(0); //remove the last one
		}
		originalInstancesBuffer.add(originalInstance);
		++processedInstances;
	}

}
