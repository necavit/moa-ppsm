package moa.streams.filters.privacy.microaggregation.generic;

import java.util.Vector;

import weka.core.Instance;


public interface PartitionAlgorithm {

	/**
	 * Performs a <em>partition</em> of the original data set (a vector of {@link Instance} objects)
	 * into clusters in such a way that the number of instances in each cluster is at least {@code K}.
	 * 
	 * @param originalInstances the original data set
	 * @param k the {@code K} parameter that defines the minimum cluster size
	 * @return the partition given by the algorithm (see {@link Partition})
	 */
	public Partition performPartition(final Vector<Instance> originalInstances, final int k);
	
	/**
	 * <b>NOTE:</b> partition algorithms for microaggregation <b>must</b> implement this method
	 * correctly.
	 * <br>
	 * If the partition returned by the partition algorithm (see {@link #performPartition(Vector)}) 
	 * is already anonymized (because of the algorithm definition or its construction), this method
	 * must return true. If the partition does not anonymize data in the clustering it performs, 
	 * it must return false.
	 * 
	 * @return {@code true} if the partition performed by the algorithm is already
	 * anonymized; {@code false} otherwise
	 */
	public boolean isPartitionAnonymized();
	
}
