package org.openmrs.module.rwandasphstudyreports.reports;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.openmrs.Program;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rowperpatientreports.dataset.definition.RowPerPatientDataSetDefinition;
import org.openmrs.module.rowperpatientreports.patientdata.definition.AllObservationValues;
import org.openmrs.module.rowperpatientreports.patientdata.definition.CustomCalculationBasedOnMultiplePatientDataDefinitions;
import org.openmrs.module.rowperpatientreports.patientdata.definition.MostRecentObservation;
import org.openmrs.module.rwandareports.customcalculator.BMI;
import org.openmrs.module.rwandareports.filter.DrugNameFilter;
import org.openmrs.module.rwandareports.filter.LastThreeObsFilter;
import org.openmrs.module.rwandareports.filter.ObservationFilter;
import org.openmrs.module.rwandareports.reporting.SetupReport;
import org.openmrs.module.rwandasphstudyreports.Cohorts;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertiesManagement;
import org.openmrs.module.rwandasphstudyreports.HIVAdultAlerts;
import org.openmrs.module.rwandasphstudyreports.Helper;
import org.openmrs.module.rwandasphstudyreports.RowPerPatientColumns;

public class SetupAdultHIVConsultationSheet implements SetupReport {

	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();

	// properties retrieved from global variables
	private Program hivProgram;

	public void setup() throws Exception {
		if("true".equals(Context.getAdministrationService().getGlobalProperty(BaseSPHReportConfig.RECREATEREPORTSONACTIVATION)))
			delete();
		setupProperties();
		
		ReportDefinition rd = createReportDefinition();
		ReportDesign design = Helper.createRowPerPatientXlsOverviewReportDesign(rd, "AdultHIVConsultationSheetV2.xls",
				"AdultHIVConsultationSheet", null);

		Properties props = new Properties();
		props.put("repeatingSections", "sheet:1,row:6,dataset:dataSet");
		props.put("sortWeight", "5000");
		design.setProperties(props);

		Helper.saveReportDesign(design);
	}

	public void delete() {
		ReportService rs = Context.getService(ReportService.class);
		for (ReportDesign rd : rs.getAllReportDesigns(false)) {
			if ("AdultHIVConsultationSheet".equals(rd.getName())) {
				rs.purgeReportDesign(rd);
			}
		}
		Helper.purgeReportDefinition("HIV-Adult Consultation Sheet");
	}

	private ReportDefinition createReportDefinition() {

		ReportDefinition reportDefinition = new ReportDefinition();
		Properties stateProperties = new Properties();
		
		reportDefinition.setUuid(BaseSPHReportConfig.SETUPADULTHIVCONSULTATIONSHEET);
		reportDefinition.setName("HIV-Adult Consultation Sheet");
		stateProperties.setProperty("Program", hivProgram.getName());
		reportDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		createDataSetDefinition(reportDefinition);

		Helper.saveReportDefinition(reportDefinition);

		return reportDefinition;
	}

	private void createDataSetDefinition(ReportDefinition reportDefinition) {
		// Create new dataset definition
		RowPerPatientDataSetDefinition dataSetDefinition = new RowPerPatientDataSetDefinition();
		dataSetDefinition.setName(reportDefinition.getName() + " Data Set");
		
		// Add Columns
		dataSetDefinition.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"),
				new HashMap<String, Object>());

		dataSetDefinition.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"),
				new HashMap<String, Object>());

		dataSetDefinition.addColumn(RowPerPatientColumns.getIMBId("Id"), new HashMap<String, Object>());

		dataSetDefinition.addColumn(RowPerPatientColumns.getTracnetId("TRACNET_ID"), new HashMap<String, Object>());

		dataSetDefinition.addColumn(RowPerPatientColumns.getAge("age"), new HashMap<String, Object>());

		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentWeight("RecentWeight", "@ddMMMyy"),
				new HashMap<String, Object>());

		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentTbTest("RecentTB", "@ddMMMyy"),
				new HashMap<String, Object>());

		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentCD4("CD4Test", "@ddMMMyy"),
				new HashMap<String, Object>());

		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentViralLoad("ViralLoad", "@ddMMMyy"),
				new HashMap<String, Object>());

		dataSetDefinition.addColumn(RowPerPatientColumns.getAccompRelationship("AccompName"),
				new HashMap<String, Object>());

		dataSetDefinition.addColumn(
				RowPerPatientColumns.getCurrentARTOrders("Regimen", "@ddMMMyy", new DrugNameFilter()),
				new HashMap<String, Object>());

		dataSetDefinition.addColumn(
				RowPerPatientColumns.getCurrentTBOrders("TBTreatment", "@ddMMMyy", new DrugNameFilter()),
				new HashMap<String, Object>());

		// Calculation definitions
		MostRecentObservation mostRecentHeight = RowPerPatientColumns.getMostRecentHeight("RecentHeight", null);

		AllObservationValues weight = RowPerPatientColumns.getAllWeightValues("weightObs", "ddMMMyy",
				new LastThreeObsFilter(), new ObservationFilter());

		AllObservationValues cd4Test = RowPerPatientColumns.getAllCD4Values("CD4Test", "ddMMMyy",
				new LastThreeObsFilter(), new ObservationFilter());
		AllObservationValues viralLoadTest = RowPerPatientColumns.getAllViralLoadsValues("viralLoadTest", "ddMMMyy",
				new LastThreeObsFilter(), new ObservationFilter());

		//TODO add PatientsWithNoVLAfter8Months alert
		CustomCalculationBasedOnMultiplePatientDataDefinitions alert = new CustomCalculationBasedOnMultiplePatientDataDefinitions();
		alert.setName("alerts");
		alert.addPatientDataToBeEvaluated(cd4Test, new HashMap<String, Object>());
		alert.addPatientDataToBeEvaluated(weight, new HashMap<String, Object>());
		alert.addPatientDataToBeEvaluated(mostRecentHeight, new HashMap<String, Object>());
		alert.addPatientDataToBeEvaluated(viralLoadTest, new HashMap<String, Object>());
		alert.setCalculator(new HIVAdultAlerts());
		alert.addParameter(new Parameter("state", "State", Date.class));
		dataSetDefinition.addColumn(alert, ParameterizableUtil.createParameterMappings("state=${state}"));

		CustomCalculationBasedOnMultiplePatientDataDefinitions bmi = new CustomCalculationBasedOnMultiplePatientDataDefinitions();
		bmi.setName("bmi");
		bmi.addPatientDataToBeEvaluated(weight, new HashMap<String, Object>());
		bmi.addPatientDataToBeEvaluated(mostRecentHeight, new HashMap<String, Object>());
		bmi.setCalculator(new BMI());
		dataSetDefinition.addColumn(bmi, new HashMap<String, Object>());

		Map<String, Object> mappings = new HashMap<String, Object>();
		
		System.out.println("\nDATASETDEFINITION:\n" + ReflectionToStringBuilder.toString(dataSetDefinition) + "\n\n");
		System.out.println("\nMAPPINGS:\n" + ReflectionToStringBuilder.toString(mappings) + "\n\n");
		dataSetDefinition.addColumn(RowPerPatientColumns.patientAttribute("Peer Educator's Name", "peerEducator"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.patientAttribute("Peer Educator's Phone Number", "peerEducatorPhone"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.patientAttribute("Phone Number", "privatePhone"), new HashMap<String, Object>());
		
		mappings.put("endDate", "${endDate}");
		// filter all dataset definitions by adult age
		SqlCohortDefinition adultPatientsCohort = Cohorts.getAdultPatients();
		dataSetDefinition.addFilter(adultPatientsCohort, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition.addFilter(Cohorts.createInProgramParameterizableByDate("adultHIV: In Program", hivProgram),
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate}"));

		reportDefinition.addDataSetDefinition("AdultHIVConsultationSheet", dataSetDefinition, mappings);
	}

	private void setupProperties() {
		hivProgram = gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
	}
}
