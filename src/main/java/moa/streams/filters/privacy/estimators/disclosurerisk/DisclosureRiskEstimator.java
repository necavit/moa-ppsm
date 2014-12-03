package moa.streams.filters.privacy.estimators.disclosurerisk;

import moa.streams.filters.privacy.InstancePair;
import moa.streams.filters.privacy.estimators.FilterEstimator;

public interface DisclosureRiskEstimator extends FilterEstimator {

	public double getCurrentDisclosureRisk();
	
	public void estimateDisclosureRiskForInstancePair(InstancePair instancePair);
	
}
