package org.openmrs.module.rwandasphstudyreports.api;

import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.junit.Test;
import org.openmrs.Program;
//import org.openmrs.ProgramWorkflowState;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.InProgramCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.service.CohortDefinitionService;
import org.openmrs.module.reporting.dataset.SimpleDataSet;
import org.openmrs.module.reporting.dataset.definition.service.DataSetDefinitionService;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.rowperpatientreports.dataset.definition.RowPerPatientDataSetDefinition;
import org.openmrs.module.rowperpatientreports.patientdata.definition.AllObservationValues;
import org.openmrs.module.rowperpatientreports.patientdata.definition.CustomCalculationBasedOnMultiplePatientDataDefinitions;
import org.openmrs.module.rowperpatientreports.patientdata.definition.MostRecentObservation;
import org.openmrs.module.rwandareports.customcalculator.BMI;
import org.openmrs.module.rwandareports.filter.DrugNameFilter;
import org.openmrs.module.rwandareports.filter.LastThreeObsFilter;
import org.openmrs.module.rwandareports.filter.ObservationFilter;
import org.openmrs.module.rwandareports.util.GlobalPropertiesManagement;
import org.openmrs.module.rwandasphstudyreports.Cohorts;
import org.openmrs.module.rwandasphstudyreports.HIVAdultAlerts;
import org.openmrs.module.rwandasphstudyreports.RowPerPatientColumns;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class SetupAdultHIVConsultationSheetTest extends StandaloneContextSensitiveTest {

	@Autowired
	@Qualifier(value = "reportingCohortDefinitionService")
	CohortDefinitionService cohortDefinitionService;


	@Autowired
	@Qualifier(value = "locationService")
	LocationService locationService;

	@Test
	public void test_dataSetDefinition() throws EvaluationException {
		System.out.println("::::::::>Adult Consultation Sheet Report<::::::::");
		
		SqlCohortDefinition adultPatientsCohort = Cohorts.getAdultPatients();
		GlobalPropertiesManagement gp = new GlobalPropertiesManagement();
		Program hivProgram = gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
		AllObservationValues weight = RowPerPatientColumns.getAllWeightValues("weightObs", "ddMMMyy",
				new LastThreeObsFilter(), new ObservationFilter());
		AllObservationValues cd4Test = RowPerPatientColumns.getAllCD4Values("CD4Test", "ddMMMyy",
				new LastThreeObsFilter(), new ObservationFilter());
		EvaluationContext context = new EvaluationContext();

		context.addParameterValue("beforeDate", new Date());
		context.addParameterValue("onDate", new Date());
		context.addParameterValue("endDate", new Date());
		context.addParameterValue("location", locationService
				.getLocation(54));/*
									 * 'Gahini HD' contains more data
									 */

		MostRecentObservation mostRecentHeight = RowPerPatientColumns.getMostRecentHeight("RecentHeight", null);

		AllObservationValues viralLoadTest = RowPerPatientColumns.getAllViralLoadsValues("viralLoadTest", "ddMMMyy",
				new LastThreeObsFilter(), new ObservationFilter());
		CustomCalculationBasedOnMultiplePatientDataDefinitions alert = new CustomCalculationBasedOnMultiplePatientDataDefinitions();
		CustomCalculationBasedOnMultiplePatientDataDefinitions bmi = new CustomCalculationBasedOnMultiplePatientDataDefinitions();

		alert.setName("alert");
		alert.addPatientDataToBeEvaluated(cd4Test, new HashMap<String, Object>());
		alert.addPatientDataToBeEvaluated(weight, new HashMap<String, Object>());
		alert.addPatientDataToBeEvaluated(mostRecentHeight, new HashMap<String, Object>());
		// alert.addPatientDataToBeEvaluated(io, new HashMap<String, Object>());
		// alert.addPatientDataToBeEvaluated(sideEffect, new HashMap<String,
		// Object>());
		alert.addPatientDataToBeEvaluated(viralLoadTest, new HashMap<String, Object>());
		// alert.addPatientDataToBeEvaluated(lastEncInMonth, new HashMap<String,
		// Object>());
		alert.setCalculator(new HIVAdultAlerts());
		alert.addParameter(new Parameter("state", "State", Date.class));

		bmi.setName("bmi");
		bmi.addPatientDataToBeEvaluated(weight, new HashMap<String, Object>());
		bmi.addPatientDataToBeEvaluated(mostRecentHeight, new HashMap<String, Object>());
		bmi.setCalculator(new BMI());
		RowPerPatientDataSetDefinition dataSetDefinition = new RowPerPatientDataSetDefinition();
		dataSetDefinition.setName("Adult HIV Consultation Sheet Data Set");
		//dataSetDefinition.addParameter(new Parameter("state", "State", ProgramWorkflowState.class));
		dataSetDefinition.addParameter(new Parameter("location", "Location/Health Center", Date.class));
		dataSetDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		
		System.out.println("::::::::> adultPatientsCohort: " + cohortDefinitionService.evaluate(adultPatientsCohort, context).size());
		dataSetDefinition.addFilter(adultPatientsCohort, null);
		
		//CohortDefinition inStatesCohorts = Cohorts.createInCurrentStateParameterized("in state", "states");
		InProgramCohortDefinition adultProgramCohort = Cohorts.createInProgramParameterizableByDate("adultHIV: In Program", hivProgram);
		
		System.out.println("::::::::> adultProgramCohort: " + cohortDefinitionService.evaluate(adultProgramCohort, context).size());
		//System.out.println("::::::::> inStatesCohorts: " + cohortDefinitionService.evaluate(inStatesCohorts, context).size());
		/*dataSetDefinition.addFilter(inStatesCohorts ,
				ParameterizableUtil.createParameterMappings("states=${state},onDate=${now}"));
		dataSetDefinition.addFilter(adultProgramCohort,
				ParameterizableUtil.createParameterMappings("onDate=${now}"));
		*/
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
				RowPerPatientColumns.getCurrentTBOrders("TB Treatment", "@ddMMMyy", new DrugNameFilter()),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(
				RowPerPatientColumns.getCurrentTBOrders("TB Treatment", "@ddMMMyy", new DrugNameFilter()),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(alert, ParameterizableUtil.createParameterMappings("state=${state}"));
		dataSetDefinition.addColumn(bmi, new HashMap<String, Object>());

		evaluateReportDataSetDefinition(dataSetDefinition, "dataSetDefinition");
	}

	private void evaluateReportDataSetDefinition(RowPerPatientDataSetDefinition datasetDef, String message)
			throws EvaluationException {

		EvaluationContext context = new EvaluationContext();
		HashMap<String, Object> mappings = new HashMap<String, Object>();
		mappings.put("location", "${location}");
		mappings.put("endDate", "${endDate}");

		EvaluationContext ec = EvaluationContext.cloneForChild(context,
				new Mapped<RowPerPatientDataSetDefinition>(datasetDef, mappings));
		System.out.println("\n" + message + " : " + ReflectionToStringBuilder.toString(datasetDef));

		SimpleDataSet datasetSimpleDataset = (SimpleDataSet) Context.getService(DataSetDefinitionService.class)
				.evaluate(datasetDef, ec);
		System.out.println(
				message + "DataSet_DATA:" + ReflectionToStringBuilder.toString(datasetSimpleDataset.getRowMap()));
		System.out.println();
		System.out.println("::::::::> " + message + ": " + datasetSimpleDataset.getRowMap().size());
		System.out.println();
	}
}
