package moa.streams.filters.privacy;

import java.io.Serializable;

import moa.core.InstancesHeader;
import moa.core.ObjectRepository;
import moa.options.ClassOption;
import moa.options.FlagOption;
import moa.streams.filters.AbstractStreamFilter;
import moa.streams.filters.privacy.estimators.disclosurerisk.BufferedIndividualRecordLinker;
import moa.streams.filters.privacy.estimators.disclosurerisk.DisclosureRiskEstimator;
import moa.streams.filters.privacy.estimators.informationloss.InformationLossEstimator;
import moa.streams.filters.privacy.estimators.informationloss.SSEEstimator;
import moa.tasks.TaskMonitor;
import weka.core.Instance;

public abstract class PrivacyFilter extends AbstractStreamFilter implements AnonymizationFilter {
	
	/** Please see {@link Serializable} */
	private static final long serialVersionUID = 5485907750792490539L;
	
	/** The option for the estimator of the information loss */
	public ClassOption informationLossEstimatorOption = new ClassOption("informationLossEstimator", 
			'I', "The estimator of the information loss due to the anonymization process.", 
			InformationLossEstimator.class, "SSEEstimator");
	
	/** The estimator of the information loss due to the anonymization */
	private InformationLossEstimator informationLossEstimator;
	
	/** The option for the estimator of the disclosure risk */
	public ClassOption disclosureRiskEstimatorOption = new ClassOption("disclosureRiskEstimator", 
			'D', "The estimator of the disclosure risk of the output stream of instances.", 
			DisclosureRiskEstimator.class, "BufferedIndividualRecordLinker");
	
	/** The estimator of the disclosure risk of the ouput stream of instances */
	private DisclosureRiskEstimator disclosureRiskEstimator;
	
	/** The option for the ability to enable the performance of an evaluation concerning
	 * Disclosure Risk and Information Loss by the {@code PrivacyFilter} */
	public FlagOption evaluationEnabledOption = new FlagOption("evaluationEnabled",
			'E', "If set, this flag option enables the calculation of the IL and DR metrics by the privacy filter.");
	
	/**
	 * Builds a privacy filter with default estimators. ({@link SSEEstimator} and
	 *  {@link BufferedIndividualRecordLinker}).
	 */
	public PrivacyFilter() {
		//empty constructor
	}
	
	@Override
	public void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		//prepare the estimators of the filter
		if (evaluationEnabledOption.isSet()) {
			this.informationLossEstimator = 
					(InformationLossEstimator) getPreparedClassOption(informationLossEstimatorOption);
			this.disclosureRiskEstimator =
					(DisclosureRiskEstimator) getPreparedClassOption(disclosureRiskEstimatorOption);
		}
		
		//prepare the anonymization filter concrete implementation (subclasses)
		prepareAnonymizationFilterForUse();
	}
	
	@Override
	public InstancesHeader getHeader() {
		return inputStream.getHeader();
	}
	
	@Override
	public Instance nextInstance() {
		InstancePair instancePair = nextAnonymizedInstancePair();
		if (instancePair != null) {
			if (evaluationEnabledOption.isSet()) {
				informationLossEstimator.performEstimationForInstances(instancePair);
				disclosureRiskEstimator.performEstimationForInstances(instancePair);
			}
			return instancePair.anonymizedInstance;
		}
		else {
			return null;
		}
	}

	@Override
	protected void restartImpl() {
		if (evaluationEnabledOption.isSet()) {
			informationLossEstimator.restart();
			disclosureRiskEstimator.restart();
		}
		// call for the PrivacyFilter subclass to do the necessary
		//  steps to restart the filter
		restartFilter();
	}
	
	public Evaluation getEvaluation() throws EvaluationNotEnabledException {
		if (evaluationEnabledOption.isSet()) {
			return new AnonymizationEvaluation(
				disclosureRiskEstimator.getCurrentDisclosureRisk(), 
				informationLossEstimator.getIncrementalInformationLoss(), 
				informationLossEstimator.getCurrentInformationLoss()
			);
		}
		else {
			throw new EvaluationNotEnabledException("Evaluation is not enabled for this privacy filter.");
		}
	}
	
	/**
	 * Retrieves the {@link DisclosureRiskEstimator} used in this filter or {@code null}
	 * if the evaluation is not enabled for the filter. This method can be useful if the 
	 * estimator can be customized.
	 * 
	 * @return the diclosure risk estimator being used in this filter or {@code null}
	 * if the evaluation is not enabled for the filter
	 */
	public DisclosureRiskEstimator getDisclosureRiskEstimator() {
		return disclosureRiskEstimator;
	}
	
	/**
	 * Retrieves the {@link InformationLossEstimator} used in this filter or {@code null}
	 * if the evaluation is not enabled for the filter. This method can be useful if the 
	 * estimator can be customized.
	 * 
	 * @return the information loss estimator being used in this filter or {@code null}
	 * if the evaluation is not enabled for the filter
	 */
	public InformationLossEstimator getInformationLossEstimator() {
		return informationLossEstimator;
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
	
	public class EvaluationNotEnabledException extends Exception {
		private static final long serialVersionUID = 9000383025619373897L;
		public EvaluationNotEnabledException() {
			super();
		}
		public EvaluationNotEnabledException(String message) {
			super(message);
		}
	}
	
}
