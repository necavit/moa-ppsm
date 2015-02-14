package moa.streams.filters.privacy.differentialprivacy;

import moa.streams.filters.privacy.AnonymizationAlgorithm;
import moa.streams.filters.privacy.InstancePair;
import moa.streams.filters.privacy.differentialprivacy.algorithms.laplace.LaplaceMechanism;
import moa.streams.filters.privacy.differentialprivacy.microaggregation.TotalOrderKNNMicroAggregator;
import weka.core.Instance;

public class MicroAggregationLaplaceCombinator extends AnonymizationAlgorithm {

	private int k;
	private int bufferSizeThreshold;
	private double epsilon;
	private TotalOrderKNNMicroAggregator microAggregator;
	private LaplaceMechanism laplaceMechanism;
	
	public MicroAggregationLaplaceCombinator(int k, int bufferSizeThreshold, double epsilon) {
		this.k = k;
		this.bufferSizeThreshold = bufferSizeThreshold;
		this.epsilon = epsilon;
		
		this.microAggregator = new TotalOrderKNNMicroAggregator(k, bufferSizeThreshold);
		this.laplaceMechanism = new LaplaceMechanism(epsilon);
	}
	
	@Override
	public void restart() {
		this.microAggregator = new TotalOrderKNNMicroAggregator(k, bufferSizeThreshold);
		this.laplaceMechanism = new LaplaceMechanism(epsilon);
	}

	@Override
	public InstancePair nextAnonymizedInstancePair() {
		Instance originalInstance = (Instance) inputStream.nextInstance().copy();
		microAggregator.addInstance(originalInstance);
		
		Instance microaggregatedInstance = microAggregator.nextAnonymizedInstance();
		if (microaggregatedInstance != null) {
			Instance anonymizedInstance = laplaceMechanism.addLaplaceNoise(microaggregatedInstance);
			return new InstancePair(originalInstance, anonymizedInstance);
		}
		else {
			return null;
		}
	}

	@Override
	public boolean hasMoreInstances() {
		return microAggregator.hasMoreInstances() || inputStream.hasMoreInstances();
	}

	
	
}
