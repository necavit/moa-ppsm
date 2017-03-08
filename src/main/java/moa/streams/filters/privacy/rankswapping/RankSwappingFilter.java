package moa.streams.filters.privacy.rankswapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import moa.options.IntOption;
import moa.streams.filters.privacy.InstancePair;
import moa.streams.filters.privacy.PrivacyFilter;
import weka.core.Instance;

public class RankSwappingFilter extends PrivacyFilter {
	
	private static final long serialVersionUID = -1297345312371342588L;

	public IntOption bufferSizeOption = new IntOption("bufferSize", 'b', 
			"The size of the buffer that is used to perform the rank swapping procedure.", 
			100, 10, Integer.MAX_VALUE);
	
	public IntOption pParameterOption = new IntOption("pParameter", 'p', 
			"The swap range limit: a swap is always performed between records at most p positions away in the rank.", 
			10, 1, 100);
	
	public IntOption randomSeedOption = new IntOption("randomSeed", 'r', 
			"The pseudo-random generator seed.", 3141592, Integer.MIN_VALUE, Integer.MAX_VALUE);
	
	private Vector<Instance> anonymizedInstancesBuffer;
	private Vector<Instance> instancesBuffer;
	private Vector<Vector<Boolean>> alreadySwappedValue;
	private boolean startToProcess;
	private Random randomGenerator;

	@Override
	public void prepareAnonymizationFilterForUse() {
		this.startToProcess = false;
		this.randomGenerator = new Random(randomSeedOption.getValue());
		this.alreadySwappedValue = new Vector<Vector<Boolean>>();
		this.instancesBuffer = new Vector<Instance>();
		this.anonymizedInstancesBuffer = new Vector<Instance>();
	}

	@Override
	public void restartAnonymizationFilter() {
		prepareAnonymizationFilterForUse();
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public InstancePair nextAnonymizedInstancePair() {
		//get the next instance from the stream
		fetchNextStreamInstance();
		
		//check whether to begin processing the buffer
		if (instancesBuffer.size() == bufferSizeOption.getValue()) {
			startToProcess = true;
		}
		
		//process or return null
		if (startToProcess){
			//return the next anonymized instance
			return processNextInstance();
		}
		else {
			//no instance can be returned if the buffer is not yet prepared to be processed
			return null;
		}
	}
	
	private void fetchNextStreamInstance() {
		//fetch newer instances from the input stream
		if (this.inputStream.hasMoreInstances()){
			//perform 2 copies, one for each instance buffer
			Instance originalInstance = (Instance) this.inputStream.nextInstance().copy();		
			Instance anonymizableInstance = (Instance) originalInstance.copy();
			
			//add instances to buffer and boolean flag indicating that it is not
			// yet anonymized
			instancesBuffer.add(originalInstance);
			anonymizedInstancesBuffer.add(anonymizableInstance);
			Vector<Boolean> swapped = new Vector<Boolean>(originalInstance.numAttributes() - 1);
			for (int i = 0; i < originalInstance.numAttributes() - 1; ++i) {
				swapped.add(false);
			}
			alreadySwappedValue.add(swapped);
		}
	}
	
	private InstancePair processNextInstance() {
		final int top = 0;
		
		for (int i = 0; i < instancesBuffer.get(top).numAttributes(); ++i){
			if (i != instancesBuffer.get(top).classIndex() && !alreadySwappedValue.get(top).get(i)){
				int instanceToSwapIndex = selectSwapForAttribute(i);
				alreadySwappedValue.get(top).set(i, true);
				alreadySwappedValue.get(instanceToSwapIndex).set(i, true);
				
				double firstValue = instancesBuffer.get(top).value(i);
				double secondValue = instancesBuffer.get(instanceToSwapIndex).value(i);
				
				anonymizedInstancesBuffer.get(top).setValue(i, secondValue);
				anonymizedInstancesBuffer.get(instanceToSwapIndex).setValue(i, firstValue);
				
			}
		}
		alreadySwappedValue.remove(top);
		InstancePair instancePair = 
				new InstancePair(instancesBuffer.remove(top), 
						         anonymizedInstancesBuffer.remove(top));
		return instancePair;
	}

	private int selectSwapForAttribute(int attributeIndex) {
		List<IndexValuePair> list = new ArrayList<IndexValuePair>();
		
		//get values not already swapped
		for (int index = 0; index < instancesBuffer.size(); ++index) {
			if (!alreadySwappedValue.get(index).get(attributeIndex)) {
				IndexValuePair pair = 
					new IndexValuePair(index, 
									   instancesBuffer.get(index).value(attributeIndex));
				list.add(pair);
			}
		}
		//rank
		Collections.sort(list);
		
		int indexToStart = 0;
		boolean found = false;
		for (int i = 0; i < list.size() && !found; ++i) {
			if (list.get(i).index == 0){
				indexToStart = i;
				found = true;
			}
		}
		
		//max between the parameter p and the remaining instances
		int finalWindow = Math.min(pParameterOption.getValue(), list.size() - (indexToStart + 1));
		if (finalWindow == 0) {
			return list.get(indexToStart).index;
		}
		
		double randomVal = randomGenerator.nextDouble();	
		
		double unifProb = 1.0 / finalWindow;
		int stepRandomlyChoosen = (int) ((randomVal / unifProb) + 1);
		int selectedIndex = indexToStart + stepRandomlyChoosen;
		return list.get(selectedIndex).index;
	}

	@Override
	public boolean hasMoreInstances() {
		return inputStream.hasMoreInstances() || instancesBuffer.size() > 0;
	}

}