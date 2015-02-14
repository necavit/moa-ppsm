package moa.streams.filters.privacy.differentialprivacy.microaggregation;

import java.util.List;
import java.util.Vector;

import moa.streams.filters.privacy.AnonymizationAlgorithm;
import moa.streams.filters.privacy.InstancePair;
import weka.core.Attribute;
import weka.core.Instance;

public class TotalOrderKNNMicroAggregation extends AnonymizationAlgorithm {

	public static final int DEFAULT_K = 3;
	public static final int DEFAULT_BUFFER_SIZE = 100;
	
	private int k;
	private int bufferSizeThreshold;
	
	private Vector<Instance> instancesBuffer;
	private Vector<Instance> anonymizedInstancesBuffer;
	private Vector<Boolean> alreadyAnonymizedInstances;
	
	private boolean startToProcess;
	private TotalOrderKNNClusterer clusterer;
	
	public TotalOrderKNNMicroAggregation() {
		this(DEFAULT_K, DEFAULT_BUFFER_SIZE);
	}
	
	public TotalOrderKNNMicroAggregation(int k, int bufferSizeThreshold) {
		this.k = k;
		this.bufferSizeThreshold = bufferSizeThreshold;
		this.instancesBuffer = new Vector<Instance>(bufferSizeThreshold);
		this.anonymizedInstancesBuffer = new Vector<Instance>(bufferSizeThreshold);
		this.alreadyAnonymizedInstances =  new Vector<Boolean>(bufferSizeThreshold);
    	this.startToProcess = false;
    	this.clusterer = new TotalOrderKNNClusterer(k);
	}

	@Override
	public void restart() {
		this.instancesBuffer = new Vector<Instance>(bufferSizeThreshold);
		this.anonymizedInstancesBuffer = new Vector<Instance>(bufferSizeThreshold);
    	this.alreadyAnonymizedInstances =  new Vector<Boolean>(bufferSizeThreshold);
    	this.startToProcess = false;
    	this.clusterer = new TotalOrderKNNClusterer(k);
	}

	@Override
	public boolean hasMoreInstances() {
		return inputStream.hasMoreInstances() || (instancesBuffer.size() > 0);
	}
	
	@Override
	public InstancePair nextAnonymizedInstancePair() {
		//get the next instance from the stream
		fetchNextStreamInstance();
		
		//check whether to begin processing the buffer
		if (instancesBuffer.size() == bufferSizeThreshold) {
			startToProcess = true;
		}
		
		//process or return null
		if (startToProcess){
			//return the next anonymized instance
			return processNextInstance();
		}
		else {
			//no instance can be returned if the buffer is not yet prepared to be processed
			return null;
		}
	}
	
	private void fetchNextStreamInstance() {
		//fetch newer instances from the input stream
		if (this.inputStream.hasMoreInstances()){
			//perform 2 copies, one for each instance buffer
			Instance originalInstance = (Instance) this.inputStream.nextInstance().copy();		
			Instance anonymizableInstance = (Instance) originalInstance.copy();
			
			//add instances to buffer and boolean flag indicating that it is not
			// yet anonymized
			instancesBuffer.add(originalInstance);
			anonymizedInstancesBuffer.add(anonymizableInstance);
			alreadyAnonymizedInstances.add(false);
			
			//feed the new instance to the clusterer
			clusterer.updateTargetInstance(originalInstance);
		}
	}
	
	private InstancePair processNextInstance() {
		final int top = 0; //semantic variable (it is indeed useless)
		
		//anonymize the next instance only if it is not yet anonymized
		if (!alreadyAnonymizedInstances.get(top)){
			anonymizeNextInstance();
		}
		
		//remove the instance from the buffer and from the list of anonymized instances
		alreadyAnonymizedInstances.remove(top);
		InstancePair anonymizedInstancePair = 
				new InstancePair(instancesBuffer.remove(top), 
						         anonymizedInstancesBuffer.remove(top));
		return anonymizedInstancePair;
	}
	
	private void anonymizeNextInstance() {
		//get the indexes of the k nearest neighbors (containing the top (target) instance)
		//  to form a cluster to be anonymized
		List<Integer> clusterIndexes = 
			clusterer.getNextKNNClusterIndexes(instancesBuffer, alreadyAnonymizedInstances);
		
		//aggregate (anonymize) the instances of the cluster
		anonymizeClusterWithIndexes(clusterIndexes);
	}
	
	private void anonymizeClusterWithIndexes(List<Integer> clusterIndexes) {
		//flag as anonymized the instances of the cluster
		setAnonymizedInstancesForIndexes(clusterIndexes);
		
		//perform multivariate aggregation:
		  //take the first instance of the cluster as the example
		final Instance targetInstance = instancesBuffer.get(clusterIndexes.get(0));
		
		  //for each attribute (variable) of the instances
		for (int attributeIndex = 0; attributeIndex < targetInstance.numAttributes(); ++attributeIndex) {
			//if it is not the target attribute (the one flagged as 'class', for classification)
			if (attributeIndex != targetInstance.classIndex()){
				//get attribute
				final Attribute attribute = targetInstance.attribute(attributeIndex);
				
				//compute aggregated value
				if (attribute.isNumeric()) {
					double newValue = aggregateNumericalAttributeForInstances(attributeIndex, clusterIndexes);
					//replace values of the instances with the aggregated one
					for (int i = 0; i < clusterIndexes.size(); ++i) {
						Instance instance =	instancesBuffer.get(clusterIndexes.get(i)); //get instance
						
						//replace value
						instance.setValue(attributeIndex, newValue);
					}
				}
			}
		}
	}
	
	private void setAnonymizedInstancesForIndexes(final List<Integer> indexes) {
		for (Integer index : indexes) {
			alreadyAnonymizedInstances.set(index, true);
		}
	}
	
	/**
	 * Performs an aggregation of values of the target attribute, considering all instances contained in
	 * the given set of indexes.
	 * 
	 * @param attributeIndex the index of the attribute on which to perform the aggregation
	 * @param clusterIndexes a list of indexes of the instances in the buffer to take into account
	 * @return the aggregated value (computed as the average)
	 */
	private double aggregateNumericalAttributeForInstances(final int attributeIndex, final List<Integer> clusterIndexes) {
		double average = 0.0;
		for (int i = 0; i < clusterIndexes.size(); ++i){
			Instance instance = instancesBuffer.get(clusterIndexes.get(i));
			average += instance.value(attributeIndex);
		}
		average = average / clusterIndexes.size();
		return average;
	}
}
