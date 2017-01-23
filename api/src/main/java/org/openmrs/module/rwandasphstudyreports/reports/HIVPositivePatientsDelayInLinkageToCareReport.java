package org.openmrs.module.rwandasphstudyreports.reports;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Program;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.CodedObsCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.common.SortCriteria;
import org.openmrs.module.reporting.common.SortCriteria.SortDirection;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rowperpatientreports.dataset.definition.RowPerPatientDataSetDefinition;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateDiff;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateDiff.DateDiffType;
import org.openmrs.module.rwandareports.reporting.SetupReport;
import org.openmrs.module.rwandasphstudyreports.Cohorts;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertiesManagement;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertyConstants;
import org.openmrs.module.rwandasphstudyreports.Helper;
import org.openmrs.module.rwandasphstudyreports.RowPerPatientColumns;

public class HIVPositivePatientsDelayInLinkageToCareReport implements SetupReport {
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

	private Concept guardianTelephone;

	private Concept contactTelephone;

	@Override
	public void setup() throws Exception {
		setupProperties();

		ReportDefinition rd = createReportDefinition();
		ReportDesign design = Helper.createRowPerPatientXlsOverviewReportDesign(rd,
				"HIVPositivePatientsDelayInLinkageToCare.xls", "HIVPositivePatientsDelayInLinkageToCare", null);
		Properties props = new Properties();

		props.put("repeatingSections", "sheet:1,row:6,dataset:HIVPositivePatientsDelayInLinkageToCare");
		props.put("sortWeight", "5000");
		design.setProperties(props);

		Helper.saveReportDesign(design);
	}

	@Override
	public void delete() {
		ReportService rs = Context.getService(ReportService.class);

		for (ReportDesign rd : rs.getAllReportDesigns(false)) {
			if ("HIVPositivePatientsDelayInLinkageToCare".equals(rd.getName())) {
				rs.purgeReportDesign(rd);
			}
		}
		Helper.purgeReportDefinition("HIVPositivePatientsDelayInLinkageToCare");
	}

	private ReportDefinition createReportDefinition() {
		ReportDefinition reportDefinition = new ReportDefinition();

		reportDefinition.setName("HIVPositivePatientsDelayInLinkageToCare");
		reportDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		reportDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		reportDefinition.addParameter(new Parameter("location", "Health Center", Location.class));
		reportDefinition.setBaseCohortDefinition(Cohorts.createParameterizedLocationCohort("At Location"),
				ParameterizableUtil.createParameterMappings("location=${location}"));

		createDataSetDefinition(reportDefinition);
		Helper.saveReportDefinition(reportDefinition);

		return reportDefinition;
	}

	private void createDataSetDefinition(ReportDefinition reportDefinition) {
		RowPerPatientDataSetDefinition dataSetDefinition = new RowPerPatientDataSetDefinition();
		DateDiff daysSinceHivPositive = RowPerPatientColumns.getDifferenceSinceLastObservation("DaysSinceHivPositive",
				hivStatus, DateDiffType.DAYS);

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
		dataSetDefinition.addFilter(Cohorts.createInProgramParameterizableByDate("adultHIV: In Program", hivProgram),
				ParameterizableUtil.createParameterMappings("onDate=${now}"));

		dataSetDefinition.addColumn(RowPerPatientColumns.getTracnetId("TRACNET_ID"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getSystemId("patientID"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getGender("sex"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getDateOfBirth("birth_date", "dd/MMM/yyyy", null),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentHIVTest("hivTest", "dd/MMM/yyyy"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getDrugOrderForStartOfART("artInitiation", "dd/MMM/yyyy"),
				new HashMap<String, Object>());

		daysSinceHivPositive.addParameter(reportDefinition.getParameter("startDate"));
		daysSinceHivPositive.addParameter(reportDefinition.getParameter("endDate"));
		dataSetDefinition.addColumn(daysSinceHivPositive,
				ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate}"));
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("nextRDV", scheduledVisit, "dd/MMM/yyyy"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("telephone", telephone, null),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("telephone2", telephone2, null),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getPatientAddress("address", true, true, true, true),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getAccompRelationship("accompagnateur"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("contactTel", contactTelephone, null),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("guardianTel", guardianTelephone, null),
				new HashMap<String, Object>());

		CodedObsCohortDefinition hivPositive = Cohorts.getHIVPositivePatients();
		SqlCohortDefinition adultPatientsCohort = Cohorts.getAdultPatients();
		SqlCohortDefinition notInART = Cohorts.getPatientsOnOrNotOnART(false);

		dataSetDefinition.addFilter(adultPatientsCohort, null);
		dataSetDefinition.addFilter(hivPositive, null);
		dataSetDefinition.addFilter(notInART, null);

		reportDefinition.addDataSetDefinition("HIVPositivePatientsDelayInLinkageToCare", dataSetDefinition, mappings);
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
		contactTelephone = gp.getConcept(GlobalPropertyConstants.CONTACT_TEL_CONCEPTID);
		guardianTelephone = gp.getConcept(GlobalPropertyConstants.GUARDIAN_TEL_CONCEPTID);
	}

}
