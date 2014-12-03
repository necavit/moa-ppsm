package moa.streams.filters.privacy;

import moa.core.InstancesHeader;
import moa.streams.InstanceStream;
import moa.streams.filters.AbstractStreamFilter;
import moa.streams.filters.privacy.estimators.disclosurerisk.BufferedIndividualRecordLinker;
import moa.streams.filters.privacy.estimators.disclosurerisk.DisclosureRiskEstimator;
import moa.streams.filters.privacy.estimators.informationloss.InformationLossEstimator;
import moa.streams.filters.privacy.estimators.informationloss.SSEEstimator;
import weka.core.Instance;

public abstract class PrivacyFilter extends AbstractStreamFilter {
	
	private static final long serialVersionUID = 5485907750792490539L;

	private final InstanceStream inputStream;
	
	private final AnonymizationAlgorithm anonymizationAlgorithm;
	
	private final InformationLossEstimator informationLossEstimator;
	
	private final DisclosureRiskEstimator disclosureRiskEstimator;
	
	public PrivacyFilter(InstanceStream inputStream, AnonymizationAlgorithm anonymizationAlgorithm,
			InformationLossEstimator informationLossEstimator, DisclosureRiskEstimator disclosureRiskEstimator) {
		this.inputStream = inputStream;
		this.anonymizationAlgorithm = anonymizationAlgorithm;
		this.informationLossEstimator = informationLossEstimator;
		this.disclosureRiskEstimator = disclosureRiskEstimator;
		algorithmSetup();
	}
	
	private void algorithmSetup() {
		this.anonymizationAlgorithm.setInputStream(this.inputStream);
	}
	
	public PrivacyFilter(InstanceStream inputStream, AnonymizationAlgorithm anonymizationAlgorithm) {
		this(inputStream, anonymizationAlgorithm, 
				new SSEEstimator(), //default information loss estimator 
				new BufferedIndividualRecordLinker()); //default disclosure risk estimator
	}
	
	@Override
	public InstancesHeader getHeader() {
		return inputStream.getHeader();
	}

	@Override
	public boolean hasMoreInstances() {
		return anonymizationAlgorithm.hasMoreInstances();
	}
	
	@Override
	public Instance nextInstance() {
		InstancePair instancePair = anonymizationAlgorithm.nextAnonymizedInstancePair();
		if (instancePair != null) {
			informationLossEstimator.estimateInformationLossForInstancePair(instancePair);
			disclosureRiskEstimator.estimateDisclosureRiskForInstancePair(instancePair);
			return instancePair.anonymizedInstance;
		}
		else {
			return null;
		}
	}

	@Override
	protected void restartImpl() {
		anonymizationAlgorithm.restart();
		informationLossEstimator.restart();
		disclosureRiskEstimator.restart();
	}
	
	/**
	 * Retrieves the {@link AnonymizationAlgorithm} used in this filter. This
	 * method can be useful if the algorithm can be customized (using options).
	 * 
	 * @return the algorithm being used in this filter
	 */
	public AnonymizationAlgorithm getAnonymizationAlgorithm() {
		return anonymizationAlgorithm;
	}
	
	public double getCurrentInformationLoss() {
		return informationLossEstimator.getCurrentInformationLoss();
	}
	
	public double getIncrementalInformationLoss() {
		return informationLossEstimator.getIncrementalInformationLoss();
	}

	public double getCurrentDisclosureRisk() {
		return disclosureRiskEstimator.getCurrentDisclosureRisk();
	}
	
}
