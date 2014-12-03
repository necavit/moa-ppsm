package moa.streams.filters.privacy.estimators.informationloss;

import moa.streams.filters.privacy.InstancePair;
import moa.streams.filters.privacy.estimators.FilterEstimator;

public interface InformationLossEstimator extends FilterEstimator {

	public double getCurrentInformationLoss();
	
	public double getIncrementalInformationLoss();
	
	public void estimateInformationLossForInstancePair(InstancePair instancePair);
	
}
