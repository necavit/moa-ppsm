package moa.streams.filters.privacy.estimators.disclosurerisk;

import java.util.Vector;

import moa.streams.filters.privacy.InstancePair;
import moa.streams.filters.privacy.utils.Metrics;
import weka.core.Instance;

public class BufferedIndividualRecordLinker implements DisclosureRiskEstimator {

	private Vector<Instance> originalInstancesBuffer;
	
	private final int bufferSize;
		
	private int recordLinkageHits;
	
	private int processedInstances;
	
	public BufferedIndividualRecordLinker(final int bufferSize) {
		this.recordLinkageHits = 0;
		this.processedInstances = 0;
		this.bufferSize = bufferSize;
		this.originalInstancesBuffer = new Vector<Instance>(bufferSize);
	}
	
	public BufferedIndividualRecordLinker() {
		this(100);
	}
	
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
	
	private void addInstanceToBuffer(Instance originalInstance) {
		if (originalInstancesBuffer.size() >= bufferSize) {
			originalInstancesBuffer.remove(0); //remove the last one
		}
		originalInstancesBuffer.add(originalInstance);
		++processedInstances;
	}

}
