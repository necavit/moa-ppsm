package it.moa.streams.filters.privacy;

import moa.classifiers.Classifier;
import moa.classifiers.drift.SingleClassifierDrift;
import moa.streams.ArffFileStream;
import moa.streams.filters.privacy.microaggregation.KAnonymityFilter;
import weka.core.Instance;


public class ExampleExecution {
	
	public static void main(String[]args){
		
		String path = "/home/necavit/workspace_TFG/moa-ppsm/moa-ppsm/src/it/resources/prova.arff";
		
		//setting '-1' to the classIndex argument, we are telling the ArffFileStream to
		//  consider the last attribute as the class (target) one
		ArffFileStream stream = new ArffFileStream(path, -1);
		
		//FIXME delete this code if necessary
		//stream.prepareForUse();
		//int windowSize = Integer.parseInt(args[1]);
		//RankSwappingFilter kAFilter = new RankSwappingFilter();
		//kAFilter.pOption.setValue(3);
		
		//build filter
		KAnonymityFilter filter = new KAnonymityFilter(stream);
		filter.bufferSizeOption.setValue(10); //the ARFF file has very few instances!
		
		//build learner (classifier)
		Classifier learner = new SingleClassifierDrift(); 
		learner.setModelContext(filter.getHeader()); 
		learner.prepareForUse();
		
		//execute!
		while (filter.hasMoreInstances()) {
			Instance instance = filter.nextInstance();
			if (instance != null) {
				System.out.println("instance: " + instance.toString());
			}
			else {
				System.out.println("nextInstance() returned null instance: the anonymization buffer is still filling up...");
			}
		}
		
		/* TODO OLD LEGACY CODE! Remove or think what to do with it!
		double numberSamplesCorrect = 0;
		double totNumInstProcessed = 0;
		String st = "";
		//while(kAFilter.hasMoreInstances() ){ //&& numberSamples < numInstances){
		while(stream.hasMoreInstances() ){ //&& numberSamples < numInstances){
			System.out.println("processing another instance");
			//Instance temp = kAFilter.nextInstance();
			Instance temp = stream.nextInstance();
			if (temp != null){				
				if ( learner.correctlyClassifies(temp) )	numberSamplesCorrect++;
				totNumInstProcessed++;
				//if (totNumInstProcessed % 1000 == 0)
					st += "accuracy ("+ totNumInstProcessed+"): "+(numberSamplesCorrect/ totNumInstProcessed )+" \n";
				
				learner.trainOnInstance(temp);
			}		
		}
		
		System.out.println(st);
		*/
	}
}

