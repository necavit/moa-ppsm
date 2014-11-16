package moa.streams.filters.privacy.microaggregation.generic;

import java.util.Vector;

import moa.core.InstancesHeader;
import moa.options.IntOption;
import moa.streams.filters.AbstractStreamFilter;
import weka.core.Instance;

public class MicroaggregationFilter extends AbstractStreamFilter {

	/**
	 * Needed to implement Serializable interfaces.
	 */
	private static final long serialVersionUID = 4863744639402079499L;

	/** The algorithm selected to perform the partition step of the microaggregation scheme. */
	private final PartitionAlgorithm partitionAlgorithm;
	
	/** The aggregation strategy to perform the aggregation step of the microaggregation scheme. */
	private final AggregationStrategy aggregationStrategy;
	
	/** A buffer of instances to be anonymized. */
	private Vector<Instance> instancesBuffer;
	
	/** The size of the instance buffer. */
	private IntOption bufferSizeOption;
	
	/** The size of the instance buffer. */
	private IntOption kAnonymityOption;
	
	/** A helper object to stream the microaggregated data partition. */
	private PartitionStreamer partitionStreamer;
	
	/** 
	 * Builds a microaggregation filter with the given partition algorithm and aggregation strategy.
	 * All other necessary parameters, such as the instance buffer size, are set by default.
	 */
	public MicroaggregationFilter(PartitionAlgorithm partitionAlgorithm, AggregationStrategy aggregationStrategy) {
		this(partitionAlgorithm, aggregationStrategy,
				new IntOption("bufferSize", 'b', "length of the historical buffer considered to perform microaggregation", 100),
				new IntOption("kAnonymity", 'k', "size of the groups to be anonymized together (aggregated)", 3));
	}
	
	public MicroaggregationFilter(PartitionAlgorithm partitionAlgorithm, AggregationStrategy aggregationStrategy,
								  IntOption bufferSizeOption, IntOption kAnonymityOption) {
		this.instancesBuffer = new Vector<Instance>();
		this.partitionAlgorithm = partitionAlgorithm;
		this.aggregationStrategy = aggregationStrategy;
		this.bufferSizeOption = bufferSizeOption;
		this.partitionStreamer = new PartitionStreamer();
		this.kAnonymityOption = kAnonymityOption;
	}
	
	@Override
	public InstancesHeader getHeader() {
		return this.inputStream.getHeader();
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Don't know what this should actually do (Auto-generated method stub)
	}

	@Override
	protected void restartImpl() {
		this.instancesBuffer = new Vector<Instance>();
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
		}		
		
		//check whether to begin processing the buffer
		if (instancesBuffer.size() == bufferSizeOption.getValue()) {
			//TODO see if this is too costly, computationally
			microaggregate();
		}
		
		//process or return null
		if (partitionStreamer.hasMoreInstances()) {
			return partitionStreamer.getNextInstance();
		}
		else {
			//no instance can be returned if the buffer is not yet prepared to be processed
			return null;
		}
	}
	
	/*
	 * Microaggregation algorithm schema:
	 *   1. Partition:
	 *     The original set of records is partitioned in clusters.
	 *   2. Aggregation: 
	 *     For each cluster in the partition, an aggregation operator
	 *     is computed and used to replace the original records.
	 */
	
	private void microaggregate() {
		//partition
		Partition partition = 
				partitionAlgorithm.performPartition(instancesBuffer, kAnonymityOption.getValue());
		
		//invalidate buffer
		instancesBuffer.removeAllElements();
		
		if (!partitionAlgorithm.isPartitionAnonymized()) {
			//aggregate (indeed, this is the actual anonymization process)
			for (Cluster cluster : partition) {
				aggregateCluster(cluster);
			}
		}
		
		//set the partition to the streamer
		partitionStreamer.setPartition(partition);
	}
	
	private void aggregateCluster(Cluster cluster) {
		Instance instance = cluster.getInstanceForIndex(0);
		//TODO
	}
}
