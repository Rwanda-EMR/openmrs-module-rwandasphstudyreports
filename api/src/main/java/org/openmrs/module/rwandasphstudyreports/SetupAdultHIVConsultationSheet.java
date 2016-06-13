package org.openmrs.module.rwandasphstudyreports;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.openmrs.Location;
import org.openmrs.Program;
import org.openmrs.api.context.Context;
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
import org.openmrs.module.rwandareports.customcalculator.HIVAdultAlerts;
import org.openmrs.module.rwandareports.filter.DrugNameFilter;
import org.openmrs.module.rwandareports.filter.LastThreeObsFilter;
import org.openmrs.module.rwandareports.filter.ObservationFilter;
import org.openmrs.module.rwandareports.reporting.SetupReport;

public class SetupAdultHIVConsultationSheet implements SetupReport {

	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();

	// properties retrieved from global variables
	private Program hivProgram;

	//private EncounterType flowsheetAdult;

	//private List<EncounterType> clinicalEnountersIncLab;

	public void setup() throws Exception {

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
		reportDefinition.setName("HIV-Adult Consultation Sheet");

		reportDefinition.addParameter(new Parameter("location", "Health Center", Location.class));

		Properties stateProperties = new Properties();
		stateProperties.setProperty("Program", hivProgram.getName());
		//stateProperties.setProperty("Workflow", Context.getAdministrationService()
				//.getGlobalProperty(GlobalPropertiesManagement.TREATMENT_GROUP_WORKFLOW));

		//reportDefinition.addParameter(new Parameter("state", "Group", ProgramWorkflowState.class, stateProperties));

		reportDefinition.setBaseCohortDefinition(Cohorts.createParameterizedLocationCohort("At Location"),
				ParameterizableUtil.createParameterMappings("location=${location}"));

		createDataSetDefinition(reportDefinition);

		Helper.saveReportDefinition(reportDefinition);

		return reportDefinition;
	}

	private void createDataSetDefinition(ReportDefinition reportDefinition) {
		// Create new dataset definition
		RowPerPatientDataSetDefinition dataSetDefinition = new RowPerPatientDataSetDefinition();
		dataSetDefinition.setName(reportDefinition.getName() + " Data Set");
		//dataSetDefinition.addParameter(new Parameter("state", "State", ProgramWorkflowState.class));

		// Add Filters
		//dataSetDefinition.addFilter(Cohorts.createInCurrentStateParameterized("in state", "states"),
				//ParameterizableUtil.createParameterMappings("states=${state},onDate=${now}"));

		dataSetDefinition.addFilter(Cohorts.createInProgramParameterizableByDate("adultHIV: In Program", hivProgram),
				ParameterizableUtil.createParameterMappings("onDate=${now}"));

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
				RowPerPatientColumns.getCurrentTBOrders("TB Treatment", "@ddMMMyy", new DrugNameFilter()),
				new HashMap<String, Object>());

		// Calculation definitions
		MostRecentObservation mostRecentHeight = RowPerPatientColumns.getMostRecentHeight("RecentHeight", null);

		AllObservationValues weight = RowPerPatientColumns.getAllWeightValues("weightObs", "ddMMMyy",
				new LastThreeObsFilter(), new ObservationFilter());

		AllObservationValues cd4Test = RowPerPatientColumns.getAllCD4Values("CD4Test", "ddMMMyy",
				new LastThreeObsFilter(), new ObservationFilter());

		/*ObservationInMostRecentEncounterOfType io = RowPerPatientColumns.getIOInMostRecentEncounterOfType("IO",
				flowsheetAdult);

		ObservationInMostRecentEncounterOfType sideEffect = RowPerPatientColumns
				.getSideEffectInMostRecentEncounterOfType("SideEffects", flowsheetAdult);
*/
		AllObservationValues viralLoadTest = RowPerPatientColumns.getAllViralLoadsValues("viralLoadTest", "ddMMMyy",
				new LastThreeObsFilter(), new ObservationFilter());
		/*RecentEncounterType lastEncInMonth = RowPerPatientColumns.getRecentEncounterType("lastEncInMonth",
				clinicalEnountersIncLab, null, null);
*/
		CustomCalculationBasedOnMultiplePatientDataDefinitions alert = new CustomCalculationBasedOnMultiplePatientDataDefinitions();
		alert.setName("alert");
		alert.addPatientDataToBeEvaluated(cd4Test, new HashMap<String, Object>());
		alert.addPatientDataToBeEvaluated(weight, new HashMap<String, Object>());
		alert.addPatientDataToBeEvaluated(mostRecentHeight, new HashMap<String, Object>());
		//alert.addPatientDataToBeEvaluated(io, new HashMap<String, Object>());
		//alert.addPatientDataToBeEvaluated(sideEffect, new HashMap<String, Object>());
		alert.addPatientDataToBeEvaluated(viralLoadTest, new HashMap<String, Object>());
		//alert.addPatientDataToBeEvaluated(lastEncInMonth, new HashMap<String, Object>());
		alert.setCalculator(new HIVAdultAlerts());
		//alert.addParameter(new Parameter("state", "State", Date.class));
		//dataSetDefinition.addColumn(alert, ParameterizableUtil.createParameterMappings("state=${state}"));

		CustomCalculationBasedOnMultiplePatientDataDefinitions bmi = new CustomCalculationBasedOnMultiplePatientDataDefinitions();
		bmi.setName("bmi");
		bmi.addPatientDataToBeEvaluated(weight, new HashMap<String, Object>());
		bmi.addPatientDataToBeEvaluated(mostRecentHeight, new HashMap<String, Object>());
		bmi.setCalculator(new BMI());
		dataSetDefinition.addColumn(bmi, new HashMap<String, Object>());

		Map<String, Object> mappings = new HashMap<String, Object>();
	//	mappings.put("state", "${state}");

		System.out.println("\nDATASETDEFINITION:\n" + ReflectionToStringBuilder.toString(dataSetDefinition) + "\n\n");
		System.out.println("\nMAPPINGS:\n" + ReflectionToStringBuilder.toString(mappings) + "\n\n");
		
		reportDefinition.addDataSetDefinition("dataSet", dataSetDefinition, mappings);
	}

	private void setupProperties() {
		hivProgram = gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM);

		//flowsheetAdult = gp.getEncounterType(GlobalPropertiesManagement.ADULT_FLOWSHEET_ENCOUNTER);

		//clinicalEnountersIncLab = gp.getEncounterTypeList(GlobalPropertiesManagement.CLINICAL_ENCOUNTER_TYPES);
	}
}
