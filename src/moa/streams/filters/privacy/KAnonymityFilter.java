package moa.streams.filters.privacy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import moa.core.InstancesHeader;
import moa.options.IntOption;
import moa.streams.filters.AbstractStreamFilter;
import weka.core.Instance;


public class KAnonymityFilter extends AbstractStreamFilter{
	
	private static final long serialVersionUID = 6339323265758039657L;
	
	private Vector<Instance> instancesBuffer;
	
	private boolean startToProcess;
	
	private Vector<Boolean> alreadyAnonymizedInstances;
	
	/**
	 * The 'K' value for the k-anonymity property to satisfy.
	 */
	public IntOption kAnonymityValueOption = new IntOption("kAnonimty", 'k',
            "k value for the k-anonymity", 3);
	
	/**
	 * The size of the historical buffer considered before starting to perform the filtering.
	 */
    public IntOption bufferSizeOption = new IntOption("bufferLength", 'b',
            "length of the historical buffer considered to perform rank swapping", 10);
	
	public KAnonymityFilter(){
    	super();
    	this.instancesBuffer = new Vector<Instance>();
    	this.alreadyAnonymizedInstances =  new Vector<Boolean>();
    	this.startToProcess = false;
    }
	
	@Override
	protected void restartImpl() {
		this.instancesBuffer = new Vector<Instance>();
		this.alreadyAnonymizedInstances =  new Vector<Boolean>();
		this.startToProcess = false;
	}
	
	@Override
	public InstancesHeader getHeader() {
		return this.inputStream.getHeader();
	}

	@Override
	public void getDescription(StringBuilder arg0, int arg1) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public boolean hasMoreInstances() {
		return this.inputStream.hasMoreInstances() || (instancesBuffer.size() > 0);
	}
	
	@Override
	public Instance nextInstance() {
		//fetch newer instances from the input stream
		if (this.inputStream.hasMoreInstances()){
			Instance instance = (Instance) this.inputStream.nextInstance().copy();		
			
			instancesBuffer.add(instance);
			alreadyAnonymizedInstances.add(false);
		}		
		
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
	
	/**
	 * Processes the next instance of the buffer.
	 * 
	 * @return the next anonymized instance
	 */
	private Instance processNextInstance() {
		final int top = 0; //semantic variable (it is indeed useless)
		
		//anonymize the next instance only if it is not yet anonymized
		if (!alreadyAnonymizedInstances.get(top)){
			anonymizeNextInstance();
		}
		
		//remove the instance from the buffer and from the list of anonymized instances
		alreadyAnonymizedInstances.remove(top);
		Instance anonymizedInstance = instancesBuffer.remove(top);
		return anonymizedInstance;
	}
	
	/**
	 * Anonymizes the next instance.
	 */
	private void anonymizeNextInstance() {
		final int top = 0; //semantic variable (it is indeed useless)
		
		List<IndexValuePair> kNearestNeighbors = new ArrayList<IndexValuePair>();
		Instance queryInstance = instancesBuffer.get(top);
		
		//iterate over all the instances in the actual buffer (except the top one)
		for (int i = 1; i < instancesBuffer.size();++i){
			//consider only instance not yet anonymized
			if (!alreadyAnonymizedInstances.get(i)){
				double distanceToQuery = distance(queryInstance, instancesBuffer.get(i));
				
				//check if we select more than K-1 neighbors, if yes delete the farthest one
				if (kNearestNeighbors.size() != (kAnonymityValueOption.getValue() - 1) 
						|| distanceToQuery <= kNearestNeighbors.get(kNearestNeighbors.size() - 1).value) {
					kNearestNeighbors.add(new IndexValuePair(i, distanceToQuery));
					Collections.sort(kNearestNeighbors);
				}
				
				if (kNearestNeighbors.size() >= kAnonymityValueOption.getValue()){
					kNearestNeighbors.remove(kNearestNeighbors.size()-1);
				}
			}
		}
		
		//anonymize the query instances and its k-1 nearest neighbors
		List<Integer> idx = new ArrayList<Integer>();
		idx.add(top);
		for (int i=0; i<kNearestNeighbors.size();++i) {
			idx.add( kNearestNeighbors.get(i).index);
			alreadyAnonymizedInstances.set(kNearestNeighbors.get(i).index,true);
		}
		
		for (int j=0; j < queryInstance.numAttributes();++j){
			if (j != queryInstance.classIndex()){//if it is not the class
				double avg = 0;
				double modeValue = 0.0;
				int countMaxModeValue = 0;
				Map<Double,Integer> map = new HashMap<Double,Integer>();
				for (int i=0; i<idx.size();++i){
					if (queryInstance.attribute(j).isNumeric()){
						avg+= instancesBuffer.get(idx.get(i)).value(j);
					}else{
						Integer temp = 0;
						if (map.containsValue(instancesBuffer.get(idx.get(i)).value(j))){
							temp = map.get(instancesBuffer.get(idx.get(i)).value(j));
						}
						map.put(instancesBuffer.get(idx.get(i)).value(j),temp+1);
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
				if (!queryInstance.attribute(j).isNumeric())newValue = modeValue;
				
				for (int i=0; i<idx.size();++i){
						instancesBuffer.get(idx.get(i)).setValue(j,newValue);
				}
			}
		}
	}

	/**
	 * Computes the distance between two given instances.
	 * 
	 * @param x the first instance
	 * @param y the second instance
	 * @return the distance between the given instances
	 */
	private double distance(Instance x, Instance y){
		double dist = 0;
		for (int i=0; i < x.numAttributes(); ++i){
			if (i != x.classIndex()) { //skip all those variables that are the target class
				if (x.attribute(i).isNumeric()){
					dist += (x.value(i) - y.value(i)) * (x.value(i) - y.value(i));
				} else {
					dist += (x.value(i) !=  y.value(i)) ? 1.0 : 0.0;
				}
			}
		}
		return Math.sqrt(dist);
	}
	
	
}


