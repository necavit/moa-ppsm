package moa.streams.filters.privacy.differentialprivacy.microaggregation;

import java.util.List;
import java.util.Vector;

import moa.streams.filters.privacy.InstancePair;

import weka.core.Attribute;
import weka.core.Instance;

public class TotalOrderKNNMicroAggregator {

	private int bufferSizeThreshold;
	private boolean startToProcess;
	
	private Vector<Instance> originalInstanceBuffer;
	private Vector<Instance> instanceBuffer;
	private Vector<Boolean> anonymized;
	
	private TotalOrderKNNClusterer clusterer;
	
	public TotalOrderKNNMicroAggregator(int k, int bufferSizeThreshold) {
		this.anonymized = new Vector<Boolean>(bufferSizeThreshold);
		this.bufferSizeThreshold = bufferSizeThreshold;
		this.originalInstanceBuffer = new Vector<Instance>(bufferSizeThreshold);
		this.instanceBuffer = new Vector<Instance>(bufferSizeThreshold);
		this.startToProcess = false;
		this.clusterer = new TotalOrderKNNClusterer(k);
	}
	
	public InstancePair nextAnonymizedInstancePair() {
		if (startToProcess) {
			return processNextInstance();
		}
		else {
			return null;
		}
	}

	public void addInstance(Instance originalInstance) {
		originalInstanceBuffer.add(originalInstance);
		instanceBuffer.add((Instance)originalInstance.copy());
		anonymized.add(false);
		clusterer.updateTargetInstance(originalInstance);
		if (instanceBuffer.size() >= bufferSizeThreshold) {
			startToProcess = true;
		}
	}
	
	public boolean hasMoreInstances() {
		return instanceBuffer.size() > 0;
	}
	
	private InstancePair processNextInstance() {
		final int top = 0;
		
		if (!anonymized.get(top)) {
			anonymizeNextInstance();
		}
		
		anonymized.remove(top);
		return new InstancePair(originalInstanceBuffer.remove(top), instanceBuffer.remove(top));
	}
	
	private void anonymizeNextInstance() {
		//get the indexes of the k nearest neighbors (containing the top (target) instance)
		//  to form a cluster to be anonymized
		List<Integer> clusterIndexes = 
			clusterer.getNextKNNClusterIndexes(instanceBuffer, anonymized);
		
		//aggregate (anonymize) the instances of the cluster
		anonymizeClusterWithIndexes(clusterIndexes);
	}
	
	private void anonymizeClusterWithIndexes(List<Integer> clusterIndexes) {
		//flag as anonymized the instances of the cluster
		setAnonymizedInstancesForIndexes(clusterIndexes);
		
		//perform multivariate aggregation:
		  //take the first instance of the cluster as the example
		final Instance targetInstance = instanceBuffer.get(clusterIndexes.get(0));
		
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
						Instance instance =	instanceBuffer.get(clusterIndexes.get(i)); //get instance
						
						//replace value
						instance.setValue(attributeIndex, newValue);
					}
				}
			}
		}
	}
	
	private void setAnonymizedInstancesForIndexes(final List<Integer> indexes) {
		for (Integer index : indexes) {
			anonymized.set(index, true);
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
			Instance instance = instanceBuffer.get(clusterIndexes.get(i));
			average += instance.value(attributeIndex);
		}
		average = average / clusterIndexes.size();
		return average;
	}

}
