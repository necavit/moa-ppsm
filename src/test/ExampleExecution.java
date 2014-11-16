package test;
import moa.streams.ArffFileStream;
import moa.streams.filters.privacy.microaggregation.KAnonymityFilter;
import moa.streams.filters.privacy.rankswapping.RankSwappingFilter;
import moa.classifiers.drift.SingleClassifierDrift;
import moa.classifiers.*;

import java.io.File;
import java.io.ObjectInputStream.GetField;
import java.net.URISyntaxException;
import java.util.ArrayList;

import weka.core.Attribute;
import weka.core.Instance;


public class ExampleExecution {
	public static void main(String[]args){
		//String path= "/Users/dinoienco/RICERCA/project/Waikato/catChangeDet/elecNormNew.arff";
		//String path= "/Users/dinoienco/Documents/workspace/Moa/src/prova.arff";
		
		//String path= "/Users/dinoienco/RICERCA/project/Waikato/batchIncremental/elecNormNew.arff";
		
		File file = null;
		try {
			file = new File(ExampleExecution.class.getResource("prova.arff").toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		String path = file.getPath();
		
		ArffFileStream	stream	= new ArffFileStream(path,-1);
		int nAttrs = stream.getHeader().numAttributes();
		stream.classIndexOption.setValue(nAttrs-1);
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for(int i=0; i<stream.getHeader().numAttributes();++i){
			attributes.add( stream.getHeader().attribute(i));
		}
		//stream.prepareForUse();
		//int windowSize = Integer.parseInt(args[1]);
		//RankSwappingFilter kAFilter = new RankSwappingFilter();
		KAnonymityFilter kAFilter = new KAnonymityFilter();
		kAFilter.setInputStream (stream);
		//kAFilter.pOption.setValue(3);
		kAFilter.kAnonymityValueOption.setValue(3);
		kAFilter.bufferSizeOption.setValue(10);
		
		System.out.println("kOption " + kAFilter.kAnonymityValueOption.getValue());
		
		Classifier learner = new SingleClassifierDrift(); 
		learner.setModelContext(kAFilter.getHeader()); 
		learner.prepareForUse();
		
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
		
	}
}

