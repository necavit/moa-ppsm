package moa.streams.filters.privacy.estimators.informationloss;

import moa.streams.filters.privacy.estimators.Estimator;

public interface InformationLossEstimator extends Estimator {

	/**
	 * @return the current information loss estimation
	 */
	public double getCurrentInformationLoss();
	
	/**
	 * @return the last increment of the information loss estimation
	 */
	public double getIncrementalInformationLoss();
	
}
