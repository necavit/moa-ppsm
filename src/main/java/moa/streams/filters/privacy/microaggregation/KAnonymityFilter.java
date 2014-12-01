package moa.streams.filters.privacy.microaggregation;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Vector;

import moa.core.InstancesHeader;
import moa.options.IntOption;
import moa.streams.InstanceStream;
import moa.streams.filters.AbstractStreamFilter;
import weka.core.Attribute;
import weka.core.Instance;

/**
 * Privacy preserving filter that anonymizes data following a microaggregation scheme,
 * in order to achieve the <em>k</em>-anonymity property for anonymized data.
 */
public class KAnonymityFilter extends AbstractStreamFilter {
	
	/** Serial version identifier, to allow this class to be {@link Serializable}. */
	private static final long serialVersionUID = 6339323265758039657L;
	
	/** The instance buffer of the filter. */
	private Vector<Instance> instancesBuffer;
	
	/** A vector to indicate whether an instance is anonymized or not. An index (ordering)
	 * correspondance is maintained with respect to the {@link #instancesBuffer} member. */
	private Vector<Boolean> alreadyAnonymizedInstances;
	
	/** Indicates whether to start processing (anonymizing) instances or not. */
	private boolean startToProcess;
	
	/** The <em>K</em> value for the <em>k</em>-anonymity property to be satisfied. */
	public final IntOption kAnonymityValueOption;
	
	/** The size of the historical buffer considered before starting to perform the filtering. */
    public final IntOption bufferSizeOption;
    
    /** The default constant value for the {@link #kAnonymityValueOption} parameter. The <em>K</em>
     * value is set to {@code 3}, the name is {@code kAnonymity} and the command line option is
     * the character {@code 'k'}. */
    public static final IntOption DEFAULT_K_ANONYMITY_OPTION = 
    		new IntOption("kAnonimty", 'k', "k value for the k-anonymity", 3);
	
    /** The default constant value for the {@link #bufferSizeOption} parameter. The size
     * value is set to {@code 100}, the name is {@code bufferLength} and the command line option is
     * the character {@code 'b'}. */
    public static final IntOption DEFAULT_BUFFER_SIZE_OPTION =
    		new IntOption("bufferLength", 'b', "length of the historical buffer considered to " +
    				"begin the microaggregation process", 100);
    
    /** Information Loss measure, calculated as the SSE (Sum of Squared Errors) of the anonymized
     * instances with respect to the original values. */
    public double currentSquaredError;
    
    /**
     * Holds the value of the increment of the total squared error ({@link #currentSquaredError}) with
     * respect to its last value.
     */
    public double incrementalSquaredError;
    
    /**
     * Builds a new <em>k</em>-anonymity filter to anonymize data using a microaggregation scheme.
     * <br>
     * Sets default values for all filter parameters (see {@link #DEFAULT_K_ANONYMITY_OPTION} and 
	 * {@link KAnonymityFilter#DEFAULT_BUFFER_SIZE_OPTION}). Note that  the input {@link InstanceStream} 
	 * remains uninitialized. Please make sure that you call {@link #setInputStream(InstanceStream)} on 
	 * this filter instance after its creation.
     */
	public KAnonymityFilter(){
    	this(DEFAULT_K_ANONYMITY_OPTION, DEFAULT_BUFFER_SIZE_OPTION);
    }
	
	/**
	 * Builds a new <em>k</em>-anonymity filter to anonymize data using a microaggregation scheme.
	 * <br>
	 * Sets the given filter parameters, but the input {@link InstanceStream} remains uninitialized.
	 * Please make sure that you call {@link #setInputStream(InstanceStream)} on this filter
	 * instance after its creation.
	 * 
	 * @param kAnonymityValueOption an {@link IntOption} representing the <em>K</em> value for 
	 * the <em>k</em>-anonymity property to be satisfied
	 * @param bufferSizeOption an {@link IntOption} representing the size of the historical buffer 
	 * considered before starting to perform the filtering.
	 */
	public KAnonymityFilter(IntOption kAnonymityValueOption, IntOption bufferSizeOption) {
		super();
		this.instancesBuffer = new Vector<Instance>();
    	this.alreadyAnonymizedInstances =  new Vector<Boolean>();
    	this.startToProcess = false;
    	this.kAnonymityValueOption = kAnonymityValueOption;
    	this.bufferSizeOption = bufferSizeOption;
    	this.currentSquaredError =  0.0;
    	this.incrementalSquaredError = 0.0;
	}
	
	/**
	 * Builds a new <em>k</em>-anonymity filter to anonymize data using a microaggregation scheme.
	 * <br>
	 * Uses the given {@link InstanceStream} as the input stream and sets default values for the 
	 * remaining parameters (see {@link #DEFAULT_K_ANONYMITY_OPTION} and 
	 * {@link KAnonymityFilter#DEFAULT_BUFFER_SIZE_OPTION}).
	 * 
	 * @param inputStream the {@link InstanceStream} that this filter will anonymize
	 */
	public KAnonymityFilter(InstanceStream inputStream){
    	this(inputStream, DEFAULT_K_ANONYMITY_OPTION, DEFAULT_BUFFER_SIZE_OPTION);
    }
	
	/**
	 * Builds a new <em>k</em>-anonymity filter to anonymize data using a microaggregation scheme.
	 * <br>
	 * Uses the given {@link InstanceStream} as the input stream and sets the given values for the 
	 * <em>k</em>-anonymity and buffer size parameters.
	 * 
	 * @param inputStream the {@link InstanceStream} that this filter will anonymize
	 * @param kAnonymityValueOption an {@link IntOption} representing the <em>K</em> value for 
	 * the <em>k</em>-anonymity property to be satisfied
	 * @param bufferSizeOption an {@link IntOption} representing the size of the historical buffer 
	 * considered before starting to perform the filtering.
	 */
	public KAnonymityFilter(InstanceStream inputStream, IntOption kAnonymityValueOption, IntOption bufferSizeOption) {
		this(kAnonymityValueOption, bufferSizeOption);
		setInputStream(inputStream);
	}
	
	@Override
	protected void restartImpl() {
		this.instancesBuffer = new Vector<Instance>();
		this.alreadyAnonymizedInstances =  new Vector<Boolean>();
		this.startToProcess = false;
		this.currentSquaredError = 0.0;
		this.incrementalSquaredError = 0.0;
	}
	
	@Override
	public InstancesHeader getHeader() {
		return this.inputStream.getHeader();
	}

	@Override
	public void getDescription(StringBuilder arg0, int arg1) {
		// Auto-generated method stub
	}
	
	@Override
	public boolean hasMoreInstances() {
		return this.inputStream.hasMoreInstances() || (instancesBuffer.size() > 0);
	}
	
	@Override
	public Instance nextInstance() {
		//fetch newer instances from the input stream
		if (this.inputStream.hasMoreInstances()){
			Instance instance = (Instance) this.inputStream.nextInstance().copy();		
			
			instancesBuffer.add(instance);
			alreadyAnonymizedInstances.add(false);
		}
		
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
	
	/**
	 * Processes the next instance of the buffer, anonymizing it if necessary.
	 * 
	 * @return the next anonymized instance
	 */
	private Instance processNextInstance() {
		final int top = 0; //semantic variable (it is indeed useless)
		
		//anonymize the next instance only if it is not yet anonymized
		if (!alreadyAnonymizedInstances.get(top)){
			double previousError = currentSquaredError;
			anonymizeNextInstance();
			incrementalSquaredError = currentSquaredError - previousError;
		}
		
		//remove the instance from the buffer and from the list of anonymized instances
		alreadyAnonymizedInstances.remove(top);
		Instance anonymizedInstance = instancesBuffer.remove(top);
		return anonymizedInstance;
	}
	
	/**
	 * Anonymizes the next instance of the buffer, using a microaggregation based procedure.
	 */
	private void anonymizeNextInstance() {
		final int top = 0; //semantic variable (it is indeed useless)
		
		//centroid or target instance of the cluster to be anonymized
		Instance targetInstance = instancesBuffer.get(top);
		
		//get the indexes of the k - 1 nearest neighbors from the top (target) instance
		//  to form a cluster to be anonymized (k - 1 because the k-th instance of 
		//  the cluster is the target one)
		List<Integer> clusterIndexes = getKNearestNeighborsForTopInstance(targetInstance);
		
		//add the target instance index to form the final cluster
		clusterIndexes.add(top);
		
		//aggregate (anonymize) the instances of the cluster
		anonymizeClusterWithIndexes(clusterIndexes);
	}
	
	/**
	 * Builds a list of the indexes of instances corresponding to the K-Nearest Neighbors of the
	 * given target instance.
	 * 
	 * @param targetInstance the instance around which the clustering is performed
	 * @return the list of indexes of the cluster
	 */
	private List<Integer> getKNearestNeighborsForTopInstance(final Instance targetInstance) {
		//initialize heap of nearest neighbors, ordered by distance
		PriorityQueue<DistanceIndexPair> kNearestNeighbors = 
				new PriorityQueue<DistanceIndexPair>(Math.max(1, kAnonymityValueOption.getValue() - 1));
		
		//iterate over all the instances in the actual buffer
		//  (except the top one, thus beginning from i = 1)
		for (int i = 1; i < instancesBuffer.size(); ++i){
			
			//consider only instances that are not yet anonymized
			if (!alreadyAnonymizedInstances.get(i)){
				double distanceToTarget = distance(targetInstance, instancesBuffer.get(i));
				
				if (kNearestNeighbors.size() < (kAnonymityValueOption.getValue() - 1)) {
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
		assert(kNearestNeighbors.size() <= (kAnonymityValueOption.getValue() - 1));
		
		//initialize return list
		List<Integer> indexesOfNearestNeighbors = new ArrayList<Integer>(kNearestNeighbors.size());
		//return the indexes of the neighbors
		for (DistanceIndexPair instance : kNearestNeighbors) {
			indexesOfNearestNeighbors.add(instance.index);
		}
		
		return indexesOfNearestNeighbors;
	}
	
	/**
	 * Performs an anonimization process to all instances of the given index list (a cluster):
	 * <ol>
	 *   <li>Sets to {@code true} the corresponding entries of the {@link #alreadyAnonymizedInstances} buffer</li>
	 *   <li>For each attribute, computes an aggregated value, considering all values from the instances of 
	 *   the cluster.</li>
	 *   <li>Imputes the aggregated value to all instances of the cluster.</li>
	 *   <li>Sums the squared error to the global error counter.</li>
	 * </ol> 
	 * 
	 * @param clusterIndexes the indexes of the instances of the cluster
	 */
	private void anonymizeClusterWithIndexes(List<Integer> clusterIndexes) {
		//flag as anonymized the instances of the cluster
		for (Integer index : clusterIndexes) {
			alreadyAnonymizedInstances.set(index, true);
		}
		
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
					
					//calculate error
					sumError(newValue, instance.value(attributeIndex));
					
					//replace value
					instance.setValue(attributeIndex, newValue);
				}
			}
		}
	}
	
	/**
	 * Adds the squared error for the given values to the global error counter ({@link #currentSquaredError})
	 * 
	 * @param aggregatedValue the distorted value
	 * @param originalValue the original value
	 */
	private void sumError(final double aggregatedValue, final double originalValue) {
		double error = originalValue - aggregatedValue;
		double squaredError = error * error;
		currentSquaredError = currentSquaredError + squaredError;
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
	 * Computes the distance between two given instances, using the following scheme:
	 * <br>
	 * <pre>{@code
	 *   procedure distance(x,y):
	 *     dist := 0
	 *     for each attribute i in x:
	 *       if i != targetAttribute:
	 *         if isNumeric(i):
	 *           dist := dist + (x_i - y_i)^2
	 *         else:
	 *           if x_i != y_i:
	 *             dist := dist + 1
	 *     dist := sqrt(dist)
	 *     return dist
	 * }</pre>
	 * 
	 * @param x the first instance
	 * @param y the second instance
	 * @return the distance between the given instances
	 */
	private double distance(Instance x, Instance y){
		double dist = 0;
		for (int i = 0; i < x.numAttributes(); ++i){
			if (i != x.classIndex()) { //skip all those variables that are the target class
				if (x.attribute(i).isNumeric()){
					dist += (x.value(i) - y.value(i)) * (x.value(i) - y.value(i));
				} else {
					dist += (x.value(i) !=  y.value(i)) ? 1.0 : 0.0;
				}
			}
		}
		return Math.sqrt(dist);
	}
	
	/**
	 * Pair used to implement the heap (priority queue) needed to perform the K-Nearest Neighbors
	 * clustering of the KAnonymity filter.
	 * <p>
	 * Besides storing the required values in some more "semantic" and inmutable variables,
	 * this class implements the {@link Comparable} interface, thus enabling proper sorting
	 * properties of objects of this class.
	 */
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