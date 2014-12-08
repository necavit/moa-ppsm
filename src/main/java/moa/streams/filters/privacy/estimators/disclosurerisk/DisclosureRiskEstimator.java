package moa.streams.filters.privacy.estimators.disclosurerisk;

import moa.streams.filters.privacy.InstancePair;
import moa.streams.filters.privacy.estimators.FilterEstimator;

public interface DisclosureRiskEstimator extends FilterEstimator {

	/**
	 * @return the current disclosure risk estimation
	 */
	public double getCurrentDisclosureRisk();
	
	/**
	 * Adds the estimation of the disclosure risk for the given {@link InstancePair}.
	 * 
	 * @param instancePair the pair of anonymized and original instances
	 */
	public void estimateDisclosureRiskForInstancePair(InstancePair instancePair);
	
}
