package utils;

import moa.streams.InstanceStream;
import moa.streams.generators.RandomRBFGenerator;

public class StreamGeneratorFactory {

	public static InstanceStream getRBFStream() {
		RandomRBFGenerator rbfStream = new RandomRBFGenerator();
		return rbfStream;
	}
	
}
