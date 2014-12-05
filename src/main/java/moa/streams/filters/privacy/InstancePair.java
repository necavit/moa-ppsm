package moa.streams.filters.privacy;

import weka.core.Instance;

public class InstancePair {

	/** The original (non-anonymized) instance */
	public final Instance originalInstance;
	
	/** The anonymized (filtered) instance */
	public final Instance anonymizedInstance;
	
	/**
	 * Builds a new {@link InstancePair} with both the original and the filtered instances.
	 * 
	 * @param originalInstance the non-anonymized (original) instance
	 * @param anonymizedInstance the anonymized instance
	 */
	public InstancePair(Instance originalInstance, Instance anonymizedInstance) {
		this.anonymizedInstance = anonymizedInstance;
		this.originalInstance = originalInstance;
	}
	
	/**
	 * @return the anonymized (filtered) instance
	 */
	public Instance getAnonymizedInstance() {
		return anonymizedInstance;
	}
	
	/**
	 * @return the original (non-anonymized) instance
	 */
	public Instance getOriginalInstance() {
		return originalInstance;
	}
	
}
