package moa.streams.filters.privacy.estimators.informationloss;

import weka.core.Instance;
import moa.streams.filters.privacy.InstancePair;

/**
 * Gives an estimation of the information loss as the Sum of Squared Errors
 * (thus the name of the class, {@code SSEEstimator}.
 */
public class SSEEstimator implements InformationLossEstimator {

	private double currentError = 0.0;
	
	private double incrementalError = 0.0;
	
	@Override
	public void restart() {
		currentError = 0.0;
		incrementalError = 0.0;
	}
	
	@Override
	public double getCurrentInformationLoss() {
		return currentError;
	}

	@Override
	public double getIncrementalInformationLoss() {
		return incrementalError;
	}
	
	@Override
	public void estimateInformationLossForInstancePair(InstancePair instancePair) {
		double lastError = currentError;
		double error = 0.0;
		
		Instance x = instancePair.originalInstance;
		Instance y = instancePair.anonymizedInstance;
		
		for (int i = 0; i < x.numAttributes(); ++i) {
			double difference = x.value(i) - y.value(i);
			error += difference * difference;
		}
		
		assert(error >= 0.0);
		
		currentError += error;
		incrementalError = currentError - lastError;
	}

}
