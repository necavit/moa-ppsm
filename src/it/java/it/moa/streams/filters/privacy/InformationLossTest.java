package it.moa.streams.filters.privacy;

import it.moa.streams.filters.privacy.PrivacyFilterExecutor.FilteredInstanceExecutor;
import moa.streams.filters.AbstractStreamFilter;
import moa.streams.filters.privacy.microaggregation.KAnonymityFilter;
import moa.streams.generators.RandomRBFGenerator;
import weka.core.Instance;

public class InformationLossTest {

	public static void main(String [] args) {
		KAnonymityFilter kaFilter = new KAnonymityFilter();
		kaFilter.kAnonymityValueOption.setValue(5);
		kaFilter.bufferSizeOption.setValue(100);
		
		RandomRBFGenerator stream = new RandomRBFGenerator();
		stream.prepareForUse();
		
		new PrivacyFilterExecutor(
			kaFilter,
			stream,
			1000, 
			new FilteredInstanceExecutor() {
				@Override
				public void onInstanceFiltered(int ithProcessedInstance, Instance instance, AbstractStreamFilter filter) {
					if (ithProcessedInstance % 1 == 0) {
						System.out.println("i=" + ithProcessedInstance 
								+ "  error=" + ((KAnonymityFilter)filter).currentSquaredError
								+ "  increment=" + ((KAnonymityFilter)filter).incrementalSquaredError);
					}
				}
			}
		).execute();
		
	}
	
}
