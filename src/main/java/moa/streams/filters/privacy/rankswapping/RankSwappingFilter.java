package moa.streams.filters.privacy.rankswapping;
import java.util.ArrayList;
import java.util.Random;

import weka.core.Instance;
import moa.core.InstancesHeader;
import moa.options.IntOption;
import moa.streams.filters.AbstractStreamFilter;
import moa.streams.filters.privacy.IndexValuePair;

import java.util.Vector;

public class RankSwappingFilter extends AbstractStreamFilter{

/*
	private class Pair implements Comparable<Pair>{
		public int index;
		public double value;
		public Pair(){
			new Pair(0,0);
		}
		
		public Pair(int index, double value){
			this.index = index;
			this.value = value;
		}
	
		public boolean equals(Object o){
			if (o instanceof Pair)
				return true;
			else return false;
		}
		
		public String toString(){
			String st = "index: ";
			return st+index+" value: "+value;
		}

		@Override
		public int compareTo(Pair b) {
			if (value < b.value)
				return 1;
			else if(value == b.value)
				return 0;
			else return -1;
		}

	}
	
*/	
	
    protected Vector<Instance> bufferInstances;
    protected Vector< Vector<Boolean> > alreadySwapped;
    protected boolean startToProcess;
    protected Random random;
	
    public RankSwappingFilter(){
    	super();
    	this.random = new Random(this.randomSeedOption.getValue());
    	bufferInstances = new Vector<Instance>();
    	alreadySwapped =  new Vector<Vector<Boolean>>();
    	startToProcess = false;
    }
    
	@Override
	protected void restartImpl() {
		this.random = new Random(this.randomSeedOption.getValue());
    	bufferInstances = new Vector<Instance>();
    	alreadySwapped =  new Vector<Vector<Boolean>>();
    	startToProcess = false;
		// TODO Auto-generated method stub
		
	}
    
    
	@Override
    public String getPurposeString() {
        return "Protects data performing rank swapping on the stream.";
    }
    
    public IntOption randomSeedOption = new IntOption("randomSeed", 'r',
            "Seed for swapping.", 1);
    
    public IntOption pOption = new IntOption("windowPermutation", 'p',
            "window for the random permutation.", 2);
    
    /*
     * length of the buffer considered to perform the swapping 
     * */
    public IntOption bOption = new IntOption("bufferLength", 'b',
            "length of the historical buffer considered to perform rank swapping", 10);
    
    private static final long serialVersionUID = 1L;

	@Override
	public InstancesHeader getHeader() {
		return this.inputStream.getHeader();
		//return null;
	}

	@Override
	public Instance nextInstance() {
		
		if (bufferInstances.size() == bOption.getValue() )
			startToProcess = true;
		
		
		if (this.inputStream.hasMoreInstances()){
			Instance inst = (Instance) this.inputStream.nextInstance().copy();
		
			Vector<Boolean> vec = new Vector<Boolean>();
			for (int i=0; i < inst.numAttributes()-1; ++i) vec.add(false);
		
			bufferInstances.add(inst);
			alreadySwapped.add(vec);
		}
		
		if (startToProcess){
			
			for (int i=0; i<bufferInstances.get(0).numAttributes();++i){
				/* we swap values under two conditions:
				 * 1) is not the class attribute
				 * 2) if we did not already used swapped this instance to swap for attribute i
				 */
				if (i != bufferInstances.get(0).classIndex() && !alreadySwapped.get(0).get(i)){
					//changing the state in the alreadySwapped variable for the choosen instance inst2swap 
					//in the buffer
					int inst2swap = selectSwap(bufferInstances, alreadySwapped, i);
					alreadySwapped.get(0).set(i,true);
					alreadySwapped.get(inst2swap).set(i,true);
					
					// swap the value between the two selected instances
					// I used two variables (firstVal, secondVal) to have a clean and easy code
					double firstVal = bufferInstances.get(0).value(i);
					double secondVal = bufferInstances.get(inst2swap).value(i);
					
					bufferInstances.get(0).setValue(i,secondVal);
					bufferInstances.get(inst2swap).setValue(i,firstVal);					
					
				}
			}
			alreadySwapped.remove(0);
			return bufferInstances.remove(0);
		}else {
			// TODO Auto-generated method stub
			return null;
		}
	}

	
	private int selectSwap(Vector<Instance> bufferInstances, Vector< Vector<Boolean> > alreadySwapped, int index){	
		
		ArrayList<IndexValuePair> list = new ArrayList<IndexValuePair>();
		
		for (int i=0; i< bufferInstances.size();++i){
			if (!alreadySwapped.get(i).get(index)){
				IndexValuePair a = new IndexValuePair(i, bufferInstances.get(i).value(index) );
				list.add(a);
			}
		}
		
		java.util.Collections.sort(list);
		
		int indexToStart = 0;
		
		boolean find = false;
		
		for (int i=0; i < list.size() && !find; ++i){
			if (list.get(i).value == 0){
				indexToStart = i;
				find = true;
			}
		}
		
		//max between the parameter p and the remaining instances
		int finalWindow = Math.min(	pOption.getValue(), list.size() - (indexToStart+1));
		//System.out.println("finalWindow: "+finalWindow);
		//System.out.println("indexToStart: "+indexToStart);
		if (finalWindow == 0)
			return list.get(indexToStart).index;
			
			
		double randomVal = this.random.nextDouble();	
		
		double unifProb = 1.0/finalWindow;
		int stepRandomlyChoosen = (int) ((randomVal / unifProb)+1);
		int selectedIndex = indexToStart + stepRandomlyChoosen;
		return list.get(selectedIndex).index;
	}
	
	@Override
	public void getDescription(StringBuilder arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean hasMoreInstances() {
		return this.inputStream.hasMoreInstances() || (bufferInstances.size()>0);
	}
	
}
