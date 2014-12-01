package it.moa.streams.filters.privacy;

import moa.core.InstancesHeader;
import moa.streams.InstanceStream;
import moa.streams.filters.AbstractStreamFilter;
import weka.core.Instance;

public class PrivacyFilterExecutor {

	private final AbstractStreamFilter filter;
	
	private final InstanceStream stream;
	
	private final int n; //number of instances
	
	private final FilteredInstanceExecutor executor;
	
	public PrivacyFilterExecutor(AbstractStreamFilter filter, InstanceStream stream, 
			int numberOfInstances, FilteredInstanceExecutor filteredInstanceExecutor) {
		this.filter = filter;
		this.n = numberOfInstances;
		this.stream = stream;
		this.executor = filteredInstanceExecutor;
		filterSetup();
	}

	private void filterSetup() {
		filter.setInputStream(stream);
		filter.prepareForUse();
	}
	
	public interface FilteredInstanceExecutor {
		
		public void onInstanceFiltered(int ithProcessedInstance, Instance instance, AbstractStreamFilter filter);
		
	}
	
	public void execute() {
		printStreamSummary();
		int instanceCounter = 0;
		while (filter.hasMoreInstances() && instanceCounter < n) {
			Instance instance = filter.nextInstance();
			if (instance != null) {
				++instanceCounter;
				executor.onInstanceFiltered(instanceCounter, instance, filter);
			}
		}
	}
	
	private void printStreamSummary() {
		System.out.println("======================= STREAM SUMMARY =======================");
		InstancesHeader header = stream.getHeader();
		System.out.println(header.toSummaryString());
		System.out.println("==============================================================");
	}

}
