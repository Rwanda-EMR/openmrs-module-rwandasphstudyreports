package org.openmrs.module.rwandasphstudyreports;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
import org.openmrs.module.rwandareports.util.GlobalPropertiesManagement;
import org.openmrs.module.rwandareports.util.RowPerPatientColumns;

public class SetupLostToFollowupPatients {
	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();

	private Program hivProgram;
	private SqlCohortDefinition adultPatientsCohort;
	private SimpleDateFormat defaultDateFormat;

	public void setup() throws Exception {
		setupProperties();

		ReportDefinition rd = createReportDefinition();

		ReportDesign design = Helper.createRowPerPatientXlsOverviewReportDesign(rd, "LostToFollowupPatients.xls",
				"LostToFollowupPatients", null);

		Properties props = new Properties();
		props.put("repeatingSections", "sheet:1,row:6,dataset:LostToFollowupPatients");
		props.put("sortWeight", "5000");
		design.setProperties(props);

		Helper.saveReportDesign(design);
	}

	public void delete() {
		ReportService rs = Context.getService(ReportService.class);
		for (ReportDesign rd : rs.getAllReportDesigns(false)) {
			if ("LostToFollowupPatients".equals(rd.getName())) {
				rs.purgeReportDesign(rd);
			}
		}
		Helper.purgeReportDefinition("Lost To FollowUp Patients Report");
	}

	private ReportDefinition createReportDefinition() {

		ReportDefinition reportDefinition = new ReportDefinition();
		reportDefinition.setName("Lost To FollowUp Patients Report");

		reportDefinition.addParameter(new Parameter("location", "Health Center", Location.class));

		Properties stateProperties = new Properties();
		stateProperties.setProperty("Program", hivProgram.getName());

		reportDefinition.setBaseCohortDefinition(Cohorts.createParameterizedLocationCohort("At Location"),
				ParameterizableUtil.createParameterMappings("location=${location}"));

		createDataSetDefinition(reportDefinition);

		Helper.saveReportDefinition(reportDefinition);

		return reportDefinition;
	}

	private void createDataSetDefinition(ReportDefinition reportDefinition) {
		RowPerPatientDataSetDefinition hIVLostToFollowup = new RowPerPatientDataSetDefinition();
		Map<String, Object> mappings = new HashMap<String, Object>();

		hIVLostToFollowup.setName(reportDefinition.getName() + " Data Set");

		hIVLostToFollowup.addFilter(adultPatientsCohort, null);

		hIVLostToFollowup.addParameter(new Parameter("location", "Location", Location.class));
		hIVLostToFollowup.addParameter(new Parameter("endDate", "End Date", Date.class));

		hIVLostToFollowup.addColumn(RowPerPatientColumns.getTracnetId("TRACNET_ID"), new HashMap<String, Object>());
		hIVLostToFollowup.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"),
				new HashMap<String, Object>());
		hIVLostToFollowup.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"),
				new HashMap<String, Object>());
		hIVLostToFollowup.addColumn(RowPerPatientColumns.getAge("age"), new HashMap<String, Object>());
		hIVLostToFollowup.addColumn(
				RowPerPatientColumns.getMostRecentWeight("RecentWeight", defaultDateFormat.toLocalizedPattern()),
				new HashMap<String, Object>());
		hIVLostToFollowup.addColumn(
				RowPerPatientColumns.getMostRecentCD4("RecentCD4Test", defaultDateFormat.toLocalizedPattern()),
				new HashMap<String, Object>());
		hIVLostToFollowup.addColumn(
				RowPerPatientColumns.getMostRecentViralLoad("RecentViralLoadTest", defaultDateFormat.toLocalizedPattern()),
				new HashMap<String, Object>());

		reportDefinition.addDataSetDefinition("dataSet", hIVLostToFollowup, mappings);
	}

	private void setupProperties() {
		hivProgram = gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
		adultPatientsCohort = Cohorts.getAdultPatients();
		defaultDateFormat = Context.getDateFormat();
	}
}
