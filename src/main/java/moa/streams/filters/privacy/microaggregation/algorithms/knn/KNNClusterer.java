package moa.streams.filters.privacy.microaggregation.algorithms.knn;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import weka.core.Instance;

public class KNNClusterer {
	
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
	public static List<Integer> getNextKNNClusterIndexes(final int k,
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
				double distanceToTarget = distance(targetInstance, instances.get(i));
				
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
	private static double distance(Instance x, Instance y){
		double dist = 0.0;
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
	
	private static final class DistanceIndexPair implements Comparable<DistanceIndexPair> {
		
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
