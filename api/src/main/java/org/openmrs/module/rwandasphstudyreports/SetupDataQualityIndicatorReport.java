package org.openmrs.module.rwandasphstudyreports;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.OrderType;
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
import org.openmrs.module.reporting.cohort.definition.InStateCohortDefinition;
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
import org.openmrs.module.rwandareports.renderer.DataQualityReportWebRenderer;
import org.openmrs.module.rwandareports.renderer.DataQualityWebRenderedForNCDandOncology;
import org.openmrs.module.rwandareports.renderer.DataQualityWebRendererForSites;
import org.openmrs.module.rwandareports.util.Indicators;

public class SetupDataQualityIndicatorReport {

	protected final Log log = LogFactory.getLog(getClass());
	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();

	// properties
	private Program pmtct;
	/*private Program pmtctCombinedClinicInfant;
	private Program pmtctCombinedClinicMother;
	private Program pediHIV;*/
	private Program adultHIV;/*
	private Program tb;
	private Program heartFailure;
	private Program dmprogram;
	private Program nutritionpro;
	private Program chronicrespiratory;
	private Program hypertention;
	private Program epilepsy;
	private ProgramWorkflowState adultOnART;
	private ProgramWorkflowState pediOnART;
	private ProgramWorkflowState PMTCTOnART;
	private ProgramWorkflowState diedinAdult;
	private ProgramWorkflowState diedinAdultgroup;
	private ProgramWorkflowState diedInPedi;
	private ProgramWorkflowState diedInTb;
	private ProgramWorkflowState diedInTbgroup;
	private ProgramWorkflowState diedInNutri;
	private ProgramWorkflowState diedInPmtct;
	private ProgramWorkflowState diedInPmtctgroup;
	private List<Concept> tbFirstLineDrugsConcepts;
	private List<Concept> tbSecondLineDrugsConcepts;*/
	private Concept reasonForExitingCare;
	private Concept transferOut;
	private Concept height;
	private Concept weight;
	private List<String> onOrAfterOnOrBeforeParamterNames = new ArrayList<String>();
	//private RelationshipType motherChildRelationship;
	private List<Program> allPrograms = new ArrayList<Program>();
	private List<Concept> allArtConceptDrug = new ArrayList<Concept>();
	private Concept onAntiretroviral;
	private OrderType drugOrderType;
	//private EncounterType transfeInEncounterType;

	public void setup() throws Exception {

		setUpProperties();

		createReportDefinitionBySite();
		//createReportDefinitionAllSites();
		//createReportDefinitionBySiteForNCD();

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

		//rd.addDataSetDefinition(createreportForNCDreport(), null);
		Helper.saveReportDefinition(rd);
		//createCustomWebRendererForNCDorOncology(rd, "DataWebRendererNCD");

		return rd;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public CohortIndicatorDataSetDefinition createIndicatorsForReports() {
		CohortIndicatorDataSetDefinition dataSetDefinition = new CohortIndicatorDataSetDefinition();
		dataSetDefinition.setName("defaultDataSetGlobal");
		// ======================================================================================================================================================================================================
		// 1. Any patients who are in Pediatric HIV program or in the Adult HIV
		// program AND on ART whose accompagnateur is not listed in EMR (or who
		// are incorrectly identified as status 'on antiretrovirals')
		// ======================================================================================================================================================================================================

		List<Program> hivPrograms = new ArrayList<Program>();
		//hivPrograms.add(pediHIV);
		hivPrograms.add(adultHIV);
		InProgramCohortDefinition inHIVprogram = Cohorts.createInProgramParameterizableByDate("DQ: inHIVProgram",
				hivPrograms, "onDate");

		/*List<ProgramWorkflowState> OnARTstates = new ArrayList<ProgramWorkflowState>();
		OnARTstates.add(adultOnART);
		OnARTstates.add(pediOnART);
		InStateCohortDefinition onARTStatusCohort = Cohorts.createInCurrentState("onARTStatus", OnARTstates);
		
		SqlCohortDefinition patientsWithAcc = Cohorts
				.createPatientsWithAccompagnateur("DQ: Patient with accompagnateur", "endDate");

		CompositionCohortDefinition patientsInHIVAndOnARTWithoutAccomp = new CompositionCohortDefinition();
		patientsInHIVAndOnARTWithoutAccomp.setName("DQ: In HIV on ART without Accompagnateur");
		patientsInHIVAndOnARTWithoutAccomp.getSearches().put("1",
				new Mapped(inHIVprogram, ParameterizableUtil.createParameterMappings("onDate=${now}")));
		//patientsInHIVAndOnARTWithoutAccomp.getSearches().put("2", new Mapped(onARTStatusCohort, ParameterizableUtil.createParameterMappings("onDate=${now}")));
		patientsInHIVAndOnARTWithoutAccomp.getSearches().put("3",
				new Mapped(patientsWithAcc, ParameterizableUtil.createParameterMappings("endDate=${now}")));
		patientsInHIVAndOnARTWithoutAccomp.setCompositionString("1 AND 2 AND (NOT 3)");

		CohortIndicator patientsInHIVOnARTWithoutAccompIndicator = Indicators.newCountIndicator(
				"hivOnARTWithoutAccompDQ: Number of patients in HIV program on ART and without Accompagnateur",
				patientsInHIVAndOnARTWithoutAccomp, null);
*/
		// ======================================================================================
		// 2. Patients enrolled in PMTCT Pregnancy for more than 8 months
		// ======================================================================================

		SqlCohortDefinition patientsInPMTCTTooLong = new SqlCohortDefinition(
				"select distinct patient_id from patient_program pp,program p where pp.program_id=p.program_id and p.name='"
						+ pmtct.getName() + "' and DATEDIFF(CURDATE(),pp.date_enrolled) > " + gp.EIGHTANDHALF_MONTHS
						+ " and pp.voided=false and pp.date_completed is null");
		CohortIndicator patientsInPMTCTTooLongIndicator = Indicators
				.newCountIndicator("PMTCTDQ: Number of patients in PMTCT program", patientsInPMTCTTooLong, null);

		// ======================================================================================
		// 3. Patients enrolled in Combined Clinic Mother for more than 19
		// months
		// ======================================================================================
		/*String sql = "select distinct patient_id from patient_program pp,program p where pp.program_id=p.program_id and p.name='"
						+ pmtctCombinedClinicMother.getName() + "' and DATEDIFF(CURDATE(),pp.date_enrolled) > "
						+ gp.NINETEEN_MONTHS + " and pp.voided=false and pp.date_completed is null";
		//SqlCohortDefinition patientsInPMTCTCCMTooLong = new SqlCohortDefinition(sql);*/
		//System.out.println("SQL:************************:" + sql + ":************************LQS");
		
		//CohortIndicator patientsInPMTCTCCMTooLongIndicator = Indicators.newCountIndicator("PMTCTCCMDQ: Number of patients in Combined Clinic Mother program", patientsInPMTCTCCMTooLong, null);

		// ======================================================================================
		// 4. Patients enrolled in Combined Clinic Infant for more than 19
		// months
		// ======================================================================================
/*
		SqlCohortDefinition patientsInPMTCTCCITooLong = new SqlCohortDefinition(
				"select distinct patient_id from patient_program pp,program p where pp.program_id=p.program_id and p.name='"
						+ pmtctCombinedClinicInfant.getName() + "' and DATEDIFF(CURDATE(),pp.date_enrolled) > "
						+ gp.NINETEEN_MONTHS + " and pp.voided=false and pp.date_completed is null");
		CohortIndicator patientsInPMTCTCCITooLongIndicator = Indicators.newCountIndicator(
				"PMTCTCCIDQ: Number of patients in Combined Clinic Infant program", patientsInPMTCTCCITooLong, null);
*/
		// ======================================================================================
		// 5. In PMTCT-pregnancy or PMTCT Combine Clinic - mother while a 'male'
		// patient
		// ======================================================================================

		List<Program> PMTCTPrograms = new ArrayList<Program>();
		PMTCTPrograms.add(pmtct);
		//PMTCTPrograms.add(pmtctCombinedClinicMother);
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
		//PmtctCombinedInfantAndCCMotherProgram.add(pmtctCombinedClinicInfant);
		//PmtctCombinedInfantAndCCMotherProgram.add(pmtctCombinedClinicMother);
		InProgramCohortDefinition inPmtctInfantprogram = Cohorts.createInProgramParameterizableByDate(
				"DQ: in PmtctCombinedInfantProgram", PmtctCombinedInfantAndCCMotherProgram, "onDate");

		//List<ProgramWorkflowState> OnARTstatesAllPrograms = new ArrayList<ProgramWorkflowState>();
		//OnARTstatesAllPrograms.add(adultOnART);
		//OnARTstatesAllPrograms.add(pediOnART);
		//OnARTstatesAllPrograms.add(PMTCTOnART);
		//InStateCohortDefinition onARTStatusAllProgramsCohort = Cohorts.createInCurrentState("onARTStatus",
				//OnARTstatesAllPrograms);

		//TODO resupport this indicator after debugging and getting this report running well
		/*SqlCohortDefinition onARTDrugs = Cohorts.getArtDrugs("On Art Drugs ever");

		CompositionCohortDefinition onARTDrugsNotOnARTStatus = new CompositionCohortDefinition();
		onARTDrugsNotOnARTStatus.setName("DQ: patients On ART Drugs Not On ART Status");
		onARTDrugsNotOnARTStatus.getSearches().put("1", new Mapped(onARTDrugs, null));
		//onARTDrugsNotOnARTStatus.getSearches().put("2",
				//new Mapped(onARTStatusAllProgramsCohort, ParameterizableUtil.createParameterMappings("onDate=${now}")));
		onARTDrugsNotOnARTStatus.getSearches().put("3",
				new Mapped(inPmtctInfantprogram, ParameterizableUtil.createParameterMappings("onDate=${now}")));
		onARTDrugsNotOnARTStatus.setCompositionString("1 AND (NOT 2) AND (NOT (1 AND 3) )");

		CohortIndicator patientsOnARTRegimenNotOnARTStatus = Indicators.newCountIndicator(
				"Patients with current ARV regimen with incorrect treatment status", onARTDrugsNotOnARTStatus, null);

		// ======================================================================================
		// 7. Patients with treatment status 'On Antiretrovirals' without an ARV
		// regimen
		// ======================================================================================

		CompositionCohortDefinition onARTStatusNotOnARTDrugs = new CompositionCohortDefinition();
		onARTStatusNotOnARTDrugs.setName("DQ: patients On ART Status Not On ART Drugs");
		onARTStatusNotOnARTDrugs.getSearches().put("1", new Mapped(onARTDrugs, null));
		//onARTStatusNotOnARTDrugs.getSearches().put("2",
				//new Mapped(onARTStatusAllProgramsCohort, ParameterizableUtil.createParameterMappings("onDate=${now}")));
		onARTStatusNotOnARTDrugs.setCompositionString("2 AND (NOT 1)");

		CohortIndicator patientsOnARTStatusNotOnARTRegimen = Indicators.newCountIndicator(
				"Patients with treatment status 'On Antiretrovirals' without an ARV regimen", onARTStatusNotOnARTDrugs,
				null);
*/
		// ===================================================================================================
		// 8. Patients with current TB regimen not currently in TB program
		// (excluding patients in HF program)
		// ===================================================================================================

		//SqlCohortDefinition onTBDrugs = Cohorts.getTbDrugs("DQ:on Tb Drugs ever");
		/*List<Program> tbPrograms = new ArrayList<Program>();
		//tbPrograms.add(tb);

		InProgramCohortDefinition inTBprogram = Cohorts.createInProgramParameterizableByDate("DQ: inTBprogram",
				tbPrograms, "onDate");

		List<Program> hfPrograms = new ArrayList<Program>();
		//hfPrograms.add(heartFailure);

		InProgramCohortDefinition inHFprogram = Cohorts.createInProgramParameterizableByDate("DQ: inHFprogram",
				hfPrograms, "onDate");

		CompositionCohortDefinition onTBDrugsNotInTBProgHFExcluded = new CompositionCohortDefinition();
		onTBDrugsNotInTBProgHFExcluded.setName("DQ: patients On TB Drugs Not In TB program and HF program excluded");
		onTBDrugsNotInTBProgHFExcluded.getSearches().put("1", new Mapped(onTBDrugs, ParameterizableUtil.createParameterMappings("now=${now}")));
		onTBDrugsNotInTBProgHFExcluded.getSearches().put("2",
				new Mapped(inTBprogram, ParameterizableUtil.createParameterMappings("onDate=${now}")));
		onTBDrugsNotInTBProgHFExcluded.getSearches().put("3",
				new Mapped(inHFprogram, ParameterizableUtil.createParameterMappings("onDate=${now}")));
		onTBDrugsNotInTBProgHFExcluded.setCompositionString("NOT (2 OR 3) AND 1");

		CohortIndicator patientsOnTBRegimenNotInTBProgramHFExcluded = Indicators.newCountIndicator(
				"Patients with current TB regimen who are not in TB Program excluding Heart Failure Program",
				onTBDrugsNotInTBProgHFExcluded, null);
*/
		// ======================================================================================
		// 9. Patients with imb invalid identifier type
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
		// 10. Active patients with no IMB or PHC ID
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
		// 11. On initial TB treatment for longer than 8 months
		// ======================================================================================

		/*SqlCohortDefinition patientsInTBTooLong = new SqlCohortDefinition(
				"select distinct patient_id from patient_program pp,program p where pp.program_id=p.program_id and p.name='"
						+ tb.getName()
						+ "' and pp.date_enrolled< DATE_SUB(pp.date_enrolled,INTERVAL 8 MONTH) and pp.voided=false and pp.date_completed is null");
		String tbFirstLineDrugsConceptIds = null;
		for (Concept concept : tbFirstLineDrugsConcepts) {
			tbFirstLineDrugsConceptIds = tbFirstLineDrugsConceptIds + "," + concept.getId();
		}

		SqlCohortDefinition onTBFirstLineDrugs = new SqlCohortDefinition(
				"select distinct o.patient_id from orders o,concept c where o.concept_id=c.concept_id and c.concept_id in ("
						+ tbFirstLineDrugsConceptIds
						+ ") and o.discontinued=0 and (auto_expire_date is null) and o.voided=0");

		String tbFirstSecondDrugsConceptIds = null;
		for (Concept concept : tbSecondLineDrugsConcepts) {
			tbFirstSecondDrugsConceptIds = tbFirstSecondDrugsConceptIds + "," + concept.getId();
		}

		SqlCohortDefinition onTBSecondLineDrugs = new SqlCohortDefinition(
				"select distinct o.patient_id from orders o,concept c where o.concept_id=c.concept_id and c.concept_id in ("
						+ tbFirstSecondDrugsConceptIds
						+ ") and o.discontinued=0 and (auto_expire_date is null) and o.voided=0");

		CompositionCohortDefinition patientsInTBTooLongOnFirstLineRegimenNotSecondLineRegimen = new CompositionCohortDefinition();
		patientsInTBTooLongOnFirstLineRegimenNotSecondLineRegimen
				.setName("DQ: patients In TB Program Too long on First Line Regimen and Not on Second Line regimen");
		patientsInTBTooLongOnFirstLineRegimenNotSecondLineRegimen.getSearches().put("1",
				new Mapped(patientsInTBTooLong, null));
		patientsInTBTooLongOnFirstLineRegimenNotSecondLineRegimen.getSearches().put("2",
				new Mapped(onTBFirstLineDrugs, null));
		patientsInTBTooLongOnFirstLineRegimenNotSecondLineRegimen.getSearches().put("3",
				new Mapped(onTBSecondLineDrugs, null));
		patientsInTBTooLongOnFirstLineRegimenNotSecondLineRegimen.setCompositionString(" 1 AND 2 AND (NOT 3) ");

		CohortIndicator patientsInTBTooLongOnFirstLineRegimenNotSecondLineRegimenIndicator = Indicators
				.newCountIndicator(
						"PMTCTDQ: Number patients In TB Program Too long on First Line Regimen and Not on Second Line regimen",
						patientsInTBTooLongOnFirstLineRegimenNotSecondLineRegimen, null);
*/		// ======================================================================================
		// 12. Patients over 100 years old
		// ======================================================================================

		AgeCohortDefinition patientsOver100Yearsold = new AgeCohortDefinition(100, null, null);

		CohortIndicator patientsOver100YearsoldIndicator = Indicators
				.newCountIndicator("PMTCTDQ: Number patients Over 100 years old", patientsOver100Yearsold, null);

		// ======================================================================================
		// 13. Patients with a visit in the last 12 months who do not have a
		// correctly structured address
		// ======================================================================================
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
		// ======================================================================================
		// 14. Patients whose status status 'deceased' but enrolled in program
		// ======================================================================================

		// Patients with died state in all programs
		//List<ProgramWorkflowState> diedStates = new ArrayList<ProgramWorkflowState>();
		/*diedStates.add(diedinAdult);
		diedStates.add(diedinAdultgroup);
		diedStates.add(diedInPedi);
		diedStates.add(diedInTb);
		diedStates.add(diedInTbgroup);
		diedStates.add(diedInNutri);
		diedStates.add(diedInPmtct);
		diedStates.add(diedInPmtctgroup);
		InStateCohortDefinition diedStateInAllProgramsCohort = Cohorts.createInCurrentState("diedState", diedStates);
*/
		// died but still active in programs
		List<Program> inAllPrograms = new ArrayList<Program>();
		//inAllPrograms.add(pediHIV);
		inAllPrograms.add(adultHIV);
		//inAllPrograms.add(tb);
		//inAllPrograms.add(nutritionpro);
		inAllPrograms.add(pmtct);
		InProgramCohortDefinition enrolledInAllPrograms = Cohorts
				.createInProgramParameterizableByDate("DQ: enrolledInAllPrograms", inAllPrograms, "onDate");

		CompositionCohortDefinition patientExitedfromcareinPrograms = new CompositionCohortDefinition();
		/*patientExitedfromcareinPrograms.setName("DQ: Exited from care in All Programs ");
		patientExitedfromcareinPrograms.getSearches().put("1",
				new Mapped(enrolledInAllPrograms, ParameterizableUtil.createParameterMappings("onDate=${now}")));
		patientExitedfromcareinPrograms.getSearches().put("2",
				new Mapped(diedStateInAllProgramsCohort, ParameterizableUtil.createParameterMappings("onDate=${now}")));
		patientExitedfromcareinPrograms.setCompositionString("1 AND 2");
		CohortIndicator patientExitedfromcareinProgramsIndicator = Indicators.newCountIndicator(
				"Number of patients With status decease but still enrolled in their programs",
				patientExitedfromcareinPrograms, null);*/
		// ======================================================================================
		// 17. Patients who status is transferred out but is currently enrolled
		// in program
		// ======================================================================================

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
		// 16. Patients with no health center
		// ======================================================================================

		PersonAttributeCohortDefinition pihHealthCenter = new PersonAttributeCohortDefinition();
		pihHealthCenter.setName("Patients at Health Center");
		pihHealthCenter.setAttributeType(Context.getPersonService().getPersonAttributeTypeByName("Health Center"));

		InverseCohortDefinition patientsWithoutHc = new InverseCohortDefinition(pihHealthCenter);
		patientsWithoutHc.setName("patientsWithoutHc");

		CohortIndicator patientWithnohealthCenterIndicator = Indicators
				.newCountIndicator("Number of patients without HC", patientsWithoutHc, null);

		// ======================================================================================
		// 17. Patients with no encounter
		// ======================================================================================

		List<Program> inPrograms = new ArrayList<Program>();
		//inPrograms.add(pediHIV);
		inPrograms.add(adultHIV);
		/*inPrograms.add(nutritionpro);
		inPrograms.add(pmtct);
		inPrograms.add(heartFailure);
		inPrograms.add(dmprogram);
		inPrograms.add(chronicrespiratory);
		inPrograms.add(hypertention);
		inPrograms.add(epilepsy);
		InProgramCohortDefinition enrolledInAllProgramsExceptTb = Cohorts
				.createInProgramParameterizableByDate("DQ: enrolledInAllProgramsExceptTb", inPrograms, "onDate");//NO, make it only HIV program

		CompositionCohortDefinition patientsWithNoEncounterInProgram = new CompositionCohortDefinition();
		patientsWithNoEncounterInProgram.setName("DQ: patients with no encounter in programs");
		patientsWithNoEncounterInProgram.getSearches().put("1",
				new Mapped(anyEncounter, ParameterizableUtil.createParameterMappings("onOrBefore=${now}")));
		patientsWithNoEncounterInProgram.getSearches().put("2", new Mapped(enrolledInAllProgramsExceptTb,
				ParameterizableUtil.createParameterMappings("onDate=${now}")));
		patientsWithNoEncounterInProgram.setCompositionString("2 AND (NOT 1)");

		CohortIndicator patientsWithNoEncounterInProgramIndicator = Indicators
				.newCountIndicator("Number with no encounter", patientsWithNoEncounterInProgram, null);
*/
		// ======================================================================================
		// 18. Patients with a BMI <12 or >35
		// ======================================================================================

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

		SqlCohortDefinition patientsOnArtbeforeProgramEnrollmentDate = new SqlCohortDefinition();
		patientsOnArtbeforeProgramEnrollmentDate.setName("patientsOnArtbeforeProgramEnrollmentDate");
		String sql7 = "select firstDrugOder.patient_id from patient_program pp,(select * from (select o.patient_id,o.date_activated from orders o, order_type ot where o.order_type_id =ot.order_type_id and o.order_type_id="
						+ drugOrderType.getOrderTypeId() + " and o.concept_id in (" + allArtConceptDrugIds.toString()
						+ ") and o.voided=0 and ot.retired=0 order by o.date_activated) as orderedOrders group by orderedOrders.patient_id) as firstDrugOder where pp.patient_id=firstDrugOder.patient_id and firstDrugOder.date_activated < pp.date_enrolled and pp.date_enrolled is not null and pp.date_completed is null and pp.voided=0";
		System.out.println("SQL:************************:" + sql7 + ":************************LQS");
		
		patientsOnArtbeforeProgramEnrollmentDate.setQuery(sql7);

		StringBuilder allProgramsIdsWithOnAntiRetroviralState = new StringBuilder();
		int k = 0;

		/*for (Program p : allPrograms) {
			programworkflow: for (ProgramWorkflow pw : p.getAllWorkflows()) {
				for (ProgramWorkflowState pws : pw.getStates()) {
					if (pws.isRetired() == false && pws != null && pws.getConcept().getName().toString()
							.equalsIgnoreCase(onAntiretroviral.getName().toString()) && k == 0) {
						allProgramsIdsWithOnAntiRetroviralState.append(p.getProgramId());
						k++;
						break programworkflow;
					} else if (pws.isRetired() == false && pws != null && pws.getConcept().getName().toString()
							.equalsIgnoreCase(onAntiretroviral.getName().toString())) {
						allProgramsIdsWithOnAntiRetroviralState.append(",");
						allProgramsIdsWithOnAntiRetroviralState.append(p.getProgramId());
						k++;
						break programworkflow;
					}
				}
			}

		}*/

		/*SqlCohortDefinition patientsInOnArtStatebeforeProgramEnrollmentDate = new SqlCohortDefinition();
		patientsInOnArtStatebeforeProgramEnrollmentDate.setName("patientsInOnArtStatebeforeHivEnrollment");
		patientsInOnArtStatebeforeProgramEnrollmentDate.setQuery(
				"select ppordered.patient_id from (select pp.patient_id,ps.start_date,pp.date_enrolled from patient_program pp,program_workflow pw,program_workflow_state pws,patient_state ps where pp.program_id in ("
						+ allProgramsIdsWithOnAntiRetroviralState.toString()
						+ ") and pp.program_id= pw.program_id and pw.program_workflow_id=pws.program_workflow_id and pp.patient_program_id= ps.patient_program_id and pws.concept_id="
						+ onAntiretroviral.getConceptId()
						+ " and ps.start_date < pp.date_enrolled and pp.date_enrolled is not null and ps.end_date is null and pp.date_completed is null and pp.voided=0  and pw.retired=0 and ps.voided=0 and pws.retired=0 order by pp.date_enrolled) as ppordered group by ppordered.patient_id");
*/
		/*SqlCohortDefinition patientswithouttransferInForm = new SqlCohortDefinition();
		patientswithouttransferInForm.setName("patientswithouttransferInForm");
		patientswithouttransferInForm.setQuery("select distinct patient_id from encounter where encounter_type="
				+ transfeInEncounterType.getEncounterTypeId() + " and form_id is not null and voided=0");
*/
		/*CompositionCohortDefinition patientsWithinvaliddatesandmissingforms = new CompositionCohortDefinition();
		patientsWithinvaliddatesandmissingforms.setName("DQ: patients with invalid dates and missing transfer in form");
		patientsWithinvaliddatesandmissingforms.getSearches().put("1",
				new Mapped(patientsOnArtbeforeProgramEnrollmentDate, null));
		//patientsWithinvaliddatesandmissingforms.getSearches().put("2", new Mapped(patientsInOnArtStatebeforeProgramEnrollmentDate, null));
		//patientsWithinvaliddatesandmissingforms.getSearches().put("3", new Mapped(patientswithouttransferInForm, null));
		patientsWithinvaliddatesandmissingforms.setCompositionString("(1 OR 2) AND (NOT 3)");

		CohortIndicator patientsOnArtbeforeHivEnrollmentIndicator = Indicators
				.newCountIndicator("Number of invalid dates and forms", patientsWithinvaliddatesandmissingforms, null);
*/
		// =====================================================================================
		// 21. PMTCT Infants without a mother relationship
		// =====================================================================================
/*
		SqlCohortDefinition infantsWithNoMotherAcc = new SqlCohortDefinition();
		infantsWithNoMotherAcc.setQuery(
				" select distinct rel.person_b FROM relationship rel, relationship_type relt, person pe WHERE rel.relationship=relt.relationship_type_id AND relt.relationship_type_id="
						+ motherChildRelationship.getRelationshipTypeId()
						+ " AND rel.voided=0 AND pe.voided=0 AND relt.retired=0 ");
		infantsWithNoMotherAcc.setName("DQ: Patients With no Mothers Relationship");

		CompositionCohortDefinition infantsInPmtctClinicInfant = new CompositionCohortDefinition();
		infantsInPmtctClinicInfant.setName(
				"DQ: Patients currently enrolled in the PMTCT Combined Clinic ÔøΩ Infant program who donÔøΩt have a non-voided Mother/Child relationship");
		infantsInPmtctClinicInfant.getSearches().put("1", new Mapped(infantsWithNoMotherAcc, null));
		infantsInPmtctClinicInfant.getSearches().put("2",
				new Mapped(inPmtctInfantprogram, ParameterizableUtil.createParameterMappings("onDate=${now}")));
		infantsInPmtctClinicInfant.setCompositionString("2 AND (NOT 1)");

		CohortIndicator infantsWithNoMotherAccIndicator = Indicators
				.newCountIndicator("DQ:Number of invalid dates and forms", infantsInPmtctClinicInfant, null);

		// =====================================================================================
		// 22. PMTCT Infants without a treatment group
		// =====================================================================================

		String infantprogramWorkflowsStates = null;
		StringBuilder infantTreatmentGroupStates = new StringBuilder();
*/
		/*for (ProgramWorkflow programWorkflow : pmtctCombinedClinicInfant.getAllWorkflows()) {
			if (programWorkflow.getConcept().getName().toString().equalsIgnoreCase("TREATMENT GROUP")) {
				int check = 0;
				for (ProgramWorkflowState programWorkflowState : programWorkflow.getStates()) {
					if (check != 0) {
						infantTreatmentGroupStates.append(",");
						infantTreatmentGroupStates.append(programWorkflowState.getId());
					} else {
						infantTreatmentGroupStates.append(programWorkflowState.getId());
					}
					check++;
				}
			}
		}*/
		/*infantprogramWorkflowsStates = infantTreatmentGroupStates.toString();
		String sql4 = "SELECT distinct p.patient_id FROM patient p, patient_program pp, patient_state ps "
						+ "WHERE ps.patient_program_id = pp.patient_program_id AND pp.patient_id = p.patient_id "
						+ "AND ps.state in (" + infantprogramWorkflowsStates
						+ " ) AND p.voided = 0 AND pp.voided = 0 AND ps.voided = 0 ";
		System.out.println("SQL:************************:" + sql4 + ":************************LQS");
		
		SqlCohortDefinition infantsInGroups = new SqlCohortDefinition(sql4);*/

		/*InProgramCohortDefinition infantProgram = Cohorts.createInProgramParameterizableByDate("DQ: infantProgram",
				pmtctCombinedClinicInfant, "onDate");
		CompositionCohortDefinition infantNotIngroupsEverComp = new CompositionCohortDefinition();
		infantNotIngroupsEverComp.setName("DQ: Patients currently in exposed Infant Program not in Groups");
		infantNotIngroupsEverComp.getSearches().put("1",
				new Mapped(infantProgram, ParameterizableUtil.createParameterMappings("onDate=${now}")));
		infantNotIngroupsEverComp.getSearches().put("2", new Mapped(infantsInGroups, null));
		infantNotIngroupsEverComp.setCompositionString("1 AND (NOT 2)");
		CohortIndicator infantNotIngroupsEverInd = Indicators.newCountIndicator("DQ:infantsWithNoTreatmentGroup",
				infantNotIngroupsEverComp, null);*/

		// =====================================================================================
		// 23. PMTCT Pregnancy without a treatment group
		// =====================================================================================
		/*String motherprogramWorkflowsStates = null;
		StringBuilder treatmentGroupStates = new StringBuilder();

		for (ProgramWorkflow programWorkflow : pmtct.getAllWorkflows()) {
			if (programWorkflow.getConcept().getName().toString().equalsIgnoreCase("TREATMENT GROUP")) {
				int check = 0;
				for (ProgramWorkflowState programWorkflowState : programWorkflow.getStates()) {
					if (check != 0) {
						treatmentGroupStates.append(",");
						treatmentGroupStates.append(programWorkflowState.getId());
					} else {
						treatmentGroupStates.append(programWorkflowState.getId());
					}
					check++;
				}
			}
		}
		motherprogramWorkflowsStates = treatmentGroupStates.toString();
		String sql5 = "SELECT distinct p.patient_id FROM patient p, patient_program pp, patient_state ps "
						+ "WHERE ps.patient_program_id = pp.patient_program_id AND pp.patient_id = p.patient_id "
						+ "AND ps.state in (" + motherprogramWorkflowsStates
						+ " ) AND p.voided = 0 AND pp.voided = 0 AND ps.voided = 0 ";
		System.out.println("SQL:************************:" + sql5 + ":************************LQS");
		
		SqlCohortDefinition pregnancyMothersInGroups = new SqlCohortDefinition(sql5);

		InProgramCohortDefinition pregnantMotherProgram = Cohorts
				.createInProgramParameterizableByDate("DQ: pregnantMotherProgram", pmtct, "onDate");
		CompositionCohortDefinition pregMotherIngroupsEverComp = new CompositionCohortDefinition();
		pregMotherIngroupsEverComp.setName("DQ: Patients currently in PMTCT Pregnancy Program not in Groups");
		pregMotherIngroupsEverComp.getSearches().put("1",
				new Mapped(pregnantMotherProgram, ParameterizableUtil.createParameterMappings("onDate=${now}")));
		pregMotherIngroupsEverComp.getSearches().put("2", new Mapped(pregnancyMothersInGroups, null));
		pregMotherIngroupsEverComp.setCompositionString("1 AND (NOT 2)");
		CohortIndicator ccMotherNotIngroupsEverInd = Indicators.newCountIndicator("DQ:cCMotherWithNotreatmentGroup",
				pregMotherIngroupsEverComp, null);*/
		// =====================================================================================

		// end of DQ applied to all sites

		// ======================================================================================
		// Add global filters to the report
		// ======================================================================================

		/*dataSetDefinition.addColumn("1",
				"Patients who are in Pediatric or Adult HIV program AND on ART whose accompagnateur is not listed in EMR",
				new Mapped(patientsInHIVOnARTWithoutAccompIndicator, null), "");
		*/dataSetDefinition.addColumn("2", "Patients enrolled in PMTCT Pregnancy for more than 8 months and a half",
				new Mapped(patientsInPMTCTTooLongIndicator, null), "");
		//dataSetDefinition.addColumn("3", "Patients enrolled in Combined Clinic Mother for more than 19 months", new Mapped(patientsInPMTCTCCMTooLongIndicator, null), "");
		//dataSetDefinition.addColumn("4", "Patients enrolled in Combined Clinic Infant for more than 19 months", new Mapped(patientsInPMTCTCCITooLongIndicator, null), "");
		dataSetDefinition.addColumn("5",
				"Patients in PMTCT-pregnancy or PMTCT Combine Clinic - mother while a 'male' patient",
				new Mapped(malesInPMTCTAndPMTCTCCMIndicator, null), "");
		/*dataSetDefinition.addColumn("6",
				"Patients with current ARV regimen with incorrect treatment status(not 'On ART')",
				new Mapped(patientsOnARTRegimenNotOnARTStatus, null), "");
		dataSetDefinition.addColumn("7", "Patients with treatment status 'On Antiretrovirals' without an ARV regimen",
				new Mapped(patientsOnARTStatusNotOnARTRegimen, null), "");*/
		/*dataSetDefinition.addColumn("8",
				"Patients with current TB regimen not currently in TB program (excluding patients in HF program)",
				new Mapped(patientsOnTBRegimenNotInTBProgramHFExcluded, null), "");*/
		dataSetDefinition.addColumn("9", "Patients with invalid IMB ID", new Mapped(patientsWithInvalidIdInd, null),
				"");
		dataSetDefinition.addColumn("10", "Active patients with no IMB or PHC ID",
				new Mapped(patientsWithIMBOrPCIdentiferanyEncounterLastYearFromNowIndicator, null), "");
		/*dataSetDefinition.addColumn("11", "On initial TB treatment for longer than 8 months",
				new Mapped(patientsInTBTooLongOnFirstLineRegimenNotSecondLineRegimenIndicator, null), "");*/
		dataSetDefinition.addColumn("12", "Patients over 100 years old",
				new Mapped(patientsOver100YearsoldIndicator, null), "");
		dataSetDefinition.addColumn("13",
				"Patients with a visit in last 12 months who do not have a correctly structured address",
				new Mapped(patientsWithNoStructuredAddressWithAnyEncounterLastYearFromNowIndicator, null), "");
		/*dataSetDefinition.addColumn("14", "Patients whose status deceased but enrolled in program",
				new Mapped(patientExitedfromcareinProgramsIndicator, null), "");*/
		//dataSetDefinition.addColumn("15",
			//	"Patients who status is transferred out but is currently enrolled in program ",
				//new Mapped(patientTransferedOutinProgramsIndicator, null), "");
		//dataSetDefinition.addColumn("16", "Patients with no health center",
				//new Mapped(patientWithnohealthCenterIndicator, null), "");
		//dataSetDefinition.addColumn("17", "Patients with no encounter", new Mapped(patientsWithNoEncounterInProgramIndicator, null), "");
		dataSetDefinition.addColumn("18", "Patients With BMI <12 or >35", new Mapped(patientsWithBMIMoreThan35, null),
				"");
		/*dataSetDefinition.addColumn("19",
				"Patients whose ART start date or 'on ART' workflow are before any programs began AND do not have a 'transfer in form",
				new Mapped(patientsOnArtbeforeHivEnrollmentIndicator, null), "");
		*/dataSetDefinition.addColumn("20", "Patients With Missing program enrollment start date",
				new Mapped(patientsMissingprogramsEnrolStartDateindicator, null), "");
		/*dataSetDefinition.addColumn("21",
				"Patients currently enrolled in the PMTCT Combined Clinic Infant program who don't have a non-voided Mother/Child relationship",
				new Mapped(infantsWithNoMotherAccIndicator, null), "");*/
		//dataSetDefinition.addColumn("22", "Patient in Combined Clinic Infant  with no treatment group", new Mapped(infantNotIngroupsEverInd, null), "");
		/*dataSetDefinition.addColumn("23", "Patient in PMTCT Pregnancy program  with no treatment group",
				new Mapped(ccMotherNotIngroupsEverInd, null), "");*/

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
		/*inAllPrograms.add(heartFailure);
		inAllPrograms.add(dmprogram);
		inAllPrograms.add(chronicrespiratory);
		inAllPrograms.add(hypertention);
		inAllPrograms.add(epilepsy);*/
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
		/*inPrograms.add(heartFailure);
		inPrograms.add(dmprogram);
		inPrograms.add(chronicrespiratory);
		inPrograms.add(hypertention);
		inPrograms.add(epilepsy);*/
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

		// ======================================================================================
		// 9. Missing program enrollment start date
		// ======================================================================================

		/*StringBuilder programs = new StringBuilder();

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
		patientsMissingprogramsEnrolStartDate.setQuery(
				"select distinct (p.patient_id) from patient_program pp, patient p, program pro where pp.patient_id=p.patient_id and pp.program_id=pro.program_id and pro.program_id in ("
						+ programs.toString() + ") and pp.date_enrolled is null and p.voided=0 and pp.voided=0 ");
		patientsMissingprogramsEnrolStartDate.setName("DQ: Patients in programs but with no program Enrollment dates");

		CohortIndicator patientsMissingprogramsEnrolStartDateindicator = Indicators
				.newCountIndicator("DQ:Number of invalid dates and forms", patientsMissingprogramsEnrolStartDate, null);
*/
		// end of DQ applied to all sites

		// ======================================================================================
		// Add global filters to the report
		// ======================================================================================

		dataSetDefinition.addColumn("1", "Patients with invalid IMB ID", new Mapped(patientsWithInvalidIdInd, null),
				"");
		dataSetDefinition.addColumn("2", "Active patients with no IMB or PHC ID",
				new Mapped(patientsWithIMBOrPCIdentiferanyEncounterLastYearFromNowIndicator, null), "");
		dataSetDefinition.addColumn("3", "Patients over 100 years old",
				new Mapped(patientsOver100YearsoldIndicator, null), "");
		dataSetDefinition.addColumn("4",
				"Patients with a visit in last 12 months who do not have a correctly structured address",
				new Mapped(patientsWithNoStructuredAddressWithAnyEncounterLastYearFromNowIndicator, null), "");
		//dataSetDefinition.addColumn("5", "Patients who status is transferred out but is currently enrolled in program ",
				//new Mapped(patientTransferedOutinProgramsIndicator, null), "");
		/*dataSetDefinition.addColumn("6", "Patients with no health center",
				new Mapped(patientWithnohealthCenterIndicator, null), "");
		dataSetDefinition.addColumn("7", "Patients with no encounter",
				new Mapped(patientsWithNoEncounterInProgramIndicator, null), "");
		dataSetDefinition.addColumn("8", "Patients With BMI <12 or >35", new Mapped(patientsWithBMIMoreThan35, null),
				"");*/
		//dataSetDefinition.addColumn("9", "Patients With Missing program enrollment start date",
				//new Mapped(patientsMissingprogramsEnrolStartDateindicator, null), "");

		return dataSetDefinition;

	}

	public EncounterIndicatorDataSetDefinition createObsDataSet() {
		EncounterIndicatorDataSetDefinition dsd = new EncounterIndicatorDataSetDefinition();
		dsd.setName("encFuture");
		dsd.addParameter(new Parameter("location", "location", Location.class));

		SqlEncounterQuery patientsWithObsInTheFuture = new SqlEncounterQuery();
		patientsWithObsInTheFuture.setName("patientsWithObsInTheFuture");
		// patientsWithObsInTheFuture.setQuery("select enc.encounter_id from
		// encounter enc, obs o where enc.encounter_id=o.encounter_id and
		// o.person_id=enc.patient_id and o.obs_datetime >
		// enc.encounter_datetime and o.voided=0 and o.voided=0 order by
		// enc.encounter_datetime desc");
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
		pmtct = gp.getProgram(GlobalPropertiesManagement.PMTCT_PREGNANCY_PROGRAM);
		//pmtctCombinedClinicInfant = gp.getProgram(GlobalPropertiesManagement.PMTCT_COMBINED_CLINIC_PROGRAM);
		//pmtctCombinedClinicMother = gp.getProgram(GlobalPropertiesManagement.PMTCT_COMBINED_MOTHER_PROGRAM);
		//pediHIV = gp.getProgram(GlobalPropertiesManagement.PEDI_HIV_PROGRAM);
		adultHIV = gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
		//tb = gp.getProgram(GlobalPropertiesManagement.TB_PROGRAM);
		// oncologyProgram =
		// gp.getProgram(GlobalPropertiesManagement.ONCOLOGY_PROGRAM);
		//heartFailure = gp.getProgram(GlobalPropertiesManagement.HEART_FAILURE_PROGRAM_NAME);
		//dmprogram = gp.getProgram(GlobalPropertiesManagement.DM_PROGRAM);
		//nutritionpro = gp.getProgram(GlobalPropertiesManagement.NUTRITION_PROGRAM);
		//chronicrespiratory = gp.getProgram(GlobalPropertiesManagement.CHRONIC_RESPIRATORY_PROGRAM);
		//hypertention = gp.getProgram(GlobalPropertiesManagement.HYPERTENSION_PROGRAM);
		//epilepsy = gp.getProgram(GlobalPropertiesManagement.EPILEPSY_PROGRAM);
		/*adultOnART = gp.getProgramWorkflowState(GlobalPropertiesManagement.ON_ANTIRETROVIRALS_STATE,
				GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW, GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
		pediOnART = gp.getProgramWorkflowState(GlobalPropertiesManagement.ON_ANTIRETROVIRALS_STATE,
				GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW, GlobalPropertiesManagement.PEDI_HIV_PROGRAM);
		PMTCTOnART = gp.getProgramWorkflowState(GlobalPropertiesManagement.ON_ANTIRETROVIRALS_STATE,
				GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW,
				GlobalPropertiesManagement.PMTCT_PREGNANCY_PROGRAM);
		tbFirstLineDrugsConcepts = gp.getConceptsByConceptSet(GlobalPropertiesManagement.TB_FIRST_LINE_DRUG_SET);
		tbSecondLineDrugsConcepts = gp.getConceptsByConceptSet(GlobalPropertiesManagement.TB_SECOND_LINE_DRUG_SET);*/
		reasonForExitingCare = gp.getConcept(GlobalPropertiesManagement.REASON_FOR_EXITING_CARE);
		transferOut = gp.getConcept(GlobalPropertiesManagement.TRASNFERED_OUT);
		/*diedinAdult = gp.getProgramWorkflowState(GlobalPropertiesManagement.PATIENT_DIED_STATE,
				GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW, GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
		diedinAdultgroup = gp.getProgramWorkflowState(GlobalPropertiesManagement.PATIENT_DIED_STATE,
				GlobalPropertiesManagement.TREATMENT_GROUP_WORKFLOW, GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
		diedInPedi = gp.getProgramWorkflowState(GlobalPropertiesManagement.PATIENT_DIED_STATE,
				GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW, GlobalPropertiesManagement.PEDI_HIV_PROGRAM);
		diedInTb = gp.getProgramWorkflowState(GlobalPropertiesManagement.PATIENT_DIED_STATE,
				GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW, GlobalPropertiesManagement.TB_PROGRAM);
		diedInTbgroup = gp.getProgramWorkflowState(GlobalPropertiesManagement.PATIENT_DIED_STATE,
				GlobalPropertiesManagement.TB_TREATMENT_GROUP_WORKFLOW, GlobalPropertiesManagement.TB_PROGRAM);
		diedInNutri = gp.getProgramWorkflowState(GlobalPropertiesManagement.PATIENT_DIED_STATE,
				GlobalPropertiesManagement.ASSISTANCE_STATUS_WORKFLOW, GlobalPropertiesManagement.NUTRITION_PROGRAM);
		diedInPmtct = gp.getProgramWorkflowState(GlobalPropertiesManagement.PATIENT_DIED_STATE,
				GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW,
				GlobalPropertiesManagement.PMTCT_PREGNANCY_PROGRAM);
		diedInPmtctgroup = gp.getProgramWorkflowState(GlobalPropertiesManagement.PATIENT_DIED_STATE,
				GlobalPropertiesManagement.PREGNANCY_STATUS_WORKFLOW,
				GlobalPropertiesManagement.PMTCT_PREGNANCY_PROGRAM);*/
		height = gp.getConcept(GlobalPropertiesManagement.HEIGHT_CONCEPT);

		weight = gp.getConcept(GlobalPropertiesManagement.WEIGHT_CONCEPT);
		onOrAfterOnOrBeforeParamterNames.add("onOrAfter");
		onOrAfterOnOrBeforeParamterNames.add("onOrBefore");
		//motherChildRelationship = gp.getRelationshipType(GlobalPropertiesManagement.MOTHER_RELATIONSHIP);
		allPrograms = Context.getProgramWorkflowService().getAllPrograms(false);

		allArtConceptDrug = gp.getConceptsByConceptSet(GlobalPropertiesManagement.ART_DRUGS_SET);
		onAntiretroviral = gp.getConcept(GlobalPropertiesManagement.ON_ART_TREATMENT_STATUS_CONCEPT);
		drugOrderType = gp.getOrderType(GlobalPropertiesManagement.DRUG_ORDER_TYPE);
		//transfeInEncounterType = gp.getEncounterType(GlobalPropertiesManagement.TRANSFER_IN_ENCOUNTER_TYPE);
	}

	private ReportDesign createCustomWebRenderer(ReportDefinition rd, String name) throws IOException {
		final ReportDesign design = new ReportDesign();
		design.setName(name);
		design.setReportDefinition(rd);
		design.setRendererType(DataQualityReportWebRenderer.class);
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
