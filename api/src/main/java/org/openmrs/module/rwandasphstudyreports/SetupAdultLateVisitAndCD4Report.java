package org.openmrs.module.rwandasphstudyreports;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.api.PatientSetService.TimeModifier;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CompositionCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InProgramCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InStateCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InverseCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.NumericObsCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.common.RangeComparator;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rowperpatientreports.dataset.definition.RowPerPatientDataSetDefinition;
import org.openmrs.module.rowperpatientreports.patientdata.definition.AllObservationValues;
import org.openmrs.module.rowperpatientreports.patientdata.definition.CustomCalculationBasedOnMultiplePatientDataDefinitions;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateDiff;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateDiff.DateDiffType;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateOfBirthShowingEstimation;
import org.openmrs.module.rowperpatientreports.patientdata.definition.FirstDrugOrderStartedRestrictedByConceptSet;
import org.openmrs.module.rowperpatientreports.patientdata.definition.MostRecentObservation;
import org.openmrs.module.rowperpatientreports.patientdata.definition.MultiplePatientDataDefinitions;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientAddress;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientProperty;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientRelationship;
import org.openmrs.module.rowperpatientreports.patientdata.definition.StateOfPatient;
import org.openmrs.module.rwandareports.customcalculator.BMICalculation;
import org.openmrs.module.rwandareports.customcalculator.DeclineHighestCD4;
import org.openmrs.module.rwandareports.customcalculator.DifferenceBetweenLastTwoObs;
import org.openmrs.module.rwandareports.filter.GroupStateFilter;
import org.openmrs.module.rwandareports.filter.TreatmentStateFilter;

public class SetupAdultLateVisitAndCD4Report {

	protected final static Log log = LogFactory.getLog(SetupAdultLateVisitAndCD4Report.class);

	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();

	// Properties retrieved from global variables
	private Program hivProgram;

	private ProgramWorkflow treatmentGroup;

	private ProgramWorkflow treatmentStatus;

	private ProgramWorkflowState onART;

	private ProgramWorkflowState following;

	//private List<EncounterType> clinicalEnountersIncLab;

	//private List<EncounterType> clinicalEncoutersExcLab;

	//private EncounterType labTestEncounterType;

	private Concept cd4;

	private Concept height;

	private Concept weight;

	private Concept viralLoad;

	public void setup() throws Exception {

		setupProperties();

		ReportDefinition rd = createReportDefinition();
		ReportDesign design = Helper.createRowPerPatientXlsOverviewReportDesign(rd, "AdultLateVisitAndCD4Template.xls",
				"XlsAdultLateVisitAndCD4Template", null);

		ReportDefinition rdp = createReportDefinitionPreArt();
		ReportDesign designp = Helper.createRowPerPatientXlsOverviewReportDesign(rdp,
				"AdultLateVisitAndCD4PreARTTemplate.xls", "XlsAdultLateVisitAndCD4PreARTTemplate", null);

		createDataSetDefinition(rd, rdp);

		Helper.saveReportDefinition(rd);
		//Helper.saveReportDefinition(rdp);
		// h.saveReportDefinition(artDecline);

		Properties props = new Properties();
		props.put("repeatingSections",
				"sheet:1,row:8,dataset:AdultARTLateVisit|sheet:2,row:8,dataset:AdultHIVLateCD4Count|sheet:3,row:8,dataset:HIVLostToFollowup|sheet:4,row:8,dataset:HIVLowBMI|sheet:5,row:8,dataset:ViralLoadGreaterThan20InTheLast3Months");
		props.put("sortWeight", "5000");
		design.setProperties(props);
		Helper.saveReportDesign(design);

		Properties propsp = new Properties();
		propsp.put("repeatingSections",
				"sheet:1,row:8,dataset:AdultPreARTLateVisit|sheet:2,row:8,dataset:AdultHIVLateCD4Count|sheet:3,row:8,dataset:HIVLostToFollowup|sheet:4,row:8,dataset:PreARTBelow350CD4|sheet:5,row:8,dataset:HIVLowBMI");
		propsp.put("sortWeight", "5000");
		designp.setProperties(propsp);
		Helper.saveReportDesign(designp);
	}

	public void delete() {
		ReportService rs = Context.getService(ReportService.class);
		for (ReportDesign rd : rs.getAllReportDesigns(false)) {
			if ("XlsAdultLateVisitAndCD4Template".equals(rd.getName())
					|| "XlsAdultLateVisitAndCD4PreARTTemplate".equals(rd.getName())) {
				rs.purgeReportDesign(rd);
			}
		}
		Helper.purgeReportDefinition("HIV-Adult ART Report-Monthly");
		Helper.purgeReportDefinition("HIV-Adult Pre ART Report-Monthly");
		// h.purgeReportDefinition("Monthly Adult Art Decline");
	}

	private ReportDefinition createReportDefinition() {
		ReportDefinition reportDefinition = new ReportDefinition();
		reportDefinition.setName("HIV-Adult ART Report-Monthly");
		reportDefinition.addParameter(new Parameter("location", "Location", Location.class));
		reportDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

		reportDefinition.setBaseCohortDefinition(Cohorts.createParameterizedLocationCohort("At Location"),
				ParameterizableUtil.createParameterMappings("location=${location}"));

		return reportDefinition;
	}

	private ReportDefinition createReportDefinitionPreArt() {
		ReportDefinition reportDefinition = new ReportDefinition();
		reportDefinition.setName("HIV-Adult Pre ART Report-Monthly");
		reportDefinition.addParameter(new Parameter("location", "Location", Location.class));
		reportDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));

		reportDefinition.setBaseCohortDefinition(Cohorts.createParameterizedLocationCohort("At Location"),
				ParameterizableUtil.createParameterMappings("location=${location}"));

		return reportDefinition;
	}
	
	private void createDataSetDefinition(ReportDefinition art, ReportDefinition preArt) {
		// ====================================================================
		// Patients Dataset definitions
		// ====================================================================

		// Create Adult ART late visit dataset definition
		RowPerPatientDataSetDefinition adultARTLateVisit = new RowPerPatientDataSetDefinition();
		adultARTLateVisit.setName("Adult ART dataSetDefinition");

		// Create Adult Pre-ART late visit dataset definition
		RowPerPatientDataSetDefinition adultPreARTLateVisit = new RowPerPatientDataSetDefinition();
		adultPreARTLateVisit.setName("Adult Pre-ART dataSetDefinition");

		// Create Adult HIV late CD4 count dataset definition
		RowPerPatientDataSetDefinition adultHIVLateCD4Count = new RowPerPatientDataSetDefinition();
		adultHIVLateCD4Count.setName("Adult HIV late CD4 dataSetDefinition");
		RowPerPatientDataSetDefinition adultHIVLateCD4Count_1 = new RowPerPatientDataSetDefinition();
		adultHIVLateCD4Count_1.setName("Adult HIV late CD4 Pre art dataSetDefinition");

		// Create HIV lost to follow-up dataset definition
		RowPerPatientDataSetDefinition hIVLostToFollowup = new RowPerPatientDataSetDefinition();
		hIVLostToFollowup.setName("Adult HIV lost to follow-up dataSetDefinition");
		RowPerPatientDataSetDefinition hIVLostToFollowup_1 = new RowPerPatientDataSetDefinition();
		hIVLostToFollowup_1.setName("Adult HIV lost to follow-up Pre Art dataSetDefinition");

		// Create Adult Pre-ART patients with CD4 below 350 dataset definition
		RowPerPatientDataSetDefinition preARTBelow350CD4 = new RowPerPatientDataSetDefinition();
		preARTBelow350CD4.setName("Adult Pre-ART patients with CD4 below 350 dataSetDefinition");

		// Patients with BMI below 16 dataset definition
		RowPerPatientDataSetDefinition hIVLowBMI = new RowPerPatientDataSetDefinition();
		hIVLowBMI.setName("Patients with BMI below 16 dataSetDefinition");
		RowPerPatientDataSetDefinition hIVLowBMI_1 = new RowPerPatientDataSetDefinition();
		hIVLowBMI_1.setName("Patients with BMI below 16 Pre art dataSetDefinition");

		// Patients whose cd4 has declined more than 50 in the last month for
		// ART patients
		RowPerPatientDataSetDefinition dataSetDefinition7 = new RowPerPatientDataSetDefinition();
		dataSetDefinition7.setName("decline50Perc");

		// Patients whose viral loads are greater than 20 in the last 3 months
		RowPerPatientDataSetDefinition viralLoadGreaterThan20InTheLast3Months = new RowPerPatientDataSetDefinition();
		viralLoadGreaterThan20InTheLast3Months.setName("Patients with Viral Load greater than 20 in the last three months");

		// 50% decline from highest CD4 count from baseline CD4 after ART
		// initiation
		RowPerPatientDataSetDefinition dataSetDefinition9 = new RowPerPatientDataSetDefinition();
		dataSetDefinition9.setName("decline50");

		// Adult HIV program Cohort definition
		InProgramCohortDefinition adultHivProgramCohort = Cohorts
				.createInProgramParameterizableByDate("adultHivProgramCohort", hivProgram);
		adultARTLateVisit.addFilter(adultHivProgramCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		adultPreARTLateVisit.addFilter(adultHivProgramCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		adultHIVLateCD4Count.addFilter(adultHivProgramCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		adultHIVLateCD4Count_1.addFilter(adultHivProgramCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		hIVLostToFollowup.addFilter(adultHivProgramCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		hIVLostToFollowup_1.addFilter(adultHivProgramCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		preARTBelow350CD4.addFilter(adultHivProgramCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		hIVLowBMI.addFilter(adultHivProgramCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		hIVLowBMI_1.addFilter(adultHivProgramCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		dataSetDefinition7.addFilter(adultHivProgramCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		viralLoadGreaterThan20InTheLast3Months.addFilter(adultHivProgramCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		dataSetDefinition9.addFilter(adultHivProgramCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));

		// ==================================================================
		// 1. Adult ART late visit
		// ==================================================================

		// ON ANTIRETROVIRALS state cohort definition.
		InStateCohortDefinition onARTStatusCohort = Cohorts
				.createInProgramStateParameterizableByDate("onARTStatusCohort", onART);

		adultARTLateVisit.addFilter(onARTStatusCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		adultHIVLateCD4Count.addFilter(onARTStatusCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		hIVLostToFollowup.addFilter(onARTStatusCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		hIVLowBMI.addFilter(onARTStatusCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		dataSetDefinition7.addFilter(onARTStatusCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		viralLoadGreaterThan20InTheLast3Months.addFilter(onARTStatusCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		dataSetDefinition9.addFilter(onARTStatusCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));

		CompositionCohortDefinition patientsWithClinicalEncounters = new CompositionCohortDefinition();
		patientsWithClinicalEncounters.addParameter(new Parameter("onOrAfter", "onOrAfter", Date.class));
		patientsWithClinicalEncounters.setCompositionString("1 OR 2");
		adultARTLateVisit.addFilter(patientsWithClinicalEncounters,
				ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
		adultPreARTLateVisit.addFilter(patientsWithClinicalEncounters,
				ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
		adultHIVLateCD4Count.addFilter(patientsWithClinicalEncounters,
				ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
		adultHIVLateCD4Count_1.addFilter(patientsWithClinicalEncounters,
				ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
		preARTBelow350CD4.addFilter(patientsWithClinicalEncounters,
				ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
		hIVLowBMI.addFilter(patientsWithClinicalEncounters,
				ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
		hIVLowBMI_1.addFilter(patientsWithClinicalEncounters,
				ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
		dataSetDefinition7.addFilter(patientsWithClinicalEncounters,
				ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
		viralLoadGreaterThan20InTheLast3Months.addFilter(patientsWithClinicalEncounters,
				ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
		dataSetDefinition9.addFilter(patientsWithClinicalEncounters,
				ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));

		// Patients without Any clinical Encounter(Test lab excluded) in last
		// three months.

		/*CompositionCohortDefinition patientsWithoutClinicalEncounters = new CompositionCohortDefinition();
		patientsWithoutClinicalEncounters.setName("patientsWithoutClinicalEncounters");
		patientsWithoutClinicalEncounters.addParameter(new Parameter("onOrAfter", "onOrAfter", Date.class));
		patientsWithoutClinicalEncounters.getSearches().put("patientsWithClinicalEncountersWithoutLabTest",
				new Mapped<CohortDefinition>(patientsWithClinicalEncountersWithoutLabTest,
						ParameterizableUtil.createParameterMappings("onOrAfter=${onOrAfter}")));
		patientsWithoutClinicalEncounters.setCompositionString("NOT patientsWithClinicalEncountersWithoutLabTest");

		// before was in 3m, now is in 6m
		adultARTLateVisit.addFilter(patientsWithoutClinicalEncounters,
				ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-6m}"));
*/
		// ==================================================================
		// 2. Adult Pre-ART late visit
		// ==================================================================

		// Following state cohort definition.

		InStateCohortDefinition followingStatusCohort = Cohorts
				.createInProgramStateParameterizableByDate("followingStatusCohort", following);

		adultPreARTLateVisit.addFilter(followingStatusCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		adultHIVLateCD4Count_1.addFilter(followingStatusCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		hIVLostToFollowup_1.addFilter(followingStatusCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		preARTBelow350CD4.addFilter(followingStatusCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));
		hIVLowBMI_1.addFilter(followingStatusCohort,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));

		// Patients without Any clinical Encounter(Test lab excluded) in last
		// six months.
		//adultPreARTLateVisit.addFilter(patientsWithoutClinicalEncounters,
		//	ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-6m}"));

		// ==================================================================
		// 3. Adult HIV late CD4 count
		// ==================================================================

		NumericObsCohortDefinition cd4CohortDefinition = Cohorts.createNumericObsCohortDefinition("cd4CohortDefinition",
				"onOrAfter", cd4, new Double(0), null, TimeModifier.LAST);

		CompositionCohortDefinition patientsWithouthCD4RecordComposition = new CompositionCohortDefinition();
		patientsWithouthCD4RecordComposition.setName("patientsWithouthCD4RecordComposition");
		patientsWithouthCD4RecordComposition.addParameter(new Parameter("onOrAfter", "onOrAfter", Date.class));
		patientsWithouthCD4RecordComposition.getSearches().put("cd4CohortDefinition", new Mapped<CohortDefinition>(
				cd4CohortDefinition, ParameterizableUtil.createParameterMappings("onOrAfter=${onOrAfter}")));
		patientsWithouthCD4RecordComposition.setCompositionString("NOT cd4CohortDefinition");

		adultHIVLateCD4Count.addFilter(patientsWithouthCD4RecordComposition,
				ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
		adultHIVLateCD4Count_1.addFilter(patientsWithouthCD4RecordComposition,
				ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-9m}"));

		// ==================================================================
		// 4. HIV lost to follow-up
		// ==================================================================

		// Patients with no encounters of any kind in the past year

		InverseCohortDefinition patientsWithoutEncountersInPastYear = new InverseCohortDefinition(
				patientsWithClinicalEncounters);
		patientsWithoutEncountersInPastYear.setName("patientsWithoutEncountersInPastYear");

		hIVLostToFollowup.addFilter(patientsWithoutEncountersInPastYear,
				ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));
		hIVLostToFollowup_1.addFilter(patientsWithoutEncountersInPastYear,
				ParameterizableUtil.createParameterMappings("onOrAfter=${endDate-12m}"));

		// ==================================================================
		// 5. Pre-ART patients with CD4 below 500
		// ==================================================================

		// Patients with CD4 below 500
		NumericObsCohortDefinition lastDC4below500 = Cohorts.createNumericObsCohortDefinition("lastDC4below500",
				"onOrBefore", cd4, 500.0, RangeComparator.LESS_THAN, TimeModifier.LAST);
		preARTBelow350CD4.addFilter(lastDC4below500,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate}"));

		// ==================================================================
		// 6. Patients with BMI less than 16
		// ==================================================================

		// Patients with BMI less than 16
		SqlCohortDefinition patientWithLowBMI = new SqlCohortDefinition();
		patientWithLowBMI.setName("patientWithLowBMI");
		patientWithLowBMI.setQuery(
				"select w.person_id from (select * from (select o.person_id,o.value_numeric from obs o,concept c where o.voided=0 and o.value_numeric is not null and o.concept_id= c.concept_id and c.concept_id='"
						+ height.getId()
						+ "' order by o.obs_datetime desc) as lastheight group by lastheight.person_id) h,(select * from (select o.person_id,o.value_numeric from obs o,concept c where o.voided=0 and o.value_numeric is not null and o.concept_id= c.concept_id and c.uuid='"
						+ weight.getUuid()
						+ "' order by o.obs_datetime desc) as lastweight group by lastweight.person_id) w,(select p.patient_id from patient p, person_attribute pa, person_attribute_type pat where p.patient_id = pa.person_id and pat.name ='Health Center' and pat.person_attribute_type_id = pa.person_attribute_type_id and pa.voided = 0 and pa.value = :location) loc where loc.patient_id=w.person_id and w.person_id=h.person_id and ROUND(((w.value_numeric*10000)/(h.value_numeric*h.value_numeric)),2)<16.0");
		patientWithLowBMI.addParameter(new Parameter("location", "location", Location.class));
		hIVLowBMI.addFilter(patientWithLowBMI,
				ParameterizableUtil.createParameterMappings("location=${location}"));
		hIVLowBMI_1.addFilter(patientWithLowBMI,
				ParameterizableUtil.createParameterMappings("location=${location}"));

		// ==================================================================
		// 7 . Patients Declining in CD4 by more than 50
		// ==================================================================

		// Patients Declining in CD4 by more than 50
		SqlCohortDefinition deciningInCD4MoreThan50 = Cohorts.createPatientsWithDecline("deciningInCD4MoreThan50", cd4,
				50);
		dataSetDefinition7.addFilter(deciningInCD4MoreThan50,
				ParameterizableUtil.createParameterMappings("beforeDate=${endDate}"));

		// ==================================================================
		// 8 . Patients with Viral Load >1000 in the last 12 months
		// ==================================================================
		SqlCohortDefinition viralLoadGreaterThan1000InLast12Months = new SqlCohortDefinition(
				"select vload.person_id from (select * from obs where concept_id=" + viralLoad.getConceptId()
						+ " and value_numeric>1000 and obs_datetime> :beforeDate and obs_datetime<= :onDate order by obs_datetime desc) as vload group by vload.person_id");
		viralLoadGreaterThan1000InLast12Months.setName("viralLoadGreaterThan1000InLast12Months");
		viralLoadGreaterThan1000InLast12Months.addParameter(new Parameter("beforeDate", "beforeDate", Date.class));
		viralLoadGreaterThan1000InLast12Months.addParameter(new Parameter("onDate", "onDate", Date.class));
		viralLoadGreaterThan1000InLast12Months.addParameter(new Parameter("location", "location", Location.class));
		viralLoadGreaterThan20InTheLast3Months.addFilter(viralLoadGreaterThan1000InLast12Months,
				ParameterizableUtil.createParameterMappings("beforeDate=${endDate-12m},onDate=${endDate}"));

		// ==================================================================
		// 9 . Patients with 50% decline from highest CD4 count from baseline
		// CD4 after ART initiation
		// ==================================================================
		SqlCohortDefinition cd4declineOfMoreThan50Percent = Cohorts.createPatientsWithDeclineFromBaseline("cd4decline",
				cd4, onART);
		dataSetDefinition9.addFilter(cd4declineOfMoreThan50Percent,
				ParameterizableUtil.createParameterMappings("beforeDate=${endDate}"));

		// ==================================================================
		// Columns of report settings
		// ==================================================================
		MultiplePatientDataDefinitions imbType = RowPerPatientColumns.getIMBId("IMB ID");
		adultARTLateVisit.addColumn(imbType, new HashMap<String, Object>());
		adultPreARTLateVisit.addColumn(imbType, new HashMap<String, Object>());
		adultHIVLateCD4Count.addColumn(imbType, new HashMap<String, Object>());
		adultHIVLateCD4Count_1.addColumn(imbType, new HashMap<String, Object>());
		hIVLostToFollowup.addColumn(imbType, new HashMap<String, Object>());
		hIVLostToFollowup_1.addColumn(imbType, new HashMap<String, Object>());
		preARTBelow350CD4.addColumn(imbType, new HashMap<String, Object>());
		hIVLowBMI.addColumn(imbType, new HashMap<String, Object>());
		hIVLowBMI_1.addColumn(imbType, new HashMap<String, Object>());
		dataSetDefinition7.addColumn(imbType, new HashMap<String, Object>());
		viralLoadGreaterThan20InTheLast3Months.addColumn(imbType, new HashMap<String, Object>());
		dataSetDefinition9.addColumn(imbType, new HashMap<String, Object>());

		PatientProperty givenName = RowPerPatientColumns.getFirstNameColumn("First Name");
		adultARTLateVisit.addColumn(givenName, new HashMap<String, Object>());
		adultPreARTLateVisit.addColumn(givenName, new HashMap<String, Object>());
		adultHIVLateCD4Count.addColumn(givenName, new HashMap<String, Object>());
		adultHIVLateCD4Count_1.addColumn(givenName, new HashMap<String, Object>());
		hIVLostToFollowup.addColumn(givenName, new HashMap<String, Object>());
		hIVLostToFollowup_1.addColumn(givenName, new HashMap<String, Object>());
		preARTBelow350CD4.addColumn(givenName, new HashMap<String, Object>());
		hIVLowBMI.addColumn(givenName, new HashMap<String, Object>());
		hIVLowBMI_1.addColumn(givenName, new HashMap<String, Object>());
		dataSetDefinition7.addColumn(givenName, new HashMap<String, Object>());
		viralLoadGreaterThan20InTheLast3Months.addColumn(givenName, new HashMap<String, Object>());
		dataSetDefinition9.addColumn(givenName, new HashMap<String, Object>());

		PatientProperty familyName = RowPerPatientColumns.getFamilyNameColumn("Last Name");
		adultARTLateVisit.addColumn(familyName, new HashMap<String, Object>());
		adultPreARTLateVisit.addColumn(familyName, new HashMap<String, Object>());
		adultHIVLateCD4Count.addColumn(familyName, new HashMap<String, Object>());
		adultHIVLateCD4Count_1.addColumn(familyName, new HashMap<String, Object>());
		hIVLostToFollowup.addColumn(familyName, new HashMap<String, Object>());
		hIVLostToFollowup_1.addColumn(familyName, new HashMap<String, Object>());
		preARTBelow350CD4.addColumn(familyName, new HashMap<String, Object>());
		hIVLowBMI.addColumn(familyName, new HashMap<String, Object>());
		hIVLowBMI_1.addColumn(familyName, new HashMap<String, Object>());
		dataSetDefinition7.addColumn(familyName, new HashMap<String, Object>());
		viralLoadGreaterThan20InTheLast3Months.addColumn(familyName, new HashMap<String, Object>());
		dataSetDefinition9.addColumn(familyName, new HashMap<String, Object>());

		PatientProperty gender = RowPerPatientColumns.getGender("Sex");
		adultARTLateVisit.addColumn(gender, new HashMap<String, Object>());
		adultPreARTLateVisit.addColumn(gender, new HashMap<String, Object>());
		adultHIVLateCD4Count.addColumn(gender, new HashMap<String, Object>());
		adultHIVLateCD4Count_1.addColumn(gender, new HashMap<String, Object>());
		hIVLostToFollowup.addColumn(gender, new HashMap<String, Object>());
		hIVLostToFollowup_1.addColumn(gender, new HashMap<String, Object>());
		preARTBelow350CD4.addColumn(gender, new HashMap<String, Object>());
		hIVLowBMI.addColumn(gender, new HashMap<String, Object>());
		hIVLowBMI_1.addColumn(gender, new HashMap<String, Object>());
		dataSetDefinition7.addColumn(gender, new HashMap<String, Object>());
		viralLoadGreaterThan20InTheLast3Months.addColumn(gender, new HashMap<String, Object>());
		dataSetDefinition9.addColumn(gender, new HashMap<String, Object>());

		DateOfBirthShowingEstimation birthdate = RowPerPatientColumns.getDateOfBirth("Date of Birth", null, null);
		adultARTLateVisit.addColumn(birthdate, new HashMap<String, Object>());
		adultPreARTLateVisit.addColumn(birthdate, new HashMap<String, Object>());
		adultHIVLateCD4Count.addColumn(birthdate, new HashMap<String, Object>());
		adultHIVLateCD4Count_1.addColumn(birthdate, new HashMap<String, Object>());
		hIVLostToFollowup.addColumn(birthdate, new HashMap<String, Object>());
		hIVLostToFollowup_1.addColumn(birthdate, new HashMap<String, Object>());
		preARTBelow350CD4.addColumn(birthdate, new HashMap<String, Object>());
		hIVLowBMI.addColumn(birthdate, new HashMap<String, Object>());
		hIVLowBMI_1.addColumn(birthdate, new HashMap<String, Object>());
		dataSetDefinition7.addColumn(birthdate, new HashMap<String, Object>());
		viralLoadGreaterThan20InTheLast3Months.addColumn(birthdate, new HashMap<String, Object>());
		dataSetDefinition9.addColumn(birthdate, new HashMap<String, Object>());

		StateOfPatient txGroup = RowPerPatientColumns.getStateOfPatient("Group", hivProgram, treatmentGroup,
				new GroupStateFilter());
		adultARTLateVisit.addColumn(txGroup, new HashMap<String, Object>());
		adultPreARTLateVisit.addColumn(txGroup, new HashMap<String, Object>());
		adultHIVLateCD4Count.addColumn(txGroup, new HashMap<String, Object>());
		adultHIVLateCD4Count_1.addColumn(txGroup, new HashMap<String, Object>());
		hIVLostToFollowup.addColumn(txGroup, new HashMap<String, Object>());
		hIVLostToFollowup_1.addColumn(txGroup, new HashMap<String, Object>());
		preARTBelow350CD4.addColumn(txGroup, new HashMap<String, Object>());
		hIVLowBMI.addColumn(txGroup, new HashMap<String, Object>());
		hIVLowBMI_1.addColumn(txGroup, new HashMap<String, Object>());
		dataSetDefinition7.addColumn(txGroup, new HashMap<String, Object>());
		viralLoadGreaterThan20InTheLast3Months.addColumn(txGroup, new HashMap<String, Object>());
		dataSetDefinition9.addColumn(txGroup, new HashMap<String, Object>());

		StateOfPatient stOfPatient = RowPerPatientColumns.getStateOfPatient("Treatment", hivProgram, treatmentStatus,
				new TreatmentStateFilter());
		
		dataSetDefinition7.addColumn(stOfPatient, new HashMap<String, Object>());
		
		dataSetDefinition9.addColumn(stOfPatient, new HashMap<String, Object>());

		MostRecentObservation returnVisitDate = RowPerPatientColumns
				.getMostRecentReturnVisitDate("Date of missed appointment", null);
		adultARTLateVisit.addColumn(returnVisitDate, new HashMap<String, Object>());
		adultPreARTLateVisit.addColumn(returnVisitDate, new HashMap<String, Object>());
		hIVLostToFollowup.addColumn(returnVisitDate, new HashMap<String, Object>());
		hIVLostToFollowup_1.addColumn(returnVisitDate, new HashMap<String, Object>());
		preARTBelow350CD4.addColumn(returnVisitDate, new HashMap<String, Object>());
		hIVLowBMI.addColumn(returnVisitDate, new HashMap<String, Object>());
		hIVLowBMI_1.addColumn(returnVisitDate, new HashMap<String, Object>());
		dataSetDefinition7.addColumn(returnVisitDate, new HashMap<String, Object>());
		viralLoadGreaterThan20InTheLast3Months.addColumn(returnVisitDate, new HashMap<String, Object>());
		dataSetDefinition9.addColumn(returnVisitDate, new HashMap<String, Object>());

		MostRecentObservation cd4Count = RowPerPatientColumns.getMostRecentCD4("Most recent CD4", null);
		adultARTLateVisit.addColumn(cd4Count, new HashMap<String, Object>());
		adultPreARTLateVisit.addColumn(cd4Count, new HashMap<String, Object>());
		adultHIVLateCD4Count.addColumn(cd4Count, new HashMap<String, Object>());
		adultHIVLateCD4Count_1.addColumn(cd4Count, new HashMap<String, Object>());
		hIVLostToFollowup.addColumn(cd4Count, new HashMap<String, Object>());
		hIVLostToFollowup_1.addColumn(cd4Count, new HashMap<String, Object>());
		preARTBelow350CD4.addColumn(cd4Count, new HashMap<String, Object>());
		hIVLowBMI.addColumn(cd4Count, new HashMap<String, Object>());
		hIVLowBMI_1.addColumn(cd4Count, new HashMap<String, Object>());
		dataSetDefinition7.addColumn(cd4Count, new HashMap<String, Object>());
		viralLoadGreaterThan20InTheLast3Months.addColumn(cd4Count, new HashMap<String, Object>());
		dataSetDefinition9.addColumn(cd4Count, new HashMap<String, Object>());

		DateDiff lateCD4InMonths = RowPerPatientColumns.getDifferenceSinceLastObservation("Late CD4 in months", cd4,
				DateDiffType.MONTHS);
		lateCD4InMonths.addParameter(new Parameter("endDate", "endDate", Date.class));
		adultARTLateVisit.addColumn(lateCD4InMonths,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		adultPreARTLateVisit.addColumn(lateCD4InMonths,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		adultHIVLateCD4Count.addColumn(lateCD4InMonths,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		adultHIVLateCD4Count_1.addColumn(lateCD4InMonths,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		hIVLostToFollowup.addColumn(lateCD4InMonths,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		hIVLostToFollowup_1.addColumn(lateCD4InMonths,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		preARTBelow350CD4.addColumn(lateCD4InMonths,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		hIVLowBMI.addColumn(lateCD4InMonths,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		hIVLowBMI_1.addColumn(lateCD4InMonths,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition7.addColumn(lateCD4InMonths,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		viralLoadGreaterThan20InTheLast3Months.addColumn(lateCD4InMonths,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition9.addColumn(lateCD4InMonths,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}"));

		PatientRelationship accompagnateur = RowPerPatientColumns.getAccompRelationship("Accompagnateur");
		adultARTLateVisit.addColumn(accompagnateur, new HashMap<String, Object>());
		adultPreARTLateVisit.addColumn(accompagnateur, new HashMap<String, Object>());
		adultHIVLateCD4Count.addColumn(accompagnateur, new HashMap<String, Object>());
		adultHIVLateCD4Count_1.addColumn(accompagnateur, new HashMap<String, Object>());
		hIVLostToFollowup.addColumn(accompagnateur, new HashMap<String, Object>());
		hIVLostToFollowup_1.addColumn(accompagnateur, new HashMap<String, Object>());
		preARTBelow350CD4.addColumn(accompagnateur, new HashMap<String, Object>());
		hIVLowBMI.addColumn(accompagnateur, new HashMap<String, Object>());
		hIVLowBMI_1.addColumn(accompagnateur, new HashMap<String, Object>());
		dataSetDefinition7.addColumn(accompagnateur, new HashMap<String, Object>());
		viralLoadGreaterThan20InTheLast3Months.addColumn(accompagnateur, new HashMap<String, Object>());
		dataSetDefinition9.addColumn(accompagnateur, new HashMap<String, Object>());

		PatientAddress address1 = RowPerPatientColumns.getPatientAddress("Address", true, true, true, true);
		adultARTLateVisit.addColumn(address1, new HashMap<String, Object>());
		adultPreARTLateVisit.addColumn(address1, new HashMap<String, Object>());
		adultHIVLateCD4Count.addColumn(address1, new HashMap<String, Object>());
		adultHIVLateCD4Count_1.addColumn(address1, new HashMap<String, Object>());
		hIVLostToFollowup.addColumn(address1, new HashMap<String, Object>());
		hIVLostToFollowup_1.addColumn(address1, new HashMap<String, Object>());
		preARTBelow350CD4.addColumn(address1, new HashMap<String, Object>());
		hIVLowBMI.addColumn(address1, new HashMap<String, Object>());
		hIVLowBMI_1.addColumn(address1, new HashMap<String, Object>());
		dataSetDefinition7.addColumn(address1, new HashMap<String, Object>());
		viralLoadGreaterThan20InTheLast3Months.addColumn(address1, new HashMap<String, Object>());
		dataSetDefinition9.addColumn(address1, new HashMap<String, Object>());

		MultiplePatientDataDefinitions tracNetId = RowPerPatientColumns.getTracnetId("TRACNET_ID");

		adultARTLateVisit.addColumn(tracNetId, new HashMap<String, Object>());
		adultPreARTLateVisit.addColumn(tracNetId, new HashMap<String, Object>());
		adultHIVLateCD4Count.addColumn(tracNetId, new HashMap<String, Object>());
		adultHIVLateCD4Count_1.addColumn(tracNetId, new HashMap<String, Object>());
		hIVLostToFollowup.addColumn(tracNetId, new HashMap<String, Object>());
		hIVLostToFollowup_1.addColumn(tracNetId, new HashMap<String, Object>());
		preARTBelow350CD4.addColumn(tracNetId, new HashMap<String, Object>());
		hIVLowBMI.addColumn(tracNetId, new HashMap<String, Object>());
		hIVLowBMI_1.addColumn(tracNetId, new HashMap<String, Object>());
		dataSetDefinition7.addColumn(tracNetId, new HashMap<String, Object>());
		viralLoadGreaterThan20InTheLast3Months.addColumn(tracNetId, new HashMap<String, Object>());
		dataSetDefinition9.addColumn(tracNetId, new HashMap<String, Object>());

		MostRecentObservation viralLoad = RowPerPatientColumns.getMostRecentViralLoad("Most recent viralLoad", null);
		// dataSetDefinition8.addColumn(viralLoad, new HashMap<String,
		// Object>());
		adultARTLateVisit.addColumn(viralLoad, new HashMap<String, Object>());
		adultHIVLateCD4Count.addColumn(viralLoad, new HashMap<String, Object>());
		hIVLostToFollowup.addColumn(viralLoad, new HashMap<String, Object>());
		hIVLowBMI.addColumn(viralLoad, new HashMap<String, Object>());
		viralLoadGreaterThan20InTheLast3Months.addColumn(viralLoad, new HashMap<String, Object>());

		MostRecentObservation weight = RowPerPatientColumns.getMostRecentWeight("Weight", "dd-mmm-yyyy");
		hIVLowBMI.addColumn(weight, new HashMap<String, Object>());
		hIVLowBMI_1.addColumn(weight, new HashMap<String, Object>());

		MostRecentObservation height = RowPerPatientColumns.getMostRecentHeight("Height", "dd-mmm-yyyy");
		hIVLowBMI.addColumn(height, new HashMap<String, Object>());
		hIVLowBMI_1.addColumn(height, new HashMap<String, Object>());

		CustomCalculationBasedOnMultiplePatientDataDefinitions bmi = new CustomCalculationBasedOnMultiplePatientDataDefinitions();
		bmi.setName("BMI");
		bmi.addPatientDataToBeEvaluated(weight, new HashMap<String, Object>());
		bmi.addPatientDataToBeEvaluated(height, new HashMap<String, Object>());
		BMICalculation bmiCalc = new BMICalculation();
		bmiCalc.setHeightName(height.getName());
		bmiCalc.setWeightName(weight.getName());
		bmi.setCalculator(bmiCalc);
		hIVLowBMI.addColumn(bmi, new HashMap<String, Object>());
		hIVLowBMI_1.addColumn(bmi, new HashMap<String, Object>());

		AllObservationValues allCD4 = RowPerPatientColumns.getAllCD4Values("allCD4Obs", "dd-mmm-yyyy", null, null);
		CustomCalculationBasedOnMultiplePatientDataDefinitions decline = new CustomCalculationBasedOnMultiplePatientDataDefinitions();
		decline.setName("Decline");
		decline.addPatientDataToBeEvaluated(allCD4, new HashMap<String, Object>());
		decline.setCalculator(new DifferenceBetweenLastTwoObs());
		dataSetDefinition7.addColumn(decline, new HashMap<String, Object>());

		FirstDrugOrderStartedRestrictedByConceptSet startArt = RowPerPatientColumns
				.getDrugOrderForStartOfART("StartART", "dd-MMM-yyyy");

		CustomCalculationBasedOnMultiplePatientDataDefinitions cd4Decline = new CustomCalculationBasedOnMultiplePatientDataDefinitions();
		cd4Decline.setName("cd4Decline");
		cd4Decline.addPatientDataToBeEvaluated(allCD4, new HashMap<String, Object>());
		cd4Decline.addPatientDataToBeEvaluated(startArt, new HashMap<String, Object>());
		DeclineHighestCD4 declineCD4 = new DeclineHighestCD4();
		declineCD4.setInitiationArt("StartART");
		declineCD4.setShortDisplay(true);
		cd4Decline.setCalculator(declineCD4);
		dataSetDefinition9.addColumn(cd4Decline, new HashMap<String, Object>());

		adultARTLateVisit.addParameter(new Parameter("location", "Location", Location.class));
		adultPreARTLateVisit.addParameter(new Parameter("location", "Location", Location.class));
		adultHIVLateCD4Count.addParameter(new Parameter("location", "Location", Location.class));
		adultHIVLateCD4Count_1.addParameter(new Parameter("location", "Location", Location.class));
		hIVLostToFollowup.addParameter(new Parameter("location", "Location", Location.class));
		hIVLostToFollowup_1.addParameter(new Parameter("location", "Location", Location.class));
		preARTBelow350CD4.addParameter(new Parameter("location", "Location", Location.class));
		hIVLowBMI.addParameter(new Parameter("location", "Location", Location.class));
		hIVLowBMI_1.addParameter(new Parameter("location", "Location", Location.class));
		dataSetDefinition7.addParameter(new Parameter("location", "Location", Location.class));
		viralLoadGreaterThan20InTheLast3Months.addParameter(new Parameter("location", "Location", Location.class));
		dataSetDefinition9.addParameter(new Parameter("location", "Location", Location.class));

		adultARTLateVisit.addParameter(new Parameter("endDate", "End Date", Date.class));
		adultPreARTLateVisit.addParameter(new Parameter("endDate", "End Date", Date.class));
		adultHIVLateCD4Count.addParameter(new Parameter("endDate", "End Date", Date.class));
		adultHIVLateCD4Count_1.addParameter(new Parameter("endDate", "End Date", Date.class));
		hIVLostToFollowup.addParameter(new Parameter("endDate", "End Date", Date.class));
		hIVLostToFollowup_1.addParameter(new Parameter("endDate", "End Date", Date.class));
		preARTBelow350CD4.addParameter(new Parameter("endDate", "End Date", Date.class));
		hIVLowBMI.addParameter(new Parameter("endDate", "End Date", Date.class));
		hIVLowBMI_1.addParameter(new Parameter("endDate", "End Date", Date.class));
		dataSetDefinition7.addParameter(new Parameter("endDate", "End Date", Date.class));
		viralLoadGreaterThan20InTheLast3Months.addParameter(new Parameter("endDate", "End Date", Date.class));
		dataSetDefinition9.addParameter(new Parameter("endDate", "End Date", Date.class));

		Map<String, Object> mappings = new HashMap<String, Object>();
		mappings.put("location", "${location}");
		mappings.put("endDate", "${endDate}");

		//art.addDataSetDefinition("AdultARTLateVisit", adultARTLateVisit, mappings);
		//art.addDataSetDefinition("AdultHIVLateCD4Count", adultHIVLateCD4Count, mappings);
		//art.addDataSetDefinition("HIVLostToFollowup", hIVLostToFollowup, mappings);
		art.addDataSetDefinition("HIVLowBMI", hIVLowBMI, mappings);
		art.addDataSetDefinition("ViralLoadGreaterThan20InTheLast3Months", viralLoadGreaterThan20InTheLast3Months, mappings);

		//preArt.addDataSetDefinition("AdultPreARTLateVisit", adultPreARTLateVisit, mappings);
		//preArt.addDataSetDefinition("AdultHIVLateCD4Count", adultHIVLateCD4Count_1, mappings);
		//preArt.addDataSetDefinition("HIVLostToFollowup", hIVLostToFollowup_1, mappings);
		//preArt.addDataSetDefinition("PreARTBelow350CD4", preARTBelow350CD4, mappings);
		//preArt.addDataSetDefinition("HIVLowBMI", hIVLowBMI_1, mappings);
	}

	private void setupProperties() {
		hivProgram = gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM);

		onART = gp.getProgramWorkflowState(GlobalPropertiesManagement.ON_ANTIRETROVIRALS_STATE,
				GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW, GlobalPropertiesManagement.ADULT_HIV_PROGRAM);

		//clinicalEnountersIncLab = gp.getEncounterTypeList(GlobalPropertiesManagement.CLINICAL_ENCOUNTER_TYPES);

		//clinicalEncoutersExcLab = gp
				//.getEncounterTypeList(GlobalPropertiesManagement.CLINICAL_ENCOUNTER_TYPES_EXC_LAB_TEST);

		following = gp.getProgramWorkflowState(GlobalPropertiesManagement.FOLLOWING_STATE,
				GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW, GlobalPropertiesManagement.ADULT_HIV_PROGRAM);

		cd4 = gp.getConcept(GlobalPropertiesManagement.CD4_TEST);

		treatmentGroup = gp.getProgramWorkflow(GlobalPropertiesManagement.TREATMENT_GROUP_WORKFLOW,
				GlobalPropertiesManagement.ADULT_HIV_PROGRAM);

		treatmentStatus = gp.getProgramWorkflow(GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW,
				GlobalPropertiesManagement.ADULT_HIV_PROGRAM);

		height = gp.getConcept(GlobalPropertiesManagement.HEIGHT_CONCEPT);

		weight = gp.getConcept(GlobalPropertiesManagement.WEIGHT_CONCEPT);

		viralLoad = gp.getConcept(GlobalPropertiesManagement.VIRAL_LOAD_TEST);

		//labTestEncounterType = gp.getEncounterType(GlobalPropertiesManagement.LAB_ENCOUNTER_TYPE);
	}
}
