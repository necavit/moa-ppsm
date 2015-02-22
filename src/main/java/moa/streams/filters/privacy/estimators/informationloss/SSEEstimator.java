package moa.streams.filters.privacy.estimators.informationloss;

import moa.core.ObjectRepository;
import moa.streams.filters.privacy.InstancePair;
import moa.streams.filters.privacy.estimators.FilterEstimator;
import moa.tasks.TaskMonitor;
import weka.core.Instance;

/**
 * Gives an estimation of the information loss as the Sum of Squared Errors
 * (thus the name of the class, {@code SSEEstimator}.
 */
public class SSEEstimator extends FilterEstimator implements InformationLossEstimator {

	/** Serializable */
	private static final long serialVersionUID = 1363697727194768299L;

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
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		currentError = 0.0;
		incrementalError = 0.0;
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub
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
	public void performEstimationForInstances(InstancePair instancePair) {
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
