package moa.streams.filters.privacy.estimators.disclosurerisk;

import moa.streams.filters.privacy.estimators.Estimator;

public interface DisclosureRiskEstimator extends Estimator {

	/**
	 * @return the current disclosure risk estimation
	 */
	public double getCurrentDisclosureRisk();
	
}
