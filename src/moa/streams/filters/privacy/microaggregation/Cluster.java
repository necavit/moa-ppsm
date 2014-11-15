package moa.streams.filters.privacy.microaggregation;

import java.util.Iterator;
import java.util.Vector;

import weka.core.Instance;

public class Cluster implements Iterable<Instance> {

	private Vector<Instance> cluster;
	
	public Cluster() {
		this.cluster = new Vector<Instance>(); 
	}
	
	public void addInstance(Instance instance) {
		cluster.add(instance);
	}
	
	public Instance get(int index) {
		return cluster.get(index);
	}
	
	public Instance getInstanceForIndex(int index) {
		return cluster.get(index);
	}
	
	public int size() {
		return cluster.size();
	}
	
	@Override
	public Iterator<Instance> iterator() {
		return cluster.iterator();
	}

}
