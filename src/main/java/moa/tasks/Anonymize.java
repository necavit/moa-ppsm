package moa.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.io.Writer;

import moa.core.InstancesHeader;
import moa.core.ObjectRepository;
import moa.options.ClassOption;
import moa.options.FileOption;
import moa.options.FlagOption;
import moa.options.IntOption;
import moa.streams.InstanceStream;
import moa.streams.filters.privacy.PrivacyFilter;
import moa.tasks.MainTask;
import moa.tasks.TaskMonitor;
import weka.core.Instance;

public class Anonymize extends MainTask {
	
	private static final long serialVersionUID = 5157111181251282973L;
	
	private static final String PURPOSE_STRING = ""; //TODO
	
	public ClassOption filterOption = new ClassOption("filter", 'f',
            "Privacy filter to be applied.", PrivacyFilter.class, 
            "noiseaddition.NoiseAdditionFilter"); //TODO when this is refactored, check the package of the default filter!!
	
	public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to be filtered.", InstanceStream.class,
            "generators.RandomRBFGenerator");
	
	public IntOption maxInstancesOption = new IntOption("maxInstances", 'm',
            "Maximum number of instances to write to file.", 10000000, 0,
            Integer.MAX_VALUE);

    public FlagOption suppressHeaderOption = new FlagOption("suppressHeader",
            'h', "Suppress header from output.");
    
    public FileOption arffFileOption = new FileOption("arffFile", 'a',
            "Destination ARFF file for the anonymized dataset.", "anonymized", "arff", true);
    
    public Anonymize() {
    	outputFileOption = new FileOption("anonymizationResultFile", 'O',
                "File to save the final report of the anonymization process.", "anonymizationReport", "moa", true);
	}
    
	@Override
	public String getPurposeString() {
		return PURPOSE_STRING;
	}
	
	@Override
	public Class<?> getTaskResultType() {
		return String.class;
	}

	@Override
	protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
		//prepare the stream and the filter
		InstanceStream stream = (InstanceStream) getPreparedClassOption(streamOption);
		PrivacyFilter filter = (PrivacyFilter) getPreparedClassOption(filterOption);
		filter.setInputStream(stream);
		
		//write the anonymized instances to a file
		File arffFile = arffFileOption.getFile();
        if (arffFile != null) {
        	int anonymizedInstances = 0;
        	try {
                Writer writer = new BufferedWriter(new FileWriter(arffFile));
                monitor.setCurrentActivityDescription("Writing anonymized stream to ARFF file");
                
                //write the stream header if appropriate 
                if (!suppressHeaderOption.isSet()) {
                    writer.write(stream.getHeader().toString());
                    writer.write("\n");
                }
                
                //request instances up to the maximum
                while ((anonymizedInstances < maxInstancesOption.getValue()) 
                		&& filter.hasMoreInstances()) {
                	Instance instance = filter.nextInstance();
                	if (instance != null) {
                		writer.write(instance.toString());
                        writer.write("\n");
                        anonymizedInstances++;
                	}
                	else {
                		//the filter might be using a buffered processing scheme
                	}
                }
                writer.close();
            } catch (Exception ex) {
                throw new RuntimeException(
                        "Failed writing to file " + arffFile, ex);
            }
    		
        	//build and return the report
    		AnonymizationReport report = 
    			new AnonymizationReport(stream.getHeader(), 
    									arffFile, 
    									anonymizedInstances, 
    									filter.getCurrentDisclosureRisk(), 
    									filter.getCurrentInformationLoss());
    		return report.toString();
        }
        throw new IllegalArgumentException("No destination file to write to.");
	}
	
	public class AnonymizationReport implements Serializable {

		private static final long serialVersionUID = 2174640107819519190L;
		
		private final int anonymizedInstances;
		private final double disclosureRisk;
		private final double informationLoss;
		private final File arffOutputFile;
		private final InstancesHeader streamHeader;
		
		public AnonymizationReport(InstancesHeader streamHeader, File arffOutputFile,
				int anonymizedInstances, double disclosureRisk, double informationLoss) {
			this.anonymizedInstances = anonymizedInstances;
			this.arffOutputFile = arffOutputFile;
			this.disclosureRisk = disclosureRisk;
			this.informationLoss = informationLoss;
			this.streamHeader = streamHeader;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder(512);
			builder.append("**** Anonymization task completed. ****\n");
			builder.append(anonymizedInstances + " instances have been anonymized from the stream with header:\n");
			builder.append(streamHeader.toString());
			builder.append("and have been stored in the file: " + arffOutputFile.getPath() + "\n");
			builder.append("(estimation) Total disclosure risk:  " + String.format("%.12f", disclosureRisk) + "\n");
			builder.append("(estimation) Total information loss: " + String.format("%.12f", informationLoss) + "\n");
			builder.append("***************************************\n");
			return builder.toString();
		}
		
	}

}
