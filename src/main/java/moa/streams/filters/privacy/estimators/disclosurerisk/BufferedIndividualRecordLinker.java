package moa.streams.filters.privacy.estimators.disclosurerisk;

import java.util.Vector;

import moa.core.ObjectRepository;
import moa.options.IntOption;
import moa.streams.filters.privacy.InstancePair;
import moa.streams.filters.privacy.estimators.FilterEstimator;
import moa.streams.filters.privacy.utils.Metrics;
import moa.tasks.TaskMonitor;
import weka.core.Instance;

public class BufferedIndividualRecordLinker extends FilterEstimator implements DisclosureRiskEstimator {
	
	/** Serializable */
	private static final long serialVersionUID = 6462301962124723040L;

	/** The current re-identification buffer (original instances) */
	private Vector<Instance> originalInstancesBuffer;
	
	/** The size of the re-identification buffer */
	public IntOption bufferSizeOption = new IntOption("bufferSize", 'b', 
			"The size of the buffer that holds original instances which are reidentified.", 
			100, 10, Integer.MAX_VALUE);
	
	private double linkageProbabilitySum;
	
	/** The number of already procesed instances */
	private int processedInstances;
	
	/**
	 * Builds an instance of this estimator with the given instance buffer size.
	 * 
	 * @param bufferSize the size of the buffer of original instances (the re-identification buffer)
	 */
	public BufferedIndividualRecordLinker(final int bufferSize) {
		this.linkageProbabilitySum = 0.0;
		this.processedInstances = 0;
		this.bufferSizeOption.setValue(bufferSize);
		this.originalInstancesBuffer = new Vector<Instance>(bufferSize);
	}
	
	/**
	 * Builds this estimator with a default {@link #bufferSize} of 100 instances.
	 */
	public BufferedIndividualRecordLinker() {
		this(100);
	}
	
	@Override
	public void restart() {
		this.linkageProbabilitySum = 0.0;
		this.processedInstances = 0;
		this.originalInstancesBuffer = new Vector<Instance>(bufferSizeOption.getValue());
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		this.linkageProbabilitySum = 0.0;
		this.processedInstances = 0;
		this.originalInstancesBuffer = new Vector<Instance>(bufferSizeOption.getValue());
	}

	@Override
	public void performEstimationForInstances(InstancePair instancePair) {
		//adds the instance, keeping the buffer with a maximum fixed size
		addInstanceToBuffer(instancePair.originalInstance);
		
		estimateLinkageProbabilityForInstance(instancePair.anonymizedInstance);
	}
	
	private void estimateLinkageProbabilityForInstance(final Instance anonymizedInstance) {
		Vector<Integer> nearestInstances = getNearestInstances(anonymizedInstance);
		
		final int targetInstance = originalInstancesBuffer.size() - 1;
		if (nearestInstances.contains(targetInstance)) {
			linkageProbabilitySum += (double) (1.0 / (double) nearestInstances.size());
		}
		else {
			// do nothing (add 0 to the probability sum)
		}
	}
	
	private Vector<Integer> getNearestInstances(final Instance anonymizedInstance) {
		//initialization
		double minimum = Metrics.distance(anonymizedInstance, originalInstancesBuffer.get(0));
		Vector<Integer> indices = new Vector<Integer>();
		indices.add(0);
		
		//traversal
		for (int i = 1; i < originalInstancesBuffer.size(); ++i) {
			double distance = Metrics.distance(anonymizedInstance, originalInstancesBuffer.get(i));
			if (distance < minimum) {
				minimum = distance;
				indices = new Vector<Integer>();
				indices.add(i);
			}
			else if (distance == minimum) {
				indices.add(i);
			}
		}
		return indices;
	}
	
	/**
	 * Adds the given instance in the re-identification buffer and discards older instances if necessary.
	 */
	private void addInstanceToBuffer(Instance originalInstance) {
		if (originalInstancesBuffer.size() >= bufferSizeOption.getValue()) {
			originalInstancesBuffer.remove(0); //remove the last one
		}
		originalInstancesBuffer.add(originalInstance);
		++processedInstances;
	}
	
	@Override
	public double getCurrentDisclosureRisk() {
		return (linkageProbabilitySum) / (double) processedInstances;
	}

}
