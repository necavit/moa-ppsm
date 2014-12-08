package moa.streams.filters.privacy.estimators.informationloss;

import moa.streams.filters.privacy.InstancePair;
import moa.streams.filters.privacy.estimators.FilterEstimator;

public interface InformationLossEstimator extends FilterEstimator {

	/**
	 * @return the current information loss estimation
	 */
	public double getCurrentInformationLoss();
	
	/**
	 * @return the last increment of the information loss estimation
	 */
	public double getIncrementalInformationLoss();
	
	/**
	 * Adds the given {@link InstancePair} to the information loss estimation.
	 * 
	 * @param instancePair the pair of anonymized and original instances
	 */
	public void estimateInformationLossForInstancePair(InstancePair instancePair);
	
}
