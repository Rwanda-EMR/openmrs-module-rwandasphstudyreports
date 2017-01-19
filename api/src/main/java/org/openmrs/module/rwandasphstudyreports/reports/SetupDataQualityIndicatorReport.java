package org.openmrs.module.rwandasphstudyreports.reports;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Program;
import org.openmrs.api.PatientSetService.TimeModifier;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.ReportingConstants;
import org.openmrs.module.reporting.cohort.definition.AgeCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CodedObsCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CompositionCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.EncounterCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.GenderCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InProgramCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InverseCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.PersonAttributeCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.common.SetComparator;
import org.openmrs.module.reporting.dataset.definition.CohortIndicatorDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.indicator.CohortIndicator;
import org.openmrs.module.reporting.query.encounter.definition.EncounterQuery;
import org.openmrs.module.reporting.query.encounter.definition.SqlEncounterQuery;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.PeriodIndicatorReportDefinition;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rwandareports.dataset.EncounterIndicatorDataSetDefinition;
import org.openmrs.module.rwandareports.indicator.EncounterIndicator;
import org.openmrs.module.rwandareports.renderer.DataQualityWebRenderedForNCDandOncology;
import org.openmrs.module.rwandareports.renderer.DataQualityWebRendererForSites;
import org.openmrs.module.rwandareports.util.Indicators;
import org.openmrs.module.rwandasphstudyreports.Cohorts;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertiesManagement;
import org.openmrs.module.rwandasphstudyreports.Helper;
import org.openmrs.module.rwandasphstudyreports.renderer.SPHDataQualityReportWebRenderer;

public class SetupDataQualityIndicatorReport {

	protected final Log log = LogFactory.getLog(getClass());
	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();

	// properties
	private Program adultHIV;
	private Concept reasonForExitingCare;
	private Concept transferOut;
	private Concept height;
	private Concept weight;
	private List<String> onOrAfterOnOrBeforeParamterNames = new ArrayList<String>();
	private List<Program> allPrograms = new ArrayList<Program>();
	private List<Concept> allArtConceptDrug = new ArrayList<Concept>();
	private Concept onAntiretroviral;

	public void setup() throws Exception {

		setUpProperties();

		createReportDefinitionBySite();
	}

	public void delete() {
		ReportService rs = Context.getService(ReportService.class);
		for (ReportDesign rd : rs.getAllReportDesigns(false)) {
			if ("DataQualityWebRenderer".equals(rd.getName()) || "DataWebRenderer".equals(rd.getName())
					|| "DataWebRendererNCD".equals(rd.getName())) {
				rs.purgeReportDesign(rd);
			}
		}

		Helper.purgeReportDefinition("DQ-Data Quality HIV/TB Report By Site");
		Helper.purgeReportDefinition("DQ-Data Quality HIV/TB Report For All Sites");
		Helper.purgeReportDefinition("DQ-Data Quality NCD/ONCOLOGY Report By Site");
	}

	// DQ Report by Site
	public ReportDefinition createReportDefinitionBySite() throws IOException {
		PeriodIndicatorReportDefinition rd = new PeriodIndicatorReportDefinition();
		rd.removeParameter(ReportingConstants.START_DATE_PARAMETER);
		rd.removeParameter(ReportingConstants.END_DATE_PARAMETER);
		rd.removeParameter(ReportingConstants.LOCATION_PARAMETER);
		rd.addParameter(new Parameter("location", "Location", Location.class));

		rd.setName("DQ-Data Quality HIV/TB Report By Site");

		rd.setupDataSetDefinition();

		rd.setBaseCohortDefinition(Cohorts.createParameterizedLocationCohort("At Location"),
				ParameterizableUtil.createParameterMappings("location=${location}"));

		rd.addDataSetDefinition(createIndicatorsForReports(), null);
		// h.saveReportDefinition(rd);
		rd.addDataSetDefinition(createObsDataSet(),
				ParameterizableUtil.createParameterMappings("location=${location}"));
		Helper.saveReportDefinition(rd);
		createCustomWebRenderer(rd, "DataQualityWebRenderer");

		return rd;
	}

	// DQ Report for all sites
	private ReportDefinition createReportDefinitionAllSites() throws IOException {

		PeriodIndicatorReportDefinition rdsites = new PeriodIndicatorReportDefinition();
		rdsites.removeParameter(ReportingConstants.START_DATE_PARAMETER);
		rdsites.removeParameter(ReportingConstants.END_DATE_PARAMETER);
		rdsites.removeParameter(ReportingConstants.LOCATION_PARAMETER);

		rdsites.setName("DQ-Data Quality HIV/TB Report For All Sites");

		rdsites.setupDataSetDefinition();

		rdsites.addDataSetDefinition(createIndicatorsForReports(), null);
		rdsites.addDataSetDefinition(createObsDataSet(),
				ParameterizableUtil.createParameterMappings("location=${location}"));
		Helper.saveReportDefinition(rdsites);
		createCustomWebRendererForSites(rdsites, "DataWebRenderer");

		return rdsites;

	}

	// DQ Report by Site for NCD
	public ReportDefinition createReportDefinitionBySiteForNCD() throws IOException {
		PeriodIndicatorReportDefinition rd = new PeriodIndicatorReportDefinition();
		rd.removeParameter(ReportingConstants.START_DATE_PARAMETER);
		rd.removeParameter(ReportingConstants.END_DATE_PARAMETER);
		rd.removeParameter(ReportingConstants.LOCATION_PARAMETER);
		rd.addParameter(new Parameter("location", "Location", Location.class));

		rd.setName("DQ-Data Quality NCD/ONCOLOGY Report By Site");

		rd.setupDataSetDefinition();

		rd.setBaseCohortDefinition(Cohorts.createParameterizedLocationCohort("At Location"),
				ParameterizableUtil.createParameterMappings("location=${location}"));

		// rd.addDataSetDefinition(createreportForNCDreport(), null);
		Helper.saveReportDefinition(rd);
		// createCustomWebRendererForNCDorOncology(rd, "DataWebRendererNCD");

		return rd;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public CohortIndicatorDataSetDefinition createIndicatorsForReports() {
		CohortIndicatorDataSetDefinition dataSetDefinition = new CohortIndicatorDataSetDefinition();
		dataSetDefinition.setName("defaultDataSetGlobal");
		List<Program> hivPrograms = new ArrayList<Program>();
		// hivPrograms.add(pediHIV);
		hivPrograms.add(adultHIV);
		InProgramCohortDefinition inHIVprogram = Cohorts.createInProgramParameterizableByDate("DQ: inHIVProgram",
				hivPrograms, "onDate");

		List<Program> PMTCTPrograms = new ArrayList<Program>();
		InProgramCohortDefinition inPMTCTPrograms = Cohorts.createInProgramParameterizableByDate("DQ: inHIVProgram",
				PMTCTPrograms, "onDate");

		GenderCohortDefinition males = Cohorts.createMaleCohortDefinition("Males patients");

		CompositionCohortDefinition malesInPMTCTAndPMTCTCCM = new CompositionCohortDefinition();
		malesInPMTCTAndPMTCTCCM.setName("DQ: Male in PMTCT and PMTCT-combined clinic mother");
		malesInPMTCTAndPMTCTCCM.getSearches().put("1",
				new Mapped(inPMTCTPrograms, ParameterizableUtil.createParameterMappings("onDate=${now}")));
		malesInPMTCTAndPMTCTCCM.getSearches().put("2", new Mapped(males, null));
		malesInPMTCTAndPMTCTCCM.setCompositionString("1 AND 2");

		CohortIndicator malesInPMTCTAndPMTCTCCMIndicator = Indicators.newCountIndicator(
				"PMTCTCCIDQ: Number of Male patients in PMTCT-Pregnancy and Combined Clinic Mother programs",
				malesInPMTCTAndPMTCTCCM, null);

		// ======================================================================================
		// 6. Patients with current ARV regimen with incorrect treatment status
		// (not "on ART)
		// ======================================================================================

		List<Program> PmtctCombinedInfantAndCCMotherProgram = new ArrayList<Program>();
		// PmtctCombinedInfantAndCCMotherProgram.add(pmtctCombinedClinicInfant);
		// PmtctCombinedInfantAndCCMotherProgram.add(pmtctCombinedClinicMother);
		InProgramCohortDefinition inPmtctInfantprogram = Cohorts.createInProgramParameterizableByDate(
				"DQ: in PmtctCombinedInfantProgram", PmtctCombinedInfantAndCCMotherProgram, "onDate");

		SqlCohortDefinition imbIds = Cohorts.getIMBId("DQ:IMB IDs");
		SqlCohortDefinition pciIds = Cohorts.getPciId("DQ: PCI IDs");
		SqlCohortDefinition patswithInvalidImb = Cohorts.getInvalidIMB("DQ: patients with invalid IMB");

		CompositionCohortDefinition patientsWithInvalidIdsnotWIthImbOrPciIds = new CompositionCohortDefinition();
		patientsWithInvalidIdsnotWIthImbOrPciIds.setName("DQ: Invalids but no IMB or PCI IDs");
		patientsWithInvalidIdsnotWIthImbOrPciIds.getSearches().put("1", new Mapped(patswithInvalidImb, null));
		patientsWithInvalidIdsnotWIthImbOrPciIds.getSearches().put("2", new Mapped(imbIds, null));
		patientsWithInvalidIdsnotWIthImbOrPciIds.getSearches().put("3", new Mapped(pciIds, null));
		patientsWithInvalidIdsnotWIthImbOrPciIds.setCompositionString("NOT (2 OR 3) AND 1");

		CohortIndicator patientsWithInvalidIdInd = Indicators.newCountIndicator("patients with invalid id check digit",
				patientsWithInvalidIdsnotWIthImbOrPciIds, null);
		List<String> parameterNames = new ArrayList<String>();
		parameterNames.add("onOrAfter");
		parameterNames.add("onOrBefore");
		EncounterCohortDefinition anyEncounter = Cohorts.createEncounterParameterizedByDate("DQ: any encounter",
				parameterNames);

		CompositionCohortDefinition patientsWithoutIMBOrPCIdentiferWithAnyEncounterLastYearFromNow = new CompositionCohortDefinition();
		patientsWithoutIMBOrPCIdentiferWithAnyEncounterLastYearFromNow.setName(
				"DQ: patients without IMB or Primary Care Identifier ids but with any encounter in last year from now");
		patientsWithoutIMBOrPCIdentiferWithAnyEncounterLastYearFromNow.getSearches().put("1", new Mapped(anyEncounter,
				ParameterizableUtil.createParameterMappings("onOrAfter=${now-12m},onOrBefore=${now}")));
		patientsWithoutIMBOrPCIdentiferWithAnyEncounterLastYearFromNow.getSearches().put("2", new Mapped(imbIds, null));
		patientsWithoutIMBOrPCIdentiferWithAnyEncounterLastYearFromNow.getSearches().put("3", new Mapped(pciIds, null));
		patientsWithoutIMBOrPCIdentiferWithAnyEncounterLastYearFromNow.setCompositionString("NOT (2 OR 3) AND 1");

		CohortIndicator patientsWithIMBOrPCIdentiferanyEncounterLastYearFromNowIndicator = Indicators.newCountIndicator(
				"patients without IMB or Primary Care Identifier ids but with any encounter in last year from now",
				patientsWithoutIMBOrPCIdentiferWithAnyEncounterLastYearFromNow, null);

		AgeCohortDefinition patientsOver100Yearsold = new AgeCohortDefinition(100, null, null);

		CohortIndicator patientsOver100YearsoldIndicator = Indicators
				.newCountIndicator("PMTCTDQ: Number patients Over 100 years old", patientsOver100Yearsold, null);

		String sql6 = "select distinct(p.patient_id) from patient p,person_address pa where p.patient_id=pa.person_id and pa.preferred=1 and p.voided=0 and (pa.state_province is null or pa.county_district is null or pa.city_village is null or pa.address3 is null or pa.address1 is null "
				+ "or pa.state_province='' or pa.county_district='' or pa.address3 is null or pa.address1='' )";
		System.out.println("SQL:************************:" + sql6 + ":************************LQS");

		SqlCohortDefinition patientsWithNoStructuredAddress = new SqlCohortDefinition(sql6);

		CompositionCohortDefinition patientsWithNoStructuredAddressWithAnyEncounterLastYearFromNow = new CompositionCohortDefinition();
		patientsWithNoStructuredAddressWithAnyEncounterLastYearFromNow
				.setName("DQ: patients With No Structured Address and with any encounter in last year from now");
		patientsWithNoStructuredAddressWithAnyEncounterLastYearFromNow.getSearches().put("1", new Mapped(anyEncounter,
				ParameterizableUtil.createParameterMappings("onOrAfter=${now-12m},onOrBefore=${now}")));
		patientsWithNoStructuredAddressWithAnyEncounterLastYearFromNow.getSearches().put("2",
				new Mapped(patientsWithNoStructuredAddress, null));
		patientsWithNoStructuredAddressWithAnyEncounterLastYearFromNow.setCompositionString("1 AND 2");

		CohortIndicator patientsWithNoStructuredAddressWithAnyEncounterLastYearFromNowIndicator = Indicators
				.newCountIndicator(
						"Number of patients With No Structured Address and with any encounter in last year from now",
						patientsWithNoStructuredAddressWithAnyEncounterLastYearFromNow, null);
		List<Program> inAllPrograms = new ArrayList<Program>();
		inAllPrograms.add(adultHIV);
		InProgramCohortDefinition enrolledInAllPrograms = Cohorts
				.createInProgramParameterizableByDate("DQ: enrolledInAllPrograms", inAllPrograms, "onDate");

		CompositionCohortDefinition patientExitedfromcareinPrograms = new CompositionCohortDefinition();
		CodedObsCohortDefinition patientsTransferredOut = Cohorts.createCodedObsCohortDefinition(
				"patientsTransferredOut", onOrAfterOnOrBeforeParamterNames, reasonForExitingCare, transferOut,
				SetComparator.IN, TimeModifier.LAST);

		CompositionCohortDefinition patientTransferedOutinPrograms = new CompositionCohortDefinition();
		patientTransferedOutinPrograms.setName("DQ: Transfered out in All Programs ");
		patientTransferedOutinPrograms.getSearches().put("1",
				new Mapped(enrolledInAllPrograms, ParameterizableUtil.createParameterMappings("onDate=${now}")));
		patientTransferedOutinPrograms.getSearches().put("2",
				new Mapped(patientsTransferredOut, ParameterizableUtil.createParameterMappings("onOrBefore=${now}")));
		patientTransferedOutinPrograms.setCompositionString("1 AND 2");
		CohortIndicator patientTransferedOutinProgramsIndicator = Indicators.newCountIndicator(
				"Number of patients Transfered out but still enrolled in their programs",
				patientTransferedOutinPrograms, null);

		PersonAttributeCohortDefinition pihHealthCenter = new PersonAttributeCohortDefinition();
		pihHealthCenter.setName("Patients at Health Center");
		pihHealthCenter.setAttributeType(Context.getPersonService().getPersonAttributeTypeByName("Health Center"));

		InverseCohortDefinition patientsWithoutHc = new InverseCohortDefinition(pihHealthCenter);
		patientsWithoutHc.setName("patientsWithoutHc");

		CohortIndicator patientWithnohealthCenterIndicator = Indicators
				.newCountIndicator("Number of patients without HC", patientsWithoutHc, null);

		List<Program> inPrograms = new ArrayList<Program>();
		// inPrograms.add(pediHIV);
		inPrograms.add(adultHIV);
		SqlCohortDefinition bmilow = new SqlCohortDefinition();
		String sql2 = "select w.person_id from "
				+ "(select * from (select o.person_id,o.value_numeric from obs o,concept c where o.concept_id= c.concept_id and c.concept_id='"
				+ height.getId() + "' " + "and o.voided=0 "
				+ "order by o.obs_datetime desc) as lastheight group by lastheight.person_id) h,"
				+ "(select * from (select o.person_id,o.value_numeric from obs o,concept c where o.concept_id= c.concept_id and c.concept_id='"
				+ weight.getId() + "' " + "and o.voided=0 "
				+ "order by o.obs_datetime desc) as lastweight group by lastweight.person_id) w "
				+ "where w.person_id=h.person_id and ROUND(((w.value_numeric*10000)/(h.value_numeric*h.value_numeric)),2)>35.0";
		bmilow.setName("bmilow");
		String sql1 = "select w.person_id from "
				+ "(select * from (select o.person_id,o.value_numeric from obs o,concept c where o.concept_id= c.concept_id and c.concept_id='"
				+ height.getId() + "' " + "and o.voided=0 "
				+ "order by o.obs_datetime desc) as lastheight group by lastheight.person_id) h,"
				+ "(select * from (select o.person_id,o.value_numeric from obs o,concept c where o.concept_id= c.concept_id and c.concept_id='"
				+ weight.getId() + "' " + "and o.voided=0 "
				+ "order by o.obs_datetime desc) as lastweight group by lastweight.person_id) w "
				+ "where w.person_id=h.person_id and ROUND(((w.value_numeric*10000)/(h.value_numeric*h.value_numeric)),2)<12.0";

		System.out.println("SQL:************************:" + sql1 + ":************************LQS");

		bmilow.setQuery(sql1);

		SqlCohortDefinition bmihight = new SqlCohortDefinition();
		bmihight.setName("bmihight");
		System.out.println("SQL:************************:" + sql2 + ":************************LQS");

		bmihight.setQuery(sql2);

		AgeCohortDefinition patientsOver15 = new AgeCohortDefinition(15, null, null);

		CompositionCohortDefinition bmimoreorless = new CompositionCohortDefinition();
		bmimoreorless.setName("bmimoreorless");
		bmimoreorless.getSearches().put("1", new Mapped(bmilow, null));
		bmimoreorless.getSearches().put("2", new Mapped(bmihight, null));
		bmimoreorless.getSearches().put("3", new Mapped(patientsOver15, null));
		bmimoreorless.setCompositionString("3 AND (1 OR 2)");

		CohortIndicator patientsWithBMIMoreThan35 = Indicators.newCountIndicator("BMI >15", bmimoreorless, null);

		// ======================================================================================
		// 19. Missing program enrollment start date
		// ======================================================================================

		StringBuilder programs = new StringBuilder();

		int i = 0;

		for (Program program : allPrograms) {
			if (i == 0) {
				programs.append(program.getProgramId());
			} else {
				programs.append(",");
				programs.append(program.getProgramId());
			}
			i++;
		}

		SqlCohortDefinition patientsMissingprogramsEnrolStartDate = new SqlCohortDefinition();
		String sql3 = "select distinct (p.patient_id) from patient_program pp, patient p, program pro where pp.patient_id=p.patient_id and pp.program_id=pro.program_id and pro.program_id in ("
				+ programs.toString() + ") and pp.date_enrolled is null and p.voided=0 and pp.voided=0 ";
		System.out.println("SQL:************************:" + sql3 + ":************************LQS");

		patientsMissingprogramsEnrolStartDate.setQuery(sql3);
		patientsMissingprogramsEnrolStartDate.setName("DQ: Patients in programs but with no program Enrollment dates");

		CohortIndicator patientsMissingprogramsEnrolStartDateindicator = Indicators
				.newCountIndicator("DQ:Number of invalid dates and forms", patientsMissingprogramsEnrolStartDate, null);

		// ======================================================================================
		// 20. Patients <15 in Adult HIV program or PMTCT-combined clinic mother
		// or PMTCT pregnancy
		// ======================================================================================

		StringBuilder allArtConceptDrugIds = new StringBuilder();

		int j = 0;

		for (Concept conc : allArtConceptDrug) {
			if (j == 0) {
				allArtConceptDrugIds.append(conc.getConceptId());
			} else {
				allArtConceptDrugIds.append(",");
				allArtConceptDrugIds.append(conc.getConceptId());
			}
			j++;
		}

		dataSetDefinition.addColumn("5",
				"Patients in PMTCT-pregnancy or PMTCT Combine Clinic - mother while a 'male' patient",
				new Mapped(malesInPMTCTAndPMTCTCCMIndicator, null), "");
		dataSetDefinition.addColumn("9", "Patients with invalid IMB ID", new Mapped(patientsWithInvalidIdInd, null),
				"");
		dataSetDefinition.addColumn("10", "Active patients with no IMB or PHC ID",
				new Mapped(patientsWithIMBOrPCIdentiferanyEncounterLastYearFromNowIndicator, null), "");
		dataSetDefinition.addColumn("12", "Patients over 100 years old",
				new Mapped(patientsOver100YearsoldIndicator, null), "");
		dataSetDefinition.addColumn("13",
				"Patients with a visit in last 12 months who do not have a correctly structured address",
				new Mapped(patientsWithNoStructuredAddressWithAnyEncounterLastYearFromNowIndicator, null), "");
		dataSetDefinition.addColumn("18", "Patients With BMI <12 or >35", new Mapped(patientsWithBMIMoreThan35, null),
				"");
		dataSetDefinition.addColumn("20", "Patients With Missing program enrollment start date",
				new Mapped(patientsMissingprogramsEnrolStartDateindicator, null), "");

		return dataSetDefinition;

	}

	public CohortIndicatorDataSetDefinition createreportForNCDreport() {
		CohortIndicatorDataSetDefinition dataSetDefinition = new CohortIndicatorDataSetDefinition();
		dataSetDefinition.setName("defaultDataSetncd");

		// ======================================================================================
		// 1. Patients with imb invalid identifier type
		// ======================================================================================

		SqlCohortDefinition imbIds = Cohorts.getIMBId("DQ:IMB IDs");
		SqlCohortDefinition pciIds = Cohorts.getPciId("DQ: PCI IDs");
		SqlCohortDefinition patswithInvalidImb = Cohorts.getInvalidIMB("DQ: patients with invalid IMB");

		CompositionCohortDefinition patientsWithInvalidIdsnotWIthImbOrPciIds = new CompositionCohortDefinition();
		patientsWithInvalidIdsnotWIthImbOrPciIds.setName("DQ: Invalids but no IMB or PCI IDs");
		patientsWithInvalidIdsnotWIthImbOrPciIds.getSearches().put("1", new Mapped(patswithInvalidImb, null));
		patientsWithInvalidIdsnotWIthImbOrPciIds.getSearches().put("2", new Mapped(imbIds, null));
		patientsWithInvalidIdsnotWIthImbOrPciIds.getSearches().put("3", new Mapped(pciIds, null));
		patientsWithInvalidIdsnotWIthImbOrPciIds.setCompositionString("NOT (2 OR 3) AND 1");

		CohortIndicator patientsWithInvalidIdInd = Indicators.newCountIndicator("patients with invalid id check digit",
				patientsWithInvalidIdsnotWIthImbOrPciIds, null);

		// ======================================================================================
		// 2. Active patients with no IMB or PHC ID
		// ======================================================================================

		List<String> parameterNames = new ArrayList<String>();
		parameterNames.add("onOrAfter");
		parameterNames.add("onOrBefore");
		EncounterCohortDefinition anyEncounter = Cohorts.createEncounterParameterizedByDate("DQ: any encounter",
				parameterNames);

		CompositionCohortDefinition patientsWithoutIMBOrPCIdentiferWithAnyEncounterLastYearFromNow = new CompositionCohortDefinition();
		patientsWithoutIMBOrPCIdentiferWithAnyEncounterLastYearFromNow.setName(
				"DQ: patients without IMB or Primary Care Identifier ids but with any encounter in last year from now");
		patientsWithoutIMBOrPCIdentiferWithAnyEncounterLastYearFromNow.getSearches().put("1", new Mapped(anyEncounter,
				ParameterizableUtil.createParameterMappings("onOrAfter=${now-12m},onOrBefore=${now}")));
		patientsWithoutIMBOrPCIdentiferWithAnyEncounterLastYearFromNow.getSearches().put("2", new Mapped(imbIds, null));
		patientsWithoutIMBOrPCIdentiferWithAnyEncounterLastYearFromNow.getSearches().put("3", new Mapped(pciIds, null));
		patientsWithoutIMBOrPCIdentiferWithAnyEncounterLastYearFromNow.setCompositionString("NOT (2 OR 3) AND 1");

		CohortIndicator patientsWithIMBOrPCIdentiferanyEncounterLastYearFromNowIndicator = Indicators.newCountIndicator(
				"patients without IMB or Primary Care Identifier ids but with any encounter in last year from now",
				patientsWithoutIMBOrPCIdentiferWithAnyEncounterLastYearFromNow, null);

		// ======================================================================================
		// 3. Patients over 100 years old
		// ======================================================================================

		AgeCohortDefinition patientsOver100Yearsold = new AgeCohortDefinition(100, null, null);

		CohortIndicator patientsOver100YearsoldIndicator = Indicators
				.newCountIndicator("PMTCTDQ: Number patients Over 100 years old", patientsOver100Yearsold, null);

		// ======================================================================================
		// 4. Patients with a visit in the last 12 months who do not have a
		// correctly structured address
		// ======================================================================================
		String sql = "select distinct(p.patient_id) from patient p,person_address pa where p.patient_id=pa.person_id and pa.preferred=1 and p.voided=0 and (pa.state_province is null or pa.county_district is null or pa.city_village is null or pa.address3 is null or pa.address1 is null "
				+ "or pa.state_province='' or pa.county_district='' or pa.address3 is null or pa.address1='' )";
		SqlCohortDefinition patientsWithNoStructuredAddress = new SqlCohortDefinition(sql);

		System.out.println("SQL:************************:" + sql + ":************************LQS");
		CompositionCohortDefinition patientsWithNoStructuredAddressWithAnyEncounterLastYearFromNow = new CompositionCohortDefinition();
		patientsWithNoStructuredAddressWithAnyEncounterLastYearFromNow
				.setName("DQ: patients With No Structured Address and with any encounter in last year from now");
		patientsWithNoStructuredAddressWithAnyEncounterLastYearFromNow.getSearches().put("1", new Mapped(anyEncounter,
				ParameterizableUtil.createParameterMappings("onOrAfter=${now-12m},onOrBefore=${now}")));
		patientsWithNoStructuredAddressWithAnyEncounterLastYearFromNow.getSearches().put("2",
				new Mapped(patientsWithNoStructuredAddress, null));
		patientsWithNoStructuredAddressWithAnyEncounterLastYearFromNow.setCompositionString("1 AND 2");

		CohortIndicator patientsWithNoStructuredAddressWithAnyEncounterLastYearFromNowIndicator = Indicators
				.newCountIndicator(
						"Number of patients With No Structured Address and with any encounter in last year from now",
						patientsWithNoStructuredAddressWithAnyEncounterLastYearFromNow, null);

		// ======================================================================================
		// 5. Patients who status is transferred out but is currently enrolled
		// in program
		// ======================================================================================

		List<Program> inAllPrograms = new ArrayList<Program>();
		/*
		 * inAllPrograms.add(heartFailure); inAllPrograms.add(dmprogram);
		 * inAllPrograms.add(chronicrespiratory);
		 * inAllPrograms.add(hypertention); inAllPrograms.add(epilepsy);
		 */
		InProgramCohortDefinition enrolledInAllPrograms = Cohorts
				.createInProgramParameterizableByDate("DQ: enrolledInAllPrograms", inAllPrograms, "onDate");

		CodedObsCohortDefinition patientsTransferredOut = Cohorts.createCodedObsCohortDefinition(
				"patientsTransferredOut", onOrAfterOnOrBeforeParamterNames, reasonForExitingCare, transferOut,
				SetComparator.IN, TimeModifier.LAST);

		CompositionCohortDefinition patientTransferedOutinPrograms = new CompositionCohortDefinition();
		patientTransferedOutinPrograms.setName("DQ: Transfered out in All Programs ");
		patientTransferedOutinPrograms.getSearches().put("1",
				new Mapped(enrolledInAllPrograms, ParameterizableUtil.createParameterMappings("onDate=${now}")));
		patientTransferedOutinPrograms.getSearches().put("2",
				new Mapped(patientsTransferredOut, ParameterizableUtil.createParameterMappings("onOrBefore=${now}")));
		patientTransferedOutinPrograms.setCompositionString("1 AND 2");
		CohortIndicator patientTransferedOutinProgramsIndicator = Indicators.newCountIndicator(
				"Number of patients Transfered out but still enrolled in their programs",
				patientTransferedOutinPrograms, null);

		// ======================================================================================
		// 6. Patients with no health center
		// ======================================================================================

		PersonAttributeCohortDefinition pihHealthCenter = new PersonAttributeCohortDefinition();
		pihHealthCenter.setName("Patients at Health Center");
		pihHealthCenter.setAttributeType(Context.getPersonService().getPersonAttributeTypeByName("Health Center"));

		InverseCohortDefinition patientsWithoutHc = new InverseCohortDefinition(pihHealthCenter);
		patientsWithoutHc.setName("patientsWithoutHc");

		CohortIndicator patientWithnohealthCenterIndicator = Indicators
				.newCountIndicator("Number of patients without HC", patientsWithoutHc, null);

		// ======================================================================================
		// 7. Patients with no encounter
		// ======================================================================================

		List<Program> inPrograms = new ArrayList<Program>();

		InProgramCohortDefinition enrolledInAllProgramsExceptTb = Cohorts
				.createInProgramParameterizableByDate("DQ: enrolledInAllProgramsExceptTb", inPrograms, "onDate");

		CompositionCohortDefinition patientsWithNoEncounterInProgram = new CompositionCohortDefinition();
		patientsWithNoEncounterInProgram.setName("DQ: patients with no encounter in programs");
		patientsWithNoEncounterInProgram.getSearches().put("1",
				new Mapped(anyEncounter, ParameterizableUtil.createParameterMappings("onOrBefore=${now}")));
		patientsWithNoEncounterInProgram.getSearches().put("2", new Mapped(enrolledInAllProgramsExceptTb,
				ParameterizableUtil.createParameterMappings("onDate=${now}")));
		patientsWithNoEncounterInProgram.setCompositionString("2 AND (NOT 1)");

		CohortIndicator patientsWithNoEncounterInProgramIndicator = Indicators
				.newCountIndicator("Number with no encounter", patientsWithNoEncounterInProgram, null);

		// ======================================================================================
		// 8. Patients with a BMI <12 or >35
		// ======================================================================================

		SqlCohortDefinition bmilow = new SqlCohortDefinition();
		String sql1 = "select w.person_id from "
				+ "(select * from (select o.person_id,o.value_numeric from obs o,concept c where o.concept_id= c.concept_id and c.concept_id='"
				+ height.getId() + "' " + "and o.voided=0 "
				+ "order by o.obs_datetime desc) as lastheight group by lastheight.person_id) h,"
				+ "(select * from (select o.person_id,o.value_numeric from obs o,concept c where o.concept_id= c.concept_id and c.concept_id='"
				+ weight.getId() + "' " + "and o.voided=0 "
				+ "order by o.obs_datetime desc) as lastweight group by lastweight.person_id) w "
				+ "where w.person_id=h.person_id and ROUND(((w.value_numeric*10000)/(h.value_numeric*h.value_numeric)),2)<12.0";

		String sql2 = "select w.person_id from "
				+ "(select * from (select o.person_id,o.value_numeric from obs o,concept c where o.concept_id= c.concept_id and c.concept_id='"
				+ height.getId() + "' " + "and o.voided=0 "
				+ "order by o.obs_datetime desc) as lastheight group by lastheight.person_id) h,"
				+ "(select * from (select o.person_id,o.value_numeric from obs o,concept c where o.concept_id= c.concept_id and c.concept_id='"
				+ weight.getId() + "' " + "and o.voided=0 "
				+ "order by o.obs_datetime desc) as lastweight group by lastweight.person_id) w "
				+ "where w.person_id=h.person_id and ROUND(((w.value_numeric*10000)/(h.value_numeric*h.value_numeric)),2)>35.0";

		System.out.println("SQL1:**********************:" + sql1);
		System.out.println("SQL2:**********************:" + sql2);
		System.out.println("SQL:**********************:LQS");

		bmilow.setName("bmilow");
		bmilow.setQuery(sql1);

		SqlCohortDefinition bmihight = new SqlCohortDefinition();
		bmihight.setName("bmihight");
		bmihight.setQuery(sql2);

		AgeCohortDefinition patientsOver15 = new AgeCohortDefinition(15, null, null);

		CompositionCohortDefinition bmimoreorless = new CompositionCohortDefinition();
		bmimoreorless.setName("bmimoreorless");
		bmimoreorless.getSearches().put("1", new Mapped(bmilow, null));
		bmimoreorless.getSearches().put("2", new Mapped(bmihight, null));
		bmimoreorless.getSearches().put("3", new Mapped(patientsOver15, null));
		bmimoreorless.setCompositionString("3 AND (1 OR 2)");

		CohortIndicator patientsWithBMIMoreThan35 = Indicators.newCountIndicator("BMI >15", bmimoreorless, null);

		dataSetDefinition.addColumn("1", "Patients with invalid IMB ID", new Mapped(patientsWithInvalidIdInd, null),
				"");
		dataSetDefinition.addColumn("2", "Active patients with no IMB or PHC ID",
				new Mapped(patientsWithIMBOrPCIdentiferanyEncounterLastYearFromNowIndicator, null), "");
		dataSetDefinition.addColumn("3", "Patients over 100 years old",
				new Mapped(patientsOver100YearsoldIndicator, null), "");
		dataSetDefinition.addColumn("4",
				"Patients with a visit in last 12 months who do not have a correctly structured address",
				new Mapped(patientsWithNoStructuredAddressWithAnyEncounterLastYearFromNowIndicator, null), "");
		dataSetDefinition.addColumn("8", "Patients With BMI <12 or >35", new Mapped(patientsWithBMIMoreThan35, null),
				"");
		return dataSetDefinition;

	}

	public EncounterIndicatorDataSetDefinition createObsDataSet() {
		EncounterIndicatorDataSetDefinition dsd = new EncounterIndicatorDataSetDefinition();
		dsd.setName("encFuture");
		dsd.addParameter(new Parameter("location", "location", Location.class));

		SqlEncounterQuery patientsWithObsInTheFuture = new SqlEncounterQuery();
		patientsWithObsInTheFuture.setName("patientsWithObsInTheFuture");
		patientsWithObsInTheFuture.setQuery("select distinct encounter_id from encounter "
				+ "where encounter_id in (select distinct e.encounter_id from encounter e, obs o "
				+ "where e.encounter_id=o.encounter_id and o.obs_datetime > e.encounter_datetime "
				+ "and o.voided=0 order by e.encounter_datetime desc) and voided=0");
		patientsWithObsInTheFuture.addParameter(new Parameter("location", "location", Location.class));

		EncounterIndicator patientsWithObsInTheFutureIndicator = new EncounterIndicator();
		patientsWithObsInTheFutureIndicator.setName("Observations in the future (except return visit date)");
		patientsWithObsInTheFutureIndicator.setEncounterQuery(new Mapped<EncounterQuery>(patientsWithObsInTheFuture,
				ParameterizableUtil.createParameterMappings("location=${location}")));

		dsd.addColumn(patientsWithObsInTheFutureIndicator);

		return dsd;
	}

	private void setUpProperties() {
		adultHIV = gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM);

		reasonForExitingCare = gp.getConcept(GlobalPropertiesManagement.REASON_FOR_EXITING_CARE);
		transferOut = gp.getConcept(GlobalPropertiesManagement.TRASNFERED_OUT);

		height = gp.getConcept(GlobalPropertiesManagement.HEIGHT_CONCEPT);

		weight = gp.getConcept(GlobalPropertiesManagement.WEIGHT_CONCEPT);
		onOrAfterOnOrBeforeParamterNames.add("onOrAfter");
		onOrAfterOnOrBeforeParamterNames.add("onOrBefore");
		allPrograms = Context.getProgramWorkflowService().getAllPrograms(false);

		allArtConceptDrug = gp.getConceptsByConceptSet(GlobalPropertiesManagement.ART_DRUGS_SET);
		onAntiretroviral = gp.getConcept(GlobalPropertiesManagement.ON_ART_TREATMENT_STATUS_CONCEPT);
	}

	private ReportDesign createCustomWebRenderer(ReportDefinition rd, String name) throws IOException {
		final ReportDesign design = new ReportDesign();
		design.setName(name);
		design.setReportDefinition(rd);
		design.setRendererType(SPHDataQualityReportWebRenderer.class);
		ReportService rs = Context.getService(ReportService.class);
		return rs.saveReportDesign(design);
	}

	private void createCustomWebRendererForSites(ReportDefinition rd, String name) throws IOException {
		final ReportDesign design = new ReportDesign();
		design.setName(name);
		design.setReportDefinition(rd);
		design.setRendererType(DataQualityWebRendererForSites.class);
		ReportService rs = Context.getService(ReportService.class);
		rs.saveReportDesign(design);
	}

	private void createCustomWebRendererForNCDorOncology(ReportDefinition rd, String name) throws IOException {
		final ReportDesign design = new ReportDesign();
		design.setName(name);
		design.setReportDefinition(rd);
		design.setRendererType(DataQualityWebRenderedForNCDandOncology.class);
		ReportService rs = Context.getService(ReportService.class);
		rs.saveReportDesign(design);
	}

}
