package moa.streams.filters.privacy.microaggregation;


public interface AggregationStrategy {

	public double aggregateNumericalVariableFor(int variableIndex, Cluster cluster);
	
	public double aggregateNominalVariableFor(int variableIndex, Cluster cluster);
	
	public double aggregateOrdinalVariableFor(int variableIndex, Cluster cluster);
	
}
