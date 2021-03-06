package org.openmrs.module.rwandasphstudyreports.reports;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rwandasphstudyreports.Helper;

public class BaseSPHReportConfig {
	
	public static String HIVPOSITIVEPATIENTSDELAYINLINKAGETOCAREREPORT = "22046346-3c38-11e7-a919-92ebcb67fe33";
	
	public static String PATIENTSONARTWITHNOCLINICALVISITSINLAST4MONTHSREPORT = "220465d0-3c38-11e7-a919-92ebcb67fe33";
	
	public static String PATIENTSNOTINITIATEDONART = "220466ca-3c38-11e7-a919-92ebcb67fe33";
	
	public static String OUTSTANDINGBASELINEVLREPORT = "220467a6-3c38-11e7-a919-92ebcb67fe33";
	
	public static String OUTSTANDINGBASELINECD4REPORT = "330467a6-3c38-11e7-a919-92ebcb67fe77";
	
	public static String VLBASEDTREATMENTFAILUREREPORT = "22046882-3c38-11e7-a919-92ebcb67fe33";
	
	public static String CD4BASEDTREATMENTFAILUREREPORT = "22046c4c-3c38-11e7-a919-92ebcb67fe33";
	
	public static String PATIENTSWITHNOVLAFTER8MONTHS = "22046d3c-3c38-11e7-a919-92ebcb67fe33";
	
	public static String SETUPADULTHIVCONSULTATIONSHEET = "7085a940-3c3c-11e7-a919-92ebcb67fe33";
	
	public static String RECREATE_REPORTS_ON_ACTIVATION = "rwandasphstudyreports.reCreateReportsOnActivation";

	public void deleteReportDefinition(String name) {
		ReportService rs = Context.getService(ReportService.class);

		if (rs != null) {
			for (ReportDesign rd : rs.getAllReportDesigns(false)) {
				if (name.equals(rd.getName())) {
					rs.purgeReportDesign(rd);
				}
			} 
		}
		Helper.purgeReportDefinition(name);
	}

	/**
	 * @param rd
	 * @param name,
	 *            design and dataset name of the same name
	 * @param excellOutputFileName
	 * @throws IOException
	 */
	public void setupReport(ReportDefinition rd, String name, String excellOutputFileName) throws IOException {
		ReportDesign design = Helper.createRowPerPatientXlsOverviewReportDesign(rd, excellOutputFileName, name, null);
		Properties props = new Properties();

		props.put("repeatingSections", "sheet:1,row:6,dataset:" + name);
		props.put("sortWeight", "5000");
		design.setProperties(props);

		Helper.saveReportDesign(design);
	}

	public ReportDefinition createReportDefinition(String name) {
		ReportDefinition reportDefinition = new ReportDefinition();

		reportDefinition.setName(name);
		reportDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		reportDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		return reportDefinition;
	}

}
