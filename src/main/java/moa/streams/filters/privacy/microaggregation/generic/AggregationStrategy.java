package moa.streams.filters.privacy.microaggregation.generic;


public interface AggregationStrategy {

	public double aggregateNumericalVariableFor(int variableIndex, Cluster cluster);
	
	public double aggregateNominalVariableFor(int variableIndex, Cluster cluster);
		
}
