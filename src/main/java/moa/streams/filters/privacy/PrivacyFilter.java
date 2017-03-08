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

public abstract class PrivacyFilter extends AbstractStreamFilter {
	
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
			//check if the estimators are null - an external restart because of the
			// enabling of the metrics feature could cause a crash due to a NullPointerException
			if (informationLossEstimator == null) {
				informationLossEstimator = 
						(InformationLossEstimator) getPreparedClassOption(informationLossEstimatorOption);
			} else {
				informationLossEstimator.restart();
			}
			if (disclosureRiskEstimator == null) { 
				disclosureRiskEstimator = 
						(DisclosureRiskEstimator) getPreparedClassOption(disclosureRiskEstimatorOption);
			} else {
				disclosureRiskEstimator.restart();
			}
		}
		// call for the PrivacyFilter subclass to do the necessary
		//  steps to restart the filter
		restartAnonymizationFilter();
	}
	
	/**
	 * If the Disclosure Risk (DR) and Information Loss (IL) evaluation is enabled, through
	 * {@link #isEvaluationEnabled()}, the corresponding DR and IL estimators are requested
	 * to perform an evaluation over the last anonymized instance.
	 * 
	 * @return a {@link PrivacyEvaluation} containing the necessary information
	 * @throws EvaluationNotEnabledException if the DR and IL evaluation is not enabled
	 */
	public PrivacyEvaluation getEvaluation() throws EvaluationNotEnabledException {
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
	 * if the evaluation is not enabled for the filter (see {@link #isEvaluationEnabled()}). 
	 * This method can be useful if the estimator can be customized.
	 * 
	 * @return the diclosure risk estimator being used in this filter or {@code null}
	 * if the evaluation is not enabled for the filter (see {@link #isEvaluationEnabled()})
	 */
	public DisclosureRiskEstimator getDisclosureRiskEstimator() {
		return disclosureRiskEstimator;
	}
	
	/**
	 * Retrieves the {@link InformationLossEstimator} used in this filter or {@code null}
	 * if the evaluation is not enabled for the filter (see {@link #isEvaluationEnabled()}).
	 * This method can be useful if the estimator can be customized.
	 * 
	 * @return the information loss estimator being used in this filter or {@code null}
	 * if the evaluation is not enabled for the filter (see {@link #isEvaluationEnabled()})
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
	
	/**
	 * Utility function to test whether or not the Disclosure Risk (DR) and Information Loss (IL)
	 * estimation feature is enabled for this filter. It is equivalent to calling:
	 * {@code filter.evaluationEnabledOption.isSet()}
	 * @return {@code true} if the feature is enabled, {@code false} otherwise
	 */
	public boolean isEvaluationEnabled() {
		return evaluationEnabledOption.isSet();
	}
	
	/**
	 * Exception class representing an error that is triggered when a user of a {@link PrivacyFilter}
	 * requests for a {@link PrivacyEvaluation} while not having enabled the feature
	 * (see {@link #isEvaluationEnabled()}).
	 */
	public class EvaluationNotEnabledException extends Exception {
		private static final long serialVersionUID = 9000383025619373897L;
		public EvaluationNotEnabledException() {
			super();
		}
		public EvaluationNotEnabledException(String message) {
			super(message);
		}
	}
	
	/**
	 * Anonymizes the next instance in the stream and returns it along with the original
	 * instance, for evaluation purposes. Be aware that the returned object might be
	 * {@code null} if the filter uses a buffered strategy.
	 * 
	 * @return the pair of anonymized and original instances or {@code null} if the filter
	 * uses a buffered strategy and the buffer is not yet full
	 */
	public abstract InstancePair nextAnonymizedInstancePair();
	
	/** Hook method to allow an anonymization filter to prepare for its future use. */
	public abstract void prepareAnonymizationFilterForUse();
	
	/** Hook method to allow an anonymization filter to be restarted to be used in the future. */
	public abstract void restartAnonymizationFilter();
	
}
