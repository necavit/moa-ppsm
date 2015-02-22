package moa.streams.filters.privacy;

public interface AnonymizationFilter {

	/**
	 * Anonymizes the next instance in the stream and returns it along with the original
	 * instance, for evaluation purposes. Be aware that the returned object might be
	 * {@code null} if the filter uses a buffered strategy.
	 * 
	 * @return the pair of anonymized and original instances or {@code null} if the filter
	 * uses a buffered strategy and the buffer is not yet full
	 */
	public InstancePair nextAnonymizedInstancePair();
	
	/**
	 * Hook method to allow an anonymization filter to prepare for its future use.
	 */
	public void prepareAnonymizationFilterForUse();
	
	/**
	 * Hook method to allow an anonymization filter to be restarted to be used in the future.
	 */
	public void restartFilter();
	
}
