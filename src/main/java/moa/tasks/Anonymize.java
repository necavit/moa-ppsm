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

	private static final String THROUGHPUT_CSV_HEADER = "Instances,TotalTime[s],"
															+ "IncrInstances,IncrTime[s],Throughput[ins/s]";
	
	private static final String THROUGHPUT_CSV_RECORD = "%d,%.3f,%d,%.3f,%.2f";
	
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
    
    /* **** Throughput evaluation options **** */
    public IntOption throughputEvaluationUpdateRateOption = new IntOption("throughputUpdateRate", 'U',
			"Number of instances to skip between throughput evaluation updates.", 100, 1, Integer.MAX_VALUE);
    
    public FileOption throughputEvaluationFileOption = new FileOption("throughputEvaluationFile", 't',
    		"Destination CSV file for the evaluation of the processing throughput of the filter", null, "csv", true);
    
    /* **** Anonymized output options **** */
    public FileOption arffFileOption = new FileOption("arffFile", 'a',
            "Destination ARFF file for the anonymized dataset.", null, "arff", true);
    
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
	
	/**
	 * Gets a reference for a file specified in the provided {@code fileOption}.
	 * If no file is specified by such option, the returned reference is {@code null}.
	 * The file gets appended the default extension if the specification did not contain it
	 * 
	 * @param fileOption a {@code FileOption} representing a file
	 * @return the {@code File} represented in the option or {@code null}
	 */
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
	
	/** Writes the {@code content} to the file opened in the provided {@code writer} */
	private void writeToFile(Writer writer, String content) throws IOException {
		if (writer != null) {
			writer.write(content);
			writer.write("\n");
		}
	}
	
	/** Flushes and closes the provided {@code writer} */
	private void closeWriter(Writer writer) throws IOException {
		if (writer != null) {
			writer.flush();
			writer.close();
		}
	}
	
	/** Returns {@code true} if there are instances left in the filter or the maximum
	 *  number of instances has not been reached; {@code false} otherwise */
	private boolean keepProcessing(long processedInstances, PrivacyFilter filter) {
		return filter.hasMoreInstances() &&                          // as long as the stream has instances,
				(maxInstancesOption.getValue() < 0 ||                //  process ALL instance in the stream
				processedInstances < maxInstancesOption.getValue()); //  OR process up to a maximum
	}
	
	/** Converts a {@code long} representing milliseconds to a {@code float} representing seconds */
	private float millisToSeconds(long millis) {
		return (millis/1000.0f);
	}
	
	/** Returns the CSV file header for the throughput evaluation */
	private String getThroughputCSVHeader() {
		return THROUGHPUT_CSV_HEADER;
	}
	
	private String getThroughputCSVRecord(long instances, long totalTimeMillis, 
										  int deltaInstances, long deltaTimeMillis) {
		float totalSeconds = millisToSeconds(totalTimeMillis);
		float deltaSeconds = millisToSeconds(deltaTimeMillis);
		float throughput = deltaInstances/deltaSeconds;
		return String.format(THROUGHPUT_CSV_RECORD,
				instances, totalSeconds, deltaInstances, deltaSeconds, throughput);
	}
	
	/** Formats the multiline string that represents the report of the anonimization report */
	private String getAnonymizationReport(String streamHeader,
			boolean silencedAnonymization, boolean silencedEvaluation,
			long anonymizedInstances, long runtimeMillis,
			double disclosureRisk, double informationLoss) {
		StringBuilder builder = new StringBuilder(1024);
		builder.append("**** **** **** **** **** ANONYMIZATION TASK COMPLETED **** **** **** **** ****\n");
		builder.append(String.format("Execution time: %.3f s\n", millisToSeconds(runtimeMillis)));
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
		Writer throughputWriter = getWriterForFileOption(throughputEvaluationFileOption);
		
		try {
			//write headers for all output files
			if (!suppressHeaderOption.isSet()) {
				writeToFile(arffWriter, stream.getHeader().toString());
			}
			if (filter.isEvaluationEnabled()) { //if the evaluation is not enabled, an exception is thrown
				writeToFile(evaluationWriter, filter.getEvaluation().getEvaluationCSVHeader());
			}
			writeToFile(throughputWriter, getThroughputCSVHeader());
			
			//begin filtering
			monitor.setCurrentActivityDescription(MONITOR_INITIAL_STATE);
			long anonymizedInstances = 0; //total instance counter
			long startTime = System.currentTimeMillis(); //total runtime counter
			long prevThroughputTime = System.currentTimeMillis(); //time for the throughput evaluation
			while (keepProcessing(anonymizedInstances, filter)) {
				Instance instance = filter.nextInstance();
				if (instance != null) {
					//process the anonymized instance
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
					//update throughput evaluation if needed
					if (anonymizedInstances % throughputEvaluationUpdateRateOption.getValue() == 0) {
						//gather data
						long currThroughputTime = System.currentTimeMillis();
						long totalTime = currThroughputTime - startTime;
						long deltaTime = currThroughputTime - prevThroughputTime;
						int deltaInstances = throughputEvaluationUpdateRateOption.getValue();
						//write CSV record
						writeToFile(throughputWriter, 
									getThroughputCSVRecord(anonymizedInstances, totalTime, deltaInstances, deltaTime));
						//update the previous time stamp
						prevThroughputTime = currThroughputTime;
					}
				}
			}
			//calculate runtime
			long endTime = System.currentTimeMillis();
			long runtimeMillis = endTime - startTime;
			
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
					anonymizedInstances, runtimeMillis,
					disclosureRisk, informationLoss
				);
			
			//write the report
			writeToFile(reportWriter, report);
			
			//flush and close the writer streams
			closeWriter(arffWriter);
			closeWriter(evaluationWriter);
			closeWriter(reportWriter);
			closeWriter(throughputWriter);
			
			//return the report
			return report;
		} catch (IOException e) {
			throw new RuntimeException("Failed to complete the task.", e);
		} catch (EvaluationNotEnabledException e) {
			throw new RuntimeException("An evaluation was requested, but the estimators were disabled", e);
		}
	}
}
