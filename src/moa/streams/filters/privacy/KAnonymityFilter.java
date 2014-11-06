package moa.streams.filters.privacy;
import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;
import java.util.PriorityQueue;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import moa.core.InstancesHeader;
import moa.options.IntOption;
import moa.streams.filters.AbstractStreamFilter;
import weka.core.Instance;


public class KAnonymityFilter extends AbstractStreamFilter{
	
	
	protected Vector<Instance> bufferInstances;
	protected boolean startToProcess;
	protected Vector<Boolean> alreadyAnonymized;
	
	
	public KAnonymityFilter(){
    	super();
    	//this.random = new Random(this.randomSeedOption.getValue());
    	bufferInstances = new Vector<Instance>();
    	alreadyAnonymized =  new Vector<Boolean>();
    	startToProcess = false;
    }
	
	@Override
	protected void restartImpl() {
		//this.random = new Random(this.randomSeedOption.getValue());
    	bufferInstances = new Vector<Instance>();
    	alreadyAnonymized =  new Vector<Boolean>();
    	startToProcess = false;
	}
	
	public IntOption kOption = new IntOption("kAnonimty", 'k',
            "k value for the k-anonymity", 3);
    
    /*
     * length of the buffer considered to perform the swapping 
     * */
    public IntOption bOption = new IntOption("bufferLength", 'b',
            "length of the historical buffer considered to perform rank swapping", 10);
	
	
	
	
	@Override
	public InstancesHeader getHeader() {
		return this.inputStream.getHeader();
	}

	@Override
	public Instance nextInstance() {
		int top = 0;
		if (bufferInstances.size() == bOption.getValue() )
			startToProcess = true;

		if (this.inputStream.hasMoreInstances()){
			Instance inst = (Instance) this.inputStream.nextInstance().copy();
		
			Vector<Boolean> vec = new Vector<Boolean>();
			for (int i=0; i < inst.numAttributes()-1; ++i) vec.add(false);
			
			bufferInstances.add(inst);
			alreadyAnonymized.add(false);
		}		
		
		if (startToProcess){
			
			if (!alreadyAnonymized.get(top)){//anonymized the instance only if it is not yet anonymized
				ArrayList<Pair> topK = new  ArrayList<Pair>();
				Instance query = bufferInstances.get(top);
				//iterate over all the instances in the actual buffer
				for (int i=1; i<bufferInstances.size();++i){
					//consider only instance not yet anonymized
					if (!alreadyAnonymized.get(i)){
						double dist = distance(query,bufferInstances.get(i));
						
						//check if we select more than K-1 neighbors, if yes delete the farthest one
						if ( topK.size() != (kOption.getValue()-1) || dist <= topK.get(topK.size()-1).value){
							topK.add(new Pair(i, dist));
							Collections.sort(topK);
						}
						
						if (topK.size() >= kOption.getValue()){
							topK.remove(topK.size()-1);
						}
					}
				}
				//anonymized the query instances and its k-1 nearest neighbors
				ArrayList<Integer> idx = new ArrayList<Integer>();
				idx.add(top);
				for (int i=0; i<topK.size();++i) {
					idx.add( topK.get(i).index);
					alreadyAnonymized.set(topK.get(i).index,true);
				}
				
				for (int j=0; j < query.numAttributes();++j){
					if (j != query.classIndex()){//if it is not the class
						double avg = 0;
						double modeValue = 0.0;
						int countMaxModeValue = 0;
						HashMap<Double,Integer> map = new HashMap<Double,Integer>();
						for (int i=0; i<idx.size();++i){
							if (query.attribute(j).isNumeric()){
								avg+= bufferInstances.get(idx.get(i)).value(j);
							}else{
								Integer temp = 0;
								if (map.containsValue(bufferInstances.get(idx.get(i)).value(j))){
									temp = map.get(bufferInstances.get(idx.get(i)).value(j));
								}
								map.put(bufferInstances.get(idx.get(i)).value(j),temp+1);
							}
						}
						for (Map.Entry<Double, Integer> entry : map.entrySet()) {
							Double key = entry.getKey();
							Integer value = entry.getValue();
							if (value > countMaxModeValue){
								countMaxModeValue = value;
								modeValue = key;
							}
						}
						double newValue = avg = avg / idx.size();
						if (!query.attribute(j).isNumeric())newValue = modeValue;
						
						for (int i=0; i<idx.size();++i){
								bufferInstances.get(idx.get(i)).setValue(j,newValue);
						}
					}
				}
			}
			
			
			alreadyAnonymized.remove(0);
			return bufferInstances.remove(0);
		}else {
			return null;
		}
	}

	@Override
	public void getDescription(StringBuilder arg0, int arg1) {
		// TODO Auto-generated method stub
	
	}



	@Override
	public boolean hasMoreInstances() {
		return this.inputStream.hasMoreInstances() || (bufferInstances.size()>0);
	}

	private double distance(Instance a, Instance b){
		double dist = 0;
		for (int i=0; i < a.numAttributes();++i){
			if (i != a.classIndex()){
				if (a.attribute(i).isNumeric()){
					dist+= (a.value(i) - b.value(i)) * (a.value(i) - b.value(i));
				}else{
					dist+= (a.value(i) !=  b.value(i))?1:0;
				}
			}
		}
		return Math.sqrt(dist);
	}
	
	
}


