package moa.tasks;

import java.io.File;

import moa.core.ObjectRepository;
import moa.core.SerializeUtils;
import moa.options.FileOption;

public class ReadAnonymizationReport extends AbstractTask {

	private static final String SUCCESS_TASK_STATUS = 
			"Finished reading report.";
	private static final String ERROR_NO_REPORT_FILE = 
			"ERROR: no report file was provided. Please check the -r option.";
	
	/**
	 * Serializable
	 */
	private static final long serialVersionUID = 3197431816690794498L;

	public FileOption readReportOption = new FileOption("readReport", 'r', 
			"The file containing the anonymization report to read and output to the std out stream.", 
			null, null, false); 
	
	@Override
	public Class<?> getTaskResultType() {
		return String.class;
	}

	private String readReport(File reportFile) {
		try {
			String report = (String) SerializeUtils.readFromFile(reportFile);
			System.out.println(report); // write to std out, as requested!
		} catch (Exception e) {
			e.printStackTrace();
		}
		return SUCCESS_TASK_STATUS;
	}
	
	@Override
	protected Object doTaskImpl(TaskMonitor monitor, ObjectRepository repository) {
		File reportFile = readReportOption.getFile();
		if (reportFile != null) {
			return readReport(reportFile);
		}
		else {
			return ERROR_NO_REPORT_FILE;
		}
	}

	
	
}
