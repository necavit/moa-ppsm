package moa.streams.filters.privacy.differentialprivacy.algorithms.laplace;

import moa.streams.filters.privacy.AnonymizationAlgorithm;
import moa.streams.filters.privacy.InstancePair;
import weka.core.Instance;

public class LaplaceNoiseAdder extends AnonymizationAlgorithm {
	
	private long randomSeed;
	private double epsilon;
	private LaplaceMechanism laplaceMechanism;
	
	public LaplaceNoiseAdder(long randomSeed, double epsilon) {
		this.laplaceMechanism = new LaplaceMechanism(randomSeed, epsilon);
	}

	@Override
	public void restart() {
		this.laplaceMechanism = new LaplaceMechanism(randomSeed, epsilon);
	}

	@Override
	public InstancePair nextAnonymizedInstancePair() {
		Instance originalInstance = (Instance) inputStream.nextInstance().copy();
		InstancePair instancePair = new InstancePair(originalInstance, 
													 laplaceMechanism.addLaplaceNoise(originalInstance));
		return instancePair;
	}

	@Override
	public boolean hasMoreInstances() {
		return inputStream.hasMoreInstances();
	}

}
