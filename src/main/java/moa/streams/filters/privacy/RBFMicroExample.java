package moa.streams.filters.privacy;

import weka.core.Instance;
import moa.streams.filters.privacy.microaggregation.MicroAggregationFilter;
import moa.streams.filters.privacy.noiseaddition.NoiseAdditionFilter;
import moa.streams.generators.RandomRBFGenerator;


public class RBFMicroExample {

	public static void main(String [] args) {
		
		RandomRBFGenerator stream = new RandomRBFGenerator();
		stream.prepareForUse();
		
		//MicroAggregationFilter filter = new MicroAggregationFilter(stream);
		NoiseAdditionFilter filter = new NoiseAdditionFilter(stream, 0.25, 1.0);
		
		int instanceCounter = 0;
		int n = 1000;
		while (filter.hasMoreInstances() && instanceCounter < n) {
			Instance instance = filter.nextInstance();
			if (instance != null) {
				++instanceCounter;
				System.out.println("i=" + instanceCounter + "  " +
								   "error=" + filter.getCurrentInformationLoss() + "  " +
								   "incError=" + filter.getIncrementalInformationLoss() + "  " + 
								   "risk=" + filter.getCurrentDisclosureRisk());
			}
		}
	}
	
}
