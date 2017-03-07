package moa.streams.filters.privacy;

public final class AnonymizationEvaluation implements PrivacyEvaluation {

	private final double disclosureRisk;
	private final double incrementalInformationLoss;
	private final double informationLoss;
	
	public AnonymizationEvaluation(double disclosureRisk, double incrementalInformationLoss, 
														  double informationLoss) {
		this.disclosureRisk = disclosureRisk;
		this.incrementalInformationLoss = incrementalInformationLoss;
		this.informationLoss = informationLoss;
	}
	
	private static final String CSV_HEADER = "TotalDisclosureRisk," +
											 "IncrementalInformationLoss," +
											 "TotalInformationLoss";
	
	@Override
	public String getEvaluationCSVHeader() {
		return CSV_HEADER;
	}
	
	@Override
	public String getEvaluationCSVRecord() {
		return this.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(128);
		builder.append(String.format("%.6f", disclosureRisk));
		builder.append(",");
		builder.append(String.format("%.6f", incrementalInformationLoss));
		builder.append(",");
		builder.append(String.format("%.6f", informationLoss));
		return builder.toString();
	}
	
	@Override
	public double getDisclosureRisk() {
		return disclosureRisk;
	}
	
	@Override
	public double getIncrementalInformationLoss() {
		return incrementalInformationLoss; 
	}
	
	@Override
	public double getInformationLoss() {
		return informationLoss;
	}
	
}
