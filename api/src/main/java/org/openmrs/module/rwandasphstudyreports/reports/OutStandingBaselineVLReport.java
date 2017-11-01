package org.openmrs.module.rwandasphstudyreports.reports;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Program;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.CompositionCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InverseCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.common.SortCriteria;
import org.openmrs.module.reporting.common.SortCriteria.SortDirection;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
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

public class OutStandingBaselineVLReport implements SetupReport {
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

	BaseSPHReportConfig config = new BaseSPHReportConfig();

	@Override
	public void setup() throws Exception {
		if("true".equals(Context.getAdministrationService().getGlobalProperty(BaseSPHReportConfig.RECREATE_REPORTS_ON_ACTIVATION)))
			delete();
		setupProperties();
		setupProperties();

		ReportDefinition rd = createReportDefinition();
		config.setupReport(rd, "OutStandingBaselineVL", "OutStandingBaselineVL.xls");
	}

	@Override
	public void delete() {
		config.deleteReportDefinition("OutStandingBaselineVL");
	}

	private ReportDefinition createReportDefinition() {
		ReportDefinition reportDefinition = new ReportDefinition();

		reportDefinition.setDescription("OutStanding Baseline Viral Load Tests");
		reportDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		reportDefinition.setName("OutStandingBaselineVL");
		reportDefinition.setUuid(BaseSPHReportConfig.OUTSTANDINGBASELINEVLREPORT);
		createDataSetDefinition(reportDefinition);
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

		sortCriteria.addSortElement("nextRDV", SortDirection.ASC);
		sortCriteria.addSortElement("familyName", SortDirection.ASC);
		sortCriteria.addSortElement("LastVisit Date", SortDirection.DESC);
		dataSetDefinition.setSortCriteria(sortCriteria);

		dataSetDefinition.addParameter(reportDefinition.getParameter("endDate"));
		dataSetDefinition.setName(reportDefinition.getName() + " Data Set");
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getDrugOrderForStartOfART("artInitiation", "dd/MMM/yyyy"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getDateOfEarliestProgramEnrolment("hivEnrolment", hivProgram, "dd/MMM/yyyy"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getDateCreatedColumn("registrationDate"),
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
		monthSinceLastVL.addParameter(reportDefinition.getParameter("endDate"));

		dataSetDefinition.addColumn(monthSinceLastVisit,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition.addColumn(monthSinceLastVL,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
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
		dataSetDefinition.addColumn(RowPerPatientColumns.patientAttribute("Peer Educator's Name", "peerEducator"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.patientAttribute("Peer Educator's Phone Number", "peerEducatorPhone"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.patientAttribute("Phone Number", "privatePhone"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.patientAttribute("Contact Person's Name", "contactPerson"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.patientAttribute("Contact Person's Phone Number", "contactPersonTel"), new HashMap<String, Object>());

		SqlCohortDefinition adultPatientsCohort = Cohorts.getAdultPatients();
		CompositionCohortDefinition hivPositive = Cohorts.getHIVPositivePatientsOrMissingResult();
		SqlCohortDefinition onART = Cohorts.getPatientsOnART(null);
		SqlCohortDefinition inHIVForAtleast8Months = Cohorts.inProgramForNMonthsFromEnrollment(hivProgram, 8);
		SqlCohortDefinition noVL = Cohorts.createNoObservationDefintion(viralLoad);

		dataSetDefinition.addFilter(adultPatientsCohort, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition.addFilter(hivPositive, null);
		dataSetDefinition.addFilter(inHIVForAtleast8Months, mappings);
		dataSetDefinition.addFilter(Cohorts.createInProgramParameterizableByDate("adultHIV: In Program", hivProgram),
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate}"));
		dataSetDefinition.addFilter(onART, null);
		dataSetDefinition.addFilter(noVL, null);
		
		dataSetDefinition.addFilter(new InverseCohortDefinition(Cohorts.getPatientsExitedFromHIVCare()), null);

		reportDefinition.addDataSetDefinition("OutStandingBaselineVL", dataSetDefinition, mappings);
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
