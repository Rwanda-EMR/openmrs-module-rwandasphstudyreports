package org.openmrs.module.rwandasphstudyreports;

import java.util.HashMap;
import java.util.Map;

import org.openmrs.Location;
import org.openmrs.Program;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rowperpatientreports.dataset.definition.RowPerPatientDataSetDefinition;
import org.openmrs.module.rwandareports.reporting.SetupReport;

public class EMRReportAlerts implements SetupReport {
	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();

	private Program hivProgram;

	public void setup() throws Exception {
		setupProperties();

		ReportDefinition rd = createReportDefinition();
		ReportDesign design = Helper.createRowPerPatientXlsOverviewReportDesign(rd, "EMR_Report_Alerts.xls",
				"EMRReportAlerts", null);

		Helper.saveReportDesign(design);
	}

	public void delete() {
		ReportService rs = Context.getService(ReportService.class);
		
		for (ReportDesign rd : rs.getAllReportDesigns(false)) {
			if ("EMRReportAlerts".equals(rd.getName())) {
				rs.purgeReportDesign(rd);
			}
		}
		Helper.purgeReportDefinition("EMR Report Alerts");
	}

	private ReportDefinition createReportDefinition() {
		ReportDefinition reportDefinition = new ReportDefinition();

		reportDefinition.setName("EMR Report Alerts");
		reportDefinition.addParameter(new Parameter("location", "Health Center", Location.class));
		reportDefinition.setBaseCohortDefinition(Cohorts.createParameterizedLocationCohort("At Location"),
				ParameterizableUtil.createParameterMappings("location=${location}"));

		createDataSetDefinition(reportDefinition);
		Helper.saveReportDefinition(reportDefinition);

		return reportDefinition;
	}

	private void createDataSetDefinition(ReportDefinition reportDefinition) {
		RowPerPatientDataSetDefinition dataSetDefinition = new RowPerPatientDataSetDefinition();

		dataSetDefinition.setName(reportDefinition.getName() + " Data Set");
		dataSetDefinition.addFilter(Cohorts.createInProgramParameterizableByDate("adultHIV: In Program", hivProgram),
				ParameterizableUtil.createParameterMappings("onDate=${now}"));
		dataSetDefinition.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getTracnetId("TRACNET_ID"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getAge("age"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentWeight("RecentWeight", "ddMMMyy"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentCD4("CD4Test", "ddMMMyy"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentViralLoad("ViralLoad", "ddMMMyy"),
				new HashMap<String, Object>());

		Map<String, Object> mappings = new HashMap<String, Object>();
		SqlCohortDefinition adultPatientsCohort = Cohorts.getAdultPatients();

		dataSetDefinition.addFilter(adultPatientsCohort, null);
		reportDefinition.addDataSetDefinition("EMR Report Alerts", dataSetDefinition, mappings);
	}

	private void setupProperties() {
		hivProgram = gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
	}
}
