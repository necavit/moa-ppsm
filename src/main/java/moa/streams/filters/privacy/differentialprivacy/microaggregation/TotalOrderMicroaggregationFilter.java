package moa.streams.filters.privacy.differentialprivacy.microaggregation;

import moa.streams.InstanceStream;
import moa.streams.filters.privacy.PrivacyFilter;

public class TotalOrderMicroaggregationFilter extends PrivacyFilter {

	private static final long serialVersionUID = 3916696070219573032L;

	public TotalOrderMicroaggregationFilter(InstanceStream inputStream) {
		super(inputStream, new TotalOrderKNNMicroAggregation(3, 10));
	}
	
	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub

	}

}
