package org.openmrs.module.rwandasphstudyreports.reports;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.api.PatientSetService.TimeModifier;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.AgeCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CodedObsCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CompositionCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.EncounterCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InProgramCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InStateCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.NumericObsCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.ProgramEnrollmentCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.common.DurationUnit;
import org.openmrs.module.reporting.common.RangeComparator;
import org.openmrs.module.reporting.common.SetComparator;
import org.openmrs.module.reporting.dataset.definition.CohortIndicatorDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.indicator.CohortIndicator;
import org.openmrs.module.reporting.query.encounter.definition.EncounterQuery;
import org.openmrs.module.reporting.query.encounter.definition.SqlEncounterQuery;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rwandareports.dataset.EncounterIndicatorDataSetDefinition;
import org.openmrs.module.rwandareports.dataset.LocationHierachyIndicatorDataSetDefinition;
import org.openmrs.module.rwandareports.indicator.EncounterIndicator;
import org.openmrs.module.rwandareports.util.Indicators;
import org.openmrs.module.rwandareports.widget.AllLocation;
import org.openmrs.module.rwandareports.widget.LocationHierarchy;
import org.openmrs.module.rwandasphstudyreports.Cohorts;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertiesManagement;
import org.openmrs.module.rwandasphstudyreports.Helper;

public class SetupIDProgramQuarterlyIndicatorReport {

	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();

	// properties
	private Program hivProgram;

	private Program pediHivProgram;

	private Program pmtctProgram;

	private Program pmtctCombinedMotherProgram;

	private ProgramWorkflowState hivPreArt;

	private ProgramWorkflowState pediPreArt;

	private ProgramWorkflowState hivArt;

	private ProgramWorkflowState pediArt;

	private List<Program> hivPrograms = new ArrayList<Program>();

	private List<Program> hivProgramsExcPMTCT = new ArrayList<Program>();

	private List<ProgramWorkflowState> preArtWorkflowStates = new ArrayList<ProgramWorkflowState>();

	private List<ProgramWorkflowState> artWorkflowStates = new ArrayList<ProgramWorkflowState>();

	private List<String> onOrAfterOnOrBefore = new ArrayList<String>();

	private List<EncounterType> hivEncounterTypes;

	private Concept cd4Concept;

	private Concept cd4PercentConcept;

	private Concept weightConcept;

	private EncounterType adultHIVEncounterType;

	private EncounterType pediHIVEncounterType;

	private Program pmtctCombinedInfantProgram;

	private List<ProgramWorkflowState> feedingWorkflowStates = new ArrayList<ProgramWorkflowState>();

	private EncounterType exposedInfantEncountertype;

	private Concept hivPCR;

	private Concept hivPositive;

	private Concept seroTest;
	private Concept ctx;
	private Concept nevirapine;

	private Concept methodOfFamilyPlanning;
	private Concept usingCondoms;
	private Concept naturalFamilyPlanning;
	private Concept viralLoadConcept;

	public void setup() throws Exception {

		setUpProperties();

		ReportDefinition rd = new ReportDefinition();
		rd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		rd.addParameter(new Parameter("endDate", "End Date", Date.class));

		Properties properties = new Properties();
		properties.setProperty("hierarchyFields", "countyDistrict:District");
		rd.addParameter(new Parameter("location", "Location", AllLocation.class, properties));

		rd.setName("HIV-Indicator Report-Quarterly");

		rd.addDataSetDefinition(createQuarterlyLocationDataSet(), ParameterizableUtil
				.createParameterMappings("startDate=${startDate},endDate=${endDate},location=${location}"));

		Helper.saveReportDefinition(rd);

		ReportDesign monthlyDesign = Helper.createRowPerPatientXlsOverviewReportDesign(rd, "HIVIndicator.xls",
				"HIVIndicators", null);
		Properties monthlyProps = new Properties();
		monthlyProps.put("repeatingSections", "sheet:1,dataset:Encounter Quarterly Data Set");
		monthlyProps.put("sortWeight", "5000");
		monthlyDesign.setProperties(monthlyProps);
		Helper.saveReportDesign(monthlyDesign);

	}

	public void delete() {
		ReportService rs = Context.getService(ReportService.class);
		for (ReportDesign rd : rs.getAllReportDesigns(false)) {
			if ("HIVIndicators".equals(rd.getName())) {
				rs.purgeReportDesign(rd);
			}
		}
		Helper.purgeReportDefinition("HIV-Indicator Report-Quarterly");

	}

	public LocationHierachyIndicatorDataSetDefinition createQuarterlyLocationDataSet() {

		LocationHierachyIndicatorDataSetDefinition ldsd = new LocationHierachyIndicatorDataSetDefinition(
				createQuarterlyEncounterBaseDataSet());
		ldsd.addBaseDefinition(createQuarterlyBaseDataSet());
		ldsd.setName("Encounter Quarterly Data Set");
		ldsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		ldsd.addParameter(new Parameter("endDate", "End Date", Date.class));
		ldsd.addParameter(new Parameter("location", "District", LocationHierarchy.class));

		return ldsd;
	}

	private EncounterIndicatorDataSetDefinition createQuarterlyEncounterBaseDataSet() {

		EncounterIndicatorDataSetDefinition eidsd = new EncounterIndicatorDataSetDefinition();

		eidsd.setName("eidsd");
		eidsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		eidsd.addParameter(new Parameter("endDate", "End Date", Date.class));

		createQuarterlyIndicators(eidsd);
		return eidsd;
	}

	private void createQuarterlyIndicators(EncounterIndicatorDataSetDefinition dsd) {

		// adults

		SqlEncounterQuery adultHivVisits = new SqlEncounterQuery();
		adultHivVisits.setName("adultHivVisits");
		adultHivVisits.setQuery("select e.encounter_id from encounter e, person p where e.encounter_type in ("
				+ pediHIVEncounterType.getEncounterTypeId() + "," + adultHIVEncounterType.getEncounterTypeId()
				+ ") and e.encounter_datetime >= :startDate and e.encounter_datetime <= :endDate and p.person_id = e.patient_id and DATEDIFF(:endDate , p.birthdate) >=5475 and e.voided=0 and p.voided=0");
		adultHivVisits.addParameter(new Parameter("startDate", "startDate", Date.class));
		adultHivVisits.addParameter(new Parameter("endDate", "endDate", Date.class));

		EncounterIndicator adultHivVisitsIndicator = new EncounterIndicator();
		adultHivVisitsIndicator.setName("adultHivVisitsIndicator");
		adultHivVisitsIndicator.setEncounterQuery(new Mapped<EncounterQuery>(adultHivVisits,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));

		dsd.addColumn(adultHivVisitsIndicator);

		SqlEncounterQuery adultHivVisitsWithWeight = new SqlEncounterQuery();
		adultHivVisitsWithWeight.setName("adultHivVisitsWithWeight");
		adultHivVisitsWithWeight.setQuery(
				"select e.encounter_id from encounter e,obs o, person p where e.encounter_id=o.encounter_id and o.concept_id="
						+ weightConcept.getConceptId() + " and e.encounter_type in ("
						+ pediHIVEncounterType.getEncounterTypeId() + "," + adultHIVEncounterType.getEncounterTypeId()
						+ ") and e.encounter_datetime >= :startDate and e.encounter_datetime <= :endDate and p.person_id = e.patient_id and DATEDIFF(:endDate , p.birthdate) >=5475 and e.voided=0 and p.voided=0 and o.voided=0");
		adultHivVisitsWithWeight.addParameter(new Parameter("startDate", "startDate", Date.class));
		adultHivVisitsWithWeight.addParameter(new Parameter("endDate", "endDate", Date.class));

		EncounterIndicator adultHivVisitsWithWeightIndicator = new EncounterIndicator();
		adultHivVisitsWithWeightIndicator.setName("adultHivVisitsWithWeightIndicator");
		adultHivVisitsWithWeightIndicator.setEncounterQuery(new Mapped<EncounterQuery>(adultHivVisitsWithWeight,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));

		dsd.addColumn(adultHivVisitsWithWeightIndicator);

		// pedi

		SqlEncounterQuery pediHivVisits = new SqlEncounterQuery();
		pediHivVisits.setName("pediHivVisits");
		pediHivVisits.setQuery("select e.encounter_id from encounter e, person p where e.encounter_type in ("
				+ pediHIVEncounterType.getEncounterTypeId() + "," + adultHIVEncounterType.getEncounterTypeId()
				+ ") and e.encounter_datetime >= :startDate and e.encounter_datetime <= :endDate and p.person_id = e.patient_id and DATEDIFF(:endDate , p.birthdate) < 5475 and e.voided=0 and p.voided=0");
		pediHivVisits.addParameter(new Parameter("startDate", "startDate", Date.class));
		pediHivVisits.addParameter(new Parameter("endDate", "endDate", Date.class));

		EncounterIndicator pediHivVisitsIndicator = new EncounterIndicator();
		pediHivVisitsIndicator.setName("pediHivVisitsIndicator");
		pediHivVisitsIndicator.setEncounterQuery(new Mapped<EncounterQuery>(pediHivVisits,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));

		dsd.addColumn(pediHivVisitsIndicator);

		SqlEncounterQuery pediHivVisitsWithWeight = new SqlEncounterQuery();
		pediHivVisitsWithWeight.setName("pediHivVisitsWithWeight");
		pediHivVisitsWithWeight.setQuery(
				"select e.encounter_id from encounter e,obs o, person p where e.encounter_id=o.encounter_id and o.concept_id="
						+ weightConcept.getConceptId() + " and e.encounter_type in ("
						+ pediHIVEncounterType.getEncounterTypeId() + "," + adultHIVEncounterType.getEncounterTypeId()
						+ ") and e.encounter_datetime >= :startDate and e.encounter_datetime <= :endDate and p.person_id = e.patient_id and DATEDIFF(:endDate , p.birthdate) < 5475 and e.voided=0 and p.voided=0");
		pediHivVisitsWithWeight.addParameter(new Parameter("startDate", "startDate", Date.class));
		pediHivVisitsWithWeight.addParameter(new Parameter("endDate", "endDate", Date.class));

		EncounterIndicator pediHivVisitsWithWeightIndicator = new EncounterIndicator();
		pediHivVisitsWithWeightIndicator.setName("pediHivVisitsWithWeightIndicator");
		pediHivVisitsWithWeightIndicator.setEncounterQuery(new Mapped<EncounterQuery>(pediHivVisitsWithWeight,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));

		dsd.addColumn(pediHivVisitsWithWeightIndicator);

	}

	// create monthly cohort Data set

	private CohortIndicatorDataSetDefinition createQuarterlyBaseDataSet() {
		CohortIndicatorDataSetDefinition dsd = new CohortIndicatorDataSetDefinition();
		dsd.setName("Quarterly Cohort Data Set");
		dsd.addParameter(new Parameter("startDate", "Start Date", Date.class));
		dsd.addParameter(new Parameter("endDate", "End Date", Date.class));

		createQuarterlyIndicators(dsd);
		return dsd;
	}

	private void createQuarterlyIndicators(CohortIndicatorDataSetDefinition dsd) {
		// HIV patients

		InProgramCohortDefinition inAnyHIVProgram = Cohorts
				.createInProgramParameterizableByDate("HIVQ: In All HIV Programs", hivPrograms, "onOrBefore");

		InProgramCohortDefinition inAnyHIVAfterStartDate = Cohorts.createInProgramParameterizableByDate(
				"HIVQ: new Patients enrolled in HIV Program during period", hivPrograms, onOrAfterOnOrBefore);

		InProgramCohortDefinition inAdultOrPediHIVProgram = Cohorts.createInProgramParameterizableByDate(
				"HIVQ: In AdultOrPedi HIV Programs", hivProgramsExcPMTCT, "onOrBefore");

		InProgramCohortDefinition inAdultOrPediHIVOnDateProgram = Cohorts.createInProgramParameterizableByDate(
				"HIVQ: In AdultOrPedi HIV Programs on Date", hivProgramsExcPMTCT, "onDate");

		ProgramEnrollmentCohortDefinition enrolledInPMTCTPregOnOrBeforeEndDate = Cohorts
				.createProgramEnrollment("Enrolled in PMTCT Pregnancy on or before the end date", pmtctProgram);
		enrolledInPMTCTPregOnOrBeforeEndDate
				.addParameter(new Parameter("enrolledOnOrBefore", "enrolledOnOrBefore", Date.class));

		ProgramEnrollmentCohortDefinition enrolledInPMTCTCCMotherOnOrBeforeEndDate = Cohorts.createProgramEnrollment(
				"Enrolled in PMTCT C C Mother on or before the end date", pmtctCombinedMotherProgram);
		enrolledInPMTCTCCMotherOnOrBeforeEndDate
				.addParameter(new Parameter("enrolledOnOrBefore", "enrolledOnOrBefore", Date.class));

		AgeCohortDefinition over15Cohort = Cohorts.createOver15AgeCohort("ageQD: Over 15");
		AgeCohortDefinition under15Cohort = Cohorts.createUnder15AgeCohort("ageQD: Under 15");

		InStateCohortDefinition preArt = Cohorts.createInCurrentState("HIVQ: preArt", preArtWorkflowStates);

		InStateCohortDefinition artOnOrBefore = Cohorts.createInCurrentState("HIVQ: ever on ART", artWorkflowStates,
				onOrAfterOnOrBefore);

		InStateCohortDefinition artCurrently = Cohorts.createInCurrentState("HIVQ: currently on ART",
				artWorkflowStates);

		InStateCohortDefinition artStartedInPeriod = Cohorts.createInCurrentState("HIVQ: started on ART",
				artWorkflowStates, onOrAfterOnOrBefore);

		EncounterCohortDefinition encounter = Cohorts.createEncounterParameterizedByDate("encounterQD: visit in period",
				onOrAfterOnOrBefore, hivEncounterTypes);

		NumericObsCohortDefinition cd4 = Cohorts.createNumericObsCohortDefinition("obsQD: CD4 count recorded",
				onOrAfterOnOrBefore, cd4Concept, 0, null, TimeModifier.ANY);

		NumericObsCohortDefinition cd4Percent = Cohorts.createNumericObsCohortDefinition("obsQD: CD4% count recorded",
				onOrAfterOnOrBefore, cd4PercentConcept, 0, null, TimeModifier.ANY);

		NumericObsCohortDefinition weight = Cohorts.createNumericObsCohortDefinition("obsQD: weight recorded",
				onOrAfterOnOrBefore, weightConcept, 0, null, TimeModifier.ANY);

		CompositionCohortDefinition inHIVUnder15 = new CompositionCohortDefinition();
		inHIVUnder15.setName("HIVQ: In all programs under 15");
		inHIVUnder15.addParameter(new Parameter("endDate", "endDate", Date.class));
		inHIVUnder15.getSearches().put("1",
				new Mapped(under15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		inHIVUnder15.getSearches().put("2",
				new Mapped(inAnyHIVProgram, ParameterizableUtil.createParameterMappings("onOrBefore=${endDate}")));
		inHIVUnder15.setCompositionString("1 AND 2");

		CompositionCohortDefinition inHIVOver15 = new CompositionCohortDefinition();
		inHIVOver15.setName("HIVQ: In all programs over 15");
		inHIVOver15.addParameter(new Parameter("endDate", "endDate", Date.class));
		inHIVOver15.getSearches().put("1",
				new Mapped(over15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		inHIVOver15.getSearches().put("2",
				new Mapped(inAnyHIVProgram, ParameterizableUtil.createParameterMappings("onOrBefore=${endDate}")));
		inHIVOver15.setCompositionString("1 AND 2");

		CompositionCohortDefinition newInHIVUnder15 = new CompositionCohortDefinition();
		newInHIVUnder15.setName("HIVQ: new in any Hiv program in period under 15");
		newInHIVUnder15.addParameter(new Parameter("endDate", "endDate", Date.class));
		newInHIVUnder15.addParameter(new Parameter("startDate", "startDate", Date.class));
		newInHIVUnder15.getSearches().put("1",
				new Mapped(over15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		newInHIVUnder15.getSearches().put("2", new Mapped(inAnyHIVAfterStartDate,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${startDate}")));
		newInHIVUnder15.getSearches().put("3",
				new Mapped(inAnyHIVProgram, ParameterizableUtil.createParameterMappings("onOrBefore=${startDate-1d}")));
		newInHIVUnder15.setCompositionString("NOT 1 AND 2 AND NOT 3");

		CompositionCohortDefinition newInHIVOver15 = new CompositionCohortDefinition();
		newInHIVOver15.setName("HIVQ: new in any Hiv program in period over 15");
		newInHIVOver15.addParameter(new Parameter("endDate", "endDate", Date.class));
		newInHIVOver15.addParameter(new Parameter("startDate", "startDate", Date.class));
		newInHIVOver15.getSearches().put("1",
				new Mapped(over15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		newInHIVOver15.getSearches().put("2", new Mapped(inAnyHIVAfterStartDate,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${startDate}")));
		newInHIVOver15.getSearches().put("3",
				new Mapped(inAnyHIVProgram, ParameterizableUtil.createParameterMappings("onOrBefore=${startDate-1d}")));
		newInHIVOver15.setCompositionString("1 AND 2 AND NOT 3");

		CompositionCohortDefinition followingUnder15 = new CompositionCohortDefinition();
		followingUnder15.setName("HIVQ: preArt in Hiv program in period under 15");
		followingUnder15.addParameter(new Parameter("endDate", "endDate", Date.class));
		followingUnder15.getSearches().put("1",
				new Mapped(under15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		followingUnder15.getSearches().put("2",
				new Mapped(preArt, ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		followingUnder15.getSearches().put("3", new Mapped(inAdultOrPediHIVOnDateProgram,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		followingUnder15.setCompositionString("1 AND 2 AND 3");

		CompositionCohortDefinition followingOver15 = new CompositionCohortDefinition();
		followingOver15.setName("HIVQ: preArt in Hiv program in period over 15");
		followingOver15.addParameter(new Parameter("endDate", "endDate", Date.class));
		followingOver15.getSearches().put("1",
				new Mapped(over15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		followingOver15.getSearches().put("2",
				new Mapped(preArt, ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		followingOver15.getSearches().put("3", new Mapped(inAdultOrPediHIVOnDateProgram,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		followingOver15.setCompositionString("1 AND 2 AND 3");

		CompositionCohortDefinition preArtWithAVisitUnder15 = new CompositionCohortDefinition();
		preArtWithAVisitUnder15.setName("HIVQ: preArt in Hiv program with a visit in period - 3 months and under 15");
		preArtWithAVisitUnder15.addParameter(new Parameter("endDate", "endDate", Date.class));
		preArtWithAVisitUnder15.addParameter(new Parameter("startDate", "startDate", Date.class));
		preArtWithAVisitUnder15.getSearches().put("1",
				new Mapped(over15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		preArtWithAVisitUnder15.getSearches().put("2",
				new Mapped(preArt, ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		preArtWithAVisitUnder15.getSearches().put("3", new Mapped(encounter,
				ParameterizableUtil.createParameterMappings("onOrAfter=${startDate-3m},onOrBefore=${endDate}")));
		preArtWithAVisitUnder15.getSearches().put("4", new Mapped(inAdultOrPediHIVOnDateProgram,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		preArtWithAVisitUnder15.setCompositionString("NOT 1 AND 2 AND 4 AND 3 ");

		CompositionCohortDefinition preArtActiveUnder15 = new CompositionCohortDefinition();
		preArtActiveUnder15
				.setName("HIVQ: Active preArt in Hiv program with a visit in period - 12 months and under 15");
		preArtActiveUnder15.addParameter(new Parameter("endDate", "endDate", Date.class));
		preArtActiveUnder15.addParameter(new Parameter("startDate", "startDate", Date.class));
		preArtActiveUnder15.getSearches().put("1",
				new Mapped(over15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		preArtActiveUnder15.getSearches().put("2",
				new Mapped(preArt, ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		preArtActiveUnder15.getSearches().put("3", new Mapped(encounter,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${endDate-12m}")));
		preArtActiveUnder15.getSearches().put("4", new Mapped(inAdultOrPediHIVOnDateProgram,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		preArtActiveUnder15.getSearches().put("5", new Mapped(weight,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${startDate-3m}")));
		preArtActiveUnder15.setCompositionString("NOT 1 AND 2 AND 4 AND (3 OR 5)");

		CompositionCohortDefinition preArtActiveOver15 = new CompositionCohortDefinition();
		preArtActiveOver15.setName("HIVQ: Active preArt in Hiv program with a visit in period - 12 months and over 15");
		preArtActiveOver15.addParameter(new Parameter("endDate", "endDate", Date.class));
		preArtActiveOver15.addParameter(new Parameter("startDate", "startDate", Date.class));
		preArtActiveOver15.getSearches().put("1",
				new Mapped(over15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		preArtActiveOver15.getSearches().put("2",
				new Mapped(preArt, ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		preArtActiveOver15.getSearches().put("3", new Mapped(encounter,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${endDate-12m}")));
		preArtActiveOver15.getSearches().put("4", new Mapped(inAdultOrPediHIVOnDateProgram,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		preArtActiveOver15.getSearches().put("5", new Mapped(weight,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${startDate-3m}")));
		preArtActiveOver15.setCompositionString("1 AND 2 AND 4 AND (3 OR 5)");

		CompositionCohortDefinition preArtWithAVisitOver15 = new CompositionCohortDefinition();
		preArtWithAVisitOver15.setName("HIVQ: preArt in Hiv program with a visit in period -3 months and over 15");
		preArtWithAVisitOver15.addParameter(new Parameter("endDate", "endDate", Date.class));
		preArtWithAVisitOver15.addParameter(new Parameter("startDate", "startDate", Date.class));
		preArtWithAVisitOver15.getSearches().put("1",
				new Mapped(over15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		preArtWithAVisitOver15.getSearches().put("2",
				new Mapped(preArt, ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		preArtWithAVisitOver15.getSearches().put("3", new Mapped(encounter,
				ParameterizableUtil.createParameterMappings("onOrAfter=${startDate-3m},onOrBefore=${endDate}")));
		preArtWithAVisitOver15.getSearches().put("4", new Mapped(inAdultOrPediHIVOnDateProgram,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		preArtWithAVisitOver15.setCompositionString("1 AND 2 AND 4 AND 3");

		CompositionCohortDefinition artUnder15 = new CompositionCohortDefinition();
		artUnder15.setName("HIVQ: ever taking ART before end date and under 15");
		artUnder15.addParameter(new Parameter("endDate", "endDate", Date.class));
		artUnder15.getSearches().put("1",
				new Mapped(over15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		artUnder15.getSearches().put("2",
				new Mapped(artOnOrBefore, ParameterizableUtil.createParameterMappings("onOrBefore=${endDate}")));
		artUnder15.getSearches().put("3", new Mapped(inAdultOrPediHIVProgram,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate}")));
		artUnder15.setCompositionString("NOT 1 AND 2 AND 3");

		CompositionCohortDefinition artOver15 = new CompositionCohortDefinition();
		artOver15.setName("HIVQ: ever taking ART before end date and over 15");
		artOver15.addParameter(new Parameter("endDate", "endDate", Date.class));
		artOver15.getSearches().put("1",
				new Mapped(over15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		artOver15.getSearches().put("2",
				new Mapped(artOnOrBefore, ParameterizableUtil.createParameterMappings("onOrBefore=${endDate}")));
		artOver15.getSearches().put("3", new Mapped(inAdultOrPediHIVProgram,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate}")));
		artOver15.setCompositionString("1 AND 2 AND 3");

		CompositionCohortDefinition currentArtUnder15 = new CompositionCohortDefinition();
		currentArtUnder15.setName("HIVQ: currently taking ART before end date and under 15");
		currentArtUnder15.addParameter(new Parameter("endDate", "endDate", Date.class));
		currentArtUnder15.getSearches().put("1",
				new Mapped(inHIVUnder15, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		currentArtUnder15.getSearches().put("2",
				new Mapped(artCurrently, ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		currentArtUnder15.setCompositionString("1 AND 2");

		CompositionCohortDefinition currentArtOver15 = new CompositionCohortDefinition();
		currentArtOver15.setName("HIVQ: currently taking ART before end date and over 15");
		currentArtOver15.addParameter(new Parameter("endDate", "endDate", Date.class));
		currentArtOver15.getSearches().put("1",
				new Mapped(inHIVOver15, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		currentArtOver15.getSearches().put("2",
				new Mapped(artCurrently, ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		currentArtOver15.setCompositionString("1 AND 2");

		CompositionCohortDefinition artWithAVisitUnder15 = new CompositionCohortDefinition();
		artWithAVisitUnder15.setName("HIVQ: on ART in Hiv program with a visit in period -3 months and under 15");
		artWithAVisitUnder15.addParameter(new Parameter("endDate", "endDate", Date.class));
		artWithAVisitUnder15.addParameter(new Parameter("startDate", "startDate", Date.class));
		artWithAVisitUnder15.getSearches().put("1",
				new Mapped(under15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		artWithAVisitUnder15.getSearches().put("2",
				new Mapped(artCurrently, ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		artWithAVisitUnder15.getSearches().put("3", new Mapped(encounter,
				ParameterizableUtil.createParameterMappings("onOrAfter=${startDate},onOrBefore=${endDate}")));
		artWithAVisitUnder15.getSearches().put("4", new Mapped(inAdultOrPediHIVOnDateProgram,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		artWithAVisitUnder15.setCompositionString("1 AND 2 AND 4 AND 3 ");

		CompositionCohortDefinition artWithAVisitOver15 = new CompositionCohortDefinition();
		artWithAVisitOver15.setName("HIVQ: preArt in Hiv program with a visit in period -3 months and over 15");
		artWithAVisitOver15.addParameter(new Parameter("endDate", "endDate", Date.class));
		artWithAVisitOver15.addParameter(new Parameter("startDate", "startDate", Date.class));
		artWithAVisitOver15.getSearches().put("1",
				new Mapped(over15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		artWithAVisitOver15.getSearches().put("2",
				new Mapped(artCurrently, ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		artWithAVisitOver15.getSearches().put("3", new Mapped(encounter,
				ParameterizableUtil.createParameterMappings("onOrAfter=${startDate},onOrBefore=${endDate}")));
		artWithAVisitOver15.getSearches().put("4", new Mapped(inAdultOrPediHIVOnDateProgram,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		artWithAVisitOver15.setCompositionString("1 AND 2 AND 4 AND 3");

		CompositionCohortDefinition startedArtOver15 = new CompositionCohortDefinition();
		startedArtOver15.setName("HIVQ: started taking ART in period over 15");
		startedArtOver15.addParameter(new Parameter("endDate", "endDate", Date.class));
		startedArtOver15.addParameter(new Parameter("startDate", "startDate", Date.class));
		startedArtOver15.getSearches().put("1",
				new Mapped(over15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		startedArtOver15.getSearches().put("2", new Mapped(artStartedInPeriod,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${startDate}")));
		startedArtOver15.getSearches().put("3",
				new Mapped(artCurrently, ParameterizableUtil.createParameterMappings("onDate=${startDate-1d}")));
		startedArtOver15.getSearches().put("4", new Mapped(inAdultOrPediHIVProgram,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate}")));
		startedArtOver15.setCompositionString("1 AND 2 AND NOT 3 AND 4");

		CompositionCohortDefinition startedArtUnder15 = new CompositionCohortDefinition();
		startedArtUnder15.setName("HIVQ: started taking ART in period under 15");
		startedArtUnder15.addParameter(new Parameter("endDate", "endDate", Date.class));
		startedArtUnder15.addParameter(new Parameter("startDate", "startDate", Date.class));
		startedArtUnder15.getSearches().put("1",
				new Mapped(over15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		startedArtUnder15.getSearches().put("2", new Mapped(artStartedInPeriod,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${startDate}")));
		startedArtUnder15.getSearches().put("3",
				new Mapped(artCurrently, ParameterizableUtil.createParameterMappings("onDate=${startDate-1d}")));
		startedArtUnder15.getSearches().put("4", new Mapped(inAdultOrPediHIVProgram,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate}")));
		startedArtUnder15.setCompositionString("NOT 1 AND 2 AND NOT 3 AND 4");

		CompositionCohortDefinition preArtWithACD4Under15 = new CompositionCohortDefinition();
		preArtWithACD4Under15.setName("HIVQ: preArt in Hiv program with a CD4 count in period -3 months and under 15");
		preArtWithACD4Under15.addParameter(new Parameter("endDate", "endDate", Date.class));
		preArtWithACD4Under15.addParameter(new Parameter("startDate", "startDate", Date.class));
		preArtWithACD4Under15.getSearches().put("1",
				new Mapped(under15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		preArtWithACD4Under15.getSearches().put("2",
				new Mapped(preArt, ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		preArtWithACD4Under15.getSearches().put("3", new Mapped(cd4,
				ParameterizableUtil.createParameterMappings("onOrAfter=${startDate-3m},onOrBefore=${endDate}")));
		preArtWithACD4Under15.getSearches().put("4", new Mapped(cd4Percent,
				ParameterizableUtil.createParameterMappings("onOrAfter=${startDate-3m},onOrBefore=${endDate}")));
		preArtWithACD4Under15.getSearches().put("5", new Mapped(inAdultOrPediHIVOnDateProgram,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		preArtWithACD4Under15.setCompositionString("1 AND 2 AND (3 OR 4) AND 5");

		CompositionCohortDefinition preArtWithACD4Over15 = new CompositionCohortDefinition();
		preArtWithACD4Over15.setName("HIVQ: preArt in Hiv program with a CD4 count in period -3 months and over 15");
		preArtWithACD4Over15.addParameter(new Parameter("endDate", "endDate", Date.class));
		preArtWithACD4Over15.addParameter(new Parameter("startDate", "startDate", Date.class));
		preArtWithACD4Over15.getSearches().put("1",
				new Mapped(over15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		preArtWithACD4Over15.getSearches().put("2",
				new Mapped(preArt, ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		preArtWithACD4Over15.getSearches().put("3", new Mapped(cd4,
				ParameterizableUtil.createParameterMappings("onOrAfter=${startDate-3m},onOrBefore=${endDate}")));
		preArtWithACD4Over15.getSearches().put("4", new Mapped(inAdultOrPediHIVOnDateProgram,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		preArtWithACD4Over15.setCompositionString("1 AND 2 AND 3 AND 4");

		CompositionCohortDefinition artWithACD4Over15 = new CompositionCohortDefinition();
		artWithACD4Over15.setName("HIVQ: on Art in Hiv program with a CD4 count in period -3 months and over 15");
		artWithACD4Over15.addParameter(new Parameter("endDate", "endDate", Date.class));
		artWithACD4Over15.addParameter(new Parameter("startDate", "startDate", Date.class));
		artWithACD4Over15.getSearches().put("1",
				new Mapped(over15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		artWithACD4Over15.getSearches().put("2",
				new Mapped(artCurrently, ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		artWithACD4Over15.getSearches().put("3", new Mapped(cd4,
				ParameterizableUtil.createParameterMappings("onOrAfter=${startDate-3m},onOrBefore=${endDate}")));
		artWithACD4Over15.getSearches().put("4", new Mapped(inAdultOrPediHIVOnDateProgram,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		artWithACD4Over15.setCompositionString("1 AND 2 AND 3 AND 4");

		CompositionCohortDefinition artWithACD4Under15 = new CompositionCohortDefinition();
		artWithACD4Under15.setName("HIVQ: on Art in Hiv program with a CD4 count in period - 3 months and under 15");
		artWithACD4Under15.addParameter(new Parameter("endDate", "endDate", Date.class));
		artWithACD4Under15.addParameter(new Parameter("startDate", "startDate", Date.class));
		artWithACD4Under15.getSearches().put("1",
				new Mapped(under15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		artWithACD4Under15.getSearches().put("2",
				new Mapped(artCurrently, ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		artWithACD4Under15.getSearches().put("3", new Mapped(cd4,
				ParameterizableUtil.createParameterMappings("onOrAfter=${startDate-3m},onOrBefore=${endDate}")));
		artWithACD4Under15.getSearches().put("4", new Mapped(cd4Percent,
				ParameterizableUtil.createParameterMappings("onOrAfter=${startDate-3m},onOrBefore=${endDate}")));
		artWithACD4Under15.getSearches().put("5", new Mapped(inAdultOrPediHIVOnDateProgram,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		artWithACD4Under15.setCompositionString("1 AND 2 AND (3 OR 4) AND 5");

		CompositionCohortDefinition hivWithAWeightUnder15 = new CompositionCohortDefinition();
		hivWithAWeightUnder15.setName("HIVQ: weight recorded in period under 15");
		hivWithAWeightUnder15.addParameter(new Parameter("endDate", "endDate", Date.class));
		hivWithAWeightUnder15.addParameter(new Parameter("startDate", "startDate", Date.class));
		hivWithAWeightUnder15.getSearches().put("1",
				new Mapped(over15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		hivWithAWeightUnder15.getSearches().put("2", new Mapped(inAdultOrPediHIVProgram,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate}")));
		hivWithAWeightUnder15.getSearches().put("3", new Mapped(weight,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${startDate}")));
		hivWithAWeightUnder15.setCompositionString("NOT 1 AND 2 AND 3");

		CompositionCohortDefinition hivWithAWeightOver15 = new CompositionCohortDefinition();
		hivWithAWeightOver15.setName("HIVQ: weight recorded in period over 15");
		hivWithAWeightOver15.addParameter(new Parameter("endDate", "endDate", Date.class));
		hivWithAWeightOver15.addParameter(new Parameter("startDate", "startDate", Date.class));
		hivWithAWeightOver15.getSearches().put("1",
				new Mapped(over15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		hivWithAWeightOver15.getSearches().put("2", new Mapped(inAdultOrPediHIVProgram,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate}")));
		hivWithAWeightOver15.getSearches().put("3", new Mapped(weight,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${startDate}")));
		hivWithAWeightOver15.setCompositionString("1 AND 2 AND 3");

		CompositionCohortDefinition hivVisitOver15 = new CompositionCohortDefinition();
		hivVisitOver15.setName("HIVQ: in Hiv program with a visit in period and over 15");
		hivVisitOver15.addParameter(new Parameter("endDate", "endDate", Date.class));
		hivVisitOver15.addParameter(new Parameter("startDate", "startDate", Date.class));
		hivVisitOver15.getSearches().put("1",
				new Mapped(over15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		hivVisitOver15.getSearches().put("3", new Mapped(encounter,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${startDate}")));
		hivVisitOver15.getSearches().put("2", new Mapped(inAdultOrPediHIVProgram,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate}")));
		hivVisitOver15.getSearches().put("4", new Mapped(weight,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${startDate}")));
		hivVisitOver15.setCompositionString("1 AND 2 AND (3 OR 4)");

		CompositionCohortDefinition hivVisitUnder15 = new CompositionCohortDefinition();
		hivVisitUnder15.setName("HIVQ: in Hiv program with a visit in period and under 15");
		hivVisitUnder15.addParameter(new Parameter("endDate", "endDate", Date.class));
		hivVisitUnder15.addParameter(new Parameter("startDate", "startDate", Date.class));
		hivVisitUnder15.getSearches().put("1",
				new Mapped(over15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		hivVisitUnder15.getSearches().put("3", new Mapped(encounter,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${startDate}")));
		hivVisitUnder15.getSearches().put("2", new Mapped(inAdultOrPediHIVProgram,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate}")));
		hivVisitUnder15.getSearches().put("4", new Mapped(weight,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${startDate}")));
		hivVisitUnder15.setCompositionString("NOT 1 AND 2 AND (3 OR 4)");

		// Question number 12
		CompositionCohortDefinition activePreArtWithVisitOver15 = new CompositionCohortDefinition();
		activePreArtWithVisitOver15
				.setName("HIVQ: acitve preArt in Hiv program with a visit in period -12 months and over 15");
		activePreArtWithVisitOver15.addParameter(new Parameter("endDate", "endDate", Date.class));
		activePreArtWithVisitOver15.getSearches().put("1", new Mapped(encounter,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${endDate-1y}")));
		activePreArtWithVisitOver15.getSearches().put("2",
				new Mapped(followingOver15, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		activePreArtWithVisitOver15.setCompositionString("1 AND 2");

		CompositionCohortDefinition activePreArtWithVisitUnder15 = new CompositionCohortDefinition();
		activePreArtWithVisitUnder15
				.setName("HIVQ: acitve preArt in Hiv program with a visit in period -12 months and under 15");
		activePreArtWithVisitUnder15.addParameter(new Parameter("endDate", "endDate", Date.class));
		activePreArtWithVisitUnder15.getSearches().put("1", new Mapped(encounter,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${endDate-1y}")));
		activePreArtWithVisitUnder15.getSearches().put("2",
				new Mapped(followingUnder15, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		activePreArtWithVisitUnder15.setCompositionString("1 AND 2");

		CompositionCohortDefinition activePreArtWithVisitOrWeightOver15 = new CompositionCohortDefinition();
		activePreArtWithVisitOrWeightOver15
				.setName("HIVQ: acitve preArt in Hiv program with a visit or weight in period -3 months and over 15");
		activePreArtWithVisitOrWeightOver15.addParameter(new Parameter("endDate", "endDate", Date.class));
		activePreArtWithVisitOrWeightOver15.addParameter(new Parameter("startDate", "startDate", Date.class));
		activePreArtWithVisitOrWeightOver15.getSearches().put("1", new Mapped(activePreArtWithVisitOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		activePreArtWithVisitOrWeightOver15.getSearches().put("2", new Mapped(preArtWithAVisitOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
		activePreArtWithVisitOrWeightOver15.setCompositionString("1 AND 2");

		CompositionCohortDefinition activePreArtWithVisitOrWeightUnder15 = new CompositionCohortDefinition();
		activePreArtWithVisitOrWeightUnder15
				.setName("HIVQ: acitve preArt in Hiv program with a visit or weight in period -3 months and under 15");
		activePreArtWithVisitOrWeightUnder15.addParameter(new Parameter("endDate", "endDate", Date.class));
		activePreArtWithVisitOrWeightUnder15.addParameter(new Parameter("startDate", "startDate", Date.class));
		activePreArtWithVisitOrWeightUnder15.getSearches().put("1", new Mapped(activePreArtWithVisitUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		activePreArtWithVisitOrWeightUnder15.getSearches().put("2", new Mapped(preArtWithAVisitUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
		activePreArtWithVisitOrWeightUnder15.setCompositionString("1 AND 2");

		// Question number 13
		CompositionCohortDefinition activeArtWithVisitOver15 = new CompositionCohortDefinition();
		activeArtWithVisitOver15
				.setName("HIVQ: acitve art in Hiv program with a visit in period -12 months and over 15");
		activeArtWithVisitOver15.addParameter(new Parameter("endDate", "endDate", Date.class));
		activeArtWithVisitOver15.getSearches().put("1", new Mapped(encounter,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${endDate-1y}")));
		activeArtWithVisitOver15.getSearches().put("2",
				new Mapped(currentArtOver15, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		activeArtWithVisitOver15.setCompositionString("1 AND 2");

		CompositionCohortDefinition activeArtWithVisitUnder15 = new CompositionCohortDefinition();
		activeArtWithVisitUnder15
				.setName("HIVQ: acitve art in Hiv program with a visit in period -12 months and under 15");
		activeArtWithVisitUnder15.addParameter(new Parameter("endDate", "endDate", Date.class));
		activeArtWithVisitUnder15.getSearches().put("1", new Mapped(encounter,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${endDate-1y}")));
		activeArtWithVisitUnder15.getSearches().put("2",
				new Mapped(currentArtUnder15, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		activeArtWithVisitUnder15.setCompositionString("1 AND 2");

		CompositionCohortDefinition activeArtWithVisitOrWeightOver15 = new CompositionCohortDefinition();
		activeArtWithVisitOrWeightOver15
				.setName("HIVQ: acitve art in Hiv program with a visit or weight in period -3 months and over 15");
		activeArtWithVisitOrWeightOver15.addParameter(new Parameter("endDate", "endDate", Date.class));
		activeArtWithVisitOrWeightOver15.addParameter(new Parameter("startDate", "startDate", Date.class));
		activeArtWithVisitOrWeightOver15.getSearches().put("1", new Mapped(activeArtWithVisitOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		activeArtWithVisitOrWeightOver15.getSearches().put("2", new Mapped(artWithAVisitOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
		activeArtWithVisitOrWeightOver15.setCompositionString("1 AND 2");

		CompositionCohortDefinition activeArtWithVisitOrWeightUnder15 = new CompositionCohortDefinition();
		activeArtWithVisitOrWeightUnder15
				.setName("HIVQ: acitve art in Hiv program with a visit or weight in period -3 months and under 15");
		activeArtWithVisitOrWeightUnder15.addParameter(new Parameter("endDate", "endDate", Date.class));
		activeArtWithVisitOrWeightUnder15.addParameter(new Parameter("startDate", "startDate", Date.class));
		activeArtWithVisitOrWeightUnder15.getSearches().put("1", new Mapped(activeArtWithVisitUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		activeArtWithVisitOrWeightUnder15.getSearches().put("2", new Mapped(artWithAVisitUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
		activeArtWithVisitOrWeightUnder15.setCompositionString("1 AND 2");

		// Question 14
		CompositionCohortDefinition activePreArtWithVisitAndCD4Over15 = new CompositionCohortDefinition();
		activePreArtWithVisitAndCD4Over15
				.setName("HIVQ: active PreArt in Hiv program with a visit and CD4 in period -3 months and over 15");
		activePreArtWithVisitAndCD4Over15.addParameter(new Parameter("endDate", "endDate", Date.class));
		activePreArtWithVisitAndCD4Over15.addParameter(new Parameter("startDate", "startDate", Date.class));
		activePreArtWithVisitAndCD4Over15.getSearches().put("1", new Mapped(activePreArtWithVisitOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		activePreArtWithVisitAndCD4Over15.getSearches().put("2", new Mapped(preArtWithACD4Over15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
		activePreArtWithVisitAndCD4Over15.setCompositionString("1 AND 2");

		CompositionCohortDefinition activePreArtWithVisitAndCD4Under15 = new CompositionCohortDefinition();
		activePreArtWithVisitAndCD4Under15
				.setName("HIVQ: active PreArt in Hiv program with a visit and CD4 in period -3 months and under 15");
		activePreArtWithVisitAndCD4Under15.addParameter(new Parameter("endDate", "endDate", Date.class));
		activePreArtWithVisitAndCD4Under15.addParameter(new Parameter("startDate", "startDate", Date.class));
		activePreArtWithVisitAndCD4Under15.getSearches().put("1", new Mapped(activePreArtWithVisitUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		activePreArtWithVisitAndCD4Under15.getSearches().put("2", new Mapped(preArtWithACD4Under15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
		activePreArtWithVisitAndCD4Under15.setCompositionString("1 AND 2");

		// Question15
		CompositionCohortDefinition activeArtWithVisitAndCD4Over15 = new CompositionCohortDefinition();
		activeArtWithVisitAndCD4Over15
				.setName("HIVQ: active art in Hiv program with a visit and CD4 in period -3 months and over 15");
		activeArtWithVisitAndCD4Over15.addParameter(new Parameter("endDate", "endDate", Date.class));
		activeArtWithVisitAndCD4Over15.addParameter(new Parameter("startDate", "startDate", Date.class));
		activeArtWithVisitAndCD4Over15.getSearches().put("1", new Mapped(activeArtWithVisitOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		activeArtWithVisitAndCD4Over15.getSearches().put("2", new Mapped(artWithACD4Over15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
		activeArtWithVisitAndCD4Over15.setCompositionString("1 AND 2");

		CompositionCohortDefinition activeArtWithVisitAndCD4Underr15 = new CompositionCohortDefinition();
		activeArtWithVisitAndCD4Underr15
				.setName("HIVQ: active art in Hiv program with a visit and CD4 in period -3 months and under 15");
		activeArtWithVisitAndCD4Underr15.addParameter(new Parameter("endDate", "endDate", Date.class));
		activeArtWithVisitAndCD4Underr15.addParameter(new Parameter("startDate", "startDate", Date.class));
		activeArtWithVisitAndCD4Underr15.getSearches().put("1", new Mapped(activeArtWithVisitUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		activeArtWithVisitAndCD4Underr15.getSearches().put("2", new Mapped(artWithACD4Under15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
		activeArtWithVisitAndCD4Underr15.setCompositionString("1 AND 2");

		CohortIndicator oneHIV = Indicators.newCountIndicator("HIVQ: In all programs over 15_", inHIVOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		CohortIndicator twoHIV = Indicators.newCountIndicator("HIVQ: In all programs under 15_", inHIVUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		CohortIndicator threeHIV = Indicators.newCountIndicator("HIVQ: new in any Hiv program in period over 15_",
				newInHIVOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator fourHIV = Indicators.newCountIndicator("HIVQ: new in any Hiv program in period under 15_",
				newInHIVUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator fiveHIV = Indicators.newCountIndicator("HIVQ: preArt in Hiv program in period over 15_",
				followingOver15, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		CohortIndicator sixHIV = Indicators.newCountIndicator("HIVQ: preArt in Hiv program in period under 15_",
				followingUnder15, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		CohortIndicator sevenHIV = Indicators.newCountIndicator(
				"HIVQ: preArt in Hiv program with a visit in period -3 months and over 15_", preArtWithAVisitOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator eightHIV = Indicators.newCountIndicator(
				"HIVQ: preArt in Hiv program with a visit in period -3 months and under 15_", preArtWithAVisitUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator nineHIV = Indicators.newCountIndicator("HIVQ: ever taking ART before end date and over 15_",
				artOver15, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		CohortIndicator tenHIV = Indicators.newCountIndicator("HIVQ: ever taking ART before end date and under 15_",
				artUnder15, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		CohortIndicator elevenHIV = Indicators.newCountIndicator(
				"HIVQ: currently taking ART before end date and over 15_", currentArtOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		CohortIndicator twelveHIV = Indicators.newCountIndicator(
				"HIVQ: currently taking ART before end date and under 15_", currentArtUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		CohortIndicator thirteenHIV = Indicators.newCountIndicator(
				"HIVQ: on ART in Hiv program with a visit in period last 6 months (last two quarters) and over 15_",
				artWithAVisitOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${endDate-6m}"));
		CohortIndicator fourteenHIV = Indicators.newCountIndicator(
				"HIVQ: on ART in Hiv program with a visit in period last 6 months (last two quarters) and under 15_",
				artWithAVisitUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${endDate-6m}"));
		CohortIndicator fifteenHIV = Indicators.newCountIndicator("HIVQ: started taking ART in period over 15_",
				startedArtOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator sixteenHIV = Indicators.newCountIndicator("HIVQ: started taking ART in period under 15_",
				startedArtUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator seventeenHIV = Indicators.newCountIndicator(
				"HIVQ: preArt in Hiv program with a CD4 count in period -3 months and over 15_", preArtWithACD4Over15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator eighteenHIV = Indicators.newCountIndicator(
				"HIVQ: preArt in Hiv program with a CD4 count in period -3 months and under 15_", preArtWithACD4Under15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));

		CohortIndicator seventeenHIVb = Indicators.newCountIndicator(
				"HIVQ: preArt in Hiv program with a CD4 count in period -6 months and over 15_", preArtWithACD4Over15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate-3m}"));
		CohortIndicator eighteenHIVb = Indicators.newCountIndicator(
				"HIVQ: preArt in Hiv program with a CD4 count in period -6 months and under 15_", preArtWithACD4Under15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate-3m}"));

		CohortIndicator nineteenHIV = Indicators.newCountIndicator(
				"HIVQ: on Art in Hiv program with a CD4 count in period -9 months and over 15_", artWithACD4Over15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate-6m}"));
		CohortIndicator twentyHIV = Indicators.newCountIndicator(
				"HIVQ: on Art in Hiv program with a CD4 count in period -9 months and under 15_", artWithACD4Under15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate-6m}"));
		CohortIndicator twentyoneHIV = Indicators.newCountIndicator("HIVQ: weight recorded in period over 15_",
				hivWithAWeightOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator twentytwoHIV = Indicators.newCountIndicator("HIVQ: weight recorded in period under 15_",
				hivWithAWeightUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator twentythreeHIV = Indicators.newCountIndicator("HIVQ: HIV visit and over 15_", hivVisitOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator twentyfourHIV = Indicators.newCountIndicator("HIVQ: HIV visit and under 15_", hivVisitUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator twentyfiveHIV = Indicators.newCountIndicator(
				"HIVQ: active preArt in HIV with a visit in period -12 months and over 15_",
				activePreArtWithVisitOver15, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		CohortIndicator twentysixHIV = Indicators.newCountIndicator(
				"HIVQ: active preArt in HIV with a visit in period -12 months and under 15_",
				activePreArtWithVisitUnder15, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		CohortIndicator twentysevenHIV = Indicators.newCountIndicator(
				"HIVQ: active preArt in HIV with a visit in period -3 months over 15_",
				activePreArtWithVisitOrWeightOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator twentyeightHIV = Indicators.newCountIndicator(
				"HIVQ: active preArt in HIV with a visit in period -3 months under 15_",
				activePreArtWithVisitOrWeightUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator twentnineHIV = Indicators.newCountIndicator(
				"HIVQ: active art in HIV with a visit in period -12 months over 15_", activeArtWithVisitOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		CohortIndicator fourtyHIV = Indicators.newCountIndicator(
				"HIVQ: active art in HIV with a visit in period -12 months under 15_", activeArtWithVisitUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		CohortIndicator fourtyoneHIV = Indicators.newCountIndicator(
				"HIVQ: active art in HIV with a visit in period -3 months over 15_", activeArtWithVisitOrWeightOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate-3m}"));
		CohortIndicator fourtytwoHIV = Indicators.newCountIndicator(
				"HIVQ: active art in HIV with a visit in period -3 months under 15_", activeArtWithVisitOrWeightUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate-3m}"));
		CohortIndicator fourtythreeHIV = Indicators.newCountIndicator(
				"HIVQ: active PreArt in HIV with a CD4 in period -3 months over 15_", activePreArtWithVisitAndCD4Over15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator fourtyfourHIV = Indicators.newCountIndicator(
				"HIVQ: active PreArt in HIV with a CD4 in period -3 months under 15_",
				activePreArtWithVisitAndCD4Under15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));

		CohortIndicator fourtythreeHIVb = Indicators.newCountIndicator(
				"HIVQ: active PreArt in HIV with a CD4 in period -6 months over 15_", activePreArtWithVisitAndCD4Over15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate-3m}"));
		CohortIndicator fourtyfourHIVb = Indicators.newCountIndicator(
				"HIVQ: active PreArt in HIV with a CD4 in period -6 months under 15_",
				activePreArtWithVisitAndCD4Under15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate-3m}"));

		CohortIndicator fourtyfiveHIV = Indicators.newCountIndicator(
				"HIVQ: active art in HIV with a CD4 in period -3 months over 15_", activeArtWithVisitAndCD4Over15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate-6m}"));
		CohortIndicator fourtysixHIV = Indicators.newCountIndicator(
				"HIVQ: active art in HIV with a CD4 in period -3 months over 15_", activeArtWithVisitAndCD4Underr15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate-6m}"));

		dsd.addColumn("HIVQ1A", "In All HIV Programs Over 15", new Mapped(oneHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ1C", "In All HIV Programs Under 15",
				new Mapped(twoHIV, ParameterizableUtil.createParameterMappings("endDate=${endDate}")), "");
		dsd.addColumn("HIVQ2A", "New In All HIV Programs Over 15", new Mapped(threeHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ2C", "New In All HIV Programs Under 15", new Mapped(fourHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ3A", "Pre ART Over 15",
				new Mapped(fiveHIV, ParameterizableUtil.createParameterMappings("endDate=${endDate}")), "");
		dsd.addColumn("HIVQ3C", "Pre ART Under 15",
				new Mapped(sixHIV, ParameterizableUtil.createParameterMappings("endDate=${endDate}")), "");
		dsd.addColumn("HIVQ4A", "Pre ART Visit in last 2 quarters Over 15", new Mapped(sevenHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ4C", "Pre ART Visit in last 2 quarters Under 15", new Mapped(eightHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ5A", "Ever started on Art at end of review and Over 15",
				new Mapped(nineHIV, ParameterizableUtil.createParameterMappings("endDate=${endDate}")), "");
		dsd.addColumn("HIVQ5C", "Ever started on Art at end of review and Under 15",
				new Mapped(tenHIV, ParameterizableUtil.createParameterMappings("endDate=${endDate}")), "");
		dsd.addColumn("HIVQ6A", "Currently on Art at end of review and Over 15",
				new Mapped(elevenHIV, ParameterizableUtil.createParameterMappings("endDate=${endDate}")), "");
		dsd.addColumn("HIVQ6C", "Currently on Art at end of review and Under 15",
				new Mapped(twelveHIV, ParameterizableUtil.createParameterMappings("endDate=${endDate}")), "");
		dsd.addColumn("HIVQ7A", "Currently on Art and had a visit in last 2 quarters and Over 15",
				new Mapped(thirteenHIV,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");
		dsd.addColumn("HIVQ7C", "Currently on Art and had a visit in last 2 quarters and Under 15",
				new Mapped(fourteenHIV,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");
		dsd.addColumn("HIVQ8A", "Started on Art and Over 15", new Mapped(fifteenHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ8C", "Started on Art and Under 15", new Mapped(sixteenHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ9A", "Pre ART and had a cd4 recorded in last 2 quarters and Over 15",
				new Mapped(seventeenHIV,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");
		dsd.addColumn("HIVQ9C", "Pre ART and had a cd4 recorded in last 2 quarters and Under 15",
				new Mapped(eighteenHIV,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");

		dsd.addColumn("HIVQ9Ab", "Pre ART and had a cd4 recorded in last 3 quarters and Over 15",
				new Mapped(seventeenHIVb,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");
		dsd.addColumn("HIVQ9Cb", "Pre ART and had a cd4 recorded in last 3 quarters and Under 15",
				new Mapped(eighteenHIVb,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");

		dsd.addColumn("HIVQ10A", "ART and had a cd4 recorded in last 2 quarters and Over 15", new Mapped(nineteenHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ10C", "ART and had a cd4 recorded in last 2 quarters and Under 15", new Mapped(twentyHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ11A", "HIV and had a weight recorded in last quarters and Over 15", new Mapped(twentyoneHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ11C", "HIV and had a weight recorded in last quarters and Under 15", new Mapped(twentytwoHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ11AD", "HIV visit and Over 15", new Mapped(twentythreeHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ11CD", "HIV and Under 15", new Mapped(twentyfourHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ12AD", "HIV preArt visit and Over 15 in last year", new Mapped(twentyfiveHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ12CD", "HIV preArt visit and Under 15 in last year", new Mapped(twentysixHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ12A", "HIV preArt visit or weight Over 15 in last quarter", new Mapped(twentysevenHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ12C", "HIV preArt visit or weight Under 15 in last quarter", new Mapped(twentyeightHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ13AD", "HIV art visit Over 15 in last year", new Mapped(twentnineHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ13CD", "HIV art visit Under 15 in last year", new Mapped(fourtyHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ13A", "HIV art visit Over 15 in last 3 months", new Mapped(fourtyoneHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ13C", "HIV art visit Under 15 in last 3 months", new Mapped(fourtytwoHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ14A", "HIV PreArt CD4 Over 15 in last 3 months", new Mapped(fourtythreeHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ14C", "HIV PreArt CD4 Under 15 in last 3 months", new Mapped(fourtyfourHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");

		dsd.addColumn("HIVQ14Ab", "HIV PreArt CD4 Over 15 in last 3 months", new Mapped(fourtythreeHIVb,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ14Cb", "HIV PreArt CD4 Under 15 in last 3 months", new Mapped(fourtyfourHIVb,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");

		dsd.addColumn("HIVQ15A", "HIV preArt CD4 Over 15 in last 3 months", new Mapped(fourtyfiveHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("HIVQ15C", "HIV preArt CD4 Under 15 in last 3 months", new Mapped(fourtysixHIV,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");

		// ===================================================================
		// PMTCT Pregnant Mother Program
		// ===================================================================

		ProgramEnrollmentCohortDefinition enrolledInPMTCTPregByStartEndDate = Cohorts
				.createProgramEnrollmentParameterizedByStartEndDate("Enrolled in PMTCT Pregnancy", pmtctProgram);

		InProgramCohortDefinition inPMTCTOnEndDate = Cohorts
				.createInProgramParameterizableByDate("In PMTCT Program on End Date", pmtctProgram);

		SqlCohortDefinition activePatientOnArt = Cohorts
				.getPatientActiveOnArtDrugsByEndDate("Active on ART drugs by End date");

		CompositionCohortDefinition activePatientOnArtInPMTCTPreg = new CompositionCohortDefinition();
		activePatientOnArtInPMTCTPreg.setName("active Patient On Art In PMTCT Pregnancy");
		activePatientOnArtInPMTCTPreg.addParameter(new Parameter("endDate", "endDate", Date.class));
		activePatientOnArtInPMTCTPreg.addParameter(new Parameter("onDate", "onDate", Date.class));
		activePatientOnArtInPMTCTPreg.getSearches().put("1",
				new Mapped(activePatientOnArt, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		activePatientOnArtInPMTCTPreg.getSearches().put("2",
				new Mapped(inPMTCTOnEndDate, ParameterizableUtil.createParameterMappings("onDate=${onDate}")));
		activePatientOnArtInPMTCTPreg.setCompositionString(" 1 AND 2");

		SqlCohortDefinition patientsWithTwoOrMoreAdultHIVEncounters = Cohorts
				.getPatientsWithNTimesOrMoreEncountersByStartAndEndDate("patients With Two Or More Adult HIV flowsheet",
						adultHIVEncounterType, 2);

		SqlCohortDefinition patientsWithOneOrMoreAdultHIVEncounters = Cohorts
				.getPatientsWithNTimesOrMoreEncountersByStartAndEndDate("patients With One Or More Adult HIV flowsheet",
						adultHIVEncounterType, 1);

		CompositionCohortDefinition patientsWithOneOrMoreAdultHIVEncountersInPMTCTPreg = new CompositionCohortDefinition();
		patientsWithOneOrMoreAdultHIVEncountersInPMTCTPreg
				.setName("patients With One Or More Adult HIV flowsheet In PMTCT Pregnancy");
		patientsWithOneOrMoreAdultHIVEncountersInPMTCTPreg
				.addParameter(new Parameter("startDate", "startDate", Date.class));
		patientsWithOneOrMoreAdultHIVEncountersInPMTCTPreg
				.addParameter(new Parameter("endDate", "endDate", Date.class));
		patientsWithOneOrMoreAdultHIVEncountersInPMTCTPreg
				.addParameter(new Parameter("enrolledOnOrAfter", "enrolledOnOrAfter", Date.class));
		patientsWithOneOrMoreAdultHIVEncountersInPMTCTPreg
				.addParameter(new Parameter("enrolledOnOrBefore", "enrolledOnOrBefore", Date.class));
		patientsWithOneOrMoreAdultHIVEncountersInPMTCTPreg.addParameter(new Parameter("onDate", "onDate", Date.class));
		patientsWithOneOrMoreAdultHIVEncountersInPMTCTPreg.getSearches().put("1",
				new Mapped(patientsWithOneOrMoreAdultHIVEncounters,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
		patientsWithOneOrMoreAdultHIVEncountersInPMTCTPreg.getSearches().put("2",
				new Mapped(inPMTCTOnEndDate, ParameterizableUtil.createParameterMappings("onDate=${onDate}")));
		/*
		 * patientsWithOneOrMoreAdultHIVEncountersInPMTCTPreg.getSearches().put(
		 * "3",new Mapped(enrolledInPMTCTPregByStartEndDate,ParameterizableUtil.
		 * createParameterMappings(
		 * "enrolledOnOrAfter=${enrolledOnOrAfter},enrolledOnOrBefore=${enrolledOnOrBefore}"
		 * ))); patientsWithOneOrMoreAdultHIVEncountersInPMTCTPreg.
		 * setCompositionString(" 1 AND 2 AND (NOT 3)");
		 */
		patientsWithOneOrMoreAdultHIVEncountersInPMTCTPreg.setCompositionString("1 AND 2");

		CompositionCohortDefinition patientsInPMTCTPregNotEnrolledByStartAndEndDate = new CompositionCohortDefinition();
		patientsInPMTCTPregNotEnrolledByStartAndEndDate
				.setName("patients In PMTCT Pregnancy but not enrolled between startdate and enddate");
		patientsInPMTCTPregNotEnrolledByStartAndEndDate
				.addParameter(new Parameter("enrolledOnOrAfter", "enrolledOnOrAfter", Date.class));
		patientsInPMTCTPregNotEnrolledByStartAndEndDate
				.addParameter(new Parameter("enrolledOnOrBefore", "enrolledOnOrBefore", Date.class));
		patientsInPMTCTPregNotEnrolledByStartAndEndDate.addParameter(new Parameter("onDate", "onDate", Date.class));
		patientsInPMTCTPregNotEnrolledByStartAndEndDate.getSearches().put("2",
				new Mapped(inPMTCTOnEndDate, ParameterizableUtil.createParameterMappings("onDate=${onDate}")));
		patientsInPMTCTPregNotEnrolledByStartAndEndDate.getSearches().put("3",
				new Mapped(enrolledInPMTCTPregByStartEndDate, ParameterizableUtil.createParameterMappings(
						"enrolledOnOrAfter=${enrolledOnOrAfter},enrolledOnOrBefore=${enrolledOnOrBefore}")));
		patientsInPMTCTPregNotEnrolledByStartAndEndDate.setCompositionString("2 AND (NOT 3)");

		CohortIndicator onePMTCTPreg = Indicators.newCountIndicator("PMTCTPregQ: Enrolled in PMTCT Pregnancy",
				enrolledInPMTCTPregByStartEndDate, ParameterizableUtil
						.createParameterMappings("enrolledOnOrAfter=${startDate},enrolledOnOrBefore=${endDate}"));

		CohortIndicator twoPMTCTPreg = Indicators.newCountIndicator(
				"PMTCTPregQ: Enrolled in PMTCT Pregnancy on the end date", inPMTCTOnEndDate,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));

		CohortIndicator threePMTCTPreg = Indicators.newCountIndicator(
				"PMTCTPregQ: Enrolled in PMTCT Pregnancy on or before the end date",
				enrolledInPMTCTPregOnOrBeforeEndDate,
				ParameterizableUtil.createParameterMappings("enrolledOnOrBefore=${endDate}"));

		CohortIndicator fourPMTCTPreg = Indicators.newCountIndicator(
				"PMTCTPregQ: Enrolled in PMTCT Pregnancy on the end date", activePatientOnArtInPMTCTPreg,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));

		CohortIndicator fivePMTCTPreg = Indicators.newCountIndicator(
				"PMTCTPregQ: patients With One Or More Adult HIV flowsheet In PMTCT Pregnancy",
				patientsWithOneOrMoreAdultHIVEncountersInPMTCTPreg, ParameterizableUtil.createParameterMappings(
						"endDate=${endDate},startDate=${startDate},enrolledOnOrAfter=${startDate},enrolledOnOrBefore=${endDate},onDate=${endDate}"));

		CohortIndicator fivePMTCTPregD = Indicators.newCountIndicator(
				"PMTCTPregQ: patients In PMTCT Pregnancy but not enrolled between startdate and enddate",
				patientsInPMTCTPregNotEnrolledByStartAndEndDate, ParameterizableUtil.createParameterMappings(
						"enrolledOnOrAfter=${startDate},enrolledOnOrBefore=${endDate},onDate=${endDate}"));

		dsd.addColumn("PregQ1", "Enrolled in PMTCT Pregnancy", new Mapped(onePMTCTPreg,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("PregQ2", "Enrolled in PMTCT Pregnancy on the end date",
				new Mapped(twoPMTCTPreg, ParameterizableUtil.createParameterMappings("endDate=${endDate}")), "");
		dsd.addColumn("PregQ3", "Enrolled in PMTCT Pregnancy on or before the end date",
				new Mapped(threePMTCTPreg, ParameterizableUtil.createParameterMappings("endDate=${endDate}")), "");
		dsd.addColumn("PregQ4", "Enrolled in PMTCT Pregnancy on the end date",
				new Mapped(fourPMTCTPreg, ParameterizableUtil.createParameterMappings("endDate=${endDate}")), "");
		dsd.addColumn("PregQ5", "patients With One Or More Adult HIV flowsheet In PMTCT Pregnancy",
				new Mapped(fivePMTCTPreg,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");

		dsd.addColumn("PregQ5D", "patients In PMTCT Pregnancy but not enrolled between startdate and enddate",
				new Mapped(fivePMTCTPregD,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");

		// =============================================================================================
		// HIV Exposed Infants
		// =============================================================================================

		ProgramEnrollmentCohortDefinition enrolledInPMTCTInfantByStartEndDate = Cohorts
				.createProgramEnrollmentParameterizedByStartEndDate("Enrolled in PMTCT Infant",
						pmtctCombinedInfantProgram);

		InProgramCohortDefinition inPMTCTInfantOnEndDate = Cohorts
				.createInProgramParameterizableByDate("In PMTCT Program on End Date", pmtctCombinedInfantProgram);

		InStateCohortDefinition inFeedingStatesOnDate = Cohorts.createInCurrentState("In feeding status on end date",
				feedingWorkflowStates, "onDate");

		CompositionCohortDefinition inFeedingStatesOnDateAndinPMTCTInfantOnEndDate = new CompositionCohortDefinition();
		inFeedingStatesOnDateAndinPMTCTInfantOnEndDate
				.setName("patient in Feeding States On Date And in PMTCT Infant OnEndDate");
		inFeedingStatesOnDateAndinPMTCTInfantOnEndDate.addParameter(new Parameter("onDate", "onDate", Date.class));
		inFeedingStatesOnDateAndinPMTCTInfantOnEndDate.getSearches().put("2",
				new Mapped(inFeedingStatesOnDate, ParameterizableUtil.createParameterMappings("onDate=${onDate}")));
		inFeedingStatesOnDateAndinPMTCTInfantOnEndDate.getSearches().put("3",
				new Mapped(inPMTCTInfantOnEndDate, ParameterizableUtil.createParameterMappings("onDate=${onDate}")));
		inFeedingStatesOnDateAndinPMTCTInfantOnEndDate.setCompositionString("2 AND 3");

		ProgramEnrollmentCohortDefinition enrolledInPMTCTInfantOnOrBeforeEndDate = Cohorts.createProgramEnrollment(
				"Enrolled in PMTCT Infant on or before the end date", pmtctCombinedInfantProgram);
		enrolledInPMTCTInfantOnOrBeforeEndDate
				.addParameter(new Parameter("enrolledOnOrBefore", "enrolledOnOrBefore", Date.class));

		SqlCohortDefinition patientsWithTwoOrMoreExposedInfantEncounters = Cohorts
				.getPatientsWithNTimesOrMoreEncountersByStartAndEndDate(
						"patients With Two Or More Exposed Infant flowsheet", exposedInfantEncountertype, 2);

		SqlCohortDefinition patientsWithOneOrMoreExposedInfantEncounters = Cohorts
				.getPatientsWithNTimesOrMoreEncountersByStartAndEndDate(
						"patients With Two Or More Exposed Infant flowsheet", exposedInfantEncountertype, 1);

		CompositionCohortDefinition patientsWithOneOrMoreExposedInfantEncountersInPMTCTInfant = new CompositionCohortDefinition();
		patientsWithOneOrMoreExposedInfantEncountersInPMTCTInfant
				.setName("patients With One Or More Exposed Infant flowsheet In PMTCT Infant");
		patientsWithOneOrMoreExposedInfantEncountersInPMTCTInfant
				.addParameter(new Parameter("startDate", "startDate", Date.class));
		patientsWithOneOrMoreExposedInfantEncountersInPMTCTInfant
				.addParameter(new Parameter("endDate", "endDate", Date.class));
		patientsWithOneOrMoreExposedInfantEncountersInPMTCTInfant
				.addParameter(new Parameter("enrolledOnOrAfter", "enrolledOnOrAfter", Date.class));
		patientsWithOneOrMoreExposedInfantEncountersInPMTCTInfant
				.addParameter(new Parameter("enrolledOnOrBefore", "enrolledOnOrBefore", Date.class));
		patientsWithOneOrMoreExposedInfantEncountersInPMTCTInfant
				.addParameter(new Parameter("onDate", "onDate", Date.class));
		patientsWithOneOrMoreExposedInfantEncountersInPMTCTInfant.getSearches().put("1",
				new Mapped(patientsWithOneOrMoreExposedInfantEncounters,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
		patientsWithOneOrMoreExposedInfantEncountersInPMTCTInfant.getSearches().put("2",
				new Mapped(inPMTCTInfantOnEndDate, ParameterizableUtil.createParameterMappings("onDate=${onDate}")));
		/*
		 * patientsWithOneOrMoreExposedInfantEncountersInPMTCTInfant.getSearches
		 * ().put("3",new
		 * Mapped(enrolledInPMTCTInfantByStartEndDate,ParameterizableUtil.
		 * createParameterMappings(
		 * "enrolledOnOrAfter=${enrolledOnOrAfter},enrolledOnOrBefore=${enrolledOnOrBefore}"
		 * ))); patientsWithOneOrMoreExposedInfantEncountersInPMTCTInfant.
		 * setCompositionString(" 1 AND 2 AND (NOT 3)");
		 */
		patientsWithOneOrMoreExposedInfantEncountersInPMTCTInfant.setCompositionString("1 AND 2");

		CompositionCohortDefinition patientsInPMTCTInfantNotEnrolledByStartAndEndDate = new CompositionCohortDefinition();
		patientsInPMTCTInfantNotEnrolledByStartAndEndDate
				.setName("patients In PMTCT infant but not enrolled between startdate and enddate");
		patientsInPMTCTInfantNotEnrolledByStartAndEndDate
				.addParameter(new Parameter("enrolledOnOrAfter", "enrolledOnOrAfter", Date.class));
		patientsInPMTCTInfantNotEnrolledByStartAndEndDate
				.addParameter(new Parameter("enrolledOnOrBefore", "enrolledOnOrBefore", Date.class));
		patientsInPMTCTInfantNotEnrolledByStartAndEndDate.addParameter(new Parameter("onDate", "onDate", Date.class));
		patientsInPMTCTInfantNotEnrolledByStartAndEndDate.getSearches().put("2",
				new Mapped(inPMTCTInfantOnEndDate, ParameterizableUtil.createParameterMappings("onDate=${onDate}")));
		patientsInPMTCTInfantNotEnrolledByStartAndEndDate.getSearches().put("3",
				new Mapped(enrolledInPMTCTInfantByStartEndDate, ParameterizableUtil.createParameterMappings(
						"enrolledOnOrAfter=${enrolledOnOrAfter},enrolledOnOrBefore=${enrolledOnOrBefore}")));
		patientsInPMTCTInfantNotEnrolledByStartAndEndDate.setCompositionString("2 AND (NOT 3)");

		AgeCohortDefinition under13Weeks = new AgeCohortDefinition();
		under13Weeks.setName("under13Weeks");
		under13Weeks.setMaxAge(12);
		under13Weeks.setMaxAgeUnit(DurationUnit.WEEKS);
		under13Weeks.addParameter(new Parameter("effectiveDate", "effectiveDate", Date.class));

		AgeCohortDefinition over13Weeks = new AgeCohortDefinition();
		over13Weeks.setName("over13Weeks");
		over13Weeks.setMinAge(13);
		over13Weeks.setMinAgeUnit(DurationUnit.WEEKS);
		over13Weeks.addParameter(new Parameter("effectiveDate", "effectiveDate", Date.class));

		CompositionCohortDefinition patientsInPMTCTInfantTurned13WeeksByStartAndEndDate = new CompositionCohortDefinition();
		patientsInPMTCTInfantTurned13WeeksByStartAndEndDate
				.setName("patients In PMTCT infant turned 13 but not enrolled between startdate and enddate");
		patientsInPMTCTInfantTurned13WeeksByStartAndEndDate
				.addParameter(new Parameter("startDate", "startDate", Date.class));
		patientsInPMTCTInfantTurned13WeeksByStartAndEndDate
				.addParameter(new Parameter("endDate", "endDate", Date.class));
		patientsInPMTCTInfantTurned13WeeksByStartAndEndDate.getSearches().put("1",
				new Mapped(inPMTCTInfantOnEndDate, ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		patientsInPMTCTInfantTurned13WeeksByStartAndEndDate.getSearches().put("2",
				new Mapped(under13Weeks, ParameterizableUtil.createParameterMappings("effectiveDate=${startDate}")));
		patientsInPMTCTInfantTurned13WeeksByStartAndEndDate.getSearches().put("3",
				new Mapped(over13Weeks, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		patientsInPMTCTInfantTurned13WeeksByStartAndEndDate.setCompositionString("1 AND 2 AND 3");

		CodedObsCohortDefinition patientsWithHivPCRObs = Cohorts.createCodedObsCohortDefinition("patientsWithHivPCRObs",
				onOrAfterOnOrBefore, hivPCR, null, SetComparator.IN, TimeModifier.ANY);
		CodedObsCohortDefinition patientsWithHivPCRObsAndValuePositive = Cohorts.createCodedObsCohortDefinition(
				"patientsWithPosHivPCRObs", onOrAfterOnOrBefore, hivPCR, hivPositive, SetComparator.IN,
				TimeModifier.LAST);

		// Question number EIQ6
		CompositionCohortDefinition patientsInPMTCTInfantTurned13WeeksByStartAndEndDateWithHivPCRObs = new CompositionCohortDefinition();
		patientsInPMTCTInfantTurned13WeeksByStartAndEndDateWithHivPCRObs
				.setName("patients In PMTCT infant turned 13 but not enrolled between startdate and enddate");
		patientsInPMTCTInfantTurned13WeeksByStartAndEndDateWithHivPCRObs
				.addParameter(new Parameter("effectiveDate", "effectiveDate", Date.class));
		patientsInPMTCTInfantTurned13WeeksByStartAndEndDateWithHivPCRObs
				.addParameter(new Parameter("onDate", "onDate", Date.class));
		patientsInPMTCTInfantTurned13WeeksByStartAndEndDateWithHivPCRObs.getSearches().put("1",
				new Mapped(inPMTCTInfantOnEndDate, ParameterizableUtil.createParameterMappings("onDate=${onDate}")));
		patientsInPMTCTInfantTurned13WeeksByStartAndEndDateWithHivPCRObs.getSearches().put("2",
				new Mapped(over13Weeks, ParameterizableUtil.createParameterMappings("effectiveDate=${effectiveDate}")));
		patientsInPMTCTInfantTurned13WeeksByStartAndEndDateWithHivPCRObs.getSearches().put("3",
				new Mapped(patientsWithHivPCRObs, null));
		patientsInPMTCTInfantTurned13WeeksByStartAndEndDateWithHivPCRObs.setCompositionString("1 AND 2 AND 3");

		// Question number EIQ7D
		CompositionCohortDefinition patientsWithHivPCREnrolledInPMTCTInfantProg = new CompositionCohortDefinition();
		patientsWithHivPCREnrolledInPMTCTInfantProg.setName("EIQ: in Exposed Infant Program with PCR during period");
		patientsWithHivPCREnrolledInPMTCTInfantProg
				.addParameter(new Parameter("enrolledOnOrBefore", "enrolledOnOrBefore", Date.class));
		patientsWithHivPCREnrolledInPMTCTInfantProg.addParameter(new Parameter("onOrBefore", "onOrBefore", Date.class));
		patientsWithHivPCREnrolledInPMTCTInfantProg.addParameter(new Parameter("onOrAfter", "onOrAfter", Date.class));
		patientsWithHivPCREnrolledInPMTCTInfantProg.getSearches().put("1",
				new Mapped(enrolledInPMTCTInfantOnOrBeforeEndDate,
						ParameterizableUtil.createParameterMappings("enrolledOnOrBefore=${enrolledOnOrBefore}")));
		patientsWithHivPCREnrolledInPMTCTInfantProg.getSearches().put("2", new Mapped(patientsWithHivPCRObs,
				ParameterizableUtil.createParameterMappings("onOrBefore=${onOrBefore},onOrAfter=${onOrAfter}")));
		patientsWithHivPCREnrolledInPMTCTInfantProg.setCompositionString("1 AND 2");
		// Question EIQ7
		CompositionCohortDefinition patientsInPMTCTInfantTurned13WeeksByStartAndEndDateWithHivPCRObsPositive = new CompositionCohortDefinition();
		patientsInPMTCTInfantTurned13WeeksByStartAndEndDateWithHivPCRObsPositive.setName(
				"patients In PMTCT infant turned but not enrolled between startDate and endDate with HIV PCR Positive");
		patientsInPMTCTInfantTurned13WeeksByStartAndEndDateWithHivPCRObsPositive
				.addParameter(new Parameter("enrolledOnOrBefore", "enrolledOnOrBefore", Date.class));
		patientsInPMTCTInfantTurned13WeeksByStartAndEndDateWithHivPCRObsPositive
				.addParameter(new Parameter("onOrBefore", "onOrBefore", Date.class));
		patientsInPMTCTInfantTurned13WeeksByStartAndEndDateWithHivPCRObsPositive
				.addParameter(new Parameter("onOrAfter", "onOrAfter", Date.class));
		patientsInPMTCTInfantTurned13WeeksByStartAndEndDateWithHivPCRObsPositive.getSearches().put("1",
				new Mapped(enrolledInPMTCTInfantOnOrBeforeEndDate,
						ParameterizableUtil.createParameterMappings("enrolledOnOrBefore=${enrolledOnOrBefore}")));
		patientsInPMTCTInfantTurned13WeeksByStartAndEndDateWithHivPCRObsPositive.getSearches().put("2", new Mapped(
				patientsWithHivPCRObsAndValuePositive,
				ParameterizableUtil.createParameterMappings("onOrBefore=${onOrBefore},onOrAfter=${onOrAfter}")));
		patientsInPMTCTInfantTurned13WeeksByStartAndEndDateWithHivPCRObsPositive.setCompositionString("1 AND 2");

		AgeCohortDefinition under18Monthss = new AgeCohortDefinition();
		under18Monthss.setName("under18Months");
		under18Monthss.setMaxAge(17);
		under18Monthss.setMaxAgeUnit(DurationUnit.MONTHS);
		under18Monthss.addParameter(new Parameter("effectiveDate", "effectiveDate", Date.class));

		AgeCohortDefinition over18Mothns = new AgeCohortDefinition();
		over18Mothns.setName("over18Months");
		over18Mothns.setMinAge(18);
		over18Mothns.setMinAgeUnit(DurationUnit.MONTHS);
		over18Mothns.addParameter(new Parameter("effectiveDate", "effectiveDate", Date.class));

		// EIQ8D
		CompositionCohortDefinition patientsInPMTCTInfantTurned18MonthssByStartAndEndDate = new CompositionCohortDefinition();
		patientsInPMTCTInfantTurned18MonthssByStartAndEndDate
				.setName("patients In PMTCT infant turned 18 month but not enrolled between startdate and enddate");
		patientsInPMTCTInfantTurned18MonthssByStartAndEndDate
				.addParameter(new Parameter("enrolledOnOrBefore", "enrolledOnOrBefore", Date.class));
		patientsInPMTCTInfantTurned18MonthssByStartAndEndDate
				.addParameter(new Parameter("effectiveDate", "effectiveDate", Date.class));
		patientsInPMTCTInfantTurned18MonthssByStartAndEndDate.getSearches().put("1",
				new Mapped(enrolledInPMTCTInfantOnOrBeforeEndDate,
						ParameterizableUtil.createParameterMappings("enrolledOnOrBefore=${enrolledOnOrBefore}")));
		patientsInPMTCTInfantTurned18MonthssByStartAndEndDate.getSearches().put("2", new Mapped(over18Mothns,
				ParameterizableUtil.createParameterMappings("effectiveDate=${effectiveDate}")));
		patientsInPMTCTInfantTurned18MonthssByStartAndEndDate.setCompositionString("1 AND 2 ");

		// EIQ8
		CodedObsCohortDefinition patientsWithSerotestObs = Cohorts.createCodedObsCohortDefinition(
				"patientsWithHivPCRObs", onOrAfterOnOrBefore, seroTest, null, SetComparator.IN, TimeModifier.ANY);
		CodedObsCohortDefinition patientsPosSerotestObs = Cohorts.createCodedObsCohortDefinition(
				"patientsWithPosHivPCRObs", onOrAfterOnOrBefore, seroTest, hivPositive, SetComparator.IN,
				TimeModifier.LAST);

		CompositionCohortDefinition patientsInPMTCTInfantTurned18MonthsByStartWithserotestEver = new CompositionCohortDefinition();
		patientsInPMTCTInfantTurned18MonthsByStartWithserotestEver
				.setName("patients In PMTCT infant turned 18 months but not enrolled between startdate and enddate");
		patientsInPMTCTInfantTurned18MonthsByStartWithserotestEver
				.addParameter(new Parameter("enrolledOnOrBefore", "enrolledOnOrBefore", Date.class));
		patientsInPMTCTInfantTurned18MonthsByStartWithserotestEver
				.addParameter(new Parameter("effectiveDate", "effectiveDate", Date.class));
		patientsInPMTCTInfantTurned18MonthsByStartWithserotestEver.getSearches().put("1",
				new Mapped(enrolledInPMTCTInfantOnOrBeforeEndDate,
						ParameterizableUtil.createParameterMappings("enrolledOnOrBefore=${enrolledOnOrBefore}")));
		patientsInPMTCTInfantTurned18MonthsByStartWithserotestEver.getSearches().put("2", new Mapped(over18Mothns,
				ParameterizableUtil.createParameterMappings("effectiveDate=${effectiveDate}")));
		patientsInPMTCTInfantTurned18MonthsByStartWithserotestEver.getSearches().put("3",
				new Mapped(patientsWithSerotestObs, null));
		patientsInPMTCTInfantTurned18MonthsByStartWithserotestEver.setCompositionString("1 AND 2 AND 3");

		// EIQ9D
		CompositionCohortDefinition patientsWithSerotestEnrolledInPMTCTInfantProg = new CompositionCohortDefinition();
		patientsWithSerotestEnrolledInPMTCTInfantProg
				.setName("HIVQ: in Exposed Infant Program with SeroTest during period");
		patientsWithSerotestEnrolledInPMTCTInfantProg
				.addParameter(new Parameter("onOrBefore", "onOrBefore", Date.class));
		patientsWithSerotestEnrolledInPMTCTInfantProg.addParameter(new Parameter("onOrAfter", "onOrAfter", Date.class));
		patientsWithSerotestEnrolledInPMTCTInfantProg
				.addParameter(new Parameter("enrolledOnOrBefore", "enrolledOnOrBefore", Date.class));
		patientsWithSerotestEnrolledInPMTCTInfantProg.getSearches().put("1",
				new Mapped(enrolledInPMTCTInfantOnOrBeforeEndDate,
						ParameterizableUtil.createParameterMappings("enrolledOnOrBefore=${enrolledOnOrBefore}")));
		patientsWithSerotestEnrolledInPMTCTInfantProg.getSearches().put("2", new Mapped(patientsWithSerotestObs,
				ParameterizableUtil.createParameterMappings("onOrBefore=${onOrBefore},onOrAfter=${onOrAfter}")));
		patientsWithSerotestEnrolledInPMTCTInfantProg.setCompositionString("1 AND 2");

		// EIQ9
		CompositionCohortDefinition patientsInPMTCTInfantTurned18MonthsByStartAndEndDateAndWithserotestPositiveAfter16MonthsOfAge = new CompositionCohortDefinition();
		patientsInPMTCTInfantTurned18MonthsByStartAndEndDateAndWithserotestPositiveAfter16MonthsOfAge
				.setName("patients In PMTCT infant turned 18 months but not enrolled between startdate and enddate");
		patientsInPMTCTInfantTurned18MonthsByStartAndEndDateAndWithserotestPositiveAfter16MonthsOfAge
				.addParameter(new Parameter("onOrBefore", "onOrBefore", Date.class));
		patientsInPMTCTInfantTurned18MonthsByStartAndEndDateAndWithserotestPositiveAfter16MonthsOfAge
				.addParameter(new Parameter("onOrAfter", "onOrAfter", Date.class));
		patientsInPMTCTInfantTurned18MonthsByStartAndEndDateAndWithserotestPositiveAfter16MonthsOfAge
				.addParameter(new Parameter("enrolledOnOrBefore", "enrolledOnOrBefore", Date.class));
		patientsInPMTCTInfantTurned18MonthsByStartAndEndDateAndWithserotestPositiveAfter16MonthsOfAge.getSearches()
				.put("1", new Mapped(enrolledInPMTCTInfantOnOrBeforeEndDate,
						ParameterizableUtil.createParameterMappings("enrolledOnOrBefore=${enrolledOnOrBefore}")));
		patientsInPMTCTInfantTurned18MonthsByStartAndEndDateAndWithserotestPositiveAfter16MonthsOfAge.getSearches()
				.put("2", new Mapped(patientsPosSerotestObs, ParameterizableUtil
						.createParameterMappings("onOrBefore=${onOrBefore},onOrAfter=${onOrAfter}")));
		patientsInPMTCTInfantTurned18MonthsByStartAndEndDateAndWithserotestPositiveAfter16MonthsOfAge
				.setCompositionString("1 AND 2");
		// EIQ10
		AgeCohortDefinition over6Weeks = new AgeCohortDefinition();
		over6Weeks.setName("over6Weeks");
		over6Weeks.setMinAge(6);
		over6Weeks.setMinAgeUnit(DurationUnit.WEEKS);
		over6Weeks.addParameter(new Parameter("effectiveDate", "effectiveDate", Date.class));

		SqlCohortDefinition patientEverOnCTX = new SqlCohortDefinition();
		patientEverOnCTX.setName("patientEverOnCTX");
		patientEverOnCTX.setQuery(
				"select distinct patient_id from orders where concept_id=" + ctx.getConceptId() + " and voided=0");

		CompositionCohortDefinition patientOver6WeeksOnCTXAndInPMTCTInfantOnEndDate = new CompositionCohortDefinition();
		patientOver6WeeksOnCTXAndInPMTCTInfantOnEndDate
				.setName("patient Over 6 Weeks On CTX And in PMTCT Infant OnEndDate");
		patientOver6WeeksOnCTXAndInPMTCTInfantOnEndDate
				.addParameter(new Parameter("effectiveDate", "effectiveDate", Date.class));
		patientOver6WeeksOnCTXAndInPMTCTInfantOnEndDate.addParameter(new Parameter("onDate", "onDate", Date.class));
		patientOver6WeeksOnCTXAndInPMTCTInfantOnEndDate.getSearches().put("1",
				new Mapped(over6Weeks, ParameterizableUtil.createParameterMappings("effectiveDate=${effectiveDate}")));
		patientOver6WeeksOnCTXAndInPMTCTInfantOnEndDate.getSearches().put("2", new Mapped(patientEverOnCTX, null));
		patientOver6WeeksOnCTXAndInPMTCTInfantOnEndDate.getSearches().put("3",
				new Mapped(inPMTCTInfantOnEndDate, ParameterizableUtil.createParameterMappings("onDate=${onDate}")));
		patientOver6WeeksOnCTXAndInPMTCTInfantOnEndDate.setCompositionString("1 AND 2 AND 3");
		// EIQ10D
		CompositionCohortDefinition patientOver6WeeksAndInPMTCTInfantOnEndDate = new CompositionCohortDefinition();
		patientOver6WeeksAndInPMTCTInfantOnEndDate.setName("patient Over 6 Weeks And in PMTCT Infant OnEndDate");
		patientOver6WeeksAndInPMTCTInfantOnEndDate.addParameter(new Parameter("startDate", "startDate", Date.class));
		patientOver6WeeksAndInPMTCTInfantOnEndDate.addParameter(new Parameter("endDate", "endDate", Date.class));
		patientOver6WeeksAndInPMTCTInfantOnEndDate
				.addParameter(new Parameter("effectiveDate", "effectiveDate", Date.class));
		patientOver6WeeksAndInPMTCTInfantOnEndDate.addParameter(new Parameter("onDate", "onDate", Date.class));
		patientOver6WeeksAndInPMTCTInfantOnEndDate.getSearches().put("1",
				new Mapped(over6Weeks, ParameterizableUtil.createParameterMappings("effectiveDate=${effectiveDate}")));
		patientOver6WeeksAndInPMTCTInfantOnEndDate.getSearches().put("3",
				new Mapped(inPMTCTInfantOnEndDate, ParameterizableUtil.createParameterMappings("onDate=${onDate}")));
		patientOver6WeeksAndInPMTCTInfantOnEndDate.setCompositionString("1 AND 3");

		SqlCohortDefinition patientOnNevirapine = new SqlCohortDefinition();
		patientOnNevirapine.setName("patientOnNevirapine");
		patientOnNevirapine.setQuery("select distinct patient_id from orders where concept_id="
				+ nevirapine.getConceptId() + " and voided=0");

		// EIQ11
		CompositionCohortDefinition patientOnNevirapineEnrolledInPMTCTInfant = new CompositionCohortDefinition();
		patientOnNevirapineEnrolledInPMTCTInfant.setName("patient On Nevirapine Enrolled In PMTCT Infant");
		patientOnNevirapineEnrolledInPMTCTInfant.addParameter(new Parameter("onDate", "onDate", Date.class));
		patientOnNevirapineEnrolledInPMTCTInfant.getSearches().put("1", new Mapped(patientOnNevirapine, null));
		patientOnNevirapineEnrolledInPMTCTInfant.getSearches().put("2",
				new Mapped(inPMTCTInfantOnEndDate, ParameterizableUtil.createParameterMappings("onDate=${onDate}")));
		patientOnNevirapineEnrolledInPMTCTInfant.setCompositionString(" 1 AND 2");

		CohortIndicator onePMTCTInfant = Indicators.newCountIndicator("PMTCTInfantQ: Enrolled in PMTCT Infant",
				enrolledInPMTCTInfantByStartEndDate, ParameterizableUtil
						.createParameterMappings("enrolledOnOrAfter=${startDate},enrolledOnOrBefore=${endDate}"));

		CohortIndicator twoPMTCTInfant = Indicators.newCountIndicator(
				"PMTCTInfantQ: Enrolled in PMTCT Infant on the end date", inPMTCTInfantOnEndDate,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));

		CohortIndicator threePMTCTInfant = Indicators.newCountIndicator(
				"PMTCTInfantQ: patient in Feeding States On Date And in PMTCT Infant OnEndDate",
				inFeedingStatesOnDateAndinPMTCTInfantOnEndDate,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));

		CohortIndicator fourPMTCTInfant = Indicators.newCountIndicator(
				"PMTCTInfantQ: Enrolled in PMTCT Infant on or before the end date",
				enrolledInPMTCTInfantOnOrBeforeEndDate,
				ParameterizableUtil.createParameterMappings("enrolledOnOrBefore=${endDate}"));

		CohortIndicator fivePMTCTInfant = Indicators.newCountIndicator(
				"PMTCTInfantQ: patients With Two Or More Exposed Infant flowsheet In PMTCT Pregnancy",
				patientsWithOneOrMoreExposedInfantEncountersInPMTCTInfant, ParameterizableUtil.createParameterMappings(
						"endDate=${endDate},startDate=${startDate},enrolledOnOrAfter=${startDate},enrolledOnOrBefore=${endDate},onDate=${endDate}"));

		CohortIndicator fivePMTCTInfantD = Indicators.newCountIndicator(
				"PMTCTInfantQ: patients In PMTCT Infant but not enrolled between startdate and enddate",
				patientsInPMTCTInfantNotEnrolledByStartAndEndDate, ParameterizableUtil.createParameterMappings(
						"enrolledOnOrAfter=${startDate},enrolledOnOrBefore=${endDate},onDate=${endDate}"));

		CohortIndicator sixPMTCTInfantD = Indicators.newCountIndicator(
				"PMTCTInfantQ: Enrolled in PMTCT Infant on the end date and turned 13 weeks by start and end date",
				patientsInPMTCTInfantTurned13WeeksByStartAndEndDate,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));

		CohortIndicator sixPMTCTInfant = Indicators.newCountIndicator(
				"PMTCTInfantQ: Enrolled in PMTCT Infant on the end date and turned 13 weeks by the end date with HIV PCR obs",
				patientsInPMTCTInfantTurned13WeeksByStartAndEndDateWithHivPCRObs,
				ParameterizableUtil.createParameterMappings("onDate=${endDate},effectiveDate=${endDate}"));

		CohortIndicator sevenPMTCTInfantD = Indicators.newCountIndicator(
				"PMTCTInfantQ: Enrolled in PMTCT Infant on the end date with HIV PCR within the period",
				patientsWithHivPCREnrolledInPMTCTInfantProg, ParameterizableUtil.createParameterMappings(
						"enrolledOnOrBefore=${endDate},onOrBefore=${endDate},onOrAfter=${startDate}"));

		CohortIndicator sevenPMTCTInfant = Indicators.newCountIndicator(
				"" + "PMTCTInfantQ: Enrolled in PMTCT Infant on the end date and turned 13 weeks by start and end date with HIV PCR obs positive",
				patientsInPMTCTInfantTurned13WeeksByStartAndEndDateWithHivPCRObsPositive,
				ParameterizableUtil.createParameterMappings(
						"enrolledOnOrBefore=${endDate},onOrBefore=${endDate},onOrAfter=${startDate}"));

		CohortIndicator eightPMTCTInfantD = Indicators.newCountIndicator(
				"PMTCTInfantQ: Enrolled in PMTCT Infant on or before the end date who turned 18 months by start",
				patientsInPMTCTInfantTurned18MonthssByStartAndEndDate, ParameterizableUtil
						.createParameterMappings("enrolledOnOrBefore=${endDate},effectiveDate=${startDate}"));

		CohortIndicator eightPMTCTInfant = Indicators.newCountIndicator(
				"PMTCTInfantQ: Enrolled in PMTCT Infant on or before the end date who turned 18 months by start and end date and sero test recorded after 16 months of age",
				patientsInPMTCTInfantTurned18MonthsByStartWithserotestEver, ParameterizableUtil
						.createParameterMappings("enrolledOnOrBefore=${endDate},effectiveDate=${startDate}"));

		CohortIndicator ninePMTCTInfantD = Indicators.newCountIndicator(
				"PMTCTInfantQ: Enrolled in PMTCT Infant on or before the end date who turned 18 months by start and sero test recorded within the period",
				patientsWithSerotestEnrolledInPMTCTInfantProg, ParameterizableUtil.createParameterMappings(
						"enrolledOnOrBefore=${endDate},onOrBefore=${endDate},onOrAfter=${startDate}"));

		CohortIndicator ninePMTCTInfant = Indicators.newCountIndicator(
				"PMTCTInfantQ: Enrolled in PMTCT Infant on or before the end date who turned 18 months by start and end date and sero test positive recorded after 16 months of age",
				patientsInPMTCTInfantTurned18MonthsByStartAndEndDateAndWithserotestPositiveAfter16MonthsOfAge,
				ParameterizableUtil.createParameterMappings(
						"enrolledOnOrBefore=${endDate},onOrBefore=${endDate},onOrAfter=${startDate}"));

		CohortIndicator tenPMTCTInfant = Indicators.newCountIndicator(
				"PMTCTInfantQ: Patient over 6 week on ctx Enrolled in PMTCT Infant on the end date",
				patientOver6WeeksOnCTXAndInPMTCTInfantOnEndDate,
				ParameterizableUtil.createParameterMappings("onDate=${endDate},effectiveDate=${startDate}"));

		CohortIndicator tenPMTCTInfantD = Indicators.newCountIndicator(
				"PMTCTInfantQ: Patient over 6 week Enrolled in PMTCT Infant on the end date",
				patientOver6WeeksAndInPMTCTInfantOnEndDate,
				ParameterizableUtil.createParameterMappings("onDate=${endDate},effectiveDate=${startDate}"));

		CohortIndicator elevenPMTCTInfant = Indicators.newCountIndicator(
				"PMTCTInfantQ: Enrolled in PMTCT Infant ever on Nevirapine", patientOnNevirapineEnrolledInPMTCTInfant,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));

		dsd.addColumn("EIQ1", "Enrolled in PMTCT Infant", new Mapped(onePMTCTInfant,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("EIQ2", "Enrolled in PMTCT Infant on the end date",
				new Mapped(twoPMTCTInfant, ParameterizableUtil.createParameterMappings("endDate=${endDate}")), "");
		dsd.addColumn("EIQ3", "patient in Feeding States On Date And in PMTCT Infant OnEndDate",
				new Mapped(threePMTCTInfant, ParameterizableUtil.createParameterMappings("endDate=${endDate}")), "");
		dsd.addColumn("EIQ4", "Enrolled in PMTCT Infant on or before the end date",
				new Mapped(fourPMTCTInfant, ParameterizableUtil.createParameterMappings("endDate=${endDate}")), "");
		dsd.addColumn("EIQ5", "patients With Two Or More Exposed Infant flowsheet In PMTCT Infant",
				new Mapped(fivePMTCTInfant,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");

		dsd.addColumn("EIQ5D", "patients In PMTCT Infant but not enrolled between startdate and enddate",
				new Mapped(fivePMTCTInfantD,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");

		dsd.addColumn("EIQ6",
				"Enrolled in PMTCT Infant on the end date and turned 13 weeks by the end date with HIV PCR obs",
				new Mapped(sixPMTCTInfant, ParameterizableUtil.createParameterMappings("endDate=${endDate}")), "");

		dsd.addColumn("EIQ6D", "Enrolled in PMTCT Infant on the end date and turned 13 weeks by start and end date",
				new Mapped(sixPMTCTInfantD,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");

		dsd.addColumn("EIQ7D", "Enrolled in PMTCT Infant on the end date HIV PCR within the period",
				new Mapped(sevenPMTCTInfantD,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");

		dsd.addColumn("EIQ7",
				"Enrolled in PMTCT Infant on the end date and turned 13 weeks by start and end date with HIV PCR obs positive",
				new Mapped(sevenPMTCTInfant,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");

		dsd.addColumn("EIQ8",
				"Enrolled in PMTCT Infant on or before the end date who turned 18 months by start and end date and sero test recorded after 16 months of age",
				new Mapped(eightPMTCTInfant,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");
		dsd.addColumn("EIQ8D", "Enrolled in PMTCT Infant on or before the end date who turned 18 months by end date",
				new Mapped(eightPMTCTInfantD,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");
		dsd.addColumn("EIQ9D",
				"Enrolled in PMTCT Infant on or before the end date who turned 18 months by start and sero test positive recorded during the period",
				new Mapped(ninePMTCTInfantD,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");
		dsd.addColumn("EIQ9",
				"Enrolled in PMTCT Infant on or before the end date who turned 18 months by start and end date and sero test positive recorded after 16 months of age",
				new Mapped(ninePMTCTInfant,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");

		dsd.addColumn("EIQ10", "Patient over 6 week on ctx Enrolled in PMTCT Infant on the end date",
				new Mapped(tenPMTCTInfant,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");
		dsd.addColumn("EIQ10D", "Patient over 6 week Enrolled in PMTCT Infant on the end date",
				new Mapped(tenPMTCTInfantD,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");

		dsd.addColumn("EIQ11", "Enrolled in PMTCT Infanf ever on Nevirapine",
				new Mapped(elevenPMTCTInfant, ParameterizableUtil.createParameterMappings("endDate=${endDate}")), "");

		// ===================================================================
		// PMTCT CC Mother Program
		// ===================================================================

		ProgramEnrollmentCohortDefinition enrolledInPMTCTCCMotherByStartEndDate = Cohorts
				.createProgramEnrollmentParameterizedByStartEndDate("Enrolled in PMTCT C C Mother",
						pmtctCombinedMotherProgram);

		InProgramCohortDefinition inPMTCTCCMotherOnEndDate = Cohorts.createInProgramParameterizableByDate(
				"In PMTCT Program C C Mother on End Date", pmtctCombinedMotherProgram);

		CompositionCohortDefinition activePatientOnArtInPMTCTCCMother = new CompositionCohortDefinition();
		activePatientOnArtInPMTCTCCMother.setName("active Patient On Art In PMTCT Pregnancy");
		activePatientOnArtInPMTCTCCMother.addParameter(new Parameter("endDate", "endDate", Date.class));
		activePatientOnArtInPMTCTCCMother.addParameter(new Parameter("onDate", "onDate", Date.class));
		activePatientOnArtInPMTCTCCMother.getSearches().put("1",
				new Mapped(activePatientOnArt, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		activePatientOnArtInPMTCTCCMother.getSearches().put("2",
				new Mapped(inPMTCTCCMotherOnEndDate, ParameterizableUtil.createParameterMappings("onDate=${onDate}")));
		activePatientOnArtInPMTCTCCMother.setCompositionString(" 1 AND 2");

		CompositionCohortDefinition patientsWithOneOrMoreAdultHIVEncountersInPMTCTCCMother = new CompositionCohortDefinition();
		patientsWithOneOrMoreAdultHIVEncountersInPMTCTCCMother
				.setName("patients With One Or More Adult HIV flowsheet In PMTCT C C Mother");
		patientsWithOneOrMoreAdultHIVEncountersInPMTCTCCMother
				.addParameter(new Parameter("startDate", "startDate", Date.class));
		patientsWithOneOrMoreAdultHIVEncountersInPMTCTCCMother
				.addParameter(new Parameter("endDate", "endDate", Date.class));
		patientsWithOneOrMoreAdultHIVEncountersInPMTCTCCMother
				.addParameter(new Parameter("enrolledOnOrAfter", "enrolledOnOrAfter", Date.class));
		patientsWithOneOrMoreAdultHIVEncountersInPMTCTCCMother
				.addParameter(new Parameter("enrolledOnOrBefore", "enrolledOnOrBefore", Date.class));
		patientsWithOneOrMoreAdultHIVEncountersInPMTCTCCMother
				.addParameter(new Parameter("onDate", "onDate", Date.class));
		patientsWithOneOrMoreAdultHIVEncountersInPMTCTCCMother.getSearches().put("1",
				new Mapped(patientsWithOneOrMoreAdultHIVEncounters,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
		patientsWithOneOrMoreAdultHIVEncountersInPMTCTCCMother.getSearches().put("2",
				new Mapped(inPMTCTCCMotherOnEndDate, ParameterizableUtil.createParameterMappings("onDate=${onDate}")));
		/*
		 * patientsWithOneOrMoreAdultHIVEncountersInPMTCTCCMother.getSearches().
		 * put("3", new
		 * Mapped(enrolledInPMTCTCCMotherByStartEndDate,ParameterizableUtil.
		 * createParameterMappings(
		 * "enrolledOnOrAfter=${enrolledOnOrAfter},enrolledOnOrBefore=${enrolledOnOrBefore}"
		 * ))); patientsWithOneOrMoreAdultHIVEncountersInPMTCTCCMother.
		 * setCompositionString(" 1 AND 2 AND (NOT 3)");
		 */
		patientsWithOneOrMoreAdultHIVEncountersInPMTCTCCMother.setCompositionString(" 1 AND 2");

		CompositionCohortDefinition patientsInPMTCTCCMotherNotEnrolledByStartAndEndDate = new CompositionCohortDefinition();
		patientsInPMTCTCCMotherNotEnrolledByStartAndEndDate
				.setName("patients In PMTCT c c Mother but not enrolled between startdate and enddate");
		patientsInPMTCTCCMotherNotEnrolledByStartAndEndDate
				.addParameter(new Parameter("enrolledOnOrAfter", "enrolledOnOrAfter", Date.class));
		patientsInPMTCTCCMotherNotEnrolledByStartAndEndDate
				.addParameter(new Parameter("enrolledOnOrBefore", "enrolledOnOrBefore", Date.class));
		patientsInPMTCTCCMotherNotEnrolledByStartAndEndDate.addParameter(new Parameter("onDate", "onDate", Date.class));
		patientsInPMTCTCCMotherNotEnrolledByStartAndEndDate.getSearches().put("2",
				new Mapped(inPMTCTCCMotherOnEndDate, ParameterizableUtil.createParameterMappings("onDate=${onDate}")));
		patientsInPMTCTCCMotherNotEnrolledByStartAndEndDate.getSearches().put("3",
				new Mapped(enrolledInPMTCTCCMotherByStartEndDate, ParameterizableUtil.createParameterMappings(
						"enrolledOnOrAfter=${enrolledOnOrAfter},enrolledOnOrBefore=${enrolledOnOrBefore}")));
		patientsInPMTCTCCMotherNotEnrolledByStartAndEndDate.setCompositionString("2 AND (NOT 3)");

		SqlCohortDefinition patientUsingMethodOfFamilyPlanning = new SqlCohortDefinition();
		patientUsingMethodOfFamilyPlanning.setName("patientUsingMethodOfFamilyPlanning");
		patientUsingMethodOfFamilyPlanning.setQuery("select distinct person_id from obs where concept_id="
				+ methodOfFamilyPlanning.getConceptId() + " and value_coded in (" + usingCondoms.getConceptId() + ","
				+ naturalFamilyPlanning.getConceptId()
				+ ") and obs_datetime>= :startDate and obs_datetime<= :endDate and voided=0");
		patientUsingMethodOfFamilyPlanning.addParameter(new Parameter("startDate", "startDate", Date.class));
		patientUsingMethodOfFamilyPlanning.addParameter(new Parameter("endDate", "endDate", Date.class));

		CompositionCohortDefinition patientsInPMTCTCCMotherUsingMethodOfFamilyPlanning = new CompositionCohortDefinition();
		patientsInPMTCTCCMotherUsingMethodOfFamilyPlanning
				.setName("patients In PMTCT  C C Mother Using Method Of Family Planning");
		patientsInPMTCTCCMotherUsingMethodOfFamilyPlanning
				.addParameter(new Parameter("startDate", "startDate", Date.class));
		patientsInPMTCTCCMotherUsingMethodOfFamilyPlanning
				.addParameter(new Parameter("endDate", "endDate", Date.class));
		patientsInPMTCTCCMotherUsingMethodOfFamilyPlanning.addParameter(new Parameter("onDate", "onDate", Date.class));
		patientsInPMTCTCCMotherUsingMethodOfFamilyPlanning.getSearches().put("2",
				new Mapped(inPMTCTCCMotherOnEndDate, ParameterizableUtil.createParameterMappings("onDate=${onDate}")));
		patientsInPMTCTCCMotherUsingMethodOfFamilyPlanning.getSearches().put("3",
				new Mapped(patientUsingMethodOfFamilyPlanning,
						ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate}")));
		patientsInPMTCTCCMotherUsingMethodOfFamilyPlanning.setCompositionString("2 AND 3");

		CohortIndicator onePMTCTCCMother = Indicators.newCountIndicator("PMTCTCCMotherQ: Enrolled in PMTCT C C Mother",
				enrolledInPMTCTCCMotherByStartEndDate, ParameterizableUtil
						.createParameterMappings("enrolledOnOrAfter=${startDate},enrolledOnOrBefore=${endDate}"));

		CohortIndicator twoPMTCTCCMother = Indicators.newCountIndicator(
				"PMTCTCCMotherQ: Enrolled in PMTCT C C Mother on the end date", inPMTCTCCMotherOnEndDate,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));

		CohortIndicator threePMTCTCCMother = Indicators.newCountIndicator(
				"PMTCTCCMotherQ: Enrolled in PMTCT C C Mother on or before the end date",
				enrolledInPMTCTCCMotherOnOrBeforeEndDate,
				ParameterizableUtil.createParameterMappings("enrolledOnOrBefore=${endDate}"));

		CohortIndicator fourPMTCTCCMother = Indicators.newCountIndicator(
				"PMTCTCCMotherQ: Enrolled in PMTCT C C Mother on the end date", activePatientOnArtInPMTCTCCMother,
				ParameterizableUtil.createParameterMappings("onDate=${endDate}"));

		CohortIndicator fivePMTCTCCMother = Indicators.newCountIndicator(
				"PMTCTCCMotherQ: patients With One Or More Adult HIV flowsheet In PMTCT C C Mother",
				patientsWithOneOrMoreAdultHIVEncountersInPMTCTCCMother, ParameterizableUtil.createParameterMappings(
						"endDate=${endDate},startDate=${startDate},enrolledOnOrAfter=${startDate},enrolledOnOrBefore=${endDate},onDate=${endDate}"));

		CohortIndicator fivePMTCTCCMotherD = Indicators.newCountIndicator(
				"PMTCTCCMotherQ: patients In PMTCT C C Mother but not enrolled between startdate and enddate",
				patientsInPMTCTCCMotherNotEnrolledByStartAndEndDate, ParameterizableUtil.createParameterMappings(
						"enrolledOnOrAfter=${startDate},enrolledOnOrBefore=${endDate},onDate=${endDate}"));

		CohortIndicator sixPMTCTCCMother = Indicators.newCountIndicator(
				"PMTCTCCMotherQ: Enrolled in PMTCT C C Mother on the end date Using Method Of Family Planning",
				patientsInPMTCTCCMotherUsingMethodOfFamilyPlanning, ParameterizableUtil
						.createParameterMappings("onDate=${endDate},startDate=${startDate},endDate=${endDate}"));

		dsd.addColumn("CCMQ1", "Enrolled in PMTCT C C Mother", new Mapped(onePMTCTCCMother,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("CCMQ2", "Enrolled in PMTCT C C Mother on the end date",
				new Mapped(twoPMTCTCCMother, ParameterizableUtil.createParameterMappings("endDate=${endDate}")), "");
		dsd.addColumn("CCMQ3", "Enrolled in PMTCT C C Mother on or before the end date",
				new Mapped(threePMTCTCCMother, ParameterizableUtil.createParameterMappings("endDate=${endDate}")), "");
		dsd.addColumn("CCMQ4", "Enrolled in PMTCT C C Mother on the end date",
				new Mapped(fourPMTCTCCMother, ParameterizableUtil.createParameterMappings("endDate=${endDate}")), "");
		dsd.addColumn("CCMQ5", "patients With Two Or More Adult HIV flowsheet In PMTCT C C Mother",
				new Mapped(fivePMTCTCCMother,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");

		dsd.addColumn("CCMQ5D", "patients In PMTCT C C Mother but not enrolled between startdate and enddate",
				new Mapped(fivePMTCTCCMotherD,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");

		dsd.addColumn("CCMQ6", "Enrolled in PMTCT C C Mother on the end date Using Method Of Family Planning",
				new Mapped(sixPMTCTCCMother,
						ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")),
				"");

		// ======================================================================================
		// Viral Load
		// ======================================================================================

		InProgramCohortDefinition currentlyInAnyHIVProgram = Cohorts
				.createInProgramParameterizableByDate("hivQD: In All HIV Programs", hivPrograms, "onDate");

		NumericObsCohortDefinition viralLoad = Cohorts.createNumericObsCohortDefinition("obsQD: Viral Load recorded",
				onOrAfterOnOrBefore, viralLoadConcept, 0, null, TimeModifier.ANY);
		;

		NumericObsCohortDefinition viralLoadFailure = Cohorts.createNumericObsCohortDefinition(
				"obsQD: Viral Load Failure recorded", onOrAfterOnOrBefore, viralLoadConcept, 10000,
				RangeComparator.GREATER_EQUAL, TimeModifier.ANY);

		CompositionCohortDefinition currentlyInHIVUnder15 = new CompositionCohortDefinition();
		currentlyInHIVUnder15.setName("hivQD: In all programs under 15 and on ART more than 12 months");
		currentlyInHIVUnder15.addParameter(new Parameter("endDate", "endDate", Date.class));
		currentlyInHIVUnder15.addParameter(new Parameter("startDate", "startDate", Date.class));
		currentlyInHIVUnder15.getSearches().put("1",
				new Mapped(over15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		currentlyInHIVUnder15.getSearches().put("2",
				new Mapped(currentlyInAnyHIVProgram, ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		currentlyInHIVUnder15.getSearches().put("3",
				new Mapped(artCurrently, ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		currentlyInHIVUnder15.getSearches().put("4", new Mapped(artOnOrBefore,
				ParameterizableUtil.createParameterMappings("onOrAfter=${startDate-1y},onOrBefore=${startDate}")));
		currentlyInHIVUnder15.setCompositionString("NOT 1 AND 2 AND 3 AND 4");

		CompositionCohortDefinition currentlyInHIVOver15 = new CompositionCohortDefinition();
		currentlyInHIVOver15.setName("hivQD: In all programs over 15 and on ART more than 12 months");
		currentlyInHIVOver15.addParameter(new Parameter("endDate", "endDate", Date.class));
		currentlyInHIVOver15.addParameter(new Parameter("startDate", "startDate", Date.class));
		currentlyInHIVOver15.getSearches().put("1",
				new Mapped(over15Cohort, ParameterizableUtil.createParameterMappings("effectiveDate=${endDate}")));
		currentlyInHIVOver15.getSearches().put("2",
				new Mapped(currentlyInAnyHIVProgram, ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		currentlyInHIVOver15.getSearches().put("3",
				new Mapped(artCurrently, ParameterizableUtil.createParameterMappings("onDate=${endDate}")));
		currentlyInHIVOver15.getSearches().put("4", new Mapped(artOnOrBefore,
				ParameterizableUtil.createParameterMappings("onOrAfter=${startDate-1y},onOrBefore=${startDate}")));
		currentlyInHIVOver15.setCompositionString("1 AND 2 AND 3 AND 4");

		CompositionCohortDefinition eligibleWithViralU15 = new CompositionCohortDefinition();
		eligibleWithViralU15.setName("hivQD: eligible with viral load under 15");
		eligibleWithViralU15.addParameter(new Parameter("endDate", "endDate", Date.class));
		eligibleWithViralU15.addParameter(new Parameter("startDate", "startDate", Date.class));
		eligibleWithViralU15.getSearches().put("1", new Mapped(currentlyInHIVUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
		eligibleWithViralU15.getSearches().put("2", new Mapped(viralLoad,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${endDate-1y}")));
		eligibleWithViralU15.setCompositionString("1 AND 2");

		CompositionCohortDefinition eligibleWithViralO15 = new CompositionCohortDefinition();
		eligibleWithViralO15.setName("hivQD: eligible with viral load over 15");
		eligibleWithViralO15.addParameter(new Parameter("endDate", "endDate", Date.class));
		eligibleWithViralO15.addParameter(new Parameter("startDate", "startDate", Date.class));
		eligibleWithViralO15.getSearches().put("1", new Mapped(currentlyInHIVOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
		eligibleWithViralO15.getSearches().put("2", new Mapped(viralLoad,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${endDate-1y}")));
		eligibleWithViralO15.setCompositionString("1 AND 2");

		CompositionCohortDefinition eligibleWithViralFailureU15 = new CompositionCohortDefinition();
		eligibleWithViralFailureU15.setName("hivQD: eligible with viral load failure under 15");
		eligibleWithViralFailureU15.addParameter(new Parameter("endDate", "endDate", Date.class));
		eligibleWithViralFailureU15.addParameter(new Parameter("startDate", "startDate", Date.class));
		eligibleWithViralFailureU15.getSearches().put("1", new Mapped(currentlyInHIVUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
		eligibleWithViralFailureU15.getSearches().put("2", new Mapped(viralLoadFailure,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${endDate-1y}")));
		eligibleWithViralFailureU15.setCompositionString("1 AND 2");

		CompositionCohortDefinition eligibleWithViralFailureO15 = new CompositionCohortDefinition();
		eligibleWithViralFailureO15.setName("hivQD: eligible with viral load failure over 15");
		eligibleWithViralFailureO15.addParameter(new Parameter("endDate", "endDate", Date.class));
		eligibleWithViralFailureO15.addParameter(new Parameter("startDate", "startDate", Date.class));
		eligibleWithViralFailureO15.getSearches().put("1", new Mapped(currentlyInHIVOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
		eligibleWithViralFailureO15.getSearches().put("2", new Mapped(viralLoadFailure,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${endDate-1y}")));
		eligibleWithViralFailureO15.setCompositionString("1 AND 2");

		// VQ4AD
		CompositionCohortDefinition eligibleActiveInHIVOver15 = new CompositionCohortDefinition();
		eligibleActiveInHIVOver15
				.setName("hivQD: eligible active in all programs over 15 and on ART more than 12 months");
		eligibleActiveInHIVOver15.addParameter(new Parameter("endDate", "endDate", Date.class));
		eligibleActiveInHIVOver15.addParameter(new Parameter("startDate", "startDate", Date.class));
		eligibleActiveInHIVOver15.getSearches().put("1", new Mapped(currentlyInHIVOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
		eligibleActiveInHIVOver15.getSearches().put("2", new Mapped(encounter,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${endDate-1y}")));
		eligibleActiveInHIVOver15.setCompositionString("1 AND 2");
		// VQ4A
		CompositionCohortDefinition eligibleActiveWitViralLoadOver15 = new CompositionCohortDefinition();
		eligibleActiveWitViralLoadOver15.setName("hivQD: eligible active in all programs with Viral load over 15 ");
		eligibleActiveWitViralLoadOver15.addParameter(new Parameter("endDate", "endDate", Date.class));
		eligibleActiveWitViralLoadOver15.addParameter(new Parameter("startDate", "startDate", Date.class));
		eligibleActiveWitViralLoadOver15.getSearches().put("1", new Mapped(eligibleActiveInHIVOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
		eligibleActiveWitViralLoadOver15.getSearches().put("2", new Mapped(viralLoad,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${endDate-1y}")));
		eligibleActiveWitViralLoadOver15.setCompositionString("1 AND 2");
		// VQ4CD
		CompositionCohortDefinition eligibleActiveInHIVUnder15 = new CompositionCohortDefinition();
		eligibleActiveInHIVUnder15
				.setName("hivQD: eligible active in all programs under 15 and on ART more than 12 months");
		eligibleActiveInHIVUnder15.addParameter(new Parameter("endDate", "endDate", Date.class));
		eligibleActiveInHIVUnder15.addParameter(new Parameter("startDate", "startDate", Date.class));
		eligibleActiveInHIVUnder15.getSearches().put("1", new Mapped(currentlyInHIVUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
		eligibleActiveInHIVUnder15.getSearches().put("2", new Mapped(encounter,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${endDate-1y}")));
		eligibleActiveInHIVUnder15.setCompositionString("1 AND 2");
		// VQ4C
		CompositionCohortDefinition eligibleActiveWitViralLoadUnder15 = new CompositionCohortDefinition();
		eligibleActiveWitViralLoadUnder15.setName("hivQD: eligible active in all programs with Viral load under 15 ");
		eligibleActiveWitViralLoadUnder15.addParameter(new Parameter("endDate", "endDate", Date.class));
		eligibleActiveWitViralLoadUnder15.addParameter(new Parameter("startDate", "startDate", Date.class));
		eligibleActiveWitViralLoadUnder15.getSearches().put("1", new Mapped(eligibleActiveInHIVUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")));
		eligibleActiveWitViralLoadUnder15.getSearches().put("2", new Mapped(viralLoad,
				ParameterizableUtil.createParameterMappings("onOrBefore=${endDate},onOrAfter=${endDate-1y}")));
		eligibleActiveWitViralLoadUnder15.setCompositionString("1 AND 2");

		CohortIndicator one = Indicators.newCountIndicator(
				"hivQD: In all programs under 15 and on ART more than 12 months_", currentlyInHIVUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator two = Indicators.newCountIndicator(
				"hivQD: In all programs over 15 and on ART more than 12 months_", currentlyInHIVOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator three = Indicators.newCountIndicator("hivQD: eligible with viral load under 15_",
				eligibleWithViralU15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator four = Indicators.newCountIndicator("hivQD: eligible with viral load over 15_",
				eligibleWithViralO15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator five = Indicators.newCountIndicator("hivQD: eligible with viral load failure under 15_",
				eligibleWithViralFailureU15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator six = Indicators.newCountIndicator("hivQD: eligible with viral load failure over 15_",
				eligibleWithViralFailureO15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator seven = Indicators.newCountIndicator("hivQD: actively eligible in all programs over 15_",
				eligibleActiveInHIVOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator eight = Indicators.newCountIndicator(
				"hivQD: actively eligible in all with viral load programs over 15_", eligibleActiveWitViralLoadOver15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator nine = Indicators.newCountIndicator("hivQD: actively eligible in all programs under 15_",
				eligibleActiveInHIVUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));
		CohortIndicator ten = Indicators.newCountIndicator(
				"hivQD: actively eligible in all with viral load programs under 15_", eligibleActiveWitViralLoadUnder15,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}"));

		dsd.addColumn("VQ2AD", "Eligible Over 15", new Mapped(two,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("VQ2CD", "Eligible Under 15", new Mapped(one,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("VQ2C", "Viral Load Under 15", new Mapped(three,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("VQ2A", "Viral Load Over 15", new Mapped(four,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("VQ3C", "Viral Load Failure Over 15", new Mapped(five,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("VQ3A", "Viral Load Failure Under 15", new Mapped(six,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("VQ4AD", "Actively Eligible Over 15", new Mapped(seven,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("VQ4A", "Actively Eligible with Viral load Over 15", new Mapped(eight,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("VQ4CD", "Actively Eligible Under 15", new Mapped(nine,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");
		dsd.addColumn("VQ4C", "Actively Eligible with Viral load Under 15", new Mapped(ten,
				ParameterizableUtil.createParameterMappings("endDate=${endDate},startDate=${startDate}")), "");

	}

	private void setUpProperties() {
		hivProgram = gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
		pediHivProgram = gp.getProgram(GlobalPropertiesManagement.PEDI_HIV_PROGRAM);
		pmtctProgram = gp.getProgram(GlobalPropertiesManagement.PMTCT_PREGNANCY_PROGRAM);
		pmtctCombinedMotherProgram = gp.getProgram(GlobalPropertiesManagement.PMTCT_COMBINED_MOTHER_PROGRAM);
		pmtctCombinedInfantProgram = gp.getProgram(GlobalPropertiesManagement.PMTCT_COMBINED_CLINIC_PROGRAM);

		pediPreArt = gp.getProgramWorkflowState(GlobalPropertiesManagement.FOLLOWING_STATE,
				GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW, GlobalPropertiesManagement.PEDI_HIV_PROGRAM);
		hivPreArt = gp.getProgramWorkflowState(GlobalPropertiesManagement.FOLLOWING_STATE,
				GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW, GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
		pediArt = gp.getProgramWorkflowState(GlobalPropertiesManagement.ON_ANTIRETROVIRALS_STATE,
				GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW, GlobalPropertiesManagement.PEDI_HIV_PROGRAM);
		hivArt = gp.getProgramWorkflowState(GlobalPropertiesManagement.ON_ANTIRETROVIRALS_STATE,
				GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW, GlobalPropertiesManagement.ADULT_HIV_PROGRAM);

		hivPrograms.add(hivProgram);
		hivPrograms.add(pediHivProgram);
		hivPrograms.add(pmtctProgram);
		hivPrograms.add(pmtctCombinedMotherProgram);

		hivProgramsExcPMTCT.add(hivProgram);
		hivProgramsExcPMTCT.add(pediHivProgram);

		preArtWorkflowStates.add(pediPreArt);
		preArtWorkflowStates.add(hivPreArt);

		artWorkflowStates.add(pediArt);
		artWorkflowStates.add(hivArt);

		hivEncounterTypes = gp.getEncounterTypeList(GlobalPropertiesManagement.HIV_ENCOUNTER_TYPES, ":");

		cd4Concept = gp.getConcept(GlobalPropertiesManagement.CD4_TEST);
		cd4PercentConcept = gp.getConcept(GlobalPropertiesManagement.CD4_PERCENTAGE_TEST);
		weightConcept = gp.getConcept(GlobalPropertiesManagement.WEIGHT_CONCEPT);

		onOrAfterOnOrBefore.add("onOrAfter");
		onOrAfterOnOrBefore.add("onOrBefore");

		adultHIVEncounterType = gp.getEncounterType(GlobalPropertiesManagement.ADULT_FLOWSHEET_ENCOUNTER);
		pediHIVEncounterType = gp.getEncounterType(GlobalPropertiesManagement.PEDI_FLOWSHEET_ENCOUNTER);
		exposedInfantEncountertype = gp.getEncounterType(GlobalPropertiesManagement.EXPOSED_INFANT_ENCOUNTER);

		feedingWorkflowStates
				.add(gp.getProgramWorkflowState(GlobalPropertiesManagement.ARTIFICIAL_FEEDING_9_18_MONTHS_STATE,
						GlobalPropertiesManagement.FEEDING_GROUP_WORKFLOW,
						GlobalPropertiesManagement.PMTCT_COMBINED_CLINIC_PROGRAM));
		feedingWorkflowStates
				.add(gp.getProgramWorkflowState(GlobalPropertiesManagement.ARTIFICIAL_FEEDING_6_9_MONTHS_STATE,
						GlobalPropertiesManagement.FEEDING_GROUP_WORKFLOW,
						GlobalPropertiesManagement.PMTCT_COMBINED_CLINIC_PROGRAM));
		feedingWorkflowStates
				.add(gp.getProgramWorkflowState(GlobalPropertiesManagement.ARTIFICIAL_FEEDING_0_6_MONTHS_STATE,
						GlobalPropertiesManagement.FEEDING_GROUP_WORKFLOW,
						GlobalPropertiesManagement.PMTCT_COMBINED_CLINIC_PROGRAM));

		hivPCR = gp.getConcept(GlobalPropertiesManagement.DBS_CONCEPT);

		hivPositive = gp.getConcept(GlobalPropertiesManagement.POSITIVE_HIV_TEST_ANSWER);
		seroTest = gp.getConcept(GlobalPropertiesManagement.SERO_TEST);
		ctx = gp.getConcept(GlobalPropertiesManagement.CTX);
		nevirapine = gp.getConcept(GlobalPropertiesManagement.NEVIRAPINE_DRUG);

		methodOfFamilyPlanning = gp.getConcept(GlobalPropertiesManagement.METHOD_OF_FAMILY_PLANNING);
		usingCondoms = gp.getConcept(GlobalPropertiesManagement.USING_CONDOMS);
		naturalFamilyPlanning = gp.getConcept(GlobalPropertiesManagement.NATURAL_FAMILY_PLANNING);
		viralLoadConcept = gp.getConcept(GlobalPropertiesManagement.VIRAL_LOAD_TEST);
	}
}