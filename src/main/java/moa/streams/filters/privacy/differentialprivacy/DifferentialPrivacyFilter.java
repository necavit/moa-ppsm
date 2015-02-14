package moa.streams.filters.privacy.differentialprivacy;

import moa.streams.InstanceStream;
import moa.streams.filters.privacy.PrivacyFilter;
import moa.streams.filters.privacy.differentialprivacy.algorithms.laplace.LaplaceNoiseAdder;

public class DifferentialPrivacyFilter extends PrivacyFilter {
	
	private static final long serialVersionUID = 2884176064303386530L;
	
	public DifferentialPrivacyFilter(InstanceStream inputStream, long randomSeed, double epsilon) {
		super(inputStream, 
				new LaplaceNoiseAdder(randomSeed, epsilon));
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub
	}

}
