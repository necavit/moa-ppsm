package moa.streams.filters.privacy.microaggregation.generic;

import java.util.Iterator;
import java.util.Vector;

public class Partition implements Iterable<Cluster> {

	/** The current partition of the dataset, in the form of
	 *  a list (a Vector) of lists (Vectors) of indexes of the
	 *  original dataset rows.
	 */
	private Vector<Cluster> clusters;
	
	/**
	 * Constructs an empty partition object.
	 */
	public Partition() {
		this.clusters = new Vector<Cluster>();
	}
	
	/**
	 * Retrieves the current partition (a list of clusters).
	 * 
	 * @return the current list of row clusters
	 */
	public Vector<Cluster> getClusters() {
		return clusters;
	}
	
	/**
	 * Retrieves the cluster located at the {@code clusterIndex} position.
	 * 
	 * @param clusterIndex the index of the desired cluster
	 * @return the desired cluster
	 */
	public Cluster getCluster(int clusterIndex) {
		return clusters.get(clusterIndex);
	}
	
	/**
	 * Adds a new cluster to the list of clusters and returns its index, to
	 * allow element additions.
	 * 
	 * @return the index of the newly added cluster
	 */
	public int addCluster() {
		clusters.add(new Cluster());
		return clusters.size() - 1;
	}

	@Override
	public Iterator<Cluster> iterator() {
		return clusters.iterator();
	}
	
}
