package org.openmrs.module.rwandasphstudyreports.reports;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Program;
import org.openmrs.module.reporting.cohort.definition.CompositionCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InverseCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.NumericObsCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.common.SortCriteria;
import org.openmrs.module.reporting.common.SortCriteria.SortDirection;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.rowperpatientreports.dataset.definition.RowPerPatientDataSetDefinition;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateDiff;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateDiff.DateDiffType;
import org.openmrs.module.rwandareports.reporting.SetupReport;
import org.openmrs.module.rwandasphstudyreports.Cohorts;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertiesManagement;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertyConstants;
import org.openmrs.module.rwandasphstudyreports.Helper;
import org.openmrs.module.rwandasphstudyreports.RowPerPatientColumns;

public class VLBasedTreatmentFailureReport implements SetupReport {
	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();

	private Program hivProgram;

	private Concept scheduledVisit;

	private List<EncounterType> encounterTypes;

	private Concept viralLoad;

	private Concept telephone;

	private Concept telephone2;

	BaseSPHReportConfig config = new BaseSPHReportConfig();

	@Override
	public void setup() throws Exception {
		setupProperties();

		ReportDefinition rd = createReportDefinition();
		config.setupReport(rd, "VLBasedTreatmentFailure", "VLBasedTreatmentFailure.xls");
	}

	@Override
	public void delete() {
		config.deleteReportDefinition("VLBasedTreatmentFailure");
	}

	private ReportDefinition createReportDefinition() {
		ReportDefinition reportDefinition = config.createReportDefinition("VLBasedTreatmentFailure");
		createDataSetDefinition(reportDefinition);
		reportDefinition.setDescription("Viral Load Based Treatment Failure");
		reportDefinition.setUuid(BaseSPHReportConfig.VLBASEDTREATMENTFAILUREREPORT);
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
		dataSetDefinition.addColumn(RowPerPatientColumns.patientAttribute("Peer Educator's Name", "peerEducator"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.patientAttribute("Peer Educator's Phone Number", "peerEducatorPhone"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.patientAttribute("Phone Number", "privatePhone"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.patientAttribute("Contact Person's Name", "contactPerson"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.patientAttribute("Contact Person's Phone Number", "contactPersonTel"), new HashMap<String, Object>());

		SqlCohortDefinition adultPatientsCohort = Cohorts.getAdultPatients();
		CompositionCohortDefinition hivPositive = Cohorts.getHIVPositivePatientsOrMissingResult();
		SqlCohortDefinition onART = Cohorts.getPatientsOnART(12);
		NumericObsCohortDefinition vlAbove1000 = Cohorts.patientsWithLastVLAbove1000();
		SqlCohortDefinition inProgramFor12MonthsFromEnrollment = Cohorts.inProgramForNMonthsFromEnrollment(hivProgram, 12);
		//SqlCohortDefinition withVLDateBetweenStartAndEndDate = Cohorts.withObsInStartEndDateRange(viralLoad);
		
		dataSetDefinition.addFilter(Cohorts.createInProgramParameterizableByDate("adultHIV: In Program", hivProgram), ParameterizableUtil.createParameterMappings("onOrAfter=${startDate},onOrBefore=${endDate}"));
		dataSetDefinition.addFilter(adultPatientsCohort, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition.addFilter(hivPositive, null);
		dataSetDefinition.addFilter(onART, null);
		//dataSetDefinition.addFilter(Cohorts.createCodedObsCohortDefinition(viralLoad, null, null, null), null);
		dataSetDefinition.addFilter(vlAbove1000, mappings);
		dataSetDefinition.addFilter(inProgramFor12MonthsFromEnrollment, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		//dataSetDefinition.addFilter(withVLDateBetweenStartAndEndDate, mappings);
		dataSetDefinition.addFilter(new InverseCohortDefinition(Cohorts.getPatientsExitedFromHIVCare()), null);
		
		reportDefinition.addDataSetDefinition("VLBasedTreatmentFailure", dataSetDefinition, mappings);
	}

	private void setupProperties() {
		hivProgram = gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
		scheduledVisit = gp.getConcept(GlobalPropertyConstants.RETURN_VISIT_CONCEPTID);
		encounterTypes = gp.getEncounterTypeList(GlobalPropertyConstants.ADULT_ENCOUNTER_TYPE_IDS);
		viralLoad = gp.getConcept(GlobalPropertyConstants.VIRAL_LOAD_CONCEPTID);
		telephone = gp.getConcept(GlobalPropertiesManagement.TELEPHONE_NUMBER_CONCEPT);
		telephone2 = gp.getConcept(GlobalPropertiesManagement.SECONDARY_TELEPHONE_NUMBER_CONCEPT);
	}
}
