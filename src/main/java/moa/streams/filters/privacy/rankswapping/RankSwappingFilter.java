package moa.streams.filters.privacy.rankswapping;

import moa.streams.InstanceStream;
import moa.streams.filters.privacy.PrivacyFilter;

public class RankSwappingFilter extends PrivacyFilter {

	private static final long serialVersionUID = -1297345312371342588L;

	public RankSwappingFilter(InstanceStream inputStream) {
		super(inputStream, 
				new RankSwapper());
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub
	}

}
