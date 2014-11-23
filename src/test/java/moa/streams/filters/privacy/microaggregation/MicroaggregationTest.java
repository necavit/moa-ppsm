package moa.streams.filters.privacy.microaggregation;

import static org.junit.Assert.assertTrue;

import moa.streams.InstanceStream;

import org.junit.Before;
import org.junit.Test;

import utils.StreamGeneratorFactory;

public class MicroaggregationTest {

	private KAnonymityFilter filter;
	
	private InstanceStream stream;
	
	@Before
	public void setup() {
		stream = StreamGeneratorFactory.getRBFStream();
		filter = new KAnonymityFilter(stream);
	}
	
	@Test
	public void foo() {
		assertTrue(true);
	}
	
}
