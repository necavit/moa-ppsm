package moa.streams.filters.privacy.rankswapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import moa.streams.filters.privacy.AnonymizationAlgorithm;
import moa.streams.filters.privacy.InstancePair;
import weka.core.Instance;

public class RankSwapper extends AnonymizationAlgorithm {

	private Vector<Instance> anonymizedInstancesBuffer;
	
	private Vector<Instance> instancesBuffer;
	
	private int bufferSize;
	
	private Vector<Vector<Boolean>> alreadySwappedValue;
	
	private boolean startToProcess;
	
	private Random randomGenerator;
	
	private long randomSeed;
	
	private int pParameter;
	
	public RankSwapper() {
		this(3141592, 100, 20);
	}
	
	public RankSwapper(long randomSeed, int bufferSize, int pParameter) {
		this.pParameter = pParameter;
		this.randomSeed = randomSeed;
		this.randomGenerator = new Random(randomSeed);
		this.bufferSize = bufferSize;
		this.startToProcess = false;
		this.alreadySwappedValue = new Vector<Vector<Boolean>>();
		this.instancesBuffer = new Vector<Instance>();
		this.anonymizedInstancesBuffer = new Vector<Instance>();
	}
	
	@Override
	public void restart() {
		this.startToProcess = false;
		this.randomGenerator = new Random(randomSeed);
		this.alreadySwappedValue = new Vector<Vector<Boolean>>();
		this.instancesBuffer = new Vector<Instance>();
		this.anonymizedInstancesBuffer = new Vector<Instance>();
	}

	@Override
	public InstancePair nextAnonymizedInstancePair() {
		//get the next instance from the stream
		fetchNextStreamInstance();
		
		//check whether to begin processing the buffer
		if (instancesBuffer.size() == bufferSize) {
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
			if (i != instancesBuffer.get(top).classIndex() && !alreadySwappedValue.get(0).get(i)){
				int instanceToSwapIndex = selectSwapForAttribute(i);
				alreadySwappedValue.get(top).set(i, true);
				alreadySwappedValue.get(instanceToSwapIndex).set(i, true);
				
				double firstValue = instancesBuffer.get(top).value(i);
				double secondValue = instancesBuffer.get(instanceToSwapIndex).value(i);
				
				anonymizedInstancesBuffer.get(top).setValue(i, secondValue);
				anonymizedInstancesBuffer.get(instanceToSwapIndex).setValue(i, firstValue);
				
			}
		}
		alreadySwappedValue.remove(0);
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
				IndexValuePair pair = new IndexValuePair(index, 
														 instancesBuffer.get(index)
														 	.value(attributeIndex));
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
		int finalWindow = Math.min(pParameter, list.size() - (indexToStart + 1));
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
