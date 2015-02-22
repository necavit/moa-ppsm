package moa.streams.filters.privacy;

public class AnonymizationEvaluation implements Evaluation {

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
	
}
