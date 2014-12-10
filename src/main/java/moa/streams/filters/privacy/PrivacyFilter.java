package moa.streams.filters.privacy;

import java.io.Serializable;

import moa.core.InstancesHeader;
import moa.streams.InstanceStream;
import moa.streams.filters.AbstractStreamFilter;
import moa.streams.filters.privacy.estimators.disclosurerisk.BufferedIndividualRecordLinker;
import moa.streams.filters.privacy.estimators.disclosurerisk.DisclosureRiskEstimator;
import moa.streams.filters.privacy.estimators.informationloss.InformationLossEstimator;
import moa.streams.filters.privacy.estimators.informationloss.SSEEstimator;
import weka.core.Instance;

public abstract class PrivacyFilter extends AbstractStreamFilter {
	
	/** Please see {@link Serializable} */
	private static final long serialVersionUID = 5485907750792490539L;

	/** The input {@link InstanceStream} */
	private final InstanceStream inputStream;
	
	/** The algorithm that will perform the anonymization process */
	private final AnonymizationAlgorithm anonymizationAlgorithm;
	
	/** The estimator of the information loss due to the anonymization */
	private InformationLossEstimator informationLossEstimator;
	
	/** The estimator of the disclosure risk of the ouput stream of instances */
	private DisclosureRiskEstimator disclosureRiskEstimator;
	
	/**
	 * Builds a privacy filter with the given algorithms and estimators.
	 * 
	 * @param inputStream please see: {@link #inputStream}
	 * @param anonymizationAlgorithm please see: {@link #anonymizationAlgorithm}
	 * @param informationLossEstimator please see: {@link #informationLossEstimator}
	 * @param disclosureRiskEstimator please see: {@link #disclosureRiskEstimator}
	 */
	public PrivacyFilter(InstanceStream inputStream, AnonymizationAlgorithm anonymizationAlgorithm,
			InformationLossEstimator informationLossEstimator, DisclosureRiskEstimator disclosureRiskEstimator) {
		this.inputStream = inputStream;
		this.anonymizationAlgorithm = anonymizationAlgorithm;
		this.informationLossEstimator = informationLossEstimator;
		this.disclosureRiskEstimator = disclosureRiskEstimator;
		algorithmSetup();
	}
	
	/**
	 * Glues the {@link #inputStream} to the {@link #anonymizationAlgorithm}.
	 */
	private void algorithmSetup() {
		this.anonymizationAlgorithm.setInputStream(this.inputStream);
	}
	
	/**
	 * Builds a privacy filter with default estimators. More precisely, the {@link #informationLossEstimator}
	 * is set to be a {@link SSEEstimator} and the {@link #disclosureRiskEstimator} is set to be an instance
	 * of the {@link BufferedIndividualRecordLinker}.
	 * 
	 * @param inputStream please see: {@link #inputStream}
	 * @param anonymizationAlgorithm please see: {@link #anonymizationAlgorithm}
	 */
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
	
	/**
	 * Retrieves the {@link DisclosureRiskEstimator} used in this filter. This
	 * method can be useful if the estimator can be customized.
	 * 
	 * @return the diclosure risk estimator being used in this filter
	 */
	public DisclosureRiskEstimator getDisclosureRiskEstimator() {
		return disclosureRiskEstimator;
	}
	
	/**
	 * Retrieves the {@link InformationLossEstimator} used in this filter. This
	 * method can be useful if the estimator can be customized.
	 * 
	 * @return the information loss estimator being used in this filter
	 */
	public InformationLossEstimator getInformationLossEstimator() {
		return informationLossEstimator;
	}
	
	/**
	 * Retrieves the current information loss estimation, as given by the {@link #informationLossEstimator}.
	 * 
	 * @return the current total information loss estimation
	 */
	public double getCurrentInformationLoss() {
		return informationLossEstimator.getCurrentInformationLoss();
	}
	
	/**
	 * Retrieves the last increment in the information loss estimation, as given by 
	 * the {@link #informationLossEstimator}.
	 * 
	 * @return the last increment in the information loss estimation
	 */
	public double getIncrementalInformationLoss() {
		return informationLossEstimator.getIncrementalInformationLoss();
	}

	/**
	 * Retrieves the current disclosure risk estimation, as given by {@link #informationLossEstimator}.
	 * 
	 * @return the current estimation for the disclosure risk metric
	 */
	public double getCurrentDisclosureRisk() {
		return disclosureRiskEstimator.getCurrentDisclosureRisk();
	}
	
	/**
	 * Configures this privacy filter to use the given {@link DisclosureRiskEstimator}.
	 * <p>
	 * <b>WARNING!</b> If you are configuring the filter after began processing instances,
	 * please call {@link #restartImpl()} in order to reset all needed resources and methods.
	 * 
	 * @param disclosureRiskEstimator the estimator to be used
	 */
	public void setDisclosureRiskEstimator(DisclosureRiskEstimator disclosureRiskEstimator) {
		this.disclosureRiskEstimator = disclosureRiskEstimator;
	}
	
	/**
	 * Configures this privacy filter to use the given {@link InformationLossEstimator}.
	 * <p>
	 * <b>WARNING!</b> If you are configuring the filter after began processing instances,
	 * please call {@link #restartImpl()} in order to reset all needed resources and methods.
	 * 
	 * @param informationLossEstimator the estimator to be used
	 */
	public void setInformationLossEstimator(InformationLossEstimator informationLossEstimator) {
		this.informationLossEstimator = informationLossEstimator;
	}
	
}
