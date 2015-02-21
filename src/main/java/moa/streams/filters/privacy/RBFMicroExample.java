package moa.streams.filters.privacy;

import moa.streams.filters.privacy.noiseaddition.NoiseAdditionFilter;
import moa.streams.generators.RandomRBFGenerator;
import weka.core.Instance;


public class RBFMicroExample {

	public static void main(String [] args) {
		
		RandomRBFGenerator stream = new RandomRBFGenerator();
		stream.prepareForUse();
		
		System.out.println(stream.getHeader().toSummaryString());
		
		//MicroAggregationFilter filter = new MicroAggregationFilter(stream);
		NoiseAdditionFilter filter = new NoiseAdditionFilter(0.1, 0.5);
		filter.setInputStream(stream);
		//RankSwappingFilter filter = new RankSwappingFilter(stream, 3141592, 100, 50);
		//DifferentialPrivacyFilter filter = new DifferentialPrivacyFilter(stream, 321868435, 0.1);
		//MADPFilter filter = new MADPFilter(stream, 100, 100, 0.1);
		/*
		BufferedIndividualRecordLinker recordLinker = 
				(BufferedIndividualRecordLinker) filter.getDisclosureRiskEstimator();
		recordLinker.setBufferSize(100);
		recordLinker.restart();
		*/
		
		int instanceCounter = 0;
		int n = 1000;
		while (filter.hasMoreInstances() && instanceCounter < n) {
			Instance instance = filter.nextInstance();
			if (instance != null) {
				++instanceCounter;
				System.out.println("i=" + instanceCounter + "  " +
								   "error=" + String.format("%.12f", filter.getCurrentInformationLoss()) + "  " +
								   "incError=" + filter.getIncrementalInformationLoss() + "  " + 
								   "risk=" + filter.getCurrentDisclosureRisk());
			}
		}
	}
	
}

//                                         SSE      DR
// no microaggregation   20512995.975807413000   0.012
// 3-microaggregation     9835429.127378795000   0.015
// 5-microaggregation     5644522.629816993000   0.016
//10-microaggregation     5086344.790240509000   0.014
//100-microaggregation      33855.762674396894   0.039
