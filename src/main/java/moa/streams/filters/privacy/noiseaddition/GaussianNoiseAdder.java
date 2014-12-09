package moa.streams.filters.privacy.noiseaddition;

import moa.streams.filters.privacy.AnonymizationAlgorithm;
import moa.streams.filters.privacy.InstancePair;
import weka.core.Instance;

public class GaussianNoiseAdder extends AnonymizationAlgorithm {

	@Override
	public void restart() {
		// TODO Auto-generated method stub
	}

	@Override
	public InstancePair nextAnonymizedInstancePair() {
		Instance originalInstance = inputStream.nextInstance();
		Instance anonymizedInstance = addNoiseToInstance(originalInstance);
		
		InstancePair instancePair = new InstancePair(originalInstance, anonymizedInstance);
		return instancePair;
	}

	private Instance addNoiseToInstance(final Instance originalInstance) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasMoreInstances() {
		return inputStream.hasMoreInstances();
	}

}
