package moa.streams.filters.privacy;

import moa.streams.InstanceStream;

public abstract class AnonymizationAlgorithm {
	
	/**
	 * The {@link InstanceStream} that the algorithm has to anonymize.
	 */
	protected InstanceStream inputStream;
	
	/**
	 * Retrieves the {@link InstanceStream} attached to this algorithm.
	 * 
	 * @return the {@link #inputStream} that is attached to the algorithm.
	 */
	public InstanceStream getInputStream() {
		return inputStream;
	}
	
	/**
	 * Attaches the given {@link InstanceStream} to the anonymization
	 * algorithm.
	 * <p>
	 * <b>NOTE:</b> this method <b>must</b> be called before any computation is
	 * performed with the algorithm.
	 * 
	 * @param inputStream the {@link InstanceStream} to be attached
	 */
	public void setInputStream(InstanceStream inputStream) {
		this.inputStream = inputStream;
	}
	
	/**
	 * Restarts the algorithm, preparing it for next use.
	 * <p>
	 * All {@link AnonymizationAlgorithm} subclasses must implement this method.
	 */
	public abstract void restart();
	
	/**
	 * Anonymizes the next instance of the {@link #inputStream} and outputs an
	 * {@link InstancePair} consisting of both the anonymized instance and the
	 * original one, in order to perform a series of estimations afterwards.
	 * <p>
	 * Be aware that some implementations may return {@code null}, because, in some
	 * cases, the anonymization process cannot be performed. Such is the case with
	 * any buffered anonymization process.
	 * <p>
	 * All {@link AnonymizationAlgorithm} subclasses must implement this method.
	 * 
	 * @return an {@link InstancePair} containing both the anonymized instance and the
	 * original one or {@code null}, if the anonymization process could not take place.
	 */
	public abstract InstancePair nextAnonymizedInstancePair();

	/**
	 * @return {@code true} if there are more instances to be anonymized; {@code false}
	 * otherwise.
	 */
	public abstract boolean hasMoreInstances();
}
