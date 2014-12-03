package moa.streams.filters.privacy;

import weka.core.Instance;

public class InstancePair {

	public final Instance originalInstance;
	
	public final Instance anonymizedInstance;
	
	public InstancePair(Instance originalInstance, Instance anonymizedInstance) {
		this.anonymizedInstance = anonymizedInstance;
		this.originalInstance = originalInstance;
	}
	
}
