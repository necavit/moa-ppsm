package moa.streams.filters.privacy.noiseaddition;

import moa.streams.InstanceStream;
import moa.streams.filters.privacy.PrivacyFilter;

public class NoiseAdditionFilter extends PrivacyFilter {
	
	private static final long serialVersionUID = 94085693669724686L;

	public NoiseAdditionFilter(InstanceStream inputStream) {
		super(inputStream,
				new GaussianNoiseAdder());
	}

	@Override
	public void getDescription(StringBuilder arg0, int arg1) {
		// TODO Auto-generated method stub
	}

	
	
}
