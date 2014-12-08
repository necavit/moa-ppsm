package moa.streams.filters.privacy.estimators.informationloss;

import weka.core.Instance;
import moa.streams.filters.privacy.InstancePair;

/**
 * Gives an estimation of the information loss as the Sum of Squared Errors
 * (thus the name of the class, {@code SSEEstimator}.
 */
public class SSEEstimator implements InformationLossEstimator {

	/** The current information loss (SSE error) */
	private double currentError = 0.0;
	
	/** The current increment on the information loss (SSE error) */
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
		
		//for each attribute, get the difference and sum its square to the global error
		for (int i = 0; i < x.numAttributes(); ++i) {
			double difference = x.value(i) - y.value(i);
			error += difference * difference;
		}
		
		assert(error >= 0.0);
		
		currentError += error;
		incrementalError = currentError - lastError;
	}

}
