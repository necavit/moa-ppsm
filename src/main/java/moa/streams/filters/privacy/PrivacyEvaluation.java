package moa.streams.filters.privacy;

public interface PrivacyEvaluation {

	public double getDisclosureRisk();
	public double getInformationLoss();
	public double getIncrementalInformationLoss();
	
	// utility functions to facilitate writing to a CSV file
	public String getEvaluationCSVHeader();
	public String getEvaluationCSVRecord();
	
}
