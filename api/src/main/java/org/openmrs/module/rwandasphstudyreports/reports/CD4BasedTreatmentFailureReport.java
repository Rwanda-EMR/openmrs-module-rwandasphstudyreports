package org.openmrs.module.rwandasphstudyreports.reports;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Program;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.CodedObsCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.common.SortCriteria;
import org.openmrs.module.reporting.common.SortCriteria.SortDirection;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.rowperpatientreports.dataset.definition.RowPerPatientDataSetDefinition;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateDiff;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateDiff.DateDiffType;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateOfFirstDrugOrderStartedRestrictedByConceptSet;
import org.openmrs.module.rwandareports.reporting.SetupReport;
import org.openmrs.module.rwandasphstudyreports.Cohorts;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertiesManagement;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertyConstants;
import org.openmrs.module.rwandasphstudyreports.Helper;
import org.openmrs.module.rwandasphstudyreports.RowPerPatientColumns;

public class CD4BasedTreatmentFailureReport implements SetupReport {

	BaseSPHReportConfig config = new BaseSPHReportConfig();
	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();

	private Program hivProgram;

	private Concept scheduledVisit;

	private List<EncounterType> encounterTypes;

	private Concept cd4Count;

	private Concept viralLoad;

	private EncounterType adultFollowUpEncounterType;

	private Concept hivStatus;

	private Concept telephone;

	private Concept telephone2;

	private Concept reasonForExitingCare;

	private Concept transferOut;

	@Override
	public void setup() throws Exception {
		if("true".equals(Context.getAdministrationService().getGlobalProperty(BaseSPHReportConfig.RECREATEREPORTSONACTIVATION)))
			delete();
		setupProperties();
		setupProperties();

		ReportDefinition rd = createReportDefinition();
		config.setupReport(rd, "CD4BasedTreatmentFailure", "CD4BasedTreatmentFailure.xls");
	}

	@Override
	public void delete() {
		config.deleteReportDefinition("CD4BasedTreatmentFailure");
	}

	private ReportDefinition createReportDefinition() {
		ReportDefinition reportDefinition = config.createReportDefinition("CD4BasedTreatmentFailure");
		createDataSetDefinition(reportDefinition);
		reportDefinition.setDescription("CD4 Count Based Treatment Failure");
		reportDefinition.setUuid(BaseSPHReportConfig.CD4BASEDTREATMENTFAILUREREPORT);
		Helper.saveReportDefinition(reportDefinition);

		return reportDefinition;
	}

	private void createDataSetDefinition(ReportDefinition reportDefinition) {
		RowPerPatientDataSetDefinition dataSetDefinition = new RowPerPatientDataSetDefinition();
		DateDiff monthSinceLastVisit = RowPerPatientColumns.getDifferenceSinceLastEncounter("MonthsSinceLastVisit",
				encounterTypes, DateDiffType.MONTHS);
		DateDiff monthSinceLastVL = RowPerPatientColumns.getDifferenceSinceLastObservation("MonthsSinceLastViralLoad",
				viralLoad, DateDiffType.MONTHS);
		SortCriteria sortCriteria = new SortCriteria();
		Map<String, Object> mappings = new HashMap<String, Object>();
		DateOfFirstDrugOrderStartedRestrictedByConceptSet artStart = RowPerPatientColumns
				.getDateOfDrugOrderForStartOfARTBeforeDate("artStart", "dd/MMM/yyyy");

		mappings.put("endDate", "${endDate}");
		mappings.put("startDate", "${startDate}");

		sortCriteria.addSortElement("nextRDV", SortDirection.ASC);
		sortCriteria.addSortElement("familyName", SortDirection.ASC);
		sortCriteria.addSortElement("LastVisit Date", SortDirection.DESC);
		dataSetDefinition.setSortCriteria(sortCriteria);

		dataSetDefinition.addParameter(reportDefinition.getParameter("startDate"));
		dataSetDefinition.addParameter(reportDefinition.getParameter("endDate"));
		dataSetDefinition.setName(reportDefinition.getName() + " Data Set");
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getDrugOrderForStartOfART("artInitiation", "dd/MMM/yyyy"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getAccompRelationship("accompagnateur"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getTracnetId("TRACNET_ID"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getSystemId("patientID"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getGender("sex"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getDateOfBirth("birth_date", "dd/MMM/yyyy", null),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentReturnVisitDate("returnVisit", "dd/MMM/yyyy"),
				new HashMap<String, Object>());
		monthSinceLastVisit.addParameter(reportDefinition.getParameter("endDate"));
		monthSinceLastVisit.addParameter(reportDefinition.getParameter("startDate"));
		monthSinceLastVL.addParameter(reportDefinition.getParameter("endDate"));
		monthSinceLastVL.addParameter(reportDefinition.getParameter("startDate"));

		dataSetDefinition.addColumn(monthSinceLastVisit,
				ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate}"));
		dataSetDefinition.addColumn(monthSinceLastVL,
				ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate}"));
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("nextRDV", scheduledVisit, "dd/MMM/yyyy"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("telephone", telephone, null),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("telephone2", telephone2, null),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getPatientAddress("address", true, true, true, true),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentViralLoad("viralLoad", "dd/MMM/yyyy"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(
				RowPerPatientColumns.getRecentEncounterType("lastvisit", encounterTypes, "dd/MMM/yyyy", null),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(
				RowPerPatientColumns.getAllViralLoadsValues("viralLoads", "dd/MMM/yyyy", null, null),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentCD4("cD4Test", "dd/MMM/yyyy"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(
				RowPerPatientColumns.getDrugRegimenInformationParameterized("regimen", false, false),
				ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate}"));
		dataSetDefinition.addColumn(
				RowPerPatientColumns.getDrugRegimenInformationParameterized("regimen", false, false),
				ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate}"));

		SqlCohortDefinition adultPatientsCohort = Cohorts.getAdultPatients();
		CodedObsCohortDefinition hivPositive = Cohorts.getHIVPositivePatients();
		SqlCohortDefinition onART = Cohorts.getPatientsOnART(12);
		SqlCohortDefinition cd4declineOfMoreThan50Percent = Cohorts.createPatientsWithDeclineFromBaseline("cd4decline",
				cd4Count);
		dataSetDefinition.addColumn(
				RowPerPatientColumns.getBaselineObservationAtMonthBeforeEndDate("cd4AtArtInitiation", cd4Count, 30, 30,
						0, artStart, ParameterizableUtil.createParameterMappings("endDate=${endDate}"), "dd/MMM/yyyy"),
				null);

		dataSetDefinition.addFilter(adultPatientsCohort, null);
		dataSetDefinition.addFilter(hivPositive, null);
		dataSetDefinition.addFilter(onART, null);
		dataSetDefinition.addFilter(cd4declineOfMoreThan50Percent, mappings);

		reportDefinition.addDataSetDefinition("CD4BasedTreatmentFailure", dataSetDefinition, mappings);
	}

	private void setupProperties() {
		hivProgram = gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
		scheduledVisit = gp.getConcept(GlobalPropertyConstants.RETURN_VISIT_CONCEPTID);
		encounterTypes = gp.getEncounterTypeList(GlobalPropertyConstants.ADULT_ENCOUNTER_TYPE_IDS);
		cd4Count = gp.getConcept(GlobalPropertyConstants.CD4_COUNT_CONCEPTID);
		viralLoad = gp.getConcept(GlobalPropertyConstants.VIRAL_LOAD_CONCEPTID);
		adultFollowUpEncounterType = gp.getEncounterType(GlobalPropertyConstants.ADULT_FOLLOWUP_ENCOUNTER_TYPEID);
		hivStatus = gp.getConcept(GlobalPropertyConstants.HIV_STATUS_CONCEPTID);
		telephone = gp.getConcept(GlobalPropertiesManagement.TELEPHONE_NUMBER_CONCEPT);
		telephone2 = gp.getConcept(GlobalPropertiesManagement.SECONDARY_TELEPHONE_NUMBER_CONCEPT);
		reasonForExitingCare = gp.getConcept(GlobalPropertiesManagement.REASON_FOR_EXITING_CARE);
		transferOut = gp.getConcept(GlobalPropertiesManagement.TRASNFERED_OUT);
	}
}
