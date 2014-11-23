package moa.streams.filters.privacy.microaggregation.generic.algorithms;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import moa.streams.filters.privacy.microaggregation.generic.Partition;
import moa.streams.filters.privacy.microaggregation.generic.PartitionAlgorithm;
import weka.core.Instance;

public class MDAVAlgorithm implements PartitionAlgorithm {

	private final Partition partition;
	
	public MDAVAlgorithm() {
		this.partition = new Partition();
	}
	
	@Override
	public Partition performPartition(Vector<Instance> originalInstances, int k) {
		//TODO
		/*
		 * The following implementation is specified in: Domingo-Ferrer and Torra, 
		 * 2005, "Ordinal, Continuous and Heterogeneous k-Anonymity Through 
		 * Microaggregation", Springer, pg 203.
		 */
		
		//add all instance indexes into the remaining instances set
		Set<Integer> remainingInstances = new HashSet<Integer>(originalInstances.size());
		for (int i = 0; i < originalInstances.size(); ++i) {
			remainingInstances.add(i);
		}
		
		while (remainingInstances.size() >= (3*k)) {
			Instance averageInstance = computeAverageInstance(originalInstances, remainingInstances);
			
			//calculate distances from the average instance
			SortedMap<Double, Integer> distanceOfInstance = new TreeMap<Double, Integer>();
			for (Integer instanceIndex : remainingInstances) {
				double distance = distance(originalInstances.get(instanceIndex), averageInstance); 
				distanceOfInstance.put(distance, instanceIndex);
			}
			
			//sort instances by distance (in ascending order)
			Vector<Integer> sortedInstancesIndices = new Vector<Integer>(distanceOfInstance.size());
			for (Entry<Double, Integer> entry : distanceOfInstance.entrySet()) {
				sortedInstancesIndices.add(entry.getValue());
			}
			
		}
		
		return partition;
	}
	
	private Instance computeAverageInstance(final Vector<Instance> originalInstances, Set<Integer> remainingInstances) {
		//TODO
		return null;
	}
	
	private double distance(Instance x, Instance y) {
		//TODO
		return 0.0;
	}

	@Override
	public boolean isPartitionAnonymized() {
		return false;
	}

}
