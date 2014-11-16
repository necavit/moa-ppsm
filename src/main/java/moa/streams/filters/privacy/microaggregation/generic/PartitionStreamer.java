package moa.streams.filters.privacy.microaggregation.generic;

import weka.core.Instance;

public class PartitionStreamer {

	/** The partition to be streamed. */
	private Partition partition = null;
	
	/** The index of the current cluster. */
	private int clusterIndex = 0;
	
	/** The index of the current instance from the cluster that
	 * {@link #clusterIndex} points to. */
	private int instanceFromClusterIndex = 0;
	
	public PartitionStreamer() {
		this.partition = null;
	}
	
	public PartitionStreamer(Partition partition) {
		this.partition = partition;
	}
	
	/**
	 * Sets the partition to be streamed with this streamer and resets
	 * the indexes of the streamer.
	 * 
	 * @param partition the partition to be traversed
	 */
	public void setPartition(final Partition partition) {
		this.partition = partition;
		this.clusterIndex = 0;
		this.instanceFromClusterIndex = 0;
	}
	
	/**
	 * @return {@code true} if there are more instances to be fetched from the
	 * partition; {@code false} otherwise
	 */
	public boolean hasMoreInstances() {
		//check if there is any partition to traverse yet
		if (partition == null) {
			return false;
		}
		
		if (clusterIndex < (partition.getClusters().size() - 1)) {
			//we have not traversed all the clusters yet
			return true;
		}
		else { 
			//we might be in the last cluster
			if (clusterIndex == (partition.getClusters().size() - 1)
					&& instanceFromClusterIndex < (partition.getCluster(clusterIndex).size() - 1)) {
				return true;
			}
			else {
				return false;
			}
		}
	}
	
	/**
	 * @return the next instance in the partition
	 */
	public Instance getNextInstance() {
		//retrieve instance
		Instance instance = partition
				.getCluster(clusterIndex)
				.getInstanceForIndex(instanceFromClusterIndex);
		
		//retrieve next indexes
		incrementIndexes();
		
		return instance;
	}
	
	private void incrementIndexes() {
		if (instanceFromClusterIndex == (partition.getCluster(clusterIndex).size() - 1)) {
			//jump to next cluster
			instanceFromClusterIndex = 0;
			clusterIndex++;
		}
		else {
			//keep on the same cluster
			instanceFromClusterIndex++;
		}
	}
	
}
