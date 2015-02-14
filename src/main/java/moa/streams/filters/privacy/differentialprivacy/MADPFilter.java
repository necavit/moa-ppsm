package moa.streams.filters.privacy.differentialprivacy;

import moa.streams.InstanceStream;
import moa.streams.filters.privacy.PrivacyFilter;

public class MADPFilter extends PrivacyFilter {

	private static final long serialVersionUID = 7341024060515650696L;

	public MADPFilter(InstanceStream inputStream, int k, int bufferSizeThreshold, double epsilon) {
		super(inputStream,
				new MicroAggregationLaplaceCombinator(k, bufferSizeThreshold, epsilon));
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

}
