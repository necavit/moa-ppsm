package moa.streams.filters.privacy.estimators;

import moa.streams.filters.privacy.InstancePair;

/**
 * Generic estimator interface.
 */
public interface Estimator {

	/**
	 * Restarts the implementation of this estimator, to prepare it for next use.
	 */
	public void restart();
	
	/**
	 * Request to perform an estimation with the given instances.
	 * 
	 * @param instancePair the instances with which the estimation is to be updated
	 */
	public void performEstimationForInstances(InstancePair instancePair);
	
}
