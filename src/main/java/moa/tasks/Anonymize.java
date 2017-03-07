package moa.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import moa.core.ObjectRepository;
import moa.options.ClassOption;
import moa.options.FileOption;
import moa.options.FlagOption;
import moa.options.IntOption;
import moa.streams.InstanceStream;
import moa.streams.filters.privacy.PrivacyEvaluation;
import moa.streams.filters.privacy.PrivacyFilter;
import moa.streams.filters.privacy.PrivacyFilter.EvaluationNotEnabledException;
import weka.core.Instance;

public class Anonymize extends MainTask {
	
	/** Serializable ID */
	private static final long serialVersionUID = 5157111181251282973L;
	
	private static final String PURPOSE_STRING = "Task to anonymize a stream of data, writing it to a file once" +
			" it is anonymized. An evaluation is also performed, based on the estimated disclosure risk of the " +
			"output dataset, along with an estimation of the information loss due to the anonymization process.";
	
	private static final String MONITOR_INITIAL_STATE = "Anonymizing stream...";
	
	private static final String MONITOR_EVALUATION_STATE = "i: %d, dr: %.3f, iil: %.2f, il: %.2f";

	private static final String MONITOR_UPDATE_STATE = "i: %d";
	
	/* **** **** **** TASK OPTIONS **** **** **** */
	
	/* **** Filter options **** */
	public ClassOption filterOption = new ClassOption("filter", 'f',
            "Privacy filter to be applied.", PrivacyFilter.class, 
            "noiseaddition.NoiseAdditionFilter -c 0.0 -a 0.25");
	
	/* **** Stream options **** */
	public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to be filtered.", InstanceStream.class,
            "generators.RandomRBFGenerator");
	
	public IntOption maxInstancesOption = new IntOption("maxInstances", 'm',
            "Maximum number of instances to process. If set to -1, keep processing until the" +
            " input stream runs out of instance (no maximum is set).", -1, -1, Integer.MAX_VALUE);
	
	/* **** Evaluation options **** */
	public IntOption evaluationUpdateRateOption = new IntOption("evaluationUpdateRate", 'u',
			"Number of instances to skip between anonymization evaluation updates.", 100, 1, Integer.MAX_VALUE);

    public FileOption evaluationFileOption = new FileOption("evaluationFile", 'e', 
    		"Destination CSV file for the anonymization process evaluation.", null, "csv", true);
    
    /* **** Anonymized output options **** */
    public FileOption arffFileOption = new FileOption("arffFile", 'a',
            "Destination ARFF file for the anonymized dataset.", "anonymized", "arff", true);
    
    public FlagOption suppressHeaderOption = new FlagOption("suppressHeader",
            'h', "Suppress header from output.");
    
    /* **** Report plaintext file options **** */
    public FileOption reportFileOption = new FileOption("reportFile", 'r', 
    		"Destination plain text file for the anonymization report.", null, "txt", true);
    
    /* **** **** **** **** **** **** **** **** **** */
    
    public Anonymize() {
    	// empty constructor
	}
    
	@Override
	public String getPurposeString() {
		return PURPOSE_STRING;
	}
	
	@Override
	public Class<?> getTaskResultType() {
		return String.class;
	}
	
	private File getFileWithExtension(FileOption fileOption) {
		File file = fileOption.getFile();
		if (file != null) {
			String filePath = file.getPath();
			if (!filePath.contains(fileOption.getDefaultFileExtension())) {
				filePath = filePath + "." + fileOption.getDefaultFileExtension();
				file = new File(filePath);
			}
		}
		return file;
	}
	
	/**
	 * Creates a writer for a file that is specified by the provided file option. If there is no
	 * specified file in the option (the default file is {@code null}), then no writer is created
	 * and {@code null} is returned.
	 * 
	 * @param fileOption the option in which the file is specified
	 * @return a writer to the file or {@code null} 
	 */
	private Writer getWriterForFileOption(FileOption fileOption) {
		File file = getFileWithExtension(fileOption);
		if (file != null) {
			try {
				return new BufferedWriter(new FileWriter(file));
			} catch (IOException e) {
				String message = String.format("Failed to open file: %s", file.getName());
				throw new RuntimeException(message, e);
			}
		}
		else return null;
	}
	
	private void writeToFile(Writer writer, String content) throws IOException {
		if (writer != null) {
			writer.write(content);
			writer.write("\n");
		}
	}
	
	private void closeWriter(Writer writer) throws IOException {
		if (writer != null) {
			writer.flush();
			writer.close();
		}
	}
	
	private boolean keepProcessing(long processedInstances, PrivacyFilter filter) {
		return filter.hasMoreInstances() &&                          // as long as the stream has instances,
				(maxInstancesOption.getValue() < 0 ||                //  process ALL instance in the stream
				processedInstances < maxInstancesOption.getValue()); //  OR process up to a maximum
	}
	
	@Override
	protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
		//prepare the stream and the filter
		InstanceStream stream = (InstanceStream) getPreparedClassOption(streamOption);
		PrivacyFilter filter = (PrivacyFilter) getPreparedClassOption(filterOption);
		filter.setInputStream(stream);
		
		//prepare the potential necessary files and variables
		Writer arffWriter = getWriterForFileOption(arffFileOption);
		Writer evaluationWriter = filter.isEvaluationEnabled() ? 
				getWriterForFileOption(evaluationFileOption) : null;
		Writer reportWriter = getWriterForFileOption(reportFileOption);
		
		try {
			//write headers for both output files
			if (!suppressHeaderOption.isSet()) {
				writeToFile(arffWriter, stream.getHeader().toString());
			}
			writeToFile(evaluationWriter, filter.getEvaluation().getEvaluationCSVHeader());
			
			//begin filtering
			monitor.setCurrentActivityDescription(MONITOR_INITIAL_STATE);
			long anonymizedInstances = 0;
			while (keepProcessing(anonymizedInstances, filter)) {
				Instance instance = filter.nextInstance();
				if (instance != null) {
					anonymizedInstances++;
					writeToFile(arffWriter, instance.toString());
					
					//update evaluation if needed (check the evaluation update rate)
					if (anonymizedInstances % evaluationUpdateRateOption.getValue() == 0) {
						//check whether the evaluation is enabled to avoid exceptions
						if (filter.isEvaluationEnabled()) {
							PrivacyEvaluation evaluation = filter.getEvaluation();
							writeToFile(evaluationWriter, evaluation.getEvaluationCSVRecord());
							monitor.setCurrentActivityDescription(
								String.format(MONITOR_EVALUATION_STATE, anonymizedInstances,
										evaluation.getDisclosureRisk(),
										evaluation.getIncrementalInformationLoss(),
										evaluation.getInformationLoss())
							);
						}
						else {
							monitor.setCurrentActivityDescription(
								String.format(MONITOR_UPDATE_STATE, anonymizedInstances));
						}
					}
				}
			}
			//get the necessary data for the report
			double disclosureRisk = 0.0;
			double informationLoss = 0.0;
			if (filter.isEvaluationEnabled()) {
				PrivacyEvaluation finalEvaluation = filter.getEvaluation();
				disclosureRisk = finalEvaluation.getDisclosureRisk();
				informationLoss = finalEvaluation.getInformationLoss();
			}
			
			//build the report
			String report = 
				getAnonymizationReport(
					stream.getHeader() == null ?  "error: no stream header available" : stream.getHeader().toString(),
					arffWriter == null ? true : false,
					evaluationWriter == null ? true : false,
					anonymizedInstances,
					disclosureRisk, informationLoss
				); 
			
			//write the report
			writeToFile(reportWriter, report);
			
			//flush and close the writer streams
			closeWriter(arffWriter);
			closeWriter(evaluationWriter);
			closeWriter(reportWriter);
			
			//return the report
			return report;
		} catch (IOException e) {
			throw new RuntimeException("Failed to complete the task.", e);
		} catch (EvaluationNotEnabledException e) {
			throw new RuntimeException("An evaluation was requested, but the estimators were disabled", e);
		}
	}
	
	private String getAnonymizationReport(String streamHeader,
			boolean silencedAnonymization, boolean silencedEvaluation,
			long anonymizedInstances,
			double disclosureRisk, double informationLoss) {
		StringBuilder builder = new StringBuilder(1024);
		builder.append("**** **** **** **** **** ANONYMIZATION TASK COMPLETED **** **** **** **** ****\n");
		builder.append(anonymizedInstances);
		builder.append(" instances have been anonymized from the stream with header:\n");
		builder.append(streamHeader);
		builder.append("\n");
		if (!silencedAnonymization) {
			builder.append("and have been stored in the file: " + arffFileOption.getFile().getPath() + "\n");
		}
		if (!silencedEvaluation) {
			builder.append("Total disclosure risk:  " + String.format("%.12f", disclosureRisk) + "\n");
			builder.append("Total information loss: " + String.format("%.12f", informationLoss) + "\n");
		}
		builder.append("**** **** **** **** **** **** **** **** **** **** **** **** **** **** **** ****\n");
		return builder.toString();
	}

}
