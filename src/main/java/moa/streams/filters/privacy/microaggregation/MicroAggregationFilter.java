package moa.streams.filters.privacy.microaggregation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Vector;

import moa.options.IntOption;
import moa.streams.filters.privacy.InstancePair;
import moa.streams.filters.privacy.PrivacyFilter;
import moa.streams.filters.privacy.utils.Metrics;
import weka.core.Attribute;
import weka.core.Instance;

public class MicroAggregationFilter extends PrivacyFilter {
	
	/** Serializable */
	private static final long serialVersionUID = 1850306955633168543L;

	/** The <em>K</em> value for the <em>k</em>-anonymity property to be satisfied. */
	public IntOption kAnonymityValueOption = new IntOption("kAnonimty", 'k', 
			"The size of the clusters that will be used to perform the aggregation", 3, 2, Integer.MAX_VALUE);

	/** The size of the historical buffer considered before starting to perform the filtering. */
    public IntOption bufferSizeOption = new IntOption("bufferLength", 'b', 
    		"Size of the historical buffer considered for the microaggregation process", 100, 10, Integer.MAX_VALUE);
    
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

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void prepareAnonymizationFilterForUse() {
		this.instancesBuffer = new Vector<Instance>(bufferSizeOption.getValue());
		this.anonymizedInstancesBuffer = new Vector<Instance>(bufferSizeOption.getValue());
    	this.alreadyAnonymizedInstances =  new Vector<Boolean>(bufferSizeOption.getValue());
    	this.startToProcess = false;
	}

	@Override
	public void restartFilter() {
		prepareAnonymizationFilterForUse();
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
				getNextKNNClusterIndexes(kAnonymityValueOption.getValue(),
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
	
	/**
	 * Builds a list with the indexes of the instances belonging to the next cluster.
	 * The cluster is made around the first instance in the instances list.
	 * <p>
	 * An additional list, {@code skip}, must be provided. All those instances with an index such 
	 * that {@code skip[index] = true} will be skipped and not considered into the clustering process.
	 * 
	 * @param k the size of the cluster
	 * @param instances the instances to be clustered
	 * @param skip the list of instances to skip
	 * @return the list of indexes of instances in the KNN cluster
	 */
	public List<Integer> getNextKNNClusterIndexes(final int k,
												  final List<Instance> instances,
												  final List<Boolean> skip) {
		assert(instances != null);
		assert(skip != null);
		assert(instances.size() == skip.size());
		
		//target instance is always the first one
		Instance targetInstance = instances.get(0);
		
		//initialize heap of nearest neighbors, ordered by distance
		PriorityQueue<DistanceIndexPair> kNearestNeighbors = 
				new PriorityQueue<DistanceIndexPair>(Math.max(1, k));
		
		//iterate over all the instances in the actual buffer
		//  (except the top one, thus beginning from i = 1)
		for (int i = 0; i < instances.size(); ++i){
			
			//consider only instances that are not yet anonymized
			if (!skip.get(i)){
				double distanceToTarget = Metrics.distance(targetInstance, instances.get(i));
				
				if (kNearestNeighbors.size() < k) {
					//there is still room for a new instance, no matter how far or near
					//  the target it is
					kNearestNeighbors.add(new DistanceIndexPair(distanceToTarget, i));
				}
				else {
					DistanceIndexPair maxPair = kNearestNeighbors.peek();
					if (maxPair != null) {
						//check if is nearer than the current farthest one
						double currentMaxDistance = maxPair.distance;
						if (distanceToTarget < currentMaxDistance) {
							//an instance has been found that is nearer than the current maximum
							//  -> remove the maximum and set the new one
							kNearestNeighbors.poll();
							kNearestNeighbors.add(new DistanceIndexPair(distanceToTarget, i));
						}
					}
				}
			}
		}
		
		//assert that we have K - 1 elements 
		assert(kNearestNeighbors.size() <= k);
		
		//initialize return list
		List<Integer> indexesOfNearestNeighbors = new ArrayList<Integer>(kNearestNeighbors.size());
		//return the indexes of the neighbors
		for (DistanceIndexPair instance : kNearestNeighbors) {
			assert(!skip.get(instance.index));
			indexesOfNearestNeighbors.add(instance.index);
		}
		
		assert(indexesOfNearestNeighbors.contains(0));
		
		return indexesOfNearestNeighbors;
	}
	
	private final class DistanceIndexPair implements Comparable<DistanceIndexPair> {
		
		/** The distance of the instance. */
		public final Double distance;
		
		/** The index of the instance. */
		public final Integer index;
		
		/**
		 * Builds a new {@code <Double, Integer>} pair, representing
		 * the distance and index of an instance with respect to some
		 * other unspecified instance.
		 * 
		 * @param distance the distance value for the instance
		 * @param index the index of the instance
		 */
		public DistanceIndexPair(Double distance, Integer index) {
			this.distance = distance;
			this.index = index;
		}
		
		@Override
		public int compareTo(DistanceIndexPair element) {
			return this.distance.compareTo(element.distance);
		}
		
	}
	
}