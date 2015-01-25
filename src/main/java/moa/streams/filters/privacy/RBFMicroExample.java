package moa.streams.filters.privacy;

import moa.streams.filters.privacy.differentialprivacy.DifferentialPrivacyFilter;
import moa.streams.filters.privacy.estimators.disclosurerisk.BufferedIndividualRecordLinker;
import moa.streams.filters.privacy.noiseaddition.NoiseAdditionFilter;
import moa.streams.filters.privacy.rankswapping.RankSwappingFilter;
import moa.streams.generators.RandomRBFGenerator;
import weka.core.Instance;


public class RBFMicroExample {

	public static void main(String [] args) {
		
		RandomRBFGenerator stream = new RandomRBFGenerator();
		stream.prepareForUse();
		
		System.out.println(stream.getHeader().toSummaryString());
		
		//MicroAggregationFilter filter = new MicroAggregationFilter(stream);
		//NoiseAdditionFilter filter = new NoiseAdditionFilter(stream, 0.1, 0.5);
		//RankSwappingFilter filter = new RankSwappingFilter(stream, 3141592, 100, 50);
		DifferentialPrivacyFilter filter = new DifferentialPrivacyFilter(stream);
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
								   "error=" + filter.getCurrentInformationLoss() + "  " +
								   "incError=" + filter.getIncrementalInformationLoss() + "  " + 
								   "risk=" + filter.getCurrentDisclosureRisk());
			}
		}
	}
	
}
