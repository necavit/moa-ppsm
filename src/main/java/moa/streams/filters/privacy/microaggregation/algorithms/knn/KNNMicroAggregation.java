package moa.streams.filters.privacy.microaggregation.algorithms.knn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import moa.options.IntOption;
import moa.streams.filters.privacy.AnonymizationAlgorithm;
import moa.streams.filters.privacy.InstancePair;
import weka.core.Attribute;
import weka.core.Instance;


public class KNNMicroAggregation extends AnonymizationAlgorithm {

	/** The <em>K</em> value for the <em>k</em>-anonymity property to be satisfied. */
	public IntOption kAnonymityValueOption = new IntOption("kAnonimty", 'k', "k value for the k-anonymity", 3);

	/** The size of the historical buffer considered before starting to perform the filtering. */
    public IntOption bufferSizeOption = new IntOption("bufferLength", 'b', "length of the historical buffer considered to " +
			"begin the microaggregation process", 100);
        
    /** Indicates whether to start processing (anonymizing) instances or not. */
	private boolean startToProcess;
	
	/** The original instances buffer of the filter. */
	private Vector<Instance> instancesBuffer;
	
	/** The anonymized instances buffer of the filter. */
	private Vector<Instance> anonymizedInstancesBuffer;
	
	/** A vector to indicate whether an instance is anonymized or not. An index (ordering)
	 * correspondance is maintained with respect to the {@link #instancesBuffer}
	 * and {@link #anonymizedInstancesBuffer} members. */
	private Vector<Boolean> alreadyAnonymizedInstances;
	
	public KNNMicroAggregation() {
		this.instancesBuffer = new Vector<Instance>(bufferSizeOption.getValue());
		this.anonymizedInstancesBuffer = new Vector<Instance>(bufferSizeOption.getValue());
    	this.alreadyAnonymizedInstances =  new Vector<Boolean>(bufferSizeOption.getValue());
    	this.startToProcess = false;
	}
	
	@Override
	public void restart() {
		this.instancesBuffer = new Vector<Instance>(bufferSizeOption.getValue());
		this.anonymizedInstancesBuffer = new Vector<Instance>(bufferSizeOption.getValue());
    	this.alreadyAnonymizedInstances =  new Vector<Boolean>(bufferSizeOption.getValue());
    	this.startToProcess = false;
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
		if (instancesBuffer.size() == bufferSizeOption.getValue()) {
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
				KNNClusterer.getNextKNNClusterIndexes(kAnonymityValueOption.getValue(),
													  instancesBuffer, alreadyAnonymizedInstances);
		
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
				double newValue;
				if (attribute.isNumeric()) {
					newValue = aggregateNumericalAttributeForInstances(attributeIndex, clusterIndexes);
				}
				else {
					newValue = aggregateNominalAttributeForInstances(attributeIndex, clusterIndexes);
				}
				
				//replace values of the instances with the aggregated one
				for (int i = 0; i < clusterIndexes.size(); ++i) {
					Instance instance =	instancesBuffer.get(clusterIndexes.get(i)); //get instance
					
					//replace value
					instance.setValue(attributeIndex, newValue);
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
	
	/**
	 * Performs an aggregation of values of the target attribute, considering all instances contained in
	 * the given set of indexes.
	 * 
	 * @param attributeIndex the index of the attribute on which to perform the aggregation
	 * @param clusterIndexes a list of indexes of the instances in the buffer to take into account
	 * @return the aggregated value (computed as the mode)
	 */
	private double aggregateNominalAttributeForInstances(final int attributeIndex, final List<Integer> clusterIndexes) {
		//map of possible (nominal) values of the attribute with their number of appearances:
		Map<Double, Integer> valueCounter = new HashMap<Double, Integer>();
		
		//count values appearance to compute mode
		for (int i = 0; i < clusterIndexes.size(); ++i){
			Instance instance = instancesBuffer.get(clusterIndexes.get(i));
			Double attributeValue = instance.value(attributeIndex);
			
			int tempCount = 0;
			if (valueCounter.containsKey(attributeValue)){
				tempCount = valueCounter.get(attributeValue);
			}
			valueCounter.put(attributeValue, tempCount + 1);
		}
		
		//get maximum count and mode
		double mode = 0.0;
		int maxCount = 0;
		for (Map.Entry<Double, Integer> valueCount : valueCounter.entrySet()) {
			Double value = valueCount.getKey();
			Integer count = valueCount.getValue();
			if (count > maxCount){
				maxCount = count;
				mode = value;
			}
		}
		
		return mode;
	}
	
}
