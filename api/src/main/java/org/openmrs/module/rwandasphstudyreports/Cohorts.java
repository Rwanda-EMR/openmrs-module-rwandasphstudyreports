package org.openmrs.module.rwandasphstudyreports;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.api.PatientSetService.TimeModifier;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.AgeCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CodedObsCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CompositionCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.DateObsCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.EncounterCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.GenderCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InProgramCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InStateCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InverseCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.NumericObsCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.PatientStateCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.ProgramEnrollmentCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.common.DurationUnit;
import org.openmrs.module.reporting.common.RangeComparator;
import org.openmrs.module.reporting.common.SetComparator;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.rwandareports.definition.DrugsActiveCohortDefinition;
import org.openmrs.module.rwandareports.definition.PatientCohortDefinition;

public class Cohorts {
	
	private static GlobalPropertiesManagement gp = new GlobalPropertiesManagement();
	
	public Log log = LogFactory.getLog(getClass());
	
	public static SqlCohortDefinition createParameterizedLocationCohort(String name) {
		
		SqlCohortDefinition location = new SqlCohortDefinition();
		location.setQuery("select p.patient_id from patient p, person_attribute pa, person_attribute_type pat where p.patient_id = pa.person_id and pat.name ='Health Center' and pa.voided = 0 and pat.person_attribute_type_id = pa.person_attribute_type_id and pa.value = :location");
		location.setName(name);
		location.addParameter(new Parameter("location", "location", Location.class));
		return location;
	}
	
	public static SqlCohortDefinition createPatientsNotVoided() {
		SqlCohortDefinition patientsNotVoided = new SqlCohortDefinition(
		        "select distinct p.patient_id from patient p where p.voided=0");
		return patientsNotVoided;
	}
	
	public static SqlCohortDefinition createPatientsWithBaseLineObservation(Concept concept, ProgramWorkflowState state,
	                                                                        Integer daysBefore, Integer daysAfter) {
		SqlCohortDefinition patientsWithBaseLineObservation = new SqlCohortDefinition(
		        "select p.patient_id from patient p, obs o, patient_program pp, patient_state ps where p.voided = 0 and o.voided = 0 and pp.voided = 0 and ps.voided = 0 "
		                + "and ps.patient_program_id = pp.patient_program_id and pp.patient_id = p.patient_id and p.patient_id = o.person_id and ps.state = "
		                + state.getId()
		                + " and o.concept_id = "
		                + concept.getConceptId()
		                + " and o.value_numeric is not null and o.obs_datetime >= DATE_SUB(ps.start_date,INTERVAL "
		                + daysBefore + " DAY) and o.obs_datetime <= DATE_ADD(ps.start_date,INTERVAL " + daysAfter + " DAY)");
		return patientsWithBaseLineObservation;
	}
	
	public static SqlCohortDefinition createPatientsWithDeclineFromBaseline(String name, Concept concept,
	                                                                        ProgramWorkflowState state) {
		SqlCohortDefinition patientsWithBaseLineObservation = new SqlCohortDefinition(
		        "select p.patient_id from patient p, person_attribute pa, person_attribute_type pat, obs o1, obs o2, patient_program pp, patient_state ps where p.voided = 0 and "
		                + "p.patient_id = pa.person_id and pat.name = 'Health Center' and pat.person_attribute_type_id = pa.person_attribute_type_id and pa.voided = 0 and pa.value = :location and "
		                + "pp.voided = 0 and ps.voided = 0 and ps.patient_program_id = pp.patient_program_id and pp.patient_id =  "
		                + "p.patient_id and ps.state = "
		                + state.getId()
		                + " and o1.concept_id = "
		                + concept.getId()
		                + " and o1.obs_id = (select obs_id from obs where "
		                + "voided = 0 and p.patient_id = person_id and concept_id = "
		                + concept.getId()
		                + " and value_numeric is not null and obs_datetime "
		                + ">= ps.start_date order by value_numeric desc LIMIT 1) and o2.obs_id = (select obs_id from obs where voided = "
		                + "0 and p.patient_id = person_id and concept_id = "
		                + concept.getId()
		                + " and value_numeric is not null and obs_datetime >= "
		                + "ps.start_date and obs_datetime <= :beforeDate order by obs_datetime desc LIMIT 1) and ((o2.value_numeric/o1.value_numeric)*100) < 50");
		patientsWithBaseLineObservation.setName(name);
		patientsWithBaseLineObservation.addParameter(new Parameter("beforeDate", "beforeDate", Date.class));
		patientsWithBaseLineObservation.addParameter(new Parameter("location", "location", Location.class));
		return patientsWithBaseLineObservation;
	}
	
	public static SqlCohortDefinition createPatientsWithDecline(String name, Concept concept, int decline) {
		SqlCohortDefinition patientsWithBaseLineObservation = new SqlCohortDefinition(
		        "select p.patient_id from patient p, person_attribute pa, person_attribute_type pat, obs o1, obs o2 where p.voided = 0 "
		                + "and p.patient_id = pa.person_id and pat.name = 'Health Center' and pat.person_attribute_type_id = pa.person_attribute_type_id and pa.voided = 0 and pa.value = :location "
		                + "and o1.concept_id = " + concept.getId() + " and o1.obs_id = (select obs_id from obs where "
		                + "voided = 0 and p.patient_id = person_id and concept_id = " + concept.getId()
		                + " and value_numeric is not null and obs_datetime <= :beforeDate"
		                + " order by value_numeric desc LIMIT 1) and o2.obs_id = (select obs_id from obs where voided = "
		                + "0 and p.patient_id = person_id and concept_id = " + concept.getId()
		                + " and value_numeric is not null and obs_datetime < o1.obs_datetime "
		                + " order by obs_datetime desc LIMIT 1) and ((o1.value_numeric - o2.value_numeric) > -" + decline
		                + ")");
		patientsWithBaseLineObservation.setName(name);
		patientsWithBaseLineObservation.addParameter(new Parameter("beforeDate", "beforeDate", Date.class));
		patientsWithBaseLineObservation.addParameter(new Parameter("location", "location", Location.class));
		return patientsWithBaseLineObservation;
	}
	
	private static String getStateString(List<ProgramWorkflowState> state) {
		String stateId = "";
		int i = 0;
		for (ProgramWorkflowState pws : state) {
			if (i > 0) {
				stateId = stateId + ",";
			}
			
			stateId = stateId + pws.getId();
			
			i++;
		}
		
		return stateId;
	}
	
	private static String getProgramString(List<Program> program) {
		String programId = "";
		int i = 0;
		for (Program p : program) {
			if (i > 0) {
				programId = programId + ",";
			}
			
			programId = programId + p.getId();
			
			i++;
		}
		
		return programId;
	}
	
	public static SqlCohortDefinition createPatientsWithBaseLineObservation(Concept concept,
	                                                                        List<ProgramWorkflowState> state,
	                                                                        Integer daysBefore, Integer daysAfter) {
		
		String stateId = getStateString(state);
		
		SqlCohortDefinition patientsWithBaseLineObservation = new SqlCohortDefinition(
		        "select p.patient_id from patient p, obs o, patient_program pp, patient_state ps where p.voided = 0 and o.voided = 0 and pp.voided = 0 and ps.voided = 0 "
		                + "and ps.patient_program_id = pp.patient_program_id and pp.patient_id = p.patient_id and p.patient_id = o.person_id and ps.patient_state_id = (select patient_state_id from patient_state where patient_program_id = pp.patient_program_id and voided = 0 and state in ("
		                + stateId
		                + ") order by start_date asc limit 1) and o.concept_id = "
		                + concept.getConceptId()
		                + " and o.value_numeric is not null and o.obs_datetime >= DATE_SUB(ps.start_date,INTERVAL "
		                + daysBefore + " DAY) and o.obs_datetime <= DATE_ADD(ps.start_date,INTERVAL " + daysAfter + " DAY)");
		return patientsWithBaseLineObservation;
	}
	
	public static SqlCohortDefinition createPatientsWithBaseLineObservationProgram(Concept concept, List<Program> program,
	                                                                               Integer daysBefore, Integer daysAfter) {
		
		String programId = getProgramString(program);
		
		SqlCohortDefinition patientsWithBaseLineObservation = new SqlCohortDefinition(
		        "select p.patient_id from patient p, obs o, patient_program pp where p.voided = 0 and o.voided = 0 and pp.voided = 0  "
		                + "and pp.patient_id = p.patient_id and p.patient_id = o.person_id and pp.patient_program_id = (select patient_program_id from patient_program where patient_id = p.patient_id and voided = 0 and program_id in ("
		                + programId + ") order by date_enrolled asc limit 1) and o.concept_id = " + concept.getConceptId()
		                + " and o.value_numeric is not null and o.obs_datetime >= DATE_SUB(pp.date_enrolled,INTERVAL "
		                + daysBefore + " DAY) and o.obs_datetime <= DATE_ADD(pp.date_enrolled,INTERVAL " + daysAfter
		                + " DAY)");
		return patientsWithBaseLineObservation;
	}
	
	public static SqlCohortDefinition createPatientInProgramDuringTime(String name) {
		Program adultProgramId = gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
		Program pediProgramId = gp.getProgram(GlobalPropertiesManagement.PEDI_HIV_PROGRAM);
		
		SqlCohortDefinition patientInhivProgram = new SqlCohortDefinition(
		        "select distinct p.patient_id from patient_program pp, patient p where p.patient_id=pp.patient_id and pp.program_id= "
		                + adultProgramId.getId()
		                + " OR pp.program_id="
		                + pediProgramId.getId()
		                + " and pp.voided=0 and p.voided=0 and pp.date_enrolled between :startDate and :endDate order by p.patient_id");
		patientInhivProgram.setName(name);
		patientInhivProgram.addParameter(new Parameter("startDate", "startDate", Date.class));
		patientInhivProgram.addParameter(new Parameter("endDate", "endDate", Location.class));
		return patientInhivProgram;
		
	}
	
	public static SqlCohortDefinition createPatientsWithBaseLineObservation(Concept concept, Concept drugOrderSet,
	                                                                        Integer daysBefore, Integer daysAfter) {
		
		SqlCohortDefinition patientsWithBaseLineObservation = new SqlCohortDefinition(
		        "select p.patient_id from patient p, obs o, orders ord  where p.voided = 0 and o.voided = 0 and p.patient_id"
		                + "= o.person_id and ord.order_id = (select order_id from orders where voided = 0 and patient_id "
		                + "= p.patient_id and concept_id in (select concept_id from concept_set where concept_set = "
		                + drugOrderSet.getConceptId() + ") order by start_date asc limit 1) and o.concept_id = "
		                + concept.getConceptId()
		                + " and o.value_numeric is not null and o.obs_datetime >= DATE_SUB(ord.start_date,INTERVAL "
		                + daysBefore + " DAY) and o.obs_datetime <= DATE_ADD(ord.start_date,INTERVAL " + daysAfter + " DAY)");
		return patientsWithBaseLineObservation;
	}
	
	public static SqlCohortDefinition createPatientsWhereDrugRegimenDoesNotMatchState(Concept conceptSet,
	                                                                                  List<ProgramWorkflowState> states) {
		String stateId = getStateString(states);
		
		SqlCohortDefinition patients = new SqlCohortDefinition(
		        "select d.patient_id from ("
		                + "select patient_id, start_date from orders where voided = 0 and concept_id in (select distinct concept_id from concept_set where concept_set = "
		                + conceptSet.getConceptId()
		                + ") group by patient_id order by start_date asc)d "
		                + "INNER JOIN "
		                + "(select p.patient_id as patient_id, ps.start_date as start_date from patient p, patient_program pp, patient_state ps where "
		                + "p.voided = 0 and pp.voided = 0 and ps.voided = 0 and ps.patient_program_id = pp.patient_program_id and pp.patient_id = p.patient_id and ps.state in ("
		                + stateId + ") group by p.patient_id order by start_date asc)s " + "on s.patient_id = d.patient_id "
		                + "where DATEDIFF( s.start_date, d.start_date ) >= 1");
		
		return patients;
	}
	
	public static SqlCohortDefinition createPatientsWithStartDateOfStateEqualsToProgramEnrolmentDate(ProgramWorkflowState state) {
		SqlCohortDefinition patientsWithBState = new SqlCohortDefinition(
		        "select p.patient_id from patient p, patient_program pp, patient_state ps where p.voided = 0 and pp.voided = 0 and ps.voided = 0 "
		                + "and ps.patient_program_id = pp.patient_program_id and pp.patient_id = p.patient_id and ps.state = "
		                + state.getId() + " and ps.start_date=pp.date_enrolled");
		return patientsWithBState;
	}
	
	public static SqlCohortDefinition createPatientsWithStatePredatingProgramEnrolment(ProgramWorkflowState state) {
		SqlCohortDefinition patientsWithBaseLineObservation = new SqlCohortDefinition(
		        "select p.patient_id from patient p, patient_program pp, patient_state ps where p.voided = 0 and pp.voided = 0 and ps.voided = 0 "
		                + "and ps.patient_program_id = pp.patient_program_id and pp.patient_id = p.patient_id and ps.state = "
		                + state.getId() + " and DATEDIFF( ps.start_date, pp.date_enrolled ) < 0");
		return patientsWithBaseLineObservation;
	}
	
	public static SqlCohortDefinition createPatientsInStateNotPredatingProgramEnrolment(ProgramWorkflowState state) {
		SqlCohortDefinition patientsInState = new SqlCohortDefinition(
		        "select pp.patient_id from patient_state ps inner join patient_program pp on ps.patient_program_id = pp.patient_program_id inner join patient p on pp.patient_id = p.patient_id where ps.voided = false and pp.voided = false and p.voided = false"
		                + " and ps.state = "
		                + state.getId()
		                + " and (ps.end_date is null or ps.end_date >= :onOrAfter) and (ps.start_date is null or ps.start_date <= :onOrBefore) and ps.start_date > pp.date_enrolled");
		patientsInState.addParameter(new Parameter("onOrAfter", "onOrAfter", Date.class));
		patientsInState.addParameter(new Parameter("onOrBefore", "onOrBefore", Location.class));
		return patientsInState;
	}
	
	public static InverseCohortDefinition createPatientsWithoutBaseLineObservation(Concept concept,
	                                                                               ProgramWorkflowState state,
	                                                                               Integer daysBefore, Integer daysAfter) {
		InverseCohortDefinition patientsWithoutBaseLineObservation = new InverseCohortDefinition(
		        createPatientsWithBaseLineObservation(concept, state, daysBefore, daysAfter));
		return patientsWithoutBaseLineObservation;
	}
	
	public static InverseCohortDefinition createPatientsWithoutBaseLineObservation(Concept concept,
	                                                                               List<ProgramWorkflowState> state,
	                                                                               Integer daysBefore, Integer daysAfter) {
		InverseCohortDefinition patientsWithoutBaseLineObservation = new InverseCohortDefinition(
		        createPatientsWithBaseLineObservation(concept, state, daysBefore, daysAfter));
		return patientsWithoutBaseLineObservation;
	}
	
	public static InverseCohortDefinition createPatientsWithoutBaseLineObservation(Concept concept, Concept drugConceptSet,
	                                                                               Integer daysBefore, Integer daysAfter) {
		InverseCohortDefinition patientsWithoutBaseLineObservation = new InverseCohortDefinition(
		        createPatientsWithBaseLineObservation(concept, drugConceptSet, daysBefore, daysAfter));
		return patientsWithoutBaseLineObservation;
	}
	
	public static InverseCohortDefinition createPatientsWithoutBaseLineObservationProgramEnrollment(Concept concept,
	                                                                                                List<Program> program,
	                                                                                                Integer daysBefore,
	                                                                                                Integer daysAfter) {
		InverseCohortDefinition patientsWithoutBaseLineObservation = new InverseCohortDefinition(
		        createPatientsWithBaseLineObservationProgram(concept, program, daysBefore, daysAfter));
		return patientsWithoutBaseLineObservation;
	}
	
	public static SqlCohortDefinition createPatientsWithAccompagnateur(String name, String parameterName) {
		SqlCohortDefinition allPatientsWithAccompagnateur = new SqlCohortDefinition(
		        "SELECT DISTINCT person_b FROM relationship WHERE relationship='1' and date_created<= :endDate and voided=0");
		allPatientsWithAccompagnateur.setName(name);
		if (parameterName != null) {
			allPatientsWithAccompagnateur.addParameter(new Parameter(parameterName, parameterName, Date.class));
		}
		return allPatientsWithAccompagnateur;
	}
	
	public static PatientCohortDefinition createPatientCohort(String name) {
		PatientCohortDefinition cohort = new PatientCohortDefinition();
		cohort.setName(name);
		cohort.addParameter(new Parameter("patientId", "patientId", String.class));
		return cohort;
	}
	
	public static AgeCohortDefinition createOver15AgeCohort(String name) {
		AgeCohortDefinition over15Cohort = new AgeCohortDefinition();
		over15Cohort.setName(name);
		over15Cohort.setMinAge(new Integer(15));
		over15Cohort.addParameter(new Parameter("effectiveDate", "endDate", Date.class));
		return over15Cohort;
	}
	
	public static AgeCohortDefinition createUnder15AgeCohort(String name) {
		AgeCohortDefinition under15Cohort = new AgeCohortDefinition();
		under15Cohort.setName(name);
		under15Cohort.setMaxAge(new Integer(14));
		under15Cohort.addParameter(new Parameter("effectiveDate", "endDate", Date.class));
		return under15Cohort;
	}
	
	public static AgeCohortDefinition createUnderAgeCohort(String name, int age) {
		AgeCohortDefinition underAgeCohort = new AgeCohortDefinition();
		underAgeCohort.setName(name);
		underAgeCohort.setMaxAge(new Integer(age));
		underAgeCohort.addParameter(new Parameter("effectiveDate", "endDate", Date.class));
		return underAgeCohort;
	}
	
	public static AgeCohortDefinition createUnder3AgeCohort(String name) {
		AgeCohortDefinition under3Cohort = new AgeCohortDefinition();
		under3Cohort.setName(name);
		under3Cohort.setMaxAge(new Integer(2));
		under3Cohort.addParameter(new Parameter("effectiveDate", "endDate", Date.class));
		return under3Cohort;
	}
	
	public static AgeCohortDefinition create3to5AgeCohort(String name) {
		AgeCohortDefinition threeTo5Cohort = new AgeCohortDefinition();
		threeTo5Cohort.setName(name);
		threeTo5Cohort.setMaxAge(new Integer(4));
		threeTo5Cohort.setMinAge(new Integer(3));
		threeTo5Cohort.addParameter(new Parameter("effectiveDate", "endDate", Date.class));
		return threeTo5Cohort;
	}
	
	public static AgeCohortDefinition createUnder18monthsCohort(String name) {
		AgeCohortDefinition at18monthsOfAge = new AgeCohortDefinition();
		at18monthsOfAge.setName(name);
		at18monthsOfAge.setMinAge(10);
		at18monthsOfAge.setMinAgeUnit(DurationUnit.MONTHS);
		at18monthsOfAge.setMaxAge(17);
		at18monthsOfAge.setMaxAgeUnit(DurationUnit.MONTHS);
		at18monthsOfAge.addParameter(new Parameter("effectiveDate", "endDate", Date.class));
		return at18monthsOfAge;
	}
	
	public static AgeCohortDefinition createinfantUnder18months(String name) {
		AgeCohortDefinition at18monthsOfAge = new AgeCohortDefinition();
		at18monthsOfAge.setName(name);
		at18monthsOfAge.setMinAge(2);
		at18monthsOfAge.setMinAgeUnit(DurationUnit.WEEKS);
		at18monthsOfAge.setMaxAge(17);
		at18monthsOfAge.setMaxAgeUnit(DurationUnit.MONTHS);
		//at18monthsOfAge.addParameter(new Parameter("effectiveDate", "endDate", Date.class));
		return at18monthsOfAge;
	}
	
	public static SqlCohortDefinition getPediOnArtIn2Weeks(String name, Program program) {
		Concept artDrugConceptSet = gp.getConcept(GlobalPropertiesManagement.ART_DRUGS_SET);
		SqlCohortDefinition onArtIn2weeks = new SqlCohortDefinition();
		onArtIn2weeks.setQuery("select distinct o.patient_id from orders o, patient_program p where "
		        + "concept_id in (select concept_id from concept_set where concept_set=" + artDrugConceptSet + ") "
		        + "and p.date_enrolled < o.start_date and p.patient_id=o.patient_id "
		        + "and DATEDIFF(o.start_date, p.date_enrolled) <= 14 and p.program_id=" + program.getProgramId()
		        + " and o.voided=0  ");
		
		return onArtIn2weeks;
	}
	
	public static SqlCohortDefinition getPatientsOnArtIn3Months(String name, List<Program> program) {
		
		String programId = getProgramString(program);
		Concept artDrugConceptSet = gp.getConcept(GlobalPropertiesManagement.ART_DRUGS_SET);
		
		SqlCohortDefinition onArtIn3months = new SqlCohortDefinition();
		onArtIn3months.setQuery("select distinct o.patient_id from orders o, patient_program p where "
		        + "concept_id in (select concept_id from concept_set where concept_set=" + artDrugConceptSet + ") "
		        + "and p.date_enrolled < o.start_date and p.patient_id=o.patient_id "
		        + "and DATEDIFF(o.start_date, p.date_enrolled) <= 90 and p.program_id in (" + programId
		        + ") and o.voided=0 ");
		
		return onArtIn3months;
		
	}
	
	public static AgeCohortDefinition createXtoYAgeCohort(String name, int minAge, int maxAge) {
		AgeCohortDefinition xToYCohort = new AgeCohortDefinition();
		xToYCohort.setName(name);
		xToYCohort.setMaxAge(new Integer(maxAge));
		xToYCohort.setMinAge(new Integer(minAge));
		xToYCohort.addParameter(new Parameter("effectiveDate", "endDate", Date.class));
		return xToYCohort;
	}
	
	public static AgeCohortDefinition createOver5AgeCohort(String name) {
		AgeCohortDefinition over5Cohort = new AgeCohortDefinition();
		over5Cohort.setName(name);
		over5Cohort.setMinAge(new Integer(5));
		over5Cohort.addParameter(new Parameter("effectiveDate", "endDate", Date.class));
		return over5Cohort;
	}
	
	public static AgeCohortDefinition createOverXAgeCohort(String name, int minAge) {
		AgeCohortDefinition overXCohort = new AgeCohortDefinition();
		overXCohort.setName(name);
		overXCohort.setMinAge(new Integer(minAge));
		overXCohort.addParameter(new Parameter("effectiveDate", "endDate", Date.class));
		return overXCohort;
	}
	
	public static AgeCohortDefinition createUnder5YearsAgeCohort(String name) {
		AgeCohortDefinition under5YearsCohort = new AgeCohortDefinition();
		under5YearsCohort.setName(name);
		under5YearsCohort.setMaxAge(new Integer(5));
		under5YearsCohort.setMaxAgeUnit(DurationUnit.YEARS);
		under5YearsCohort.addParameter(new Parameter("effectiveDate", "endDate", Date.class));
		return under5YearsCohort;
	}
	
	public static SqlCohortDefinition createUnder15AtEnrollmentCohort(String name, Program program) {
		SqlCohortDefinition patientsUnderFifteenAtEnrollementDate = new SqlCohortDefinition(
		        "select distinct pp.patient_id from person p,patient_program pp where p.person_id=pp.patient_id and DATEDIFF(pp.date_enrolled,p.birthdate)<5478 and pp.program_id= "
		                + program.getId() + " and p.voided=0 and pp.voided=0");
		patientsUnderFifteenAtEnrollementDate.setName(name);
		
		return patientsUnderFifteenAtEnrollementDate;
	}
	
	public static SqlCohortDefinition create15orOverAtEnrollmentCohort(String name, Program program) {
		SqlCohortDefinition patientsOverFifteenAtEnrollementDate = new SqlCohortDefinition(
		        "select distinct pp.patient_id from person p,patient_program pp where p.person_id=pp.patient_id and DATEDIFF(pp.date_enrolled,p.birthdate)>=5478 and pp.program_id= "
		                + program.getId() + " and p.voided=0 and pp.voided=0");
		patientsOverFifteenAtEnrollementDate.setName(name);
		
		return patientsOverFifteenAtEnrollementDate;
	}
	
	public static SqlCohortDefinition createUnder18monthsAtEnrollmentCohort(String name, Program program) {
		SqlCohortDefinition patientsUnder18monthsAtEnrollementDate = new SqlCohortDefinition(
		        "select distinct pp.patient_id from person p,patient_program pp where p.person_id=pp.patient_id and DATEDIFF(pp.date_enrolled,p.birthdate)<547 and pp.program_id= "
		                + program.getId() + " and p.voided=0 and pp.voided=0");
		patientsUnder18monthsAtEnrollementDate.setName(name);
		
		return patientsUnder18monthsAtEnrollementDate;
	}
	
	public static SqlCohortDefinition createUnder18monthsAtStartOfArtbyStartEndDate(String name, Program program,
	                                                                                ProgramWorkflowState state) {
		SqlCohortDefinition under18AtstartOfART = new SqlCohortDefinition(
		        "select DISTINCT pp.patient_id FROM person p,patient_program pp,patient pa,patient_state ps "
		                + "WHERE p.person_id=pp.patient_id AND ps.patient_program_id = pp.patient_program_id AND pp.patient_id = pa.patient_id "
		                + "AND pp.program_id="
		                + program.getProgramId()
		                + " AND ps.state="
		                + state.getId()
		                + " AND DATEDIFF(ps.start_date,p.birthdate)<547 "
		                + "AND (ps.start_date>= :startDate and ps.start_date<= :endDate and ps.end_date is null) AND p.voided=0 AND pp.voided=0 AND pa.voided=0 AND ps.voided=0");
		under18AtstartOfART.setName(name);
		under18AtstartOfART.addParameter(new Parameter("startDate", "startDate", Date.class));
		under18AtstartOfART.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		return under18AtstartOfART;
	}
	
	public static SqlCohortDefinition createUnder5AtEnrollmentCohort(String name, Program program) {
		SqlCohortDefinition patientsUnder5AtEnrollementDate = new SqlCohortDefinition(
		        "select distinct pp.patient_id from person p,patient_program pp where p.person_id=pp.patient_id and DATEDIFF(pp.date_enrolled,p.birthdate)< 1826 and pp.program_id= "
		                + program.getId() + " and p.voided=0 and pp.voided=0");
		patientsUnder5AtEnrollementDate.setName(name);
		
		return patientsUnder5AtEnrollementDate;
	}
	
	public static SqlCohortDefinition createUnder5yrsAtStartOfArtbyStartEndDate(String name, Program program,
	                                                                            ProgramWorkflowState state) {
		SqlCohortDefinition under5tstartOfART = new SqlCohortDefinition(
		        "select DISTINCT pp.patient_id FROM person p,patient_program pp,patient pa,patient_state ps "
		                + "WHERE p.person_id=pp.patient_id AND ps.patient_program_id = pp.patient_program_id AND pp.patient_id = pa.patient_id "
		                + "AND pp.program_id="
		                + program.getProgramId()
		                + " AND ps.state="
		                + state.getId()
		                + " AND DATEDIFF(ps.start_date,p.birthdate)<=1826 "
		                + "AND (ps.start_date>= :startDate and ps.start_date<= :endDate AND ps.end_date is null) AND p.voided=0 AND pp.voided=0 AND pa.voided=0 AND ps.voided=0");
		under5tstartOfART.setName(name);
		under5tstartOfART.addParameter(new Parameter("startDate", "startDate", Date.class));
		under5tstartOfART.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		return under5tstartOfART;
	}
	
	public static SqlCohortDefinition createUnder15yrsAtStartOfArtbyStartEndDate(String name, Program program,
	                                                                             ProgramWorkflowState state) {
		SqlCohortDefinition under15tstartOfART = new SqlCohortDefinition(
		        "select DISTINCT pp.patient_id FROM person p,patient_program pp,patient pa,patient_state ps "
		                + "WHERE p.person_id=pp.patient_id AND ps.patient_program_id = pp.patient_program_id AND pp.patient_id = pa.patient_id "
		                + "AND pp.program_id="
		                + program.getProgramId()
		                + " AND ps.state="
		                + state.getId()
		                + " AND DATEDIFF(ps.start_date,p.birthdate) < 5479 "
		                + "AND (ps.start_date>= :startDate and ps.start_date<= :endDate AND ps.end_date is null) AND p.voided=0 AND pp.voided=0 AND pa.voided=0 AND ps.voided=0");
		under15tstartOfART.setName(name);
		under15tstartOfART.addParameter(new Parameter("startDate", "startDate", Date.class));
		under15tstartOfART.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		return under15tstartOfART;
	}
	
	public static SqlCohortDefinition createOver15yrsAtStartOfArtbyStartEndDate(String name, Program program,
	                                                                            ProgramWorkflowState state) {
		SqlCohortDefinition over15tstartOfART = new SqlCohortDefinition(
		        "select DISTINCT pp.patient_id FROM person p,patient_program pp,patient pa,patient_state ps "
		                + "WHERE p.person_id=pp.patient_id AND ps.patient_program_id = pp.patient_program_id AND pp.patient_id = pa.patient_id "
		                + "AND pp.program_id="
		                + program.getProgramId()
		                + " AND ps.state="
		                + state.getId()
		                + " AND DATEDIFF(ps.start_date,p.birthdate) >= 5479 "
		                + "AND (ps.start_date>= :startDate and ps.start_date<= :endDate AND ps.end_date is null) AND p.voided=0 AND pp.voided=0 AND pa.voided=0 AND ps.voided=0");
		over15tstartOfART.setName(name);
		over15tstartOfART.addParameter(new Parameter("startDate", "startDate", Date.class));
		over15tstartOfART.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		return over15tstartOfART;
	}
	
	public static SqlCohortDefinition createUnder15yrsAtStartOfArtbyCompletedDuringP(String name, Program program,
	                                                                                 ProgramWorkflowState state) {
		SqlCohortDefinition over15tstartOfARTCompleted = new SqlCohortDefinition(
		        "select DISTINCT pp.patient_id FROM person p,patient_program pp,patient pa,patient_state ps "
		                + "WHERE p.person_id=pp.patient_id AND ps.patient_program_id = pp.patient_program_id AND pp.patient_id = pa.patient_id "
		                + "AND pp.program_id="
		                + program.getProgramId()
		                + " AND ps.state="
		                + state.getId()
		                + " AND DATEDIFF(ps.start_date,p.birthdate) < 5479 "
		                + "AND (ps.end_date between :startDate and :endDate) AND p.voided=0 AND pp.voided=0 AND pa.voided=0 AND ps.voided=0 ");
		over15tstartOfARTCompleted.setName(name);
		over15tstartOfARTCompleted.addParameter(new Parameter("startDate", "startDate", Date.class));
		over15tstartOfARTCompleted.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		return over15tstartOfARTCompleted;
	}
	
	public static SqlCohortDefinition createOver15yrsAtStartOfArtbyCompletedDuringP(String name, Program program,
	                                                                                ProgramWorkflowState state) {
		SqlCohortDefinition over15tstartOfARTCompleted = new SqlCohortDefinition(
		        "select DISTINCT pp.patient_id FROM person p,patient_program pp,patient pa,patient_state ps "
		                + "WHERE p.person_id=pp.patient_id AND ps.patient_program_id = pp.patient_program_id AND pp.patient_id = pa.patient_id "
		                + "AND pp.program_id="
		                + program.getProgramId()
		                + " AND ps.state="
		                + state.getId()
		                + " AND DATEDIFF(ps.start_date,p.birthdate) >= 5479 "
		                + "AND (ps.end_date between :startDate and :endDate) AND p.voided=0 AND pp.voided=0 AND pa.voided=0 AND ps.voided=0 ");
		over15tstartOfARTCompleted.setName(name);
		over15tstartOfARTCompleted.addParameter(new Parameter("startDate", "startDate", Date.class));
		over15tstartOfARTCompleted.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		return over15tstartOfARTCompleted;
	}
	
	public static SqlCohortDefinition createArtOrPreArtAndActiveonPatientDiedORTransferedStateDuringPeriod(String name,
	                                                                                                       Program program,
	                                                                                                       ProgramWorkflowState state1,
	                                                                                                       ProgramWorkflowState state2) {
		SqlCohortDefinition preOnpreArtandActiveOnDiedState = new SqlCohortDefinition(
		        "select DISTINCT pp.patient_id FROM person p,patient_program pp,patient pa,patient_state ps1,patient_state ps2  "
		                + "WHERE p.person_id=pp.patient_id AND ps1.patient_program_id = pp.patient_program_id AND ps2.patient_program_id = pp.patient_program_id AND pp.patient_id = pa.patient_id "
		                + "AND pp.program_id="
		                + program.getProgramId()
		                + " AND ps1.state="
		                + state1.getId()
		                + " AND ps2.state="
		                + state2.getId()
		                + " AND ps1.end_date=ps2.start_date "
		                + "AND (ps2.start_date>= :startDate and ps2.start_date<= :endDate and ps2.end_date is null) AND p.voided=0 AND pp.voided=0 AND pa.voided=0 AND ps2.voided=0");
		preOnpreArtandActiveOnDiedState.setName(name);
		preOnpreArtandActiveOnDiedState.addParameter(new Parameter("startDate", "startDate", Date.class));
		preOnpreArtandActiveOnDiedState.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		return preOnpreArtandActiveOnDiedState;
		
	}
	
	public static SqlCohortDefinition createPatientBackToProgramThisYear(String name) {
		SqlCohortDefinition patientBackToProgram = new SqlCohortDefinition(
		        "select DISTINCT patient_id FROM encounter WHERE patient_id "
		                + "IN (select DISTINCT e.patient_id FROM encounter e, patient p WHERE e.patient_id=p.patient_id "
		                + "AND DATEDIFF(:startDate, e.encounter_datetime) >= 90 AND e.encounter_type IN  ("
		                + Context.getAdministrationService().getGlobalProperty("ClinicalencounterTypeIds.labTestExcl")
		                + " ) "
		                + "AND p.voided=0 AND e.voided=0) AND encounter_datetime >= :startDate AND encounter_datetime <= :endDate AND voided=0  ");
		patientBackToProgram.setName("TR:lostbacktoProgramThismonthinART");
		patientBackToProgram.addParameter(new Parameter("startDate", "startDate", Date.class));
		patientBackToProgram.addParameter(new Parameter("endDate", "endDate", Date.class));
		return patientBackToProgram;
		
	}
	
	public static InProgramCohortDefinition createInProgram(String name, Program program) {
		InProgramCohortDefinition inProgram = new InProgramCohortDefinition();
		inProgram.setName(name);
		
		List<Program> programs = new ArrayList<Program>();
		programs.add(program);
		
		inProgram.setPrograms(programs);
		
		return inProgram;
	}
	
	public static InProgramCohortDefinition createInProgram(String name, List<Program> programs) {
		InProgramCohortDefinition inProgram = new InProgramCohortDefinition();
		inProgram.setName(name);
		
		inProgram.setPrograms(programs);
		
		return inProgram;
	}
	
	public static InProgramCohortDefinition createInProgramParameterizableByDate(String name, Program program) {
		InProgramCohortDefinition inProgram = createInProgram(name, program);
		inProgram.addParameter(new Parameter("onDate", "On Date", Date.class));
		return inProgram;
	}
	
	public static InProgramCohortDefinition createInProgramParameterizableByDate(String name, List<Program> programs,
	                                                                             String parameterName) {
		InProgramCohortDefinition inProgram = createInProgram(name, programs);
		inProgram.addParameter(new Parameter(parameterName, parameterName, Date.class));
		return inProgram;
	}
	
	public static InProgramCohortDefinition createInProgramParameterizableByDate(String name, Program programs,
	                                                                             String parameterName) {
		InProgramCohortDefinition inProgram = createInProgram(name, programs);
		inProgram.addParameter(new Parameter(parameterName, parameterName, Date.class));
		return inProgram;
	}
	
	public static InProgramCohortDefinition createInProgramParameterizableByDate(String name, List<Program> programs,
	                                                                             List<String> parameterName) {
		InProgramCohortDefinition inProgram = createInProgram(name, programs);
		
		for (String p : parameterName) {
			inProgram.addParameter(new Parameter(p, p, Date.class));
		}
		return inProgram;
	}
	
	public static InProgramCohortDefinition createInProgramParameterizableByDate(String name, Program programs,
	                                                                             List<String> parameterName) {
		InProgramCohortDefinition inProgram = createInProgram(name, programs);
		
		for (String p : parameterName) {
			inProgram.addParameter(new Parameter(p, p, Date.class));
		}
		return inProgram;
	}
	
	public static InProgramCohortDefinition createInProgramParameterizableByStartEndDate(String name, Program program) {
		InProgramCohortDefinition inProgram = createInProgram(name, program);
		inProgram.addParameter(new Parameter("onOrAfter", "onOrAfter", Date.class));
		inProgram.addParameter(new Parameter("onOrBefore", "onOrBefore", Date.class));
		return inProgram;
	}
	
	public static ProgramEnrollmentCohortDefinition createProgramEnrollment(String name, Program program) {
		
		ProgramEnrollmentCohortDefinition programEnrollmentCohortDefinition = new ProgramEnrollmentCohortDefinition();
		programEnrollmentCohortDefinition.setName(name);
		
		List<Program> programs = new ArrayList<Program>();
		programs.add(program);
		
		programEnrollmentCohortDefinition.setPrograms(programs);
		return programEnrollmentCohortDefinition;
	}
	
	public static CompositionCohortDefinition createEnrolledInProgramDuringPeriod(String name, Program program) {
		ProgramEnrollmentCohortDefinition enrolledBefore = createProgramEnrollment(name, program);
		enrolledBefore.addParameter(new Parameter("enrolledOnOrBefore", "enrolledOnOrBefore", Date.class));
		
		CompositionCohortDefinition enrolledDuringPeriod = new CompositionCohortDefinition();
		enrolledDuringPeriod.setName(name);
		enrolledDuringPeriod.addParameter(new Parameter("endDate", "endDate", Date.class));
		enrolledDuringPeriod.addParameter(new Parameter("startDate", "startDate", Date.class));
		enrolledDuringPeriod.getSearches().put("1",
		    new Mapped(enrolledBefore, ParameterizableUtil.createParameterMappings("enrolledOnOrBefore=${startDate-1d}")));
		enrolledDuringPeriod.getSearches().put("2",
		    new Mapped(enrolledBefore, ParameterizableUtil.createParameterMappings("enrolledOnOrBefore=${endDate}")));
		enrolledDuringPeriod.setCompositionString("2 and not 1");
		
		return enrolledDuringPeriod;
	}
	
	public static ProgramEnrollmentCohortDefinition createProgramEnrollmentParameterizedByStartEndDate(String name,
	                                                                                                   Program program) {
		
		ProgramEnrollmentCohortDefinition programEnrollmentCohortDefinition = createProgramEnrollment(name, program);
		programEnrollmentCohortDefinition.addParameter(new Parameter("enrolledOnOrAfter", "enrolledOnOrAfter", Date.class));
		programEnrollmentCohortDefinition
		        .addParameter(new Parameter("enrolledOnOrBefore", "enrolledOnOrBefore", Date.class));
		return programEnrollmentCohortDefinition;
	}
	
	public static InStateCohortDefinition createInProgramStateParameterizableByDate(String name, ProgramWorkflowState state) {
		InStateCohortDefinition stateCohort = new InStateCohortDefinition();
		
		List<ProgramWorkflowState> states = new ArrayList<ProgramWorkflowState>();
		states.add(state);
		
		stateCohort.setStates(states);
		stateCohort.setName(name);
		stateCohort.addParameter(new Parameter("onDate", "On Date", Date.class));
		
		return stateCohort;
	}
	
	public static PatientStateCohortDefinition createPatientStateCohortDefinition(String name,
	                                                                              ProgramWorkflowState programWorkflowState) {
		PatientStateCohortDefinition patientState = new PatientStateCohortDefinition();
		patientState.setName(name);
		
		List<ProgramWorkflowState> programWorkFlowStateList = new ArrayList<ProgramWorkflowState>();
		programWorkFlowStateList.add(programWorkflowState);
		patientState.setStates(programWorkFlowStateList);
		
		return patientState;
	}
	
	public static PatientStateCohortDefinition createPatientStateEverCohortDefinition(String name,
	                                                                                  List<ProgramWorkflowState> states) {
		PatientStateCohortDefinition patientState = new PatientStateCohortDefinition();
		patientState.setName(name);
		patientState.setStates(states);
		
		return patientState;
	}
	
	public static InStateCohortDefinition createInCurrentStateParameterized(String name, String parameterName) {
		InStateCohortDefinition state = new InStateCohortDefinition();
		state.addParameter(new Parameter(parameterName, parameterName, ProgramWorkflowState.class));
		state.addParameter(new Parameter("onDate", "On Date", Date.class));
		state.setName(name);
		
		return state;
	}
	
	public static InStateCohortDefinition createInCurrentState(String name, List<ProgramWorkflowState> states) {
		InStateCohortDefinition state = new InStateCohortDefinition();
		state.setName(name);
		state.setStates(states);
		state.addParameter(new Parameter("onDate", "On Date", Date.class));
		
		return state;
	}
	
	public static InStateCohortDefinition createInCurrentState(String name, List<ProgramWorkflowState> states,
	                                                           String parameterName) {
		InStateCohortDefinition state = new InStateCohortDefinition();
		state.setName(name);
		state.setStates(states);
		state.addParameter(new Parameter(parameterName, parameterName, Date.class));
		
		return state;
	}
	
	public static InStateCohortDefinition createInCurrentState(String name, List<ProgramWorkflowState> states,
	                                                           List<String> parameterName) {
		InStateCohortDefinition state = createInCurrentState(name, states);
		
		for (String p : parameterName) {
			state.addParameter(new Parameter(p, p, Date.class));
		}
		
		return state;
	}
	
	public static EncounterCohortDefinition createEncounterParameterizedByDate(String name, String parameterName,
	                                                                           List<EncounterType> encounters) {
		EncounterCohortDefinition encounter = createEncounterParameterizedByDate(name, parameterName);
		encounter.setEncounterTypeList(encounters);
		return encounter;
	}
	
	public static EncounterCohortDefinition createEncounterParameterizedByDate(String name, List<String> parameterNames,
	                                                                           List<EncounterType> encounters) {
		EncounterCohortDefinition encounter = createEncounterParameterizedByDate(name, parameterNames);
		encounter.setEncounterTypeList(encounters);
		return encounter;
	}
	
	public static EncounterCohortDefinition createEncounterParameterizedByDate(String name, List<String> parameterNames,
	                                                                           EncounterType encounterType) {
		List<EncounterType> encounters = new ArrayList<EncounterType>();
		encounters.add(encounterType);
		
		EncounterCohortDefinition encounter = createEncounterParameterizedByDate(name, parameterNames);
		encounter.setEncounterTypeList(encounters);
		return encounter;
	}
	
	public static EncounterCohortDefinition createEncounterParameterizedByDate(String name, String parameterName) {
		EncounterCohortDefinition encounter = new EncounterCohortDefinition();
		encounter.setName(name);
		encounter.addParameter(new Parameter(parameterName, parameterName, Date.class));
		return encounter;
	}
	
	public static EncounterCohortDefinition createEncounterParameterizedByDate(String name, List<String> parameterNames) {
		EncounterCohortDefinition encounter = new EncounterCohortDefinition();
		encounter.setName(name);
		if (parameterNames != null) {
			for (String p : parameterNames) {
				encounter.addParameter(new Parameter(p, p, Date.class));
			}
		}
		return encounter;
	}
	
	public static EncounterCohortDefinition createEncounterBasedOnForms(String name, String parameterName, List<Form> forms) {
		EncounterCohortDefinition encounter = createEncounterParameterizedByDate(name, parameterName);
		encounter.setFormList(forms);
		return encounter;
	}
	
	public static EncounterCohortDefinition createEncounterBasedOnForms(String name, List<String> parameterNames,
	                                                                    List<Form> forms) {
		EncounterCohortDefinition encounter = createEncounterParameterizedByDate(name, parameterNames);
		encounter.setFormList(forms);
		return encounter;
	}
	
	public static EncounterCohortDefinition createEncounterBasedOnForms(String name, List<Form> forms) {
		EncounterCohortDefinition encounter = new EncounterCohortDefinition();
		encounter.setName(name);
		encounter.setFormList(forms);
		return encounter;
	}
	
	public static NumericObsCohortDefinition createNumericObsCohortDefinition(String name, Concept question, double value,
	                                                                          RangeComparator setComparator,
	                                                                          TimeModifier timeModifier) {
		
		NumericObsCohortDefinition obsCohortDefinition = new NumericObsCohortDefinition();
		
		obsCohortDefinition.setName(name);
		
		if (question != null)
			obsCohortDefinition.setQuestion(question);
		
		if (setComparator != null)
			obsCohortDefinition.setOperator1(setComparator);
		
		if (timeModifier != null)
			obsCohortDefinition.setTimeModifier(timeModifier);
		
		if (value != 0) {
			obsCohortDefinition.setValue1(value);
		}
		
		return obsCohortDefinition;
	}
	
	public static NumericObsCohortDefinition createNumericObsCohortDefinition(String name, String parameterName,
	                                                                          Concept question, double value,
	                                                                          RangeComparator setComparator,
	                                                                          TimeModifier timeModifier) {
		
		NumericObsCohortDefinition obsCohortDefinition = createNumericObsCohortDefinition(parameterName, question, value,
		    setComparator, timeModifier);
		
		if (parameterName != null) {
			obsCohortDefinition.addParameter(new Parameter(parameterName, parameterName, Date.class));
		}
		
		return obsCohortDefinition;
	}
	
	public static NumericObsCohortDefinition createNumericObsCohortDefinition(String name, List<String> parameterNames,
	                                                                          Concept question, double value,
	                                                                          RangeComparator setComparator,
	                                                                          TimeModifier timeModifier) {
		
		NumericObsCohortDefinition obsCohortDefinition = createNumericObsCohortDefinition(name, question, value,
		    setComparator, timeModifier);
		
		if (parameterNames != null) {
			for (String p : parameterNames) {
				obsCohortDefinition.addParameter(new Parameter(p, p, Date.class));
			}
		}
		
		return obsCohortDefinition;
	}
	
	public static InverseCohortDefinition createNoObservationDefintion(Concept concept) {
		
		SqlCohortDefinition query = new SqlCohortDefinition("select person_id from obs where voided = 0 and concept_id = "
		        + concept.getId());
		
		InverseCohortDefinition noObs = new InverseCohortDefinition(query);
		
		return noObs;
	}
	
	public static CodedObsCohortDefinition createCodedObsCohortDefinition(Concept question, Concept value,
	                                                                      SetComparator setComparator,
	                                                                      TimeModifier timeModifier) {
		CodedObsCohortDefinition obsCohortDefinition = new CodedObsCohortDefinition();
		
		if (question != null) {
			obsCohortDefinition.setQuestion(question);
		}
		if (setComparator != null) {
			obsCohortDefinition.setOperator(setComparator);
		}
		if (timeModifier != null) {
			obsCohortDefinition.setTimeModifier(timeModifier);
		}
		
		List<Concept> valueList = new ArrayList<Concept>();
		if (value != null) {
			valueList.add(value);
			obsCohortDefinition.setValueList(valueList);
		}
		return obsCohortDefinition;
	}
	
	public static CodedObsCohortDefinition createCodedObsCohortDefinition(String name, Concept question, Concept value,
	                                                                      SetComparator setComparator,
	                                                                      TimeModifier timeModifier) {
		CodedObsCohortDefinition obsCohortDefinition = createCodedObsCohortDefinition(question, value, setComparator,
		    timeModifier);
		obsCohortDefinition.setName(name);
		return obsCohortDefinition;
	}
	
	public static CodedObsCohortDefinition createCodedObsCohortDefinition(String name, String parameterName,
	                                                                      Concept question, Concept value,
	                                                                      SetComparator setComparator,
	                                                                      TimeModifier timeModifier) {
		CodedObsCohortDefinition obsCohortDefinition = createCodedObsCohortDefinition(name, question, value, setComparator,
		    timeModifier);
		if (parameterName != null) {
			obsCohortDefinition.addParameter(new Parameter(parameterName, parameterName, Date.class));
		}
		return obsCohortDefinition;
	}
	
	public static CodedObsCohortDefinition createCodedObsCohortDefinition(String name, List<String> parameterNames,
	                                                                      Concept question, Concept value,
	                                                                      SetComparator setComparator,
	                                                                      TimeModifier timeModifier) {
		CodedObsCohortDefinition obsCohortDefinition = createCodedObsCohortDefinition(name, question, value, setComparator,
		    timeModifier);
		if (parameterNames != null) {
			for (String p : parameterNames) {
				obsCohortDefinition.addParameter(new Parameter(p, p, Date.class));
			}
		}
		return obsCohortDefinition;
	}
	
	public static GenderCohortDefinition createFemaleCohortDefinition(String name) {
		GenderCohortDefinition femaleDefinition = new GenderCohortDefinition();
		femaleDefinition.setName(name);
		femaleDefinition.setFemaleIncluded(true);
		return femaleDefinition;
	}
	
	public static GenderCohortDefinition createMaleCohortDefinition(String name) {
		GenderCohortDefinition maleDefinition = new GenderCohortDefinition();
		maleDefinition.setName(name);
		maleDefinition.setMaleIncluded(true);
		return maleDefinition;
	}
	
	public static DrugsActiveCohortDefinition createDrugsActiveCohort(String name, String parameterName, List<Drug> drugs) {
		DrugsActiveCohortDefinition drugsActive = new DrugsActiveCohortDefinition();
		drugsActive.setName(name);
		drugsActive.setDrugs(drugs);
		drugsActive.addParameter(new Parameter(parameterName, parameterName, Date.class));
		return drugsActive;
	}
	
	public static CompositionCohortDefinition createHIVDiagnosisDate(String name) {
		DateObsCohortDefinition dateOfDiagnosis = new DateObsCohortDefinition();
		
		Concept diagnosisConcept = gp.getConcept(GlobalPropertiesManagement.HIV_DIAGNOSIS_DATE);
		
		dateOfDiagnosis.setQuestion(diagnosisConcept);
		dateOfDiagnosis.setTimeModifier(TimeModifier.ANY);
		
		CodedObsCohortDefinition positiveHIV = createCodedObsCohortDefinition("positiveHIV",
		    gp.getConcept(GlobalPropertiesManagement.HIV_TEST),
		    gp.getConcept(GlobalPropertiesManagement.POSITIVE_HIV_TEST_ANSWER), SetComparator.IN, TimeModifier.ANY);
		
		CompositionCohortDefinition diagnosis = new CompositionCohortDefinition();
		diagnosis.setName("diagnosis");
		diagnosis.getSearches().put("date", new Mapped<CohortDefinition>(dateOfDiagnosis, new HashMap<String, Object>()));
		diagnosis.getSearches().put("test", new Mapped<CohortDefinition>(positiveHIV, new HashMap<String, Object>()));
		diagnosis.setCompositionString("date or test");
		diagnosis.setCompositionString("test");
		return diagnosis;
	}
	
	public static SqlCohortDefinition getPatientsWithObservationInFormBetweenStartAndEndDate(String name, Form form,
	                                                                                         Concept concept) {
		SqlCohortDefinition query = new SqlCohortDefinition(
		        "select distinct o.person_id from encounter e, obs o where e.encounter_id=o.encounter_id and e.form_id="
		                + form.getId()
		                + " and o.concept_id="
		                + concept.getId()
		                + " and o.voided=0 and e.voided=0 and o.obs_datetime>= :startDate and o.obs_datetime<= :endDate and (o.value_numeric is NOT NULL or o.value_coded is NOT NULL or o.value_datetime is NOT NULL or o.value_boolean is NOT NULL)");
		query.setName(name);
		query.addParameter(new Parameter("startDate", "startDate", Date.class));
		query.addParameter(new Parameter("endDate", "endDate", Date.class));
		return query;
	}
	
	public static SqlCohortDefinition getPatientsWithObservationInForm(String name, Form form, Concept concept) {
		SqlCohortDefinition query = new SqlCohortDefinition(
		        "select distinct o.person_id from encounter e, obs o where e.encounter_id=o.encounter_id and e.form_id="
		                + form.getId()
		                + " and o.concept_id="
		                + concept.getId()
		                + " and o.voided=0 and e.voided=0 and (o.value_numeric is NOT NULL or o.value_coded is NOT NULL or o.value_datetime is NOT NULL or o.value_boolean is NOT NULL)");
		query.setName(name);
		return query;
	}
	
	public static SqlCohortDefinition getPatientsWithObservationValueDateTimeInFormBetweenStartAndEndDate(String name,
	                                                                                                      Form form,
	                                                                                                      Concept concept) {
		SqlCohortDefinition query = new SqlCohortDefinition(
		        "select distinct o.person_id from encounter e, obs o where e.encounter_id=o.encounter_id and e.form_id="
		                + form.getId() + " and o.concept_id=" + concept.getId()
		                + " and o.voided=0 and e.voided=0 and o.value_datetime>= :startDate and o.value_datetime<= :endDate");
		query.setName(name);
		query.addParameter(new Parameter("startDate", "startDate", Date.class));
		query.addParameter(new Parameter("endDate", "endDate", Date.class));
		return query;
	}
	
	public static SqlCohortDefinition getPatientsWithObservationInFormBetweenStartAndEndDateAndObsValueGreaterThanOrEqualTo(String name,
	                                                                                                                        Form form,
	                                                                                                                        Concept concept,
	                                                                                                                        int obsValue) {
		SqlCohortDefinition query = new SqlCohortDefinition(
		        "select distinct o.person_id from encounter e, obs o where e.encounter_id=o.encounter_id and e.form_id="
		                + form.getId() + " and o.concept_id=" + concept.getId() + " and o.value_numeric >=" + obsValue
		                + " and o.voided=0 and e.voided=0 and o.obs_datetime>= :startDate and o.obs_datetime<= :endDate");
		query.setName(name);
		query.addParameter(new Parameter("startDate", "startDate", Date.class));
		query.addParameter(new Parameter("endDate", "endDate", Date.class));
		return query;
	}
	
	public static SqlCohortDefinition getPatientsWithObservationBetweenStartAndEndDateAndObsValueGreaterThanOrEqualTo(String name,
	                                                                                                                  Concept concept,
	                                                                                                                  int obsValue) {
		SqlCohortDefinition query = new SqlCohortDefinition(
		        "select distinct o.person_id from encounter e, obs o where e.encounter_id=o.encounter_id and o.concept_id="
		                + concept.getId()
		                + " and o.value_numeric >="
		                + obsValue
		                + " and o.voided=0 and e.voided=0 and o.obs_datetime>= :startDate and o.obs_datetime<= :endDate and (o.value_numeric is NOT NULL or o.value_coded is NOT NULL or o.value_datetime is NOT NULL or o.value_boolean is NOT NULL)");
		query.setName(name);
		query.addParameter(new Parameter("startDate", "startDate", Date.class));
		query.addParameter(new Parameter("endDate", "endDate", Date.class));
		return query;
	}
	
	public static SqlCohortDefinition getPatientsWithObservationInFormBetweenStartAndEndDate(String name, List<Form> forms,
	                                                                                         Concept concept) {
		SqlCohortDefinition query = new SqlCohortDefinition();
		StringBuilder queryStr = new StringBuilder();
		queryStr.append("select distinct o.person_id from encounter e, obs o where e.encounter_id=o.encounter_id and e.form_id in(");
		
		int i = 0;
		for (Form f : forms) {
			if (i > 0) {
				queryStr.append(",");
			}
			queryStr.append(f.getId());
			
			i++;
		}
		
		queryStr.append(") and o.concept_id=");
		queryStr.append(concept.getId());
		queryStr.append(" and o.voided=0 and e.voided=0 and o.obs_datetime>= :startDate and o.obs_datetime<= :endDate and (o.value_numeric is NOT NULL or o.value_coded is NOT NULL or o.value_datetime is NOT NULL or o.value_boolean is NOT NULL)");
		query.setQuery(queryStr.toString());
		query.setName(name);
		query.addParameter(new Parameter("startDate", "startDate", Date.class));
		query.addParameter(new Parameter("endDate", "endDate", Date.class));
		return query;
	}
	
	public static SqlCohortDefinition getPatientWithStructuredDistrict(String name, String districtname) {
		SqlCohortDefinition query = new SqlCohortDefinition(
		        "select DISTINCT (p.patient_id) FROM patient p, person_address pa WHERE p.patient_id=pa.person_id AND p.voided=0 and pa.voided=0 "
		                + "AND pa.county_district='" + districtname + "' AND pa.preferred=1");
		return query;
	}
	
	public static SqlCohortDefinition getPatientWithunStructuredDistrict(String name) {
		SqlCohortDefinition query = new SqlCohortDefinition(
		        "select DISTINCT(p.patient_id) FROM patient p,person_address pa WHERE p.patient_id=pa.person_id AND pa.preferred=1 AND p.voided=0 "
		                + "AND ( pa.voided=1 OR pa.county_district='' ) ");
		return query;
	}
	
	public static SqlCohortDefinition getPatientsWithOutcomeprogramEndReasons(String name, Concept outcomequestion,
	                                                                          Concept outcomevalues) {
		SqlCohortDefinition query = new SqlCohortDefinition(
		        "select DISTINCT pp.patient_id FROM patient_program pp, program pro WHERE pro.program_id=pp.program_id AND pro.outcomes_concept_id="
		                + +outcomequestion.getId()
		                + " AND pp.outcome_concept_id="
		                + outcomevalues.getId()
		                + " AND pp.date_completed IS not null AND pp.voided=0 AND pp.date_completed>= :startDate AND pp.date_completed<= :endDate ");
		query.setName(name);
		query.addParameter(new Parameter("startDate", "startDate", Date.class));
		query.addParameter(new Parameter("endDate", "endDate", Date.class));
		return query;
	}
	
	public static SqlCohortDefinition getPatientsWithObservationInFormBetweenStartAndEndDate(String name, List<Form> forms,
	                                                                                         Concept concept,
	                                                                                         Concept obsAnswer) {
		SqlCohortDefinition query = new SqlCohortDefinition();
		StringBuilder queryStr = new StringBuilder();
		queryStr.append("select distinct o.person_id from encounter e, obs o where e.encounter_id=o.encounter_id and e.form_id in(");
		
		int i = 0;
		for (Form f : forms) {
			if (i > 0) {
				queryStr.append(",");
			}
			queryStr.append(f.getId());
			
			i++;
		}
		
		queryStr.append(") and o.concept_id=");
		queryStr.append(concept.getId());
		queryStr.append(" and o.value_coded=");
		queryStr.append(obsAnswer.getId());
		queryStr.append(" and o.voided=0 and e.voided=0 and o.obs_datetime>= :startDate and o.obs_datetime<= :endDate and (o.value_numeric is NOT NULL or o.value_coded is NOT NULL or o.value_datetime is NOT NULL or o.value_boolean is NOT NULL)");
		query.setQuery(queryStr.toString());
		query.setName(name);
		query.addParameter(new Parameter("startDate", "startDate", Date.class));
		query.addParameter(new Parameter("endDate", "endDate", Date.class));
		return query;
	}
	
	public static SqlCohortDefinition getPatientsWithObservationInFormBetweenStartAndEndDate(String name, Form form,
	                                                                                         Concept concept,
	                                                                                         Concept obsAnswer) {
		SqlCohortDefinition query = new SqlCohortDefinition();
		StringBuilder queryStr = new StringBuilder();
		queryStr.append("select distinct o.person_id from encounter e, obs o where e.encounter_id=o.encounter_id and e.form_id=");
		queryStr.append(form.getId());
		queryStr.append(" and o.concept_id=");
		queryStr.append(concept.getId());
		queryStr.append(" and o.value_coded=");
		queryStr.append(obsAnswer.getId());
		queryStr.append(" and o.voided=0 and e.voided=0 and o.obs_datetime>= :startDate and o.obs_datetime<= :endDate");
		query.setQuery(queryStr.toString());
		query.setName(name);
		query.addParameter(new Parameter("startDate", "startDate", Date.class));
		query.addParameter(new Parameter("endDate", "endDate", Date.class));
		return query;
	}
	
	public static SqlCohortDefinition getPatientsWithObservationInFormBetweenStartAndEndDate(String name, List<Form> forms,
	                                                                                         Concept concept,
	                                                                                         List<Concept> obsAnswers) {
		SqlCohortDefinition query = new SqlCohortDefinition();
		StringBuilder queryStr = new StringBuilder();
		queryStr.append("select lst.patient_id from (select last_encounter.patient_id, last_encounter.encounter_id from (select e.patient_id, e.encounter_datetime, e.encounter_id from encounter e where e.form_id in(");
		
		int i = 0;
		for (Form f : forms) {
			if (i > 0) {
				queryStr.append(",");
			}
			queryStr.append(f.getId());
			
			i++;
		}
		
		queryStr.append(") and e.encounter_datetime >= :startDate and e.encounter_datetime <= :endDate and e.voided=0 order by e.encounter_datetime desc) as last_encounter group by last_encounter.patient_id) as lst, obs o where lst.encounter_id=o.encounter_id and o.voided=0 and o.concept_id=");
		queryStr.append(concept.getId());
		queryStr.append(" and o.value_coded in (");
		
		int y = 0;
		for (Concept c : obsAnswers) {
			if (y > 0) {
				queryStr.append(",");
			}
			queryStr.append(c.getId());
			
			y++;
		}
		queryStr.append(")");
		query.setQuery(queryStr.toString());
		query.setName(name);
		query.addParameter(new Parameter("startDate", "startDate", Date.class));
		query.addParameter(new Parameter("endDate", "endDate", Date.class));
		return query;
	}
	
	public static SqlCohortDefinition getPatientsWithObservationInFormBetweenStartAndEndDate(String name, List<Form> forms,
	                                                                                         List<Concept> concepts) {
		SqlCohortDefinition query = new SqlCohortDefinition();
		StringBuilder queryStr = new StringBuilder();
		queryStr.append("select distinct o.person_id from encounter e, obs o where e.encounter_id=o.encounter_id and e.form_id in(");
		
		int i = 0;
		for (Form f : forms) {
			if (i > 0) {
				queryStr.append(",");
			}
			queryStr.append(f.getFormId());
			
			i++;
		}
		
		queryStr.append(") and o.concept_id in (");
		
		int j = 0;
		
		for (Concept concept : concepts) {
			if (j > 0) {
				queryStr.append(",");
			}
			queryStr.append(concept.getConceptId());
			j++;
		}
		queryStr.append(") and o.voided=0 and e.voided=0 and o.obs_datetime>= :startDate and o.obs_datetime<= :endDate and (o.value_numeric is NOT NULL or o.value_coded is NOT NULL or o.value_datetime is NOT NULL or o.value_boolean is NOT NULL)");
		query.setQuery(queryStr.toString());
		query.setName(name);
		query.addParameter(new Parameter("startDate", "startDate", Date.class));
		query.addParameter(new Parameter("endDate", "endDate", Date.class));
		return query;
	}
	
	public static SqlCohortDefinition getPatientsWithTwoObservationsBothInFormBetweenStartAndEndDate(String name, Form form,
	                                                                                                 Concept concept1,
	                                                                                                 Concept concept2) {
		SqlCohortDefinition query = new SqlCohortDefinition();
		StringBuilder queryStr = new StringBuilder();
		queryStr.append("select distinct firstSelect.person_id from ");
		
		queryStr.append("(select distinct o.person_id,o.encounter_id from encounter e, obs o where e.encounter_id=o.encounter_id and e.form_id =");
		
		queryStr.append(form.getFormId());
		queryStr.append(" and o.voided=0 and e.voided=0 and o.obs_datetime>= :startDate and o.obs_datetime<= :endDate and (o.value_numeric is NOT NULL or o.value_coded is NOT NULL or o.value_datetime is NOT NULL or o.value_boolean is NOT NULL)");
		
		queryStr.append(" and o.concept_id =" + concept1.getConceptId() + ") as firstSelect,");
		
		queryStr.append("(select distinct o.person_id,o.encounter_id from encounter e, obs o where e.encounter_id=o.encounter_id and e.form_id =");
		
		queryStr.append(form.getFormId());
		queryStr.append(" and o.voided=0 and e.voided=0 and o.obs_datetime>= :startDate and o.obs_datetime<= :endDate and (o.value_numeric is NOT NULL or o.value_coded is NOT NULL or o.value_datetime is NOT NULL or o.value_boolean is NOT NULL)");
		
		queryStr.append(" and o.concept_id =" + concept2.getConceptId() + ") as secondSelect");
		
		queryStr.append(" where firstSelect.encounter_id = secondSelect.encounter_id");
		
		//queryStr.append(" and o.voided=0 and e.voided=0 and o.obs_datetime>= :startDate and o.obs_datetime<= :endDate and (o.value_numeric is NOT NULL or o.value_coded is NOT NULL or o.value_datetime is NOT NULL or o.value_boolean is NOT NULL)");
		query.setQuery(queryStr.toString());
		query.setName(name);
		query.addParameter(new Parameter("startDate", "startDate", Date.class));
		query.addParameter(new Parameter("endDate", "endDate", Date.class));
		return query;
	}
	
	public static CompositionCohortDefinition getNewPatientsWithObservationAtIntakeBetweenStartAndEndDate(String name,
	                                                                                                      Program program,
	                                                                                                      Form form,
	                                                                                                      Concept concept) {
		SqlCohortDefinition glucoseAtIntake = Cohorts.getPatientsWithObservationInFormBetweenStartAndEndDate("atIntake",
		    form, concept);
		
		CompositionCohortDefinition patientEnrolledInProgram = Cohorts.createEnrolledInProgramDuringPeriod(
		    "EnrolledInProgram", program);
		
		CompositionCohortDefinition getNewPatientsWithObservation = new CompositionCohortDefinition();
		getNewPatientsWithObservation.setName(name);
		getNewPatientsWithObservation.addParameter(new Parameter("startDate", "startDate", Date.class));
		getNewPatientsWithObservation.addParameter(new Parameter("endDate", "endDate", Date.class));
		getNewPatientsWithObservation.getSearches().put(
		    "1",
		    new Mapped<CohortDefinition>(patientEnrolledInProgram, ParameterizableUtil
		            .createParameterMappings("startDate=${startDate},endDate=${endDate}")));
		getNewPatientsWithObservation.getSearches().put(
		    "2",
		    new Mapped<CohortDefinition>(glucoseAtIntake, ParameterizableUtil
		            .createParameterMappings("startDate=${startDate},endDate=${endDate}")));
		getNewPatientsWithObservation.setCompositionString("1 AND 2");
		
		return getNewPatientsWithObservation;
	}
	
	public static SqlCohortDefinition getPatientsOnCurrentRegimenBasedOnEndDate(String name, List<Concept> conceptSet) {
		SqlCohortDefinition patientOnRegimen = new SqlCohortDefinition();
		
		StringBuilder query = new StringBuilder("select distinct patient_id from orders where concept_id in (");
		
		int i = 0;
		for (Concept c : conceptSet) {
			if (i > 0) {
				query.append(",");
			}
			query.append(c.getId());
			i++;
		}
		
		query.append(") and voided=0 and start_date <= :endDate and (discontinued=0 or discontinued_date > :endDate)");
		patientOnRegimen.setQuery(query.toString());
		patientOnRegimen.addParameter(new Parameter("endDate", "endDate", Date.class));
		patientOnRegimen.setName(name);
		
		return patientOnRegimen;
	}
	
	public static SqlCohortDefinition getPatientsOnNOrMoreCurrentRegimenBasedOnEndDate(String name,
	                                                                                   List<Concept> conceptSet, int number) {
		SqlCohortDefinition patientOnRegimen = new SqlCohortDefinition();
		
		StringBuilder query = new StringBuilder(
		        "select patient_id from (select distinct patient_id, count(*) as total_orders from orders where concept_id in (");
		
		int i = 0;
		for (Concept c : conceptSet) {
			if (i > 0) {
				query.append(",");
			}
			query.append(c.getId());
			i++;
		}
		
		query.append(") and voided=0 and start_date <= :endDate and (discontinued=0 or discontinued_date > :endDate) group by patient_id) as b where b.total_orders >="
		        + number + "");
		patientOnRegimen.setQuery(query.toString());
		patientOnRegimen.addParameter(new Parameter("endDate", "endDate", Date.class));
		patientOnRegimen.setName(name);
		
		return patientOnRegimen;
	}
	
	public static SqlCohortDefinition getPatientsOnCurrentRegimenBasedOnEndDate(String name, Concept concept) {
		SqlCohortDefinition patientOnRegimen = new SqlCohortDefinition();
		
		StringBuilder query = new StringBuilder("select distinct patient_id from orders where concept_id in (");
		query.append(concept.getId());
		query.append(") and voided=0 and start_date <= :endDate and (discontinued=0 or discontinued_date > :endDate)");
		patientOnRegimen.setQuery(query.toString());
		patientOnRegimen.addParameter(new Parameter("endDate", "endDate", Date.class));
		patientOnRegimen.setName(name);
		
		return patientOnRegimen;
	}
	
	public static SqlCohortDefinition getPatientsOnCurrentRegimenBasedOnStartDateEndDate(String name, Concept concept) {
		SqlCohortDefinition patientOnRegimen = new SqlCohortDefinition();
		
		StringBuilder query = new StringBuilder("select distinct patient_id from orders where concept_id in (");
		query.append(concept.getId());
		query.append(") and voided=0 and start_date >= :startDate and discontinued=0 and discontinued_date <= :endDate");
		patientOnRegimen.setQuery(query.toString());
		patientOnRegimen.addParameter(new Parameter("startDate", "startDate", Date.class));
		patientOnRegimen.addParameter(new Parameter("endDate", "endDate", Date.class));
		patientOnRegimen.setName(name);
		
		return patientOnRegimen;
	}
	
	public static SqlCohortDefinition getPatientsCotrimoRegimenBasedOnStartDateEndDate(String name, Concept concept) {
		SqlCohortDefinition patientOnRegimen = new SqlCohortDefinition();
		
		StringBuilder query = new StringBuilder("select distinct patient_id from orders where concept_id in (");
		query.append(concept.getId());
		query.append(") and voided=0 and start_date >= :startDate and discontinued=0 and start_date <= :endDate");
		patientOnRegimen.setQuery(query.toString());
		patientOnRegimen.addParameter(new Parameter("startDate", "startDate", Date.class));
		patientOnRegimen.addParameter(new Parameter("endDate", "endDate", Date.class));
		patientOnRegimen.setName(name);
		
		return patientOnRegimen;
	}
	
	public static SqlCohortDefinition getPatientsOnRegimenBasedOnStartDateEndDate(String name, Concept concept) {
		SqlCohortDefinition patientOnRegimen = new SqlCohortDefinition();
		
		StringBuilder query = new StringBuilder("select distinct patient_id from orders where concept_id in (");
		query.append(concept.getId());
		query.append(") and voided=0 and start_date >= :startDate and discontinued=0 and start_date <= :endDate");
		patientOnRegimen.setQuery(query.toString());
		patientOnRegimen.addParameter(new Parameter("startDate", "startDate", Date.class));
		patientOnRegimen.addParameter(new Parameter("endDate", "endDate", Date.class));
		patientOnRegimen.setName(name);
		
		return patientOnRegimen;
	}
	
	public static SqlCohortDefinition getPatientsWithObservationAtLastVisit(String name, Concept concept,
	                                                                        EncounterType encounterType) {
		SqlCohortDefinition obsAtLastVist = new SqlCohortDefinition(
		        "select o.person_id from obs o,(select * from (select * from encounter e where e.encounter_type="
		                + encounterType.getId()
		                + " and e.voided=0 order by e.encounter_datetime desc) as lastencbypatient group by lastencbypatient.patient_id) as lastenc where lastenc.encounter_id=o.encounter_id and o.concept_id= "
		                + concept.getId() + " and o.voided=0 group by o.person_id");
		return obsAtLastVist;
	}
	
	public static SqlCohortDefinition getPatientsWithlastObservation(String name, Concept concept, Concept valueConcept) {
		SqlCohortDefinition lastObs = new SqlCohortDefinition(
		        "select lastObsbypatient.person_id FROM (select * FROM obs o WHERE o.concept_id="
		                + concept.getId()
		                + " AND o.value_coded="
		                + valueConcept.getId()
		                + " AND o.obs_datetime >= :onOrAfter AND o.obs_datetime <= :onOrBefore AND o.voided=0 ORDER BY o.obs_datetime DESC) AS lastObsbypatient "
		                + " GROUP BY lastObsbypatient.person_id");
		lastObs.addParameter(new Parameter("onOrAfter", "onOrAfter", Date.class));
		lastObs.addParameter(new Parameter("onOrBefore", "onOrBefore", Date.class));
		return lastObs;
	}
	
	public static SqlCohortDefinition getPatientsWithFirstDrugOrdersOnlyDurindStartEndDate(String name) {
		
		Concept ArtDrugOrderConceptSet = gp.getConcept(GlobalPropertiesManagement.ART_DRUGS_SET);
		SqlCohortDefinition firstOrder = new SqlCohortDefinition(
		
		"select DISTINCT d.patient_id FROM (select DISTINCT * FROM  (select DISTINCT * FROM orders o "
		        + "WHERE o.concept_id in (select DISTINCT concept_id FROM concept_set where concept_set="
		        + ArtDrugOrderConceptSet.getConceptId() + " )"
		        + "AND o.voided=0 and (discontinued=0 OR discontinued_date is null) ORDER BY o.start_date ASC) AS s "
		        + "GROUP BY s.patient_id ) AS d WHERE d.start_date>= :onOrAfter AND d.start_date<= :onOrBefore");
		
		firstOrder.addParameter(new Parameter("onOrAfter", "onOrAfter", Date.class));
		firstOrder.addParameter(new Parameter("onOrBefore", "onOrBefore", Date.class));
		
		return firstOrder;
		
	}
	
	public static SqlCohortDefinition getPatientsWithObservationsBetweenStartDateAndEndDate(String name,
	                                                                                        List<Concept> concepts) {
		SqlCohortDefinition obsBetweenStartDateAndEndDate = new SqlCohortDefinition();
		
		StringBuilder query = new StringBuilder("select distinct o.person_id from obs o where o.concept_id in (");
		int i = 0;
		for (Concept concept : concepts) {
			if (i > 0)
				query.append(",");
			query.append(concept.getId());
			i++;
		}
		query.append(") and o.voided=0 and o.obs_datetime>= :start and o.obs_datetime<= :end and o.value_numeric is NOT NULL");
		
		obsBetweenStartDateAndEndDate.setQuery(query.toString());
		obsBetweenStartDateAndEndDate.addParameter(new Parameter("start", "start", Date.class));
		obsBetweenStartDateAndEndDate.addParameter(new Parameter("end", "end", Date.class));
		
		return obsBetweenStartDateAndEndDate;
	}
	
	public static SqlCohortDefinition getPatientWithProgramAndConcept(String name, Program program, Concept concept,
	                                                                  Concept valueCoded) {
		SqlCohortDefinition codedInProgram = new SqlCohortDefinition(
		        "select lastObsbypatient.person_id FROM (select * FROM obs o WHERE (select distinct p.patient_id from patient p, patient_program pp "
		                + "where pp.voided = 0  and p.voided=0 and pp.patient_id = p.patient_id and p.patient_id=o.person_id and pp.program_id="
		                + program.getProgramId()
		                + ") "
		                + "and o.concept_id="
		                + concept.getId()
		                + " AND o.value_coded="
		                + valueCoded.getId()
		                + " and o.voided=0 ORDER BY o.obs_datetime DESC) "
		                + "AS lastObsbypatient GROUP BY lastObsbypatient.person_id");
		return codedInProgram;
	}
	
	public static SqlCohortDefinition getPatientsWithObservationsBetweenStartDateAndEndDate(String name, Concept concept) {
		SqlCohortDefinition obsBetweenStartDateAndEndDate = new SqlCohortDefinition();
		
		StringBuilder query = new StringBuilder("select distinct o.person_id from obs o where o.concept_id= ");
		
		query.append(concept.getId());
		
		query.append(" and o.voided=0 and o.obs_datetime>= :start and o.obs_datetime<= :end and (o.value_numeric is NOT NULL or o.value_coded is NOT NULL)");
		
		obsBetweenStartDateAndEndDate.setQuery(query.toString());
		obsBetweenStartDateAndEndDate.addParameter(new Parameter("start", "start", Date.class));
		obsBetweenStartDateAndEndDate.addParameter(new Parameter("end", "end", Date.class));
		
		return obsBetweenStartDateAndEndDate;
	}
	
	public static SqlCohortDefinition getPatientsOnRegimenAtLastVisit(String name, List<Concept> concepts,
	                                                                  EncounterType encounterType) {
		SqlCohortDefinition regimenAtLastVist = new SqlCohortDefinition();
		
		StringBuilder query = new StringBuilder(
		        "select o.patient_id from orders o,(select * from (select * from encounter e where e.encounter_type="
		                + encounterType.getId()
		                + " and e.voided=0 order by e.encounter_datetime desc) as lastencbypatient group by lastencbypatient.patient_id) as lastenc where lastenc.patient_id=o.patient_id and lastenc.encounter_datetime>o.start_date and o.concept_id in ( ");
		
		int i = 0;
		
		for (Concept concept : concepts) {
			if (i > 0)
				query.append(",");
			query.append(concept.getId());
			i++;
		}
		query.append(") and (o.discontinued=0 or discontinued_date >= lastenc.encounter_datetime) and o.voided=0 group by o.patient_id");
		regimenAtLastVist.setQuery(query.toString());
		
		return regimenAtLastVist;
	}
	
	public static SqlCohortDefinition getPatientsEverNotOnRegimen(String name, List<Concept> concepts) {
		SqlCohortDefinition patientsEverNotOnRegimen = new SqlCohortDefinition();
		
		StringBuilder query = new StringBuilder("select o.patient_id from orders o where o.concept_id in ( ");
		
		int i = 0;
		
		for (Concept concept : concepts) {
			if (i > 0)
				query.append(",");
			query.append(concept.getId());
			i++;
		}
		query.append(") and o.voided=0");
		patientsEverNotOnRegimen.setQuery(query.toString());
		
		return patientsEverNotOnRegimen;
	}
	
	public static SqlCohortDefinition getPatientsWithObservationInFormBetweenStartAndEndDate(String name,
	                                                                                         EncounterType encounterType,
	                                                                                         Concept concept) {
		SqlCohortDefinition query = new SqlCohortDefinition(
		        "select distinct o.person_id from encounter e, obs o where e.encounter_id=o.encounter_id and e.encounter_type="
		                + encounterType.getId()
		                + " and o.concept_id="
		                + concept.getId()
		                + " and o.voided=0 and e.voided=0 and o.obs_datetime>= :startDate and o.obs_datetime<= :endDate and (o.value_numeric is NOT NULL or o.value_coded is NOT NULL or o.value_datetime is NOT NULL or o.value_boolean is NOT NULL)");
		query.setName(name);
		query.addParameter(new Parameter("startDate", "startDate", Date.class));
		query.addParameter(new Parameter("endDate", "endDate", Date.class));
		return query;
	}
	
	public static SqlCohortDefinition getInvalidIMB(String name) {
		
		SqlCohortDefinition invalidimb = new SqlCohortDefinition();
		PatientIdentifierType invalidimbIdentifier = gp
		        .getPatientIdentifier(GlobalPropertiesManagement.INVALID_IMB_IDENTIFIER);
		invalidimb
		        .setQuery("select distinct pp.patient_id from patient pp, patient_identifier pi, patient_identifier_type pit where pp.patient_id=pi.patient_id and pit.patient_identifier_type_id=pi.identifier_type and pi.identifier_type="
		                + invalidimbIdentifier.getId() + " ");
		invalidimb.setName(name);
		
		return invalidimb;
	}
	
	public static SqlCohortDefinition getIMBId(String name) {
		
		SqlCohortDefinition imbId = new SqlCohortDefinition();
		PatientIdentifierType imbIdentifier = gp.getPatientIdentifier(GlobalPropertiesManagement.IMB_IDENTIFIER);
		imbId.setQuery("select distinct pp.patient_id from patient pp, patient_identifier pi, patient_identifier_type pit where pp.patient_id=pi.patient_id and pit.patient_identifier_type_id=pi.identifier_type and pi.identifier_type="
		        + imbIdentifier.getId() + " ");
		imbId.setName(name);
		
		return imbId;
	}
	
	public static SqlCohortDefinition getPciId(String name) {
		
		SqlCohortDefinition phcId = new SqlCohortDefinition();
		
		PatientIdentifierType primaryCareId = gp.getPatientIdentifier(GlobalPropertiesManagement.PC_IDENTIFIER);
		phcId.setQuery("select distinct pp.patient_id from patient pp, patient_identifier pi, patient_identifier_type pit where pp.patient_id=pi.patient_id and pit.patient_identifier_type_id=pi.identifier_type and pi.identifier_type="
		        + primaryCareId.getId() + " ");
		phcId.setName(name);
		
		return phcId;
	}
	
	public static SqlCohortDefinition getArtDrugs(String name) {
		List<Concept> artDrugsconcepts;
		artDrugsconcepts = gp.getConceptsByConceptSet(GlobalPropertiesManagement.ART_DRUGS_SET);
		String stringOfIdsOfConcepts = null;
		for (Concept concept : artDrugsconcepts) {
			stringOfIdsOfConcepts = stringOfIdsOfConcepts + "," + concept.getId();
		}
		SqlCohortDefinition onARTDrugs = new SqlCohortDefinition();
		onARTDrugs
		        .setQuery("select distinct o.patient_id from orders o,concept c where o.concept_id=c.concept_id and c.concept_id in ("
		                + stringOfIdsOfConcepts + ") and o.discontinued=0 and auto_expire_date is null and o.voided=0");
		onARTDrugs.setName(name);
		
		return onARTDrugs;
	}
	
	public static SqlCohortDefinition getPatientActiveOnArtDrugsByEndDate(String name) {
		Concept artDrugsconceptSet = gp.getConcept(GlobalPropertiesManagement.ART_DRUGS_SET);
		SqlCohortDefinition onARTDrugs = new SqlCohortDefinition();
		onARTDrugs
		        .setQuery("select distinct patient_id from orders where concept_id in (select concept_id from concept_set where concept_set="
		                + artDrugsconceptSet.getConceptId()
		                + ") and start_date<= :endDate and (discontinued_date>= :endDate or discontinued_date is null) and voided=0");
		onARTDrugs.setName(name);
		onARTDrugs.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		return onARTDrugs;
	}
	
	public static SqlCohortDefinition getTbDrugs(String name) {
		List<Concept> tbDrugsconcepts;
		tbDrugsconcepts = gp.getConceptsByConceptSet(GlobalPropertiesManagement.TB_TREATMENT_DRUGS);
		String stringOfIdsOfTbDrugsConcepts = null;
		for (Concept concept : tbDrugsconcepts) {
			stringOfIdsOfTbDrugsConcepts = stringOfIdsOfTbDrugsConcepts + "," + concept.getId();
		}
		SqlCohortDefinition onTBDrugs = new SqlCohortDefinition();
		onTBDrugs
		        .setQuery("select distinct o.patient_id from orders o,concept c where o.concept_id=c.concept_id and c.concept_id in ("
		                + stringOfIdsOfTbDrugsConcepts
		                + ") and o.discontinued=0 and (auto_expire_date is null or auto_expire_date > :now) and o.voided=0");
		onTBDrugs.addParameter(new Parameter("now", "now", Date.class));
		onTBDrugs.setName(name);
		
		return onTBDrugs;
	}
	
	public static SqlCohortDefinition geOnTBDrugsByStartEndDate(String name) {
		Concept tbDrugsconceptSet = gp.getConcept(GlobalPropertiesManagement.TB_TREATMENT_DRUGS);
		SqlCohortDefinition onTBDrugs = new SqlCohortDefinition();
		onTBDrugs
		        .setQuery("select distinct patient_id from orders where concept_id in (select concept_id from concept_set where concept_set="
		                + tbDrugsconceptSet.getConceptId()
		                + ") and start_date>= :startDate and start_date<= :endDate and voided=0");
		onTBDrugs.setName(name);
		onTBDrugs.addParameter(new Parameter("startDate", "startDate", Date.class));
		onTBDrugs.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		return onTBDrugs;
	}
	
	public static SqlCohortDefinition getPatientsWithLateVisitBasedOnReturnDateConcept(String name,
	                                                                                   EncounterType encounterType) {
		Concept returnVisit = gp.getConcept(GlobalPropertiesManagement.RETURN_VISIT_DATE);
		
		StringBuilder query = new StringBuilder(
		        "select p.patient_id from patient p, obs o, encounter e where p.voided = 0 and o.obs_id = (select obs_id o2 from obs o2, encounter e2 where o2.voided = 0 and p.patient_id = o2.person_id and o2.concept_id = ");
		query.append(returnVisit.getId());
		query.append(" and o2.value_datetime is not null and o2.encounter_id = e2.encounter_id and e2.encounter_type = ");
		query.append(encounterType.getId());
		query.append(" order by o2.obs_datetime desc LIMIT 1) and e.encounter_id = (select encounter_id from encounter where encounter_type = ");
		query.append(encounterType.getId());
		query.append(" and patient_id = p.patient_id order by encounter_datetime desc LIMIT 1) and e.patient_id = o.person_id and p.patient_id = e.patient_id and o.value_datetime > e.encounter_datetime  and o.value_datetime < :endDate");
		
		SqlCohortDefinition lateVisit = new SqlCohortDefinition(query.toString());
		lateVisit.setName(name);
		lateVisit.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		return lateVisit;
		
	}
	
	public static DateObsCohortDefinition createDateObsCohortDefinition(Concept concept, RangeComparator operator1,
	                                                                    RangeComparator operator2, TimeModifier timeModifier) {
		DateObsCohortDefinition due = new DateObsCohortDefinition();
		due.setOperator1(operator1);
		due.setOperator2(operator2);
		due.setTimeModifier(timeModifier);
		due.addParameter(new Parameter("value1", "value1", Date.class));
		due.addParameter(new Parameter("value2", "value2", Date.class));
		due.setName("patients due");
		due.setQuestion(concept);
		return due;
		
	}
	
	public static SqlCohortDefinition getMondayToSundayPatientReturnVisit(List<Form> forms) {
		
		SqlCohortDefinition cohortquery = new SqlCohortDefinition();
		Concept returnVisitDate = gp.getConcept(GlobalPropertiesManagement.RETURN_VISIT_DATE);
		StringBuilder formIds = new StringBuilder();
		int i = 0;
		for (Form form : forms) {
			if (i == 0) {
				formIds.append(form.getFormId());
			} else {
				formIds.append(",");
				formIds.append(form.getFormId());
			}
			i++;
		}
		//cohortquery.setQuery("select o.person_id from obs o,(select * from (select * from encounter where (form_id="+asthmaDDBFormId+" or encounter_type="+flowsheetAsthmas.getEncounterTypeId()+") order by encounter_datetime desc) as ordred_enc group by ordred_enc.patient_id) as last_enc where o.encounter_id=last_enc.encounter_id and last_enc.voided=0 and o.voided=0 and o.concept_id="+returnVisitDate.getConceptId()+" and o.value_datetime>=(select DATE_FORMAT(CURDATE()+(- (select IF(DAYOFWEEK(CURDATE())=1,6,DAYOFWEEK(CURDATE())-2) as sun)),'%Y-%m-%d')) and o.value_datetime<=(select DATE_FORMAT(CURDATE()+(- (select IF(DAYOFWEEK(CURDATE())=1,6,DAYOFWEEK(CURDATE())-2) as sun)+6),'%Y-%m-%d')) order by o.value_datetime");
		cohortquery
		        .setQuery("select o.person_id from obs o,(select * from (select * from encounter where form_id in ("
		                + formIds.toString()
		                + ")order by encounter_datetime desc) as ordred_enc group by ordred_enc.patient_id) as last_enc where o.encounter_id=last_enc.encounter_id and last_enc.voided=0 and o.voided=0 and o.concept_id="
		                + returnVisitDate.getConceptId()
		                + " and o.value_datetime>= :start and o.value_datetime<= :end order by o.value_datetime");
		cohortquery.addParameter(new Parameter("start", "start", Date.class));
		cohortquery.addParameter(new Parameter("end", "end", Date.class));
		return cohortquery;
	}
	
	public static SqlCohortDefinition getMondayToSundayPatientReturnVisitAndFollowUp(List<Form> forms) {
		
		SqlCohortDefinition cohortquery = new SqlCohortDefinition();		
		Concept returnVisitDate = gp.getConcept(GlobalPropertiesManagement.RETURN_VISIT_DATE);		
		Concept followupDate = gp.getConcept(GlobalPropertiesManagement.FOLLOW_UP_DATE);
		
		StringBuilder formIds = new StringBuilder();
		
		int i = 0;
		
		for (Form form : forms) {
			if (i == 0) {
				formIds.append(form.getFormId());
				
			} else {
				formIds.append(",");
				formIds.append(form.getFormId());
			}
			i++;
			
		}
		
		//cohortquery.setQuery("select o.person_id from obs o,(select * from (select * from encounter where (form_id="+asthmaDDBFormId+" or encounter_type="+flowsheetAsthmas.getEncounterTypeId()+") order by encounter_datetime desc) as ordred_enc group by ordred_enc.patient_id) as last_enc where o.encounter_id=last_enc.encounter_id and last_enc.voided=0 and o.voided=0 and o.concept_id="+returnVisitDate.getConceptId()+" and o.value_datetime>=(select DATE_FORMAT(CURDATE()+(- (select IF(DAYOFWEEK(CURDATE())=1,6,DAYOFWEEK(CURDATE())-2) as sun)),'%Y-%m-%d')) and o.value_datetime<=(select DATE_FORMAT(CURDATE()+(- (select IF(DAYOFWEEK(CURDATE())=1,6,DAYOFWEEK(CURDATE())-2) as sun)+6),'%Y-%m-%d')) order by o.value_datetime");
		
		cohortquery
		        .setQuery("select o.person_id from obs o,(select * from (select * from encounter where form_id in ("
		                
		                + formIds.toString()
		                
		                + ")order by encounter_datetime desc) as ordred_enc group by ordred_enc.patient_id) as last_enc where o.encounter_id=last_enc.encounter_id and last_enc.voided=0 and o.voided=0 and o.concept_id in ("
		                
		                + returnVisitDate.getConceptId() + "," + followupDate.getConceptId() + ")"
		                
		                + " and o.value_datetime>= :start and o.value_datetime<= :end order by o.value_datetime");
		
		cohortquery.addParameter(new Parameter("start", "start", Date.class));
		
		cohortquery.addParameter(new Parameter("end", "end", Date.class));
		
		return cohortquery;
		
	}
	
	public static SqlCohortDefinition getMondayToSundayPatientReturnVisit(List<Form> forms, Concept visitDate) {
		
		SqlCohortDefinition cohortquery = new SqlCohortDefinition();
		//Concept returnVisitDate = gp.getConcept(GlobalPropertiesManagement.RETURN_VISIT_DATE);
		StringBuilder formIds = new StringBuilder();
		int i = 0;
		for (Form form : forms) {
			if (i == 0) {
				formIds.append(form.getFormId());
			} else {
				formIds.append(",");
				formIds.append(form.getFormId());
			}
			i++;
		}
		//cohortquery.setQuery("select o.person_id from obs o,(select * from (select * from encounter where (form_id="+asthmaDDBFormId+" or encounter_type="+flowsheetAsthmas.getEncounterTypeId()+") order by encounter_datetime desc) as ordred_enc group by ordred_enc.patient_id) as last_enc where o.encounter_id=last_enc.encounter_id and last_enc.voided=0 and o.voided=0 and o.concept_id="+returnVisitDate.getConceptId()+" and o.value_datetime>=(select DATE_FORMAT(CURDATE()+(- (select IF(DAYOFWEEK(CURDATE())=1,6,DAYOFWEEK(CURDATE())-2) as sun)),'%Y-%m-%d')) and o.value_datetime<=(select DATE_FORMAT(CURDATE()+(- (select IF(DAYOFWEEK(CURDATE())=1,6,DAYOFWEEK(CURDATE())-2) as sun)+6),'%Y-%m-%d')) order by o.value_datetime");
		cohortquery
		        .setQuery("select o.person_id from obs o,(select * from (select * from encounter where form_id in ("
		                + formIds.toString()
		                + ")order by encounter_datetime desc) as ordred_enc group by ordred_enc.patient_id) as last_enc where o.encounter_id=last_enc.encounter_id and last_enc.voided=0 and o.voided=0 and o.concept_id="
		                + visitDate.getConceptId()
		                + " and o.value_datetime>= :start and o.value_datetime<= :end order by o.value_datetime");
		cohortquery.addParameter(new Parameter("start", "start", Date.class));
		cohortquery.addParameter(new Parameter("end", "end", Date.class));
		return cohortquery;
	}
	
	public static SqlCohortDefinition getMondayToSundayPatientReturnVisit(List<Form> forms, List<Concept> visitDates) {
		
		SqlCohortDefinition cohortquery = new SqlCohortDefinition();
		//Concept returnVisitDate = gp.getConcept(GlobalPropertiesManagement.RETURN_VISIT_DATE);
		StringBuilder formIds = new StringBuilder();
		int i = 0;
		for (Form form : forms) {
			if (i == 0) {
				formIds.append(form.getFormId());
			} else {
				formIds.append(",");
				formIds.append(form.getFormId());
			}
			i++;
		}
		StringBuilder conceptIds = new StringBuilder();
		
		int j = 0;
		for (Concept concept : visitDates) {
			if (j == 0) {
				conceptIds.append(concept.getConceptId());
			} else {
				conceptIds.append(",");
				conceptIds.append(concept.getConceptId());
			}
			j++;
		}
		//cohortquery.setQuery("select o.person_id from obs o,(select * from (select * from encounter where (form_id="+asthmaDDBFormId+" or encounter_type="+flowsheetAsthmas.getEncounterTypeId()+") order by encounter_datetime desc) as ordred_enc group by ordred_enc.patient_id) as last_enc where o.encounter_id=last_enc.encounter_id and last_enc.voided=0 and o.voided=0 and o.concept_id="+returnVisitDate.getConceptId()+" and o.value_datetime>=(select DATE_FORMAT(CURDATE()+(- (select IF(DAYOFWEEK(CURDATE())=1,6,DAYOFWEEK(CURDATE())-2) as sun)),'%Y-%m-%d')) and o.value_datetime<=(select DATE_FORMAT(CURDATE()+(- (select IF(DAYOFWEEK(CURDATE())=1,6,DAYOFWEEK(CURDATE())-2) as sun)+6),'%Y-%m-%d')) order by o.value_datetime");
		cohortquery
		        .setQuery("select o.person_id from obs o,(select * from (select * from encounter where form_id in ("
		                + formIds.toString()
		                + ")order by encounter_datetime desc) as ordred_enc group by ordred_enc.patient_id) as last_enc where o.encounter_id=last_enc.encounter_id and last_enc.voided=0 and o.voided=0 and o.concept_id in ("
		                + conceptIds.toString()
		                + ") and o.value_datetime>= :start and o.value_datetime<= :end order by o.value_datetime");
		cohortquery.addParameter(new Parameter("start", "start", Date.class));
		cohortquery.addParameter(new Parameter("end", "end", Date.class));
		return cohortquery;
	}
	
	public static SqlCohortDefinition createPatientsLateForVisit(List<Form> forms, EncounterType encounterType) {
		
		StringBuilder sql = new StringBuilder();
		sql.append("select o.person_id from obs o, (select * from (select * from encounter where form_id in (");
		
		boolean first = true;
		for (Form f : forms) {
			if (!first) {
				sql.append(",");
			}
			
			sql.append(f.getFormId());
			first = false;
		}
		
		sql.append(") and voided=0 order by encounter_datetime desc) as e group by e.patient_id) as last_encounters, (select * from (select * from encounter where encounter_type=");
		sql.append(encounterType.getEncounterTypeId());
		sql.append(" and voided=0 order by encounter_datetime desc) as e group by e.patient_id) as last_Visit where last_encounters.encounter_id=o.encounter_id and last_encounters.encounter_datetime<o.value_datetime and o.voided=0 and o.concept_id=");
		
		Concept nextVisit = gp.getConcept(GlobalPropertiesManagement.RETURN_VISIT_DATE);
		sql.append(nextVisit.getConceptId());
		
		sql.append(" and DATEDIFF(:endDate,o.value_datetime)>7 and (not last_Visit.encounter_datetime > o.value_datetime) and last_Visit.patient_id=o.person_id");
		
		SqlCohortDefinition lateVisit = new SqlCohortDefinition(sql.toString());
		lateVisit.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		return lateVisit;
	}
	
	public static CompositionCohortDefinition createPatientsLateForVisit(Concept concept, EncounterType encounterType) {
		
		StringBuilder sql = new StringBuilder();
		sql.append("select lastObs.person_id from (select * from (select * from obs where voided = 0 and concept_id=  ");
		sql.append(concept.getConceptId());
		sql.append(" order by value_datetime desc) as o group by o.person_id) as lastObs, (select * from (select * from encounter where encounter_type=");
		sql.append(encounterType.getEncounterTypeId());
		sql.append("  and voided=0 order by encounter_datetime desc) as e group by e.patient_id) as last_Visit where ");
		sql.append(" DATEDIFF(:endDate,lastObs.value_datetime)>6 and (not last_Visit.encounter_datetime > lastObs.value_datetime) and last_Visit.patient_id=lastObs.person_id");
		
		SqlCohortDefinition lateVisit = new SqlCohortDefinition(sql.toString());
		lateVisit.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		StringBuilder sql2 = new StringBuilder();
		sql2.append("select o.person_id from obs o where o.voided=0 and o.concept_id=");
		sql2.append(concept.getConceptId());
		sql2.append(" and DATEDIFF(:endDate,o.value_datetime)>6 and o.person_id not in(select patient_id from encounter where encounter_type =");
		sql2.append(encounterType.getEncounterTypeId());
		sql2.append(" and voided = 0)");
		
		SqlCohortDefinition lateVisitNoEncounter = new SqlCohortDefinition(sql2.toString());
		lateVisitNoEncounter.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		CompositionCohortDefinition visit = new CompositionCohortDefinition();
		visit.addParameter(new Parameter("endDate", "endDate", Date.class));
		visit.getSearches().put("1",
		    new Mapped<CohortDefinition>(lateVisit, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		visit.getSearches().put(
		    "2",
		    new Mapped<CohortDefinition>(lateVisitNoEncounter, ParameterizableUtil
		            .createParameterMappings("endDate=${endDate}")));
		visit.setCompositionString("1 OR 2");
		
		return visit;
	}
	
	public static CompositionCohortDefinition createPatientsLateForPDCVisit(Concept concept, EncounterType encounterType) {
		
		StringBuilder sql = new StringBuilder();
		sql.append("select lastObs.person_id from (select * from (select * from obs where voided = 0 and concept_id=  ");
		sql.append(concept.getConceptId());
		sql.append(" order by value_datetime desc) as o group by o.person_id) as lastObs, (select * from (select * from encounter where encounter_type=");
		sql.append(encounterType.getEncounterTypeId());
		sql.append("  and voided=0 order by encounter_datetime desc) as e group by e.patient_id) as last_Visit where ");
		sql.append(" DATEDIFF(:endDate,lastObs.value_datetime)>1 and (not last_Visit.encounter_datetime > lastObs.value_datetime) and last_Visit.patient_id=lastObs.person_id");
		
		SqlCohortDefinition lateVisit = new SqlCohortDefinition(sql.toString());
		lateVisit.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		StringBuilder sql2 = new StringBuilder();
		sql2.append("select o.person_id from obs o where o.voided=0 and o.concept_id=");
		sql2.append(concept.getConceptId());
		sql2.append(" and DATEDIFF(:endDate,o.value_datetime)>1 and o.person_id not in(select patient_id from encounter where encounter_type =");
		sql2.append(encounterType.getEncounterTypeId());
		sql2.append(" and voided = 0)");
		
		SqlCohortDefinition lateVisitNoEncounter = new SqlCohortDefinition(sql2.toString());
		lateVisitNoEncounter.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		CompositionCohortDefinition visit = new CompositionCohortDefinition();
		visit.addParameter(new Parameter("endDate", "endDate", Date.class));
		visit.getSearches().put("1", new Mapped<CohortDefinition>(lateVisit, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		visit.getSearches().put("2", new Mapped<CohortDefinition>(lateVisitNoEncounter, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		visit.setCompositionString("1 OR 2");
		
		return visit;
	}
	
	public static CompositionCohortDefinition createPatientsLateForVisit(List<Concept> concepts, List<Form> forms) {
		
		StringBuilder sql = new StringBuilder();
		sql.append("select lastObs.person_id from (select * from (select * from obs where voided = 0 and concept_id in (");
		boolean second = true;
		for (Concept c : concepts) {
			if (!second) {
				sql.append(",");
			}
			
			sql.append(c.getConceptId());
			second = false;
		}
		sql.append(") order by value_datetime desc) as o group by o.person_id) as lastObs, (select * from (select * from encounter where form_id in (");
		boolean first = true;
		for (Form f : forms) {
			if (!first) {
				sql.append(",");
			}
			
			sql.append(f.getFormId());
			first = false;
		}
		sql.append(")  and voided=0 order by encounter_datetime desc) as e group by e.patient_id) as last_Visit where ");
		sql.append(" DATEDIFF(:endDate,lastObs.value_datetime)>6 and (not last_Visit.encounter_datetime > lastObs.value_datetime) and last_Visit.patient_id=lastObs.person_id");
		
		SqlCohortDefinition lateVisit = new SqlCohortDefinition(sql.toString());
		lateVisit.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		StringBuilder sql2 = new StringBuilder();
		sql2.append("select o.person_id from obs o where o.voided=0 and o.concept_id in (");
		boolean third = true;
		for (Concept c : concepts) {
			if (!third) {
				sql2.append(",");
			}
			
			sql2.append(c.getConceptId());
			third = false;
		}
		sql2.append(") and DATEDIFF(:endDate,o.value_datetime)>6 and o.person_id not in(select patient_id from encounter where form_id in (");
		boolean fourth = true;
		for (Form f : forms) {
			if (!fourth) {
				sql2.append(",");
			}
			
			sql2.append(f.getFormId());
			fourth = false;
		}
		sql2.append(") and voided = 0)");
		
		SqlCohortDefinition lateVisitNoEncounter = new SqlCohortDefinition(sql2.toString());
		lateVisitNoEncounter.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		CompositionCohortDefinition visit = new CompositionCohortDefinition();
		visit.addParameter(new Parameter("endDate", "endDate", Date.class));
		visit.getSearches().put("1",
		    new Mapped<CohortDefinition>(lateVisit, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		visit.getSearches().put(
		    "2",
		    new Mapped<CohortDefinition>(lateVisitNoEncounter, ParameterizableUtil
		            .createParameterMappings("endDate=${endDate}")));
		visit.setCompositionString("1 OR 2");
		
		return visit;
	}
	
	public static CompositionCohortDefinition createPatientsLateForVisit(Concept concept, List<Form> forms) {
		
		StringBuilder sql = new StringBuilder();
		sql.append("select lastObs.person_id from (select * from (select * from obs where voided = 0 and concept_id=");
		
		sql.append(concept.getConceptId());
		
		sql.append(" order by value_datetime desc) as o group by o.person_id) as lastObs, (select * from (select * from encounter where form_id in (");
		boolean first = true;
		for (Form f : forms) {
			if (!first) {
				sql.append(",");
			}
			
			sql.append(f.getFormId());
			first = false;
		}
		sql.append(")  and voided=0 order by encounter_datetime desc) as e group by e.patient_id) as last_Visit where ");
		sql.append(" DATEDIFF(:endDate,lastObs.value_datetime)>6 and (not last_Visit.encounter_datetime > lastObs.value_datetime) and last_Visit.patient_id=lastObs.person_id");
		
		SqlCohortDefinition lateVisit = new SqlCohortDefinition(sql.toString());
		lateVisit.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		StringBuilder sql2 = new StringBuilder();
		sql2.append("select o.person_id from obs o where o.voided=0 and o.concept_id=");
		sql2.append(concept.getConceptId());
		sql2.append(" and DATEDIFF(:endDate,o.value_datetime)>6 and o.person_id not in(select patient_id from encounter where form_id in (");
		boolean fourth = true;
		for (Form f : forms) {
			if (!fourth) {
				sql2.append(",");
			}
			
			sql2.append(f.getFormId());
			fourth = false;
		}
		sql2.append(") and voided = 0)");
		
		SqlCohortDefinition lateVisitNoEncounter = new SqlCohortDefinition(sql2.toString());
		lateVisitNoEncounter.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		CompositionCohortDefinition visit = new CompositionCohortDefinition();
		visit.addParameter(new Parameter("endDate", "endDate", Date.class));
		visit.getSearches().put("1",
		    new Mapped<CohortDefinition>(lateVisit, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		visit.getSearches().put(
		    "2",
		    new Mapped<CohortDefinition>(lateVisitNoEncounter, ParameterizableUtil
		            .createParameterMappings("endDate=${endDate}")));
		visit.setCompositionString("1 OR 2");
		
		return visit;
	}
	
	public static CompositionCohortDefinition createPatientsLateForVisit(Concept concept, Form form) {
		
		StringBuilder sql = new StringBuilder();
		sql.append("select lastObs.person_id from (select * from (select * from obs where voided = 0 and concept_id=  ");
		sql.append(concept.getConceptId());
		sql.append(" order by value_datetime desc) as o group by o.person_id) as lastObs, (select * from (select * from encounter where form_id=");
		sql.append(form.getFormId());
		sql.append("  and voided=0 order by encounter_datetime desc) as e group by e.patient_id) as last_Visit where ");
		sql.append(" DATEDIFF(:endDate,lastObs.value_datetime)>6 and (not last_Visit.encounter_datetime > lastObs.value_datetime) and last_Visit.patient_id=lastObs.person_id");
		
		SqlCohortDefinition lateVisit = new SqlCohortDefinition(sql.toString());
		lateVisit.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		StringBuilder sql2 = new StringBuilder();
		sql2.append("select o.person_id from obs o where o.voided=0 and o.concept_id=");
		sql2.append(concept.getConceptId());
		sql2.append(" and DATEDIFF(:endDate,o.value_datetime)>6 and o.person_id not in(select patient_id from encounter where form_id =");
		sql2.append(form.getFormId());
		sql2.append(" and voided = 0)");
		
		SqlCohortDefinition lateVisitNoEncounter = new SqlCohortDefinition(sql2.toString());
		lateVisitNoEncounter.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		CompositionCohortDefinition visit = new CompositionCohortDefinition();
		visit.addParameter(new Parameter("endDate", "endDate", Date.class));
		visit.getSearches().put("1",
		    new Mapped<CohortDefinition>(lateVisit, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		visit.getSearches().put(
		    "2",
		    new Mapped<CohortDefinition>(lateVisitNoEncounter, ParameterizableUtil
		            .createParameterMappings("endDate=${endDate}")));
		visit.setCompositionString("1 OR 2");
		
		return visit;
	}
	
	public static CompositionCohortDefinition createPatientLateForVisit(Concept concept, Form form) {
		
		StringBuilder sql = new StringBuilder();
		sql.append("select lastObs.person_id from (select * from (select * from obs where voided = 0 and concept_id=  ");
		sql.append(concept.getConceptId());
		sql.append(" order by value_datetime desc) as o group by o.person_id) as lastObs, (select * from (select * from encounter where form_id!=");
		sql.append(form.getFormId());
		sql.append("  and voided=0 order by encounter_datetime desc) as e group by e.patient_id) as last_Visit where ");
		sql.append(" DATEDIFF(:endDate,lastObs.value_datetime)>7 and (not last_Visit.encounter_datetime > lastObs.value_datetime) and last_Visit.patient_id=lastObs.person_id");
		
		SqlCohortDefinition lateVisit = new SqlCohortDefinition(sql.toString());
		
		lateVisit.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		StringBuilder sql2 = new StringBuilder();
		
		sql2.append("select o.person_id from obs o where o.voided=0 and o.concept_id=");
		
		sql2.append(concept.getConceptId());
		
		sql2.append(" and DATEDIFF(:endDate,o.value_datetime)>7 and o.person_id not in(select patient_id from encounter where form_id =");
		
		sql2.append(form.getFormId());
		sql2.append(" and voided = 0)");
		
		SqlCohortDefinition lateVisitNoEncounter = new SqlCohortDefinition(sql2.toString());
		
		lateVisitNoEncounter.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		CompositionCohortDefinition visit = new CompositionCohortDefinition();
		
		visit.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		visit.getSearches().put("1",
		    new Mapped<CohortDefinition>(lateVisit, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		
		visit.getSearches().put(
		    "2",
		    new Mapped<CohortDefinition>(lateVisitNoEncounter, ParameterizableUtil
		            .createParameterMappings("endDate=${endDate}")));
		
		visit.setCompositionString("1 OR 2");
		
		return visit;
		
	}
	
	public static CompositionCohortDefinition createPatientsWhereMostRecentEncounterIsForm(Form form,
	                                                                                       EncounterType encounterType) {
		
		StringBuilder sql = new StringBuilder();
		sql.append("select lastResult.patient_id from (select * from (select * from encounter where voided = 0 and form_id=");
		sql.append(form.getFormId());
		sql.append(" order by encounter_datetime desc) as o group by o.patient_id) as lastResult, (select * from (select * from encounter where encounter_type=");
		sql.append(encounterType.getEncounterTypeId());
		sql.append("  and voided=0 order by encounter_datetime desc) as e group by e.patient_id) as last_Visit where ");
		sql.append(" DATEDIFF(:endDate,lastResult.encounter_datetime)>=0 and (not last_Visit.encounter_datetime > lastResult.encounter_datetime) and last_Visit.patient_id=lastResult.patient_id");
		
		SqlCohortDefinition lateVisit = new SqlCohortDefinition(sql.toString());
		lateVisit.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		StringBuilder sql2 = new StringBuilder();
		sql2.append("select o.patient_id from encounter o where o.voided=0 and o.form_id=");
		sql2.append(form.getFormId());
		sql2.append(" and o.patient_id not in(select patient_id from encounter where encounter_type =");
		sql2.append(encounterType.getEncounterTypeId());
		sql2.append(" and voided = 0)");
		
		SqlCohortDefinition lateVisitNoEncounter = new SqlCohortDefinition(sql2.toString());
		lateVisitNoEncounter.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		CompositionCohortDefinition visit = new CompositionCohortDefinition();
		visit.addParameter(new Parameter("endDate", "endDate", Date.class));
		visit.getSearches().put("1",
		    new Mapped<CohortDefinition>(lateVisit, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		visit.getSearches().put(
		    "2",
		    new Mapped<CohortDefinition>(lateVisitNoEncounter, ParameterizableUtil
		            .createParameterMappings("endDate=${endDate}")));
		visit.setCompositionString("1 OR 2");
		
		return visit;
	}
	
	public static CompositionCohortDefinition createPatientsWhereMostRecentEncounterIsForm(Form form, Concept concept) {
		
		StringBuilder sql = new StringBuilder();
		sql.append("select lastResult.patient_id from (select * from (select * from encounter where voided = 0 and form_id=");
		sql.append(form.getFormId());
		sql.append(" order by encounter_datetime desc) as o group by o.patient_id) as lastResult, (select * from (select * from obs where voided = 0 and concept_id=  ");
		sql.append(concept.getConceptId());
		sql.append(" order by obs_datetime desc) as o group by o.person_id) as lastObs where ");
		sql.append(" DATEDIFF(:endDate,lastResult.encounter_datetime)>=0 and (not lastObs.obs_datetime > lastResult.encounter_datetime) and lastObs.person_id=lastResult.patient_id");
		
		SqlCohortDefinition lateVisit = new SqlCohortDefinition(sql.toString());
		lateVisit.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		StringBuilder sql2 = new StringBuilder();
		sql2.append("select o.patient_id from encounter o where o.voided=0 and o.form_id=");
		sql2.append(form.getFormId());
		sql2.append(" and o.patient_id not in(select person_id from obs where concept_id =");
		sql2.append(concept.getConceptId());
		sql2.append(" and voided = 0)");
		
		SqlCohortDefinition lateVisitNoEncounter = new SqlCohortDefinition(sql2.toString());
		lateVisitNoEncounter.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		CompositionCohortDefinition visit = new CompositionCohortDefinition();
		visit.addParameter(new Parameter("endDate", "endDate", Date.class));
		visit.getSearches().put("1",
		    new Mapped<CohortDefinition>(lateVisit, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		visit.getSearches().put(
		    "2",
		    new Mapped<CohortDefinition>(lateVisitNoEncounter, ParameterizableUtil
		            .createParameterMappings("endDate=${endDate}")));
		visit.setCompositionString("1 OR 2");
		
		return visit;
	}
	
	public static CompositionCohortDefinition createPatientsDueForVisit(Concept concept, Form form) {
		
		StringBuilder sql = new StringBuilder();
		sql.append("select lastObs.person_id from (select * from (select * from obs where voided = 0 and concept_id=  ");
		sql.append(concept.getConceptId());
		sql.append(" order by value_datetime desc) as o group by o.person_id) as lastObs, (select * from (select * from encounter where form_id=");
		sql.append(form.getFormId());
		sql.append("  and voided=0 order by encounter_datetime desc) as e group by e.patient_id) as last_Visit where ");
		sql.append(" DATEDIFF(lastObs.value_datetime,:endDate)>=0 and DATEDIFF(lastObs.value_datetime,:endDate)<=7 and (not last_Visit.encounter_datetime > lastObs.obs_datetime) and last_Visit.patient_id=lastObs.person_id");
		
		SqlCohortDefinition lateVisit = new SqlCohortDefinition(sql.toString());
		lateVisit.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		StringBuilder sql2 = new StringBuilder();
		sql2.append("select o.person_id from obs o where o.voided=0 and o.concept_id=");
		sql2.append(concept.getConceptId());
		sql2.append(" and DATEDIFF(o.value_datetime,:endDate)>=0 and DATEDIFF(o.value_datetime,:endDate)<=7 and o.person_id not in(select patient_id from encounter where form_id=");
		sql2.append(form.getFormId());
		sql2.append(" and voided = 0)");
		
		SqlCohortDefinition lateVisitNoEncounter = new SqlCohortDefinition(sql2.toString());
		lateVisitNoEncounter.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		CompositionCohortDefinition visit = new CompositionCohortDefinition();
		visit.addParameter(new Parameter("endDate", "endDate", Date.class));
		visit.getSearches().put("1",
		    new Mapped<CohortDefinition>(lateVisit, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		visit.getSearches().put(
		    "2",
		    new Mapped<CohortDefinition>(lateVisitNoEncounter, ParameterizableUtil
		            .createParameterMappings("endDate=${endDate}")));
		visit.setCompositionString("1 OR 2");
		
		return visit;
	}
	
	public static CompositionCohortDefinition createPatientsOverdueForVisit(Form initial, Form visit) {
		
		StringBuilder sql = new StringBuilder();
		sql.append("select lastObs.patient_id from (select * from (select * from encounter where voided = 0 and form_id=  ");
		sql.append(initial.getFormId());
		sql.append(" order by encounter_datetime desc) as o group by o.patient_id) as lastObs, (select * from (select * from encounter where form_id=");
		sql.append(visit.getFormId());
		sql.append("  and voided=0 order by encounter_datetime desc) as e group by e.patient_id) as last_Visit where ");
		sql.append(" DATEDIFF(:endDate,lastObs.encounter_datetime)>21  and (not last_Visit.encounter_datetime > lastObs.encounter_datetime) and last_Visit.patient_id=lastObs.patient_id");
		
		SqlCohortDefinition lateVisit = new SqlCohortDefinition(sql.toString());
		lateVisit.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		StringBuilder sql2 = new StringBuilder();
		sql2.append("select o.patient_id from encounter o where o.voided=0 and o.form_id=");
		sql2.append(initial.getFormId());
		sql2.append(" and DATEDIFF(:endDate,o.encounter_datetime)>21 and o.patient_id not in(select patient_id from encounter where form_id=");
		sql2.append(visit.getFormId());
		sql2.append(" and voided = 0)");
		
		SqlCohortDefinition lateVisitNoEncounter = new SqlCohortDefinition(sql2.toString());
		lateVisitNoEncounter.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		CompositionCohortDefinition visitDef = new CompositionCohortDefinition();
		visitDef.addParameter(new Parameter("endDate", "endDate", Date.class));
		visitDef.getSearches().put("1",
		    new Mapped<CohortDefinition>(lateVisit, ParameterizableUtil.createParameterMappings("endDate=${endDate}")));
		visitDef.getSearches().put(
		    "2",
		    new Mapped<CohortDefinition>(lateVisitNoEncounter, ParameterizableUtil
		            .createParameterMappings("endDate=${endDate}")));
		visitDef.setCompositionString("1 OR 2");
		
		return visitDef;
	}
	
	public static SqlCohortDefinition getPatientsWithNTimesOrMoreEncountersByStartAndEndDate(String name,
	                                                                                         EncounterType encType, int times) {
		SqlCohortDefinition nTimesEncounter = new SqlCohortDefinition();
		nTimesEncounter.setName(name);
		/*nTimesEncounter
		        .setQuery("select patient_id from (select patient_id,count(patient_id) as times from encounter where encounter_type="
		                + encType.getEncounterTypeId()
		                + " and encounter_datetime>= :startDate and encounter_datetime<= :endDate and voided=0 group by patient_id) as moreenc where times>="
		                + times + "");
		 */
		nTimesEncounter
		        .setQuery("select patient_id from encounter where encounter_type="
		                + encType.getEncounterTypeId()
		                + " and encounter_datetime>= :startDate and encounter_datetime<= :endDate and voided=0  group by patient_id");
		nTimesEncounter.addParameter(new Parameter("startDate", "startDate", Date.class));
		nTimesEncounter.addParameter(new Parameter("endDate", "endDate", Date.class));
		return nTimesEncounter;
	}
	
	public static SqlCohortDefinition getPatientsEnrolledFortheFirstTimeinHIvProgram(String name, Program program, int times) {
		SqlCohortDefinition nTimesEncounter = new SqlCohortDefinition();
		nTimesEncounter.setName(name);
		nTimesEncounter
		        .setQuery("select DISTINCT patient_id FROM (select patient_id,count(patient_id) AS times FROM patient_program WHERE program_id="
		                + program.getProgramId()
		                + " AND date_enrolled>= :startDate AND date_enrolled<= :endDate AND date_completed is null AND voided=0 group by patient_id) AS oneprog WHERE times="
		                + times + "");
		nTimesEncounter.addParameter(new Parameter("startDate", "startDate", Date.class));
		nTimesEncounter.addParameter(new Parameter("endDate", "endDate", Date.class));
		return nTimesEncounter;
	}
	
	public static SqlCohortDefinition getPatientsWithEchocardiographyDocumented(String name) {
		Concept echocardiographResult = gp.getConcept(GlobalPropertiesManagement.DDB_ECHOCARDIOGRAPH_RESULT);
		Concept notDone = gp.getConcept(GlobalPropertiesManagement.NOT_DONE);
		Concept tbDrugsconceptSet = gp.getConcept(GlobalPropertiesManagement.DDB_ECHOCARDIOGRAPH_COMMENT);
		
		SqlCohortDefinition withEchocardiographyDocumented = new SqlCohortDefinition();
		withEchocardiographyDocumented.setQuery("select distinct person_id from obs where ((obs.concept_id ="
		        + echocardiographResult.getConceptId() + " and obs.value_coded !=" + notDone.getConceptId()
		        + ") or obs.concept_id=" + tbDrugsconceptSet.getConceptId()
		        + ") and obs.voided=0 and obs.obs_datetime>= :startDate and obs.obs_datetime<= :endDate");
		withEchocardiographyDocumented.setName(name);
		withEchocardiographyDocumented.addParameter(new Parameter("startDate", "startDate", Date.class));
		withEchocardiographyDocumented.addParameter(new Parameter("endDate", "endDate", Date.class));
		
		return withEchocardiographyDocumented;
	}
	
	public static SqlCohortDefinition getPatientsOnRegimenAtLastVisit(String name, Concept concept,
	                                                                  EncounterType encounterType) {
		SqlCohortDefinition regimenAtLastVist = new SqlCohortDefinition();
		
		StringBuilder query = new StringBuilder(
		        "select o.patient_id from orders o,(select * from (select * from encounter e where e.encounter_type="
		                + encounterType.getId()
		                + " and e.voided=0 order by e.encounter_datetime desc) as lastencbypatient group by lastencbypatient.patient_id) as lastenc where lastenc.patient_id=o.patient_id and lastenc.encounter_datetime>o.start_date and o.concept_id= ");
		
		query.append(concept.getId());
		
		query.append(" and o.discontinued=0 and o.voided=0 group by o.patient_id");
		regimenAtLastVist.setQuery(query.toString());
		
		return regimenAtLastVist;
	}
	
}
