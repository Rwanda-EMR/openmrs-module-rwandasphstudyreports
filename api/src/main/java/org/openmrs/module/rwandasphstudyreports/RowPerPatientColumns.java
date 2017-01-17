package org.openmrs.module.rwandasphstudyreports;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.rowperpatientreports.patientdata.definition.AgeAtDateOfOtherDefinition;
import org.openmrs.module.rowperpatientreports.patientdata.definition.AllDrugOrdersRestrictedByConcept;
import org.openmrs.module.rowperpatientreports.patientdata.definition.AllDrugOrdersRestrictedByConceptSet;
import org.openmrs.module.rowperpatientreports.patientdata.definition.AllObservationValues;
import org.openmrs.module.rowperpatientreports.patientdata.definition.BaselineDrugOrder;
import org.openmrs.module.rowperpatientreports.patientdata.definition.BaselineEncounter;
import org.openmrs.module.rowperpatientreports.patientdata.definition.BaselineObservation;
import org.openmrs.module.rowperpatientreports.patientdata.definition.BaselineObservationAnswer;
import org.openmrs.module.rowperpatientreports.patientdata.definition.BaselineProgramEnrollment;
import org.openmrs.module.rowperpatientreports.patientdata.definition.CurrentOrdersRestrictedByConceptSet;
import org.openmrs.module.rowperpatientreports.patientdata.definition.CustomCalculationBasedOnMultiplePatientDataDefinitions;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateDiff;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateDiff.DateDiffType;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateOfAllProgramEnrolment;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateOfBirthShowingEstimation;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateOfFirstDrugOrderStartedRestrictedByConceptSet;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateOfMostRecentEncounterOfType;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateOfObsAfterDateOfOtherDefinition;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateOfPatientData;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateOfProgramCompletion;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateOfProgramEnrolment;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateOfWorkflowStateChange;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DatesOfVisitsByStartDateAndEndDate;
import org.openmrs.module.rowperpatientreports.patientdata.definition.EnrolledInProgram;
import org.openmrs.module.rowperpatientreports.patientdata.definition.EvaluateDefinitionForOtherPersonData;
import org.openmrs.module.rowperpatientreports.patientdata.definition.FirstDrugOrderStartedAfterDateRestrictedByConceptSet;
import org.openmrs.module.rowperpatientreports.patientdata.definition.FirstDrugOrderStartedRestrictedByConceptSet;
import org.openmrs.module.rowperpatientreports.patientdata.definition.FirstRecordedObservation;
import org.openmrs.module.rowperpatientreports.patientdata.definition.FirstRecordedObservationWithCodedConceptAnswer;
import org.openmrs.module.rowperpatientreports.patientdata.definition.FirstStateOfPatient;
import org.openmrs.module.rowperpatientreports.patientdata.definition.FullHistoryOfProgramWorkflowStates;
import org.openmrs.module.rowperpatientreports.patientdata.definition.MostRecentObservation;
import org.openmrs.module.rowperpatientreports.patientdata.definition.MostRecentObsgroup;
import org.openmrs.module.rowperpatientreports.patientdata.definition.MostRecentProgramWorkflowState;
import org.openmrs.module.rowperpatientreports.patientdata.definition.MultiplePatientDataDefinitions;
import org.openmrs.module.rowperpatientreports.patientdata.definition.ObsAfterPeriodOfTimeFromEncounterDate;
import org.openmrs.module.rowperpatientreports.patientdata.definition.ObsValueAfterDateOfOtherDefinition;
import org.openmrs.module.rowperpatientreports.patientdata.definition.ObsValueBeforeDateOfOtherDefinition;
import org.openmrs.module.rowperpatientreports.patientdata.definition.ObservationInMostRecentEncounter;
import org.openmrs.module.rowperpatientreports.patientdata.definition.ObservationInMostRecentEncounterOfType;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientAddress;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientAgeInMonths;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientAttribute;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientHash;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientIdentifier;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientProperty;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientRelationship;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PersonData;
import org.openmrs.module.rowperpatientreports.patientdata.definition.RecentEncounter;
import org.openmrs.module.rowperpatientreports.patientdata.definition.RecentEncounterType;
import org.openmrs.module.rowperpatientreports.patientdata.definition.ResultFilter;
import org.openmrs.module.rowperpatientreports.patientdata.definition.RetrievePersonByRelationship;
import org.openmrs.module.rowperpatientreports.patientdata.definition.RetrievePersonByRelationshipAndByProgram;
import org.openmrs.module.rowperpatientreports.patientdata.definition.RowPerPatientData;
import org.openmrs.module.rowperpatientreports.patientdata.definition.StateOfPatient;
import org.openmrs.module.rowperpatientreports.patientdata.definition.StateOfPatientMatchWithEncounter;
import org.openmrs.module.rowperpatientreports.patientdata.definition.SystemIdentifier;
import org.openmrs.module.rwandareports.customcalculator.BooleanCalculation;
import org.openmrs.module.rwandareports.definition.ArtSwitch;
import org.openmrs.module.rwandareports.definition.ArtSwitchDate;
import org.openmrs.module.rwandareports.definition.CurrentPatientDrugOrder;
import org.openmrs.module.rwandareports.definition.CurrentPatientProgram;
import org.openmrs.module.rwandareports.definition.DrugRegimenInformation;
import org.openmrs.module.rwandareports.definition.HIVOutcome;
import org.openmrs.module.rwandareports.definition.LastWeekMostRecentObservation;
import org.openmrs.module.rwandareports.definition.RegimenDateInformation;
import org.openmrs.module.rwandareports.definition.StartOfArt;

public class RowPerPatientColumns {

	static GlobalPropertiesManagement gp = new GlobalPropertiesManagement();

	public static PatientProperty getFirstNameColumn(String name) {
		PatientProperty givenName = new PatientProperty("givenName");
		givenName.setName(name);
		return givenName;
	}

	public static PatientProperty getMiddleNameColumn(String name) {
		PatientProperty givenName = new PatientProperty("middleName");
		givenName.setName(name);
		return givenName;
	}

	public static PatientProperty getFamilyNameColumn(String name) {
		PatientProperty familyName = new PatientProperty("familyName");
		familyName.setName(name);
		return familyName;
	}

	public static PatientProperty getAge(String name) {
		PatientProperty age = new PatientProperty("age");
		age.setName(name);
		return age;
	}

	public static PatientAgeInMonths getAgeInMonths(String name) {
		PatientAgeInMonths ageInMonths = new PatientAgeInMonths();
		ageInMonths.setName(name);
		return ageInMonths;
	}

	public static AgeAtDateOfOtherDefinition getAgeAtDateOfOtherDefinition(String name, DateOfPatientData definition) {
		AgeAtDateOfOtherDefinition age = new AgeAtDateOfOtherDefinition();
		age.setDateOfPatientData(definition, new HashMap<String, Object>());
		return age;
	}

	public static PatientProperty getGender(String name) {
		PatientProperty gender = new PatientProperty("gender");
		gender.setName(name);
		return gender;
	}

	public static DateOfBirthShowingEstimation getDateOfBirth(String name, String dateFormat,
			String estimatedDateFormat) {
		DateOfBirthShowingEstimation birthdate = new DateOfBirthShowingEstimation();
		birthdate.setName(name);

		if (dateFormat != null) {
			birthdate.setDateFormat(dateFormat);
		}
		if (estimatedDateFormat != null) {
			birthdate.setEstimatedDateFormat(estimatedDateFormat);
		}
		return birthdate;
	}

	public static MultiplePatientDataDefinitions getIMBId(String name) {
		PatientIdentifierType imbType = gp.getPatientIdentifier(GlobalPropertiesManagement.IMB_IDENTIFIER);
		PatientIdentifier imbId = new PatientIdentifier(imbType);

		PatientIdentifierType pcType = gp.getPatientIdentifier(GlobalPropertiesManagement.PC_IDENTIFIER);
		PatientIdentifier pcId = new PatientIdentifier(pcType);

		MultiplePatientDataDefinitions id = new MultiplePatientDataDefinitions();
		id.setName(name);
		id.addPatientDataDefinition(imbId, new HashMap<String, Object>());
		id.addPatientDataDefinition(pcId, new HashMap<String, Object>());

		return id;
	}

	public static MultiplePatientDataDefinitions getTracnetId(String name) {
		PatientIdentifierType tracNetId = gp.getPatientIdentifier(GlobalPropertiesManagement.TRACNET_IDENTIFIER);
		PatientIdentifier id = new PatientIdentifier(tracNetId);

		MultiplePatientDataDefinitions ids = new MultiplePatientDataDefinitions();
		ids.setName(name);
		ids.addPatientDataDefinition(id, new HashMap<String, Object>());

		return ids;
	}

	public static SystemIdentifier getSystemId(String name) {
		SystemIdentifier systemId = new SystemIdentifier();
		systemId.setName(name);
		return systemId;
	}

	public static RetrievePersonByRelationship getMother() {
		RetrievePersonByRelationship mother = new RetrievePersonByRelationship();
		mother.setRelationshipTypeId(
				gp.getRelationshipType(GlobalPropertiesManagement.MOTHER_RELATIONSHIP).getRelationshipTypeId());
		mother.setRetrievePersonAorB("A");
		return mother;
	}

	public static RetrievePersonByRelationshipAndByProgram getMother(Program program) {
		RetrievePersonByRelationshipAndByProgram mother = new RetrievePersonByRelationshipAndByProgram();
		mother.setRelationshipTypeId(
				gp.getRelationshipType(GlobalPropertiesManagement.MOTHER_RELATIONSHIP).getRelationshipTypeId());
		mother.setRetrievePersonAorB("A");
		mother.setProgram(program);
		return mother;
	}

	public static StateOfPatient getStateOfPatient(String name, Program program, ProgramWorkflow programWorkflow,
			ResultFilter filter) {
		StateOfPatient state = new StateOfPatient();
		state.setPatientProgram(program);
		state.setPatienProgramWorkflow(programWorkflow);
		state.setName(name);

		if (filter != null) {
			state.setFilter(filter);
		}

		return state;
	}

	public static StateOfPatientMatchWithEncounter getStateOfPatientMatchingWithEncounter(String name, Program program,
			ProgramWorkflow programWorkflow, ArrayList<Form> forms, ResultFilter filter) {
		StateOfPatientMatchWithEncounter state = new StateOfPatientMatchWithEncounter();
		state.setProgram(program);
		state.setPatienProgramWorkflow(programWorkflow);
		state.setName(name);
		state.setForms(forms);

		if (filter != null) {
			state.setFilter(filter);
		}

		return state;
	}

	public static FirstStateOfPatient getFirstStateOfPatient(String name, Program program,
			ProgramWorkflow programWorkflow, ResultFilter filter) {
		FirstStateOfPatient state = new FirstStateOfPatient();
		state.setPatientProgram(program);
		state.setPatienProgramWorkflow(programWorkflow);
		state.setName(name);

		if (filter != null) {
			state.setFilter(filter);
		}

		return state;
	}

	public static StateOfPatient getStateOfPatient(String name, Program program, ProgramWorkflow programWorkflow,
			boolean includeCompleted, ResultFilter filter) {
		StateOfPatient state = new StateOfPatient();
		state.setPatientProgram(program);
		state.setPatienProgramWorkflow(programWorkflow);
		state.setName(name);
		state.setIncludeCompleted(includeCompleted);

		if (filter != null) {
			state.setFilter(filter);
		}

		return state;
	}

	public static CurrentPatientProgram getCurrentPatientProgram(String name, Program program) {
		CurrentPatientProgram ppr = new CurrentPatientProgram(program);
		ppr.setName(name);
		return ppr;
	}

	public static RecentEncounterType getRecentEncounterType(String name, List<EncounterType> encounterTypes,
			ResultFilter filter) {
		RecentEncounterType lastEncounter = new RecentEncounterType();
		lastEncounter.setName(name);
		lastEncounter.setEncounterTypes(encounterTypes);
		lastEncounter.setFilter(filter);
		return lastEncounter;
	}

	public static RecentEncounterType getRecentEncounterType(String name, List<EncounterType> encounterTypes,
			String dateFormat, ResultFilter filter) {
		RecentEncounterType lastEncounter = getRecentEncounterType(name, encounterTypes, filter);
		if (dateFormat != null) {
			lastEncounter.setDateFormat(dateFormat);
		}
		return lastEncounter;
	}

	public static RecentEncounter getRecentEncounter(String name, List<Form> forms, List<EncounterType> encounterTypes,
			String dateFormat, ResultFilter filter) {
		RecentEncounter lastEncounter = new RecentEncounter();
		lastEncounter.setName(name);
		if (encounterTypes != null) {
			lastEncounter.setEncounterTypes(encounterTypes);
		}
		if (forms != null) {
			lastEncounter.setForms(forms);
		}
		lastEncounter.setFilter(filter);
		if (dateFormat != null) {
			lastEncounter.setDateFormat(dateFormat);
		}
		return lastEncounter;
	}

	public static DateOfMostRecentEncounterOfType getDateOfMostRecentEncounterType(String name,
			List<EncounterType> encounterTypes, String dateFormat) {
		DateOfMostRecentEncounterOfType lastEncounter = new DateOfMostRecentEncounterOfType();
		lastEncounter.setName(name);
		lastEncounter.setEncounterTypes(encounterTypes);
		if (dateFormat != null) {
			lastEncounter.setDateFormat(dateFormat);
		}
		return lastEncounter;
	}

	public static DateOfMostRecentEncounterOfType getDateOfMostRecentEncounterType(String name,
			List<EncounterType> encounterTypes, String parameterName, String dateFormat) {
		DateOfMostRecentEncounterOfType lastEncounter = new DateOfMostRecentEncounterOfType();
		lastEncounter.addParameter(new Parameter(parameterName, parameterName, Date.class));
		lastEncounter.setName(name);
		lastEncounter.setEncounterTypes(encounterTypes);
		if (dateFormat != null) {
			lastEncounter.setDateFormat(dateFormat);
		}
		return lastEncounter;
	}

	public static DateDiff getDifferenceSinceLastEncounter(String name, List<EncounterType> encounterTypes,
			DateDiffType differenceType) {
		DateDiff lastVisit = new DateDiff();
		lastVisit.setName(name);
		if (differenceType == null)
			differenceType = DateDiffType.DAYS; // Should prevent null values
												// instead
		lastVisit.setDateDiffType(differenceType);
		lastVisit.setEncounterTypes(encounterTypes);
		return lastVisit;
	}

	public static DateDiff getDifferenceSinceLastObservation(String name, Concept concept,
			DateDiffType differenceType) {
		DateDiff lastObs = new DateDiff();
		lastObs.setName(name);
		if (differenceType == null)
			differenceType = DateDiffType.DAYS; // Should prevent null values
												// instead
		lastObs.setDateDiffType(differenceType);
		lastObs.setConcept(concept);
		return lastObs;
	}

	public static MultiplePatientDataDefinitions getMultiplePatientDataDefinitions(String name,
			List<RowPerPatientData> definitions) {
		MultiplePatientDataDefinitions mult = new MultiplePatientDataDefinitions();
		mult.setName(name);

		for (RowPerPatientData pd : definitions) {
			mult.addPatientDataDefinition(pd, new HashMap<String, Object>());
		}
		return mult;
	}

	public static PatientAttribute getHealthCenter(String name) {
		PatientAttribute healthCenter = new PatientAttribute();
		healthCenter.setAttribute("Health Center");
		healthCenter.setName(name);
		return healthCenter;
	}

	public static PatientHash getPatientHash(String name) {
		PatientHash hash = new PatientHash();
		hash.setName(name);
		return hash;
	}

	public static StateOfPatient getTreatmentGroupOfHIVPatient(String name, ResultFilter filter) {
		return getStateOfPatient(name, gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM),
				gp.getProgramWorkflow(GlobalPropertiesManagement.TREATMENT_GROUP_WORKFLOW,
						GlobalPropertiesManagement.ADULT_HIV_PROGRAM),
				filter);
	}

	public static StateOfPatient getTreatmentGroupOfHIVPatientIncludingCompleted(String name, ResultFilter filter) {
		return getStateOfPatient(name, gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM),
				gp.getProgramWorkflow(GlobalPropertiesManagement.TREATMENT_GROUP_WORKFLOW,
						GlobalPropertiesManagement.ADULT_HIV_PROGRAM),
				true, filter);
	}

	public static StateOfPatient getTreatmentGroupOfPediHIVPatient(String name, ResultFilter filter) {
		return getStateOfPatient(name, gp.getProgram(GlobalPropertiesManagement.PEDI_HIV_PROGRAM),
				gp.getProgramWorkflow(GlobalPropertiesManagement.TREATMENT_GROUP_WORKFLOW,
						GlobalPropertiesManagement.PEDI_HIV_PROGRAM),
				filter);
	}

	public static StateOfPatient getTreatmentGroupOfPediHIVPatientIncludingCompleted(String name, ResultFilter filter) {
		return getStateOfPatient(name, gp.getProgram(GlobalPropertiesManagement.PEDI_HIV_PROGRAM),
				gp.getProgramWorkflow(GlobalPropertiesManagement.TREATMENT_GROUP_WORKFLOW,
						GlobalPropertiesManagement.PEDI_HIV_PROGRAM),
				true, filter);
	}

	public static StateOfPatient getTreatmentStatusOfHIVPatientIncludingCompleted(String name, ResultFilter filter) {
		return getStateOfPatient(name, gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM),
				gp.getProgramWorkflow(GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW,
						GlobalPropertiesManagement.ADULT_HIV_PROGRAM),
				true, filter);
	}

	public static StateOfPatient getTreatmentStatusOfPediHIVPatientIncludingCompleted(String name,
			ResultFilter filter) {
		return getStateOfPatient(name, gp.getProgram(GlobalPropertiesManagement.PEDI_HIV_PROGRAM),
				gp.getProgramWorkflow(GlobalPropertiesManagement.TREATMENT_STATUS_WORKFLOW,
						GlobalPropertiesManagement.PEDI_HIV_PROGRAM),
				true, filter);
	}

	public static MultiplePatientDataDefinitions getTreatmentGroupOfAllHIVPatient(String name, ResultFilter filter) {
		MultiplePatientDataDefinitions mdd = new MultiplePatientDataDefinitions();
		mdd.setName(name);
		mdd.addPatientDataDefinition(getTreatmentGroupOfHIVPatient(name, filter), new HashMap<String, Object>());
		mdd.addPatientDataDefinition(getTreatmentGroupOfPediHIVPatient(name, filter), new HashMap<String, Object>());
		return mdd;
	}

	public static MultiplePatientDataDefinitions getTreatmentGroupOfAllHIVPatientIncludingCompleted(String name,
			ResultFilter filter) {
		MultiplePatientDataDefinitions mdd = new MultiplePatientDataDefinitions();
		mdd.setName(name);
		mdd.addPatientDataDefinition(getTreatmentGroupOfHIVPatientIncludingCompleted(name, filter),
				new HashMap<String, Object>());
		mdd.addPatientDataDefinition(getTreatmentGroupOfPediHIVPatientIncludingCompleted(name, filter),
				new HashMap<String, Object>());
		return mdd;
	}

	public static MultiplePatientDataDefinitions getTreamentStatusOfAllHIVPatientIncludingCompleted(String name) {
		MultiplePatientDataDefinitions mdd = new MultiplePatientDataDefinitions();
		mdd.setName(name);
		mdd.addPatientDataDefinition(getTreatmentStatusOfHIVPatientIncludingCompleted(name, null),
				new HashMap<String, Object>());
		mdd.addPatientDataDefinition(getTreatmentStatusOfPediHIVPatientIncludingCompleted(name, null),
				new HashMap<String, Object>());
		return mdd;
	}

	public static MostRecentObservation getMostRecentTbTest(String name, String dateFormat) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.TB_TEST_CONCEPT), dateFormat);
	}

	public static MostRecentObservation getMostRecentHbA1c(String name, String dateFormat) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.HBA1C), dateFormat);
	}

	public static MostRecentObservation getMostRecentCreatinine(String name, String dateFormat) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.SERUM_CREATININE), dateFormat);
	}

	public static MostRecentObservation getMostRecentSBP(String name, String dateFormat) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.SYSTOLIC_BLOOD_PRESSURE), dateFormat);
	}

	public static MostRecentObservation getMostRecentPatientPhoneNumber(String name, String dateFormat) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.TELEPHONE_NUMBER_CONCEPT), dateFormat);
	}

	public static MostRecentObservation getMostRecentWeight(String name, String dateFormat) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.WEIGHT_CONCEPT), dateFormat);
	}

	public static MostRecentObservation getMostRecentWeight(String name, String dateFormat, ResultFilter resultFilter) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.WEIGHT_CONCEPT), dateFormat, resultFilter);
	}

	public static MostRecentObservation getMostRecentHeight(String name, String dateFormat) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.HEIGHT_CONCEPT), dateFormat);
	}

	public static MostRecentObservation getMostRecentBSA(String name, String dateFormat) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.BSA_CONCEPT), dateFormat);

	}

	public static MostRecentObservation getMostRecentIO(String name, String dateFormat) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.OPPORTUNISTIC_INFECTIONS), dateFormat);
	}

	public static MostRecentObservation getMostRecenSideEffect(String name, String dateFormat) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.ADVERSE_EFFECT_CONCEPT), dateFormat);
	}

	public static MostRecentObservation getMostRecentHeight(String name, String dateFormat, ResultFilter resultFilter) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.HEIGHT_CONCEPT), dateFormat, resultFilter);
	}

	public static MostRecentObservation getMostRecentCD4(String name, String dateFormat) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.CD4_TEST), dateFormat);
	}

	public static MostRecentObservation getMostRecentCD4(String name, String dateFormat, ResultFilter resultFilter) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.CD4_TEST), dateFormat, resultFilter);
	}

	public static MostRecentObservation getMostRecentCD4Percentage(String name, String dateFormat) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.CD4_PERCENTAGE_TEST), dateFormat);
	}

	public static MostRecentObservation getMostRecentViralLoad(String name, String dateFormat) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.VIRAL_LOAD_TEST), dateFormat);
	}

	public static MostRecentObservation getMostRecentReturnVisitDate(String name, String dateFormat) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.RETURN_VISIT_DATE), dateFormat);
	}

	public static MostRecentObservation getMostRecentReturnVisitDate(String name, String dateFormat,
			ResultFilter resultFilter) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.RETURN_VISIT_DATE), dateFormat,
				resultFilter);
	}

	public static MostRecentObservation getMostRecentTemperature(String name, String dateFormat) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.TEMPERATURE), dateFormat);
	}

	public static MostRecentObservation getMostRecentINR(String name, String dateFormat) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.INTERNATIONAL_NORMALIZED_RATIO),
				dateFormat);
	}

	public static LastWeekMostRecentObservation getLastWeekMostRecentReturnVisitDate(String name, String dateFormat,
			ResultFilter resultFilter) {
		return getLastWeekMostRecent(name, gp.getConcept(GlobalPropertiesManagement.RETURN_VISIT_DATE), dateFormat,
				resultFilter);
	}

	public static MostRecentObservation getMostRecentPeakFlow(String name, String dateFormat) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.PEAK_FLOW_AFTER_SALBUTAMOL), dateFormat);
	}

	public static MostRecentObservation getMostRecentSystolicPB(String name, String dateFormat) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.SYSTOLIC_BLOOD_PRESSURE), dateFormat);
	}

	public static MostRecentObservation getMostRecentDiastolicPB(String name, String dateFormat) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.DIASTOLIC_BLOOD_PRESSURE), dateFormat);
	}

	public static MostRecentObservation getMostRecentSeizure(String name, String dateFormat) {
		return getMostRecent(name, gp.getConcept(GlobalPropertiesManagement.SEIZURE_CONCEPT), dateFormat);
	}

	public static AllObservationValues getAllWeightValues(String name, String dateFormat, ResultFilter resultFilter,
			ResultFilter outputFilter) {
		return getAllObservationValues(name, gp.getConcept(GlobalPropertiesManagement.WEIGHT_CONCEPT), dateFormat,
				resultFilter, outputFilter);
	}

	public static AllObservationValues getAllINRValues(String name, String dateFormat, ResultFilter resultFilter,
			ResultFilter outputFilter) {
		return getAllObservationValues(name, gp.getConcept(GlobalPropertiesManagement.INTERNATIONAL_NORMALIZED_RATIO),
				dateFormat, resultFilter, outputFilter);
	}

	public static AllObservationValues getAllCD4Values(String name, String dateFormat, ResultFilter resultFilter,
			ResultFilter outputFilter) {
		return getAllObservationValues(name, gp.getConcept(GlobalPropertiesManagement.CD4_TEST), dateFormat,
				resultFilter, outputFilter);
	}

	public static AllObservationValues getAllViralLoadsValues(String name, String dateFormat, ResultFilter resultFilter,
			ResultFilter outputFilter) {
		return getAllObservationValues(name, gp.getConcept(GlobalPropertiesManagement.VIRAL_LOAD_TEST), dateFormat,
				resultFilter, outputFilter);
	}

	public static AllObservationValues getAllAsthmaClassificationValues(String name, String dateFormat,
			ResultFilter resultFilter, ResultFilter outputFilter) {
		return getAllObservationValues(name, gp.getConcept(GlobalPropertiesManagement.ASTHMA_CLASSIFICATION),
				dateFormat, resultFilter, outputFilter);
	}

	public static ObservationInMostRecentEncounterOfType getIOInMostRecentEncounterOfType(String name,
			EncounterType encounterType) {
		return getObservationInMostRecentEncounterOfType(name, gp.getConcept(GlobalPropertiesManagement.IO_CONCEPT),
				encounterType);
	}

	public static ObservationInMostRecentEncounterOfType getReturnVisitInMostRecentEncounterOfType(String name,
			EncounterType encounterType) {
		return getObservationInMostRecentEncounterOfType(name,
				gp.getConcept(GlobalPropertiesManagement.RETURN_VISIT_DATE), encounterType);
	}

	public static ObservationInMostRecentEncounterOfType getSideEffectInMostRecentEncounterOfType(String name,
			EncounterType encounterType) {
		return getObservationInMostRecentEncounterOfType(name,
				gp.getConcept(GlobalPropertiesManagement.SIDE_EFFECT_CONCEPT), encounterType);
	}

	public static ObservationInMostRecentEncounterOfType getSeizureInMostRecentEncounterOfType(String name,
			EncounterType encounterType,
			ObservationInMostRecentEncounterOfType observationInMostRecentEncounterOfType) {
		return getObservationInMostRecentEncounterOfType(name,
				gp.getConcept(GlobalPropertiesManagement.SEIZURE_CONCEPT), encounterType);
	}

	public static ObservationInMostRecentEncounterOfType getNextVisitInMostRecentEncounterOfTypes(String name,
			EncounterType encounterType, ObservationInMostRecentEncounterOfType observationInMostRecentEncounterOfType,
			ResultFilter resultFilter) {
		return getObservationInMostRecentEncounterOfType(name,
				gp.getConcept(GlobalPropertiesManagement.RETURN_VISIT_DATE), encounterType, resultFilter);
	}

	public static PatientRelationship getAccompRelationship(String name) {
		return getPatientRelationship(name,
				gp.getRelationshipType(GlobalPropertiesManagement.ACCOMPAGNATUER_RELATIONSHIP).getRelationshipTypeId(),
				"A", null);
	}

	public static PatientRelationship getAccompRelationship(String name, ResultFilter accompagnateurFilter) {
		return getPatientRelationship(name,
				gp.getRelationshipType(GlobalPropertiesManagement.ACCOMPAGNATUER_RELATIONSHIP).getRelationshipTypeId(),
				"A", accompagnateurFilter);
	}

	public static RetrievePersonByRelationshipAndByProgram getMotherRelationship(String name, Program program) {
		RetrievePersonByRelationshipAndByProgram mother = new RetrievePersonByRelationshipAndByProgram();
		mother.setRelationshipTypeId(
				gp.getRelationshipType(GlobalPropertiesManagement.MOTHER_RELATIONSHIP).getRelationshipTypeId());
		mother.setRetrievePersonAorB("A");
		mother.setProgram(program);
		return mother;
	}

	public static PatientRelationship getMotherRelationship(String name) {
		return getPatientRelationship(name,
				gp.getRelationshipType(GlobalPropertiesManagement.MOTHER_RELATIONSHIP).getRelationshipTypeId(), "A",
				null);
	}

	public static CurrentOrdersRestrictedByConceptSet getCurrentARTOrders(String name, String dateFormat,
			ResultFilter drugFilter) {
		return getCurrentOrdersRestrictedByConceptSet(name, gp.getConcept(GlobalPropertiesManagement.ART_DRUGS_SET),
				dateFormat, drugFilter);
	}

	public static CurrentOrdersRestrictedByConceptSet getCurrentTBOrders(String name, String dateFormat,
			ResultFilter drugFilter) {
		return getCurrentOrdersRestrictedByConceptSet(name,
				gp.getConcept(GlobalPropertiesManagement.TB_TREATMENT_DRUGS), dateFormat, drugFilter);
	}

	public static CurrentOrdersRestrictedByConceptSet getCurrentDiabetesOrders(String name, String dateFormat,
			ResultFilter drugFilter) {
		return getCurrentOrdersRestrictedByConceptSet(name,
				gp.getConcept(GlobalPropertiesManagement.DIABETES_TREATMENT_DRUG_SET), dateFormat, drugFilter);
	}

	public static CurrentOrdersRestrictedByConceptSet getCurrentAsthmaOrders(String name, String dateFormat,
			ResultFilter drugFilter) {
		return getCurrentOrdersRestrictedByConceptSet(name,
				gp.getConcept(GlobalPropertiesManagement.CHRONIC_RESPIRATORY_DISEASE_TREATMENT_DRUGS), dateFormat,
				drugFilter);
	}

	public static CurrentOrdersRestrictedByConceptSet getCurrentHypertensionOrders(String name, String dateFormat,
			ResultFilter drugFilter) {
		return getCurrentOrdersRestrictedByConceptSet(name,
				gp.getConcept(GlobalPropertiesManagement.HYPERTENSION_TREATMENT_DRUGS), dateFormat, drugFilter);
	}

	public static CurrentOrdersRestrictedByConceptSet getCurrentEpilepsyOrders(String name, String dateFormat,
			ResultFilter drugFilter) {
		return getCurrentOrdersRestrictedByConceptSet(name,
				gp.getConcept(GlobalPropertiesManagement.EPILEPSY_TREATMENT_DRUGS), dateFormat, drugFilter);
	}

	public static MostRecentObsgroup getMostRecentObsgroup(String name, Concept concept, Concept member,
			String dateFormat) {
		MostRecentObsgroup mostRecent = new MostRecentObsgroup();
		mostRecent.setConcept(concept);
		mostRecent.setName(name);
		mostRecent.setMemberToDisplay(member);
		if (dateFormat != null) {
			mostRecent.setDateFormat(dateFormat);
		}
		return mostRecent;
	}

	public static MostRecentObservation getMostRecent(String name, Concept concept, String dateFormat) {
		MostRecentObservation mostRecent = new MostRecentObservation();
		mostRecent.setConcept(concept);
		mostRecent.setName(name);
		if (dateFormat != null) {
			mostRecent.setDateFormat(dateFormat);
		}
		return mostRecent;
	}

	public static LastWeekMostRecentObservation getLastWeekMostRecent(String name, Concept concept, String dateFormat) {
		LastWeekMostRecentObservation mostRecent = new LastWeekMostRecentObservation();
		mostRecent.addParameter(new Parameter("endDate", "endDate", Date.class));
		mostRecent.setConcept(concept);
		mostRecent.setName(name);
		if (dateFormat != null) {
			mostRecent.setDateFormat(dateFormat);
		}
		return mostRecent;
	}

	public static MostRecentObservation getMostRecent(String name, Concept concept, String dateFormat,
			ResultFilter resultFilter) {
		MostRecentObservation mostRecent = getMostRecent(name, concept, dateFormat);
		if (resultFilter != null) {
			mostRecent.setFilter(resultFilter);
		}
		return mostRecent;
	}

	public static LastWeekMostRecentObservation getLastWeekMostRecent(String name, Concept concept, String dateFormat,
			ResultFilter resultFilter) {
		LastWeekMostRecentObservation mostRecent = getLastWeekMostRecent(name, concept, dateFormat);
		if (resultFilter != null) {
			mostRecent.setFilter(resultFilter);
		}
		return mostRecent;
	}

	public static AllObservationValues getAllObservationValues(String name, Concept concept, String dateFormat,
			ResultFilter resultFilter, ResultFilter outputFilter) {
		AllObservationValues allObs = new AllObservationValues();
		allObs.setConcept(concept);
		allObs.setName(name);
		if (resultFilter != null) {
			allObs.setFilter(resultFilter);
		}
		if (dateFormat != null) {
			allObs.setDateFormat(dateFormat);
		}
		if (outputFilter != null) {
			allObs.setOutputFilter(outputFilter);
		}
		return allObs;
	}

	public static AllObservationValues getAllObservationValuesBeforeEndDate(String name, Concept concept,
			int minResults, String dateFormat, ResultFilter resultFilter, ResultFilter outputFilter) {
		AllObservationValues allObs = getAllObservationValues(name, concept, dateFormat, resultFilter, outputFilter);
		allObs.addParameter(new Parameter("endDate", "endDate", Date.class));
		allObs.setMinResultsOutput(minResults);
		return allObs;
	}

	public static ObservationInMostRecentEncounterOfType getObservationInMostRecentEncounterOfType(String name,
			Concept concept, EncounterType encounterType, ResultFilter resultFilter) {

		ObservationInMostRecentEncounterOfType oe = new ObservationInMostRecentEncounterOfType();
		oe.setName(name);
		oe.setObservationConcept(concept);
		List<EncounterType> encounterTypes = new ArrayList<EncounterType>();
		encounterTypes.add(encounterType);
		oe.setEncounterTypes(encounterTypes);

		if (resultFilter != null) {

			oe.setFilter(resultFilter);
		}
		return oe;
	}

	public static ObservationInMostRecentEncounter getObservationInMostRecentEncounter(String name, Concept concept,
			List<Form> forms, List<EncounterType> encounterTypes, ResultFilter resultFilter) {

		ObservationInMostRecentEncounter oe = new ObservationInMostRecentEncounter();
		oe.setName(name);
		oe.setObservationConcept(concept);
		if (encounterTypes != null) {
			oe.setEncounterTypes(encounterTypes);
		}
		if (forms != null) {
			oe.setForms(forms);
		}
		if (resultFilter != null) {

			oe.setFilter(resultFilter);
		}
		return oe;
	}

	public static ObservationInMostRecentEncounterOfType getObservationInMostRecentEncounterOfType(String name,
			Concept concept, EncounterType encounterType) {

		ObservationInMostRecentEncounterOfType oe = new ObservationInMostRecentEncounterOfType();
		oe.setName(name);
		oe.setObservationConcept(concept);
		// oe.setFilter(dateFilter);
		List<EncounterType> encounterTypes = new ArrayList<EncounterType>();
		encounterTypes.add(encounterType);
		oe.setEncounterTypes(encounterTypes);

		return oe;
	}

	public static PatientRelationship getPatientRelationship(String name, int relationshipTypeId, String side,
			ResultFilter accompagnateurFilter) {
		PatientRelationship rel = new PatientRelationship();
		if (accompagnateurFilter != null) {
			rel.setResultFilter(accompagnateurFilter);
		}
		rel.setName(name);
		rel.setRelationshipTypeId(relationshipTypeId);
		rel.setRetrievePersonAorB(side);
		return rel;
	}

	public static CurrentOrdersRestrictedByConceptSet getCurrentOrdersRestrictedByConceptSet(String name,
			Concept drugConcept, String dateFormat, ResultFilter drugFilter) {
		CurrentOrdersRestrictedByConceptSet co = new CurrentOrdersRestrictedByConceptSet();
		co.setDrugConceptSetConcept(drugConcept);
		if (dateFormat != null) {
			co.setDateFormat(dateFormat);
		}
		if (drugFilter != null) {
			co.setDrugFilter(drugFilter);
		}
		co.setName(name);
		return co;
	}

	public static PatientAddress getPatientAddress(String name, boolean district, boolean sector, boolean cell,
			boolean umudugudu) {
		PatientAddress address = new PatientAddress();
		address.setName(name);
		address.setDescription("Address");
		address.setIncludeCountry(false);
		address.setIncludeProvince(false);
		address.setIncludeDistrict(district);
		address.setIncludeSector(sector);
		address.setIncludeCell(cell);
		address.setIncludeUmudugudu(umudugudu);
		return address;
	}

	public static PatientAddress getPatientAddress(String name, boolean country, boolean province, boolean district,
			boolean sector, boolean cell, boolean umudugudu) {
		PatientAddress address = new PatientAddress();
		address.setName(name);
		address.setDescription("Address");
		address.setIncludeCountry(country);
		address.setIncludeProvince(province);
		address.setIncludeDistrict(district);
		address.setIncludeSector(sector);
		address.setIncludeCell(cell);
		address.setIncludeUmudugudu(umudugudu);
		return address;
	}

	public static EvaluateDefinitionForOtherPersonData getDefinitionForOtherPerson(String name, PersonData person,
			RowPerPatientData definition) {
		EvaluateDefinitionForOtherPersonData otherDef = new EvaluateDefinitionForOtherPersonData();
		otherDef.setPersonData(person, new HashMap<String, Object>());
		otherDef.setDefinition(definition, new HashMap<String, Object>());
		otherDef.setName(name);
		return otherDef;
	}

	public static ObsValueAfterDateOfOtherDefinition getObsValueAfterDateOfOtherDefinition(String name, Concept concept,
			DateOfPatientData patientData, String dateFormat) {
		ObsValueAfterDateOfOtherDefinition ovadood = new ObsValueAfterDateOfOtherDefinition();
		ovadood.setConcept(concept);
		ovadood.setName(name);
		ovadood.setDateOfPatientData(patientData, new HashMap<String, Object>());

		if (dateFormat != null) {
			ovadood.setDateFormat(dateFormat);
		}

		return ovadood;
	}

	public static ObsValueAfterDateOfOtherDefinition getObsValueAfterDateOfOtherDefinition(String name, Concept concept,
			Concept groupConcept, DateOfPatientData patientData, String dateFormat) {
		ObsValueAfterDateOfOtherDefinition ovadood = new ObsValueAfterDateOfOtherDefinition();
		ovadood.setConcept(concept);
		ovadood.setName(name);
		ovadood.setDateOfPatientData(patientData, new HashMap<String, Object>());
		ovadood.setGroupConcept(groupConcept);
		if (dateFormat != null) {
			ovadood.setDateFormat(dateFormat);
		}

		return ovadood;
	}

	public static BaselineObservation getBaselineObservation(String name, Concept concept, int before, int after,
			DateOfPatientData patientData, String dateFormat) {
		BaselineObservation baseline = new BaselineObservation();
		baseline.setConcept(concept);
		baseline.setName(name);
		baseline.setDateOfPatientData(patientData, new HashMap<String, Object>());
		baseline.setAfter(after);
		baseline.setBefore(before);

		if (dateFormat != null) {
			baseline.setDateFormat(dateFormat);
		}

		return baseline;
	}

	public static BaselineObservationAnswer getBaselineObservationAnswer(String name, List<Concept> questions,
			Concept answer, int before, int after, DateOfPatientData patientData, String dateFormat) {
		BaselineObservationAnswer baseline = new BaselineObservationAnswer();
		baseline.setAnswer(answer);
		baseline.setQuestions(questions);
		baseline.setName(name);
		baseline.setDateOfPatientData(patientData, new HashMap<String, Object>());
		baseline.setAfter(after);
		baseline.setBefore(before);

		if (dateFormat != null) {
			baseline.setDateFormat(dateFormat);
		}

		return baseline;
	}

	public static BaselineObservationAnswer getBaselineObservationAnswerBeforeEndDate(String name,
			List<Concept> questions, Concept answer, int before, int after, DateOfPatientData patientData,
			String dateFormat) {
		BaselineObservationAnswer baseline = getBaselineObservationAnswer(name, questions, answer, before, after,
				patientData, dateFormat);
		baseline.addParameter(new Parameter("endDate", "endDate", Date.class));

		return baseline;
	}

	public static BaselineObservation getBaselineObservationAtMonth(String name, Concept concept, int before, int after,
			int offset, DateOfPatientData patientData, String dateFormat) {
		BaselineObservation baseline = getBaselineObservation(name, concept, before, after, patientData, dateFormat);
		baseline.setOffset(offset, Calendar.MONTH);

		return baseline;
	}

	public static BaselineObservation getBaselineObservationAtMonthBeforeEndDate(String name, Concept concept,
			int before, int after, int offset, DateOfPatientData patientData, Map<String, Object> parameters,
			String dateFormat) {
		BaselineObservation baseline = getBaselineObservationAtMonth(name, concept, before, after, offset, patientData,
				dateFormat);
		baseline.addParameter(new Parameter("endDate", "endDate", Date.class));
		baseline.setDateOfPatientData(patientData, parameters);

		return baseline;
	}

	public static BaselineObservationAnswer getBaselineObservationAnswerAtMonth(String name, List<Concept> questions,
			Concept answer, int before, int after, int offset, DateOfPatientData patientData, String dateFormat) {
		BaselineObservationAnswer baseline = getBaselineObservationAnswer(name, questions, answer, before, after,
				patientData, dateFormat);
		baseline.setOffset(offset, Calendar.MONTH);

		return baseline;
	}

	public static BaselineObservationAnswer getBaselineObservationAnswerAtMonthBeforeEndDate(String name,
			List<Concept> questions, Concept answer, int before, int after, int offset, DateOfPatientData patientData,
			String dateFormat) {
		BaselineObservationAnswer baseline = getBaselineObservationAnswerAtMonth(name, questions, answer, before, after,
				offset, patientData, dateFormat);
		baseline.addParameter(new Parameter("endDate", "endDate", Date.class));

		return baseline;
	}

	public static BaselineEncounter getBaselineEncounterAtMonth(String name, List<EncounterType> types, int before,
			int after, int offset, DateOfPatientData patientData, String dateFormat) {
		BaselineEncounter baseline = new BaselineEncounter();
		baseline.setEncounterTypes(types);
		baseline.setName(name);
		baseline.setDateOfPatientData(patientData, new HashMap<String, Object>());
		baseline.setAfter(after);
		baseline.setBefore(before);
		baseline.setOffset(offset, Calendar.MONTH);

		if (dateFormat != null) {
			baseline.setDateFormat(dateFormat);
		}

		return baseline;
	}

	public static BaselineEncounter getBaselineEncounterAtMonthBeforeEndDate(String name, List<EncounterType> types,
			int before, int after, int offset, DateOfPatientData patientData, Map<String, Object> parameters,
			String dateFormat) {
		BaselineEncounter baseline = getBaselineEncounterAtMonth(name, types, before, after, offset, patientData,
				dateFormat);
		baseline.addParameter(new Parameter("endDate", "endDate", Date.class));
		baseline.setDateOfPatientData(patientData, parameters);

		return baseline;
	}

	public static BaselineProgramEnrollment getBaselineProgramEnrollment(String name, Program program, int before,
			int after, DateOfPatientData patientData, String dateFormat) {
		BaselineProgramEnrollment baseline = new BaselineProgramEnrollment();
		baseline.setProgram(program);
		baseline.setName(name);
		baseline.setDateOfPatientData(patientData, new HashMap<String, Object>());
		baseline.setAfter(after);
		baseline.setBefore(before);

		if (dateFormat != null) {
			baseline.setDateFormat(dateFormat);
		}

		return baseline;
	}

	public static BaselineProgramEnrollment getBaselineProgramEnrollmentAtMonth(String name, Program program,
			int before, int after, int offset, DateOfPatientData patientData, String dateFormat) {
		BaselineProgramEnrollment baseline = getBaselineProgramEnrollment(name, program, before, after, patientData,
				dateFormat);
		baseline.setOffset(offset, Calendar.MONTH);

		return baseline;
	}

	public static BaselineProgramEnrollment getBaselineProgramEnrollmentAtMonthBeforeEndDate(String name,
			Program program, int before, int after, int offset, DateOfPatientData patientData,
			Map<String, Object> parameters, String dateFormat) {
		BaselineProgramEnrollment baseline = getBaselineProgramEnrollmentAtMonth(name, program, before, after, offset,
				patientData, dateFormat);
		baseline.addParameter(new Parameter("endDate", "endDate", Date.class));
		baseline.setDateOfPatientData(patientData, parameters);

		return baseline;
	}

	public static BaselineProgramEnrollment getBaselineProgramEnrollmentBeforeEndDate(String name, Program program,
			int before, int after, DateOfPatientData patientData, Map<String, Object> parameters, String dateFormat) {
		BaselineProgramEnrollment baseline = getBaselineProgramEnrollment(name, program, before, after, patientData,
				dateFormat);
		baseline.addParameter(new Parameter("endDate", "endDate", Date.class));
		baseline.setDateOfPatientData(patientData, parameters);

		return baseline;
	}

	public static BaselineObservation getBaselineCD4(String name, String dateFormat) {
		BaselineObservation baseline = new BaselineObservation();
		baseline.setConcept(gp.getConcept(GlobalPropertiesManagement.CD4_TEST));
		baseline.setName(name);
		baseline.setDateOfPatientData(getDateOfHIVEnrolment("hivEnrollment", dateFormat),
				new HashMap<String, Object>());
		baseline.setAfter(30);
		baseline.setBefore(90);

		if (dateFormat != null) {
			baseline.setDateFormat(dateFormat);
		}

		return baseline;
	}

	public static BaselineDrugOrder getBaselineDrugOrder(String name, Concept concept, int before, int after,
			DateOfPatientData patientData, Map<String, Object> parameters, String dateFormat) {
		BaselineDrugOrder baseline = new BaselineDrugOrder();
		baseline.setDrugConcept(concept);
		baseline.setName(name);
		baseline.setDateOfPatientData(patientData, parameters);
		baseline.setAfter(after);
		baseline.setBefore(before);

		if (dateFormat != null) {
			baseline.setDateFormat(dateFormat);
		}

		return baseline;
	}

	public static BaselineDrugOrder getBaselineDrugOrderBeforeEndDate(String name, Concept concept, int before,
			int after, DateOfPatientData patientData, Map<String, Object> parameters, String dateFormat) {
		BaselineDrugOrder baseline = getBaselineDrugOrder(name, concept, before, after, patientData, parameters,
				dateFormat);
		baseline.addParameter(new Parameter("endDate", "endDate", Date.class));

		return baseline;
	}

	public static BaselineDrugOrder getBaselineDrugOrderAtMonthBeforeEndDate(String name, Concept concept, int before,
			int after, int offset, DateOfPatientData patientData, Map<String, Object> parameters, String dateFormat) {
		BaselineDrugOrder baseline = getBaselineDrugOrderBeforeEndDate(name, concept, before, after, patientData,
				parameters, dateFormat);
		baseline.setOffset(offset, Calendar.MONTH);

		return baseline;
	}

	public static ObsValueBeforeDateOfOtherDefinition getObsValueBeforeDateOfOtherDefinition(String name,
			Concept concept, DateOfPatientData patientData, String dateFormat) {
		ObsValueBeforeDateOfOtherDefinition ovbdood = new ObsValueBeforeDateOfOtherDefinition();
		ovbdood.setConcept(concept);
		ovbdood.setName(name);
		ovbdood.setDateOfPatientData(patientData, new HashMap<String, Object>());

		if (dateFormat != null) {
			ovbdood.setDateFormat(dateFormat);
		}

		return ovbdood;
	}

	public static DateOfObsAfterDateOfOtherDefinition getDateOfObsAfterDateOfOtherDefinition(String name,
			Concept concept, DateOfPatientData patientData) {
		DateOfObsAfterDateOfOtherDefinition dooadood = new DateOfObsAfterDateOfOtherDefinition();
		dooadood.setConcept(concept);
		dooadood.setName(name);
		dooadood.setDateOfPatientData(patientData, new HashMap<String, Object>());
		return dooadood;
	}

	public static DateOfObsAfterDateOfOtherDefinition getDateOfObsAfterDateOfOtherDefinition(String name,
			Concept concept, Concept group, DateOfPatientData patientData) {
		DateOfObsAfterDateOfOtherDefinition dooadood = new DateOfObsAfterDateOfOtherDefinition();
		dooadood.setConcept(concept);
		dooadood.setName(name);
		dooadood.setGroupConcept(group);
		dooadood.setDateOfPatientData(patientData, new HashMap<String, Object>());
		return dooadood;
	}

	public static DateOfWorkflowStateChange getDateOfWorkflowStateChange(String name, Concept workflowConcept,
			String dateFormat) {
		DateOfWorkflowStateChange startDate = new DateOfWorkflowStateChange();
		startDate.setConcept(workflowConcept);
		startDate.setName(name);
		if (dateFormat != null) {
			startDate.setDateFormat(dateFormat);
		}
		return startDate;
	}

	public static DateOfWorkflowStateChange getDateOfWorkflowStateChange(String name, Concept workflowConcept,
			String parameterName, String dateFormat) {
		DateOfWorkflowStateChange startDate = new DateOfWorkflowStateChange();
		startDate.addParameter(new Parameter(parameterName, parameterName, Date.class));
		startDate.setConcept(workflowConcept);
		startDate.setName(name);
		if (dateFormat != null) {
			startDate.setDateFormat(dateFormat);
		}
		return startDate;
	}

	public static DateOfProgramEnrolment getDateOfProgramEnrolment(String name, Program program, String dateFormat) {
		DateOfProgramEnrolment progEnrol = new DateOfProgramEnrolment();
		progEnrol.setName(name);
		if (program != null) {
			progEnrol.setProgramId(program.getProgramId());
		} else { // this is only for the crown report
			progEnrol.addParameter(new Parameter("programs", "Program", Program.class));
		}
		if (dateFormat != null) {
			progEnrol.setDateFormat(dateFormat);
		}
		return progEnrol;
	}

	public static DateOfProgramEnrolment getDateOfProgramEnrolment(String name, Program program, Boolean returnEarliest,
			String dateFormat) {
		DateOfProgramEnrolment progEnrol = new DateOfProgramEnrolment();
		progEnrol.setName(name);
		progEnrol.setReturnEarliest(returnEarliest);
		progEnrol.setProgramId(program.getProgramId());
		if (dateFormat != null) {
			progEnrol.setDateFormat(dateFormat);
		}
		return progEnrol;
	}

	public static DateOfProgramEnrolment getDateOfProgramEnrolment(String name, Program program, String parameter,
			String dateFormat) {
		DateOfProgramEnrolment progEnrol = new DateOfProgramEnrolment();
		progEnrol.addParameter(new Parameter(parameter, parameter, Date.class));
		progEnrol.setName(name);
		progEnrol.setProgramId(program.getProgramId());
		if (dateFormat != null) {
			progEnrol.setDateFormat(dateFormat);
		}
		return progEnrol;
	}

	public static DateOfProgramCompletion getDateOfProgramCompletion(String name, Program program, String dateFormat) {
		DateOfProgramCompletion progEnrol = new DateOfProgramCompletion();
		progEnrol.setName(name);
		if (program != null) {
			progEnrol.setProgramId(program.getProgramId());
		} else { // this is only for the crown report
			progEnrol.addParameter(new Parameter("programs", "Program", Program.class));
		}
		if (dateFormat != null) {
			progEnrol.setDateFormat(dateFormat);
		}
		return progEnrol;
	}

	public static DateOfAllProgramEnrolment getDateOfAllProgramEnrolment(String name, Program program,
			String dateFormat) {
		DateOfAllProgramEnrolment progEnrol = new DateOfAllProgramEnrolment();
		progEnrol.setName(name);
		progEnrol.setPatientProgram(program);
		if (dateFormat != null) {
			progEnrol.setDateFormat(dateFormat);
		}
		return progEnrol;
	}

	public static DateOfProgramEnrolment getDateOfEarliestProgramEnrolment(String name, Program program,
			String dateFormat) {
		DateOfProgramEnrolment progEnrol = new DateOfProgramEnrolment();
		progEnrol.setName(name);
		progEnrol.setProgramId(program.getProgramId());
		progEnrol.setReturnEarliest(true);
		if (dateFormat != null) {
			progEnrol.setDateFormat(dateFormat);
		}
		return progEnrol;
	}

	public static FirstDrugOrderStartedRestrictedByConceptSet getFirstDrugOrderStartedRestrictedByConceptSet(
			String name, Concept conceptSet) {
		FirstDrugOrderStartedRestrictedByConceptSet startDateDrugs = new FirstDrugOrderStartedRestrictedByConceptSet();
		startDateDrugs.setName(name);
		startDateDrugs.setDrugConceptSetConcept(conceptSet);
		return startDateDrugs;
	}

	public static FirstDrugOrderStartedRestrictedByConceptSet getFirstDrugOrderStartedRestrictedByConceptSet(
			String name, Concept conceptSet, String dateFormat) {
		FirstDrugOrderStartedRestrictedByConceptSet startDateDrugs = getFirstDrugOrderStartedRestrictedByConceptSet(
				name, conceptSet);
		if (dateFormat != null) {
			startDateDrugs.setDateFormat(dateFormat);
		}
		return startDateDrugs;
	}

	public static FirstDrugOrderStartedRestrictedByConceptSet getFirstDrugOrderStartedRestrictedByConceptSet(
			String name, Concept conceptSet, String parameterName, String dateFormat) {
		FirstDrugOrderStartedRestrictedByConceptSet startDateDrugs = getFirstDrugOrderStartedRestrictedByConceptSet(
				name, conceptSet);
		startDateDrugs.addParameter(new Parameter(parameterName, parameterName, Date.class));
		if (dateFormat != null) {
			startDateDrugs.setDateFormat(dateFormat);
		}
		return startDateDrugs;
	}

	public static FirstDrugOrderStartedAfterDateRestrictedByConceptSet getFirstDrugOrderStartedAfterDateRestrictedByConceptSet(
			String name, Concept conceptSet, DateOfPatientData patientData) {
		FirstDrugOrderStartedAfterDateRestrictedByConceptSet initial = new FirstDrugOrderStartedAfterDateRestrictedByConceptSet();
		initial.setName(name);
		initial.setDrugConceptSetConcept(conceptSet);
		initial.setDateOfPatientData(patientData, new HashMap<String, Object>());
		return initial;
	}

	public static DateOfFirstDrugOrderStartedRestrictedByConceptSet getDateOfFirstDrugOrderStartedRestrictedByConceptSet(
			String name, Concept conceptSet, String parameterName, String dateFormat) {
		DateOfFirstDrugOrderStartedRestrictedByConceptSet startDateDrugs = getDateOfFirstDrugOrderStartedRestrictedByConceptSet(
				name, conceptSet);
		startDateDrugs.addParameter(new Parameter(parameterName, parameterName, Date.class));
		if (dateFormat != null) {
			startDateDrugs.setDateFormat(dateFormat);
		}
		return startDateDrugs;
	}

	public static DateOfFirstDrugOrderStartedRestrictedByConceptSet getDateOfFirstDrugOrderStartedRestrictedByConceptSet(
			String name, Concept conceptSet) {
		DateOfFirstDrugOrderStartedRestrictedByConceptSet startDateDrugs = new DateOfFirstDrugOrderStartedRestrictedByConceptSet();
		startDateDrugs.setName(name);
		startDateDrugs.setDrugConceptSetConcept(conceptSet);
		return startDateDrugs;
	}

	public static FirstDrugOrderStartedRestrictedByConceptSet getDrugOrderForStartOfART(String name) {
		return getFirstDrugOrderStartedRestrictedByConceptSet(name,
				gp.getConcept(GlobalPropertiesManagement.ART_DRUGS_SET));
	}

	public static FirstDrugOrderStartedRestrictedByConceptSet getDrugOrderForStartOfART(String name,
			String dateFormat) {
		return getFirstDrugOrderStartedRestrictedByConceptSet(name,
				gp.getConcept(GlobalPropertiesManagement.ART_DRUGS_SET), dateFormat);
	}

	public static FirstDrugOrderStartedRestrictedByConceptSet getDrugOrderForStartOfARTBeforeDate(String name,
			String dateFormat) {
		return getFirstDrugOrderStartedRestrictedByConceptSet(name,
				gp.getConcept(GlobalPropertiesManagement.ART_DRUGS_SET), "endDate", dateFormat);
	}

	public static DateOfFirstDrugOrderStartedRestrictedByConceptSet getDateOfDrugOrderForStartOfARTBeforeDate(
			String name, String dateFormat) {
		return getDateOfFirstDrugOrderStartedRestrictedByConceptSet(name,
				gp.getConcept(GlobalPropertiesManagement.ART_DRUGS_SET), "endDate", dateFormat);
	}

	public static DateOfProgramEnrolment getDateOfHIVEnrolment(String name, String dateFormat) {
		return getDateOfProgramEnrolment(name, gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM), true,
				dateFormat);
	}

	public static DateOfProgramEnrolment getDateOfPediHIVEnrolment(String name, String dateFormat) {
		return getDateOfProgramEnrolment(name, gp.getProgram(GlobalPropertiesManagement.PEDI_HIV_PROGRAM), true,
				dateFormat);
	}

	public static MultiplePatientDataDefinitions getDateOfAllHIVEnrolment(String name, String dateFormat) {
		MultiplePatientDataDefinitions mdd = new MultiplePatientDataDefinitions();
		mdd.setName(name);
		mdd.addPatientDataDefinition(getDateOfPediHIVEnrolment(name, dateFormat), new HashMap<String, Object>());
		mdd.addPatientDataDefinition(getDateOfHIVEnrolment(name, dateFormat), new HashMap<String, Object>());
		return mdd;
	}

	public static MultiplePatientDataDefinitions getDateOfAllPMTCTEnrolment(String name, String dateFormat) {

		Map<String, Object> mappings = new HashMap<String, Object>();
		mappings.put("endDate", "${endDate}");

		MultiplePatientDataDefinitions mdd = new MultiplePatientDataDefinitions();
		mdd.addParameter(new Parameter("endDate", "endDate", Date.class));
		mdd.setName(name);
		mdd.addPatientDataDefinition(getDateOfProgramEnrolment(name,
				gp.getProgram(GlobalPropertiesManagement.PMTCT_PREGNANCY_PROGRAM), "endDate", dateFormat), mappings);
		mdd.addPatientDataDefinition(getDateOfProgramEnrolment(name,
				gp.getProgram(GlobalPropertiesManagement.PMTCT_COMBINED_MOTHER_PROGRAM), "endDate", dateFormat),
				mappings);
		return mdd;
	}

	public static DateOfProgramCompletion getDateOfHIVCompletion(String name, String dateFormat) {
		return getDateOfProgramCompletion(name, gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM),
				dateFormat);
	}

	public static DateOfProgramCompletion getDateOfPediHIVCompletion(String name, String dateFormat) {
		return getDateOfProgramCompletion(name, gp.getProgram(GlobalPropertiesManagement.PEDI_HIV_PROGRAM), dateFormat);
	}

	public static MultiplePatientDataDefinitions getDateOfAllHIVCompletion(String name, String dateFormat) {
		MultiplePatientDataDefinitions mdd = new MultiplePatientDataDefinitions();
		mdd.setName(name);
		mdd.addPatientDataDefinition(getDateOfHIVCompletion(name, dateFormat), new HashMap<String, Object>());
		mdd.addPatientDataDefinition(getDateOfPediHIVCompletion(name, dateFormat), new HashMap<String, Object>());
		return mdd;
	}

	public static FirstDrugOrderStartedAfterDateRestrictedByConceptSet getDrugOrderForStartOfARTAfterDate(String name,
			DateOfPatientData patientData) {
		return getFirstDrugOrderStartedAfterDateRestrictedByConceptSet(name,
				gp.getConcept(GlobalPropertiesManagement.ART_DRUGS_SET), patientData);
	}

	public static StartOfArt getDrugOrdersForStartOfARTBeforeDate(String name) {
		StartOfArt art = new StartOfArt();
		art.addParameter(new Parameter("endDate", "endDate", Date.class));
		art.setName(name);
		return art;
	}

	public static ArtSwitch getDrugOrdersForARTSwitchBeforeDate(String name, RowPerPatientData artData,
			Map<String, Object> parameters) {
		ArtSwitch art = new ArtSwitch();
		art.addParameter(new Parameter("endDate", "endDate", Date.class));
		art.setArtData(artData, parameters);
		art.setName(name);
		return art;
	}

	public static ArtSwitchDate getDateForARTSwitchBeforeDate(String name, RowPerPatientData artData,
			Map<String, Object> parameters) {
		ArtSwitchDate art = new ArtSwitchDate();
		art.addParameter(new Parameter("endDate", "endDate", Date.class));
		art.setArtData(artData, parameters);
		art.setName(name);
		return art;
	}

	public static AllDrugOrdersRestrictedByConcept getAllDrugOrdersRestrictedByConcept(String name, Concept concept) {
		AllDrugOrdersRestrictedByConcept all = new AllDrugOrdersRestrictedByConcept();
		all.setName(name);
		all.setConcept(concept);
		return all;
	}

	public static AllDrugOrdersRestrictedByConceptSet getAllDrugOrdersRestrictedByConceptSet(String name,
			Concept concept) {
		AllDrugOrdersRestrictedByConceptSet all = new AllDrugOrdersRestrictedByConceptSet();
		all.setName(name);
		all.setDrugConceptSetConcept(concept);
		return all;
	}

	public static FirstRecordedObservationWithCodedConceptAnswer getFirstRecordedObservationWithCodedConceptAnswer(
			String name, Concept question, Concept answer, String dateFormat) {
		FirstRecordedObservationWithCodedConceptAnswer firstRecorded = new FirstRecordedObservationWithCodedConceptAnswer();
		firstRecorded.setName(name);
		firstRecorded.setAnswerRequired(answer);
		firstRecorded.setQuestion(question);

		if (dateFormat != null) {
			firstRecorded.setDateFormat("dd-MMM-yyyy");
		}
		return firstRecorded;
	}

	public static FullHistoryOfProgramWorkflowStates getFullHistoryOfProgramWorkflowStates(String name,
			List<ProgramWorkflow> workflows, String dateFormat) {
		FullHistoryOfProgramWorkflowStates history = new FullHistoryOfProgramWorkflowStates();
		history.setName(name);
		history.setWorkflows(workflows);
		history.setDateFormat(dateFormat);
		return history;
	}

	public static DrugRegimenInformation getDrugRegimenInformation(String name) {
		DrugRegimenInformation info = new DrugRegimenInformation();
		info.setName(name);

		return info;
	}

	public static DrugRegimenInformation getDrugRegimenInformationParameterized(String name, boolean showStartDate,
			boolean showDrugDetails) {
		DrugRegimenInformation info = new DrugRegimenInformation();
		info.setName(name);
		info.setShowStartDate(showStartDate);
		info.setShowDrugDetails(showDrugDetails);
		info.addParameter(new Parameter("asOfDate", "asOfDate", Date.class));
		info.addParameter(new Parameter("untilDate", "untilDate", Date.class));
		return info;
	}

	public static DrugRegimenInformation getDrugRegimenInformationParameterized(String name, Concept indication,
			boolean showStartDate, boolean showDrugDetails) {
		DrugRegimenInformation info = getDrugRegimenInformationParameterized(name, showStartDate, showDrugDetails);
		info.setIndication(indication);

		return info;
	}

	public static RegimenDateInformation getRegimenDateInformationParameterized(String name, String format) {
		RegimenDateInformation info = new RegimenDateInformation();
		info.setName(name);
		info.addParameter(new Parameter("asOfDate", "asOfDate", Date.class));
		info.addParameter(new Parameter("untilDate", "untilDate", Date.class));

		if (format != null) {
			info.setDateFormat(format);
		}
		return info;
	}

	public static CustomCalculationBasedOnMultiplePatientDataDefinitions getBooleanRepresentation(String name,
			RowPerPatientData patientData) {
		CustomCalculationBasedOnMultiplePatientDataDefinitions definition = new CustomCalculationBasedOnMultiplePatientDataDefinitions();
		definition.addPatientDataToBeEvaluated(patientData, new HashMap<String, Object>());
		definition.setName(name);
		definition.setCalculator(new BooleanCalculation());
		return definition;
	}

	public static CustomCalculationBasedOnMultiplePatientDataDefinitions getBooleanRepresentation(String name,
			String parameter, Map<String, Object> parameters, RowPerPatientData patientData) {
		CustomCalculationBasedOnMultiplePatientDataDefinitions definition = getBooleanRepresentation(name, patientData);
		definition.addParameter(new Parameter(parameter, parameter, Date.class));
		definition.addPatientDataToBeEvaluated(patientData, parameters);
		definition.setCalculator(new BooleanCalculation());
		return definition;
	}

	public static MostRecentProgramWorkflowState getMostRecentProgramWorkflowState(String name,
			List<ProgramWorkflow> workflows, ResultFilter filter) {
		MostRecentProgramWorkflowState recent = new MostRecentProgramWorkflowState();
		recent.setName(name);
		recent.setWorkflows(workflows);
		if (filter != null) {
			recent.setFilter(filter);
		}
		return recent;
	}

	public static CurrentPatientDrugOrder getCurrentDrugOrders(String name, ResultFilter drugFilter) {
		CurrentPatientDrugOrder co = new CurrentPatientDrugOrder();
		if (drugFilter != null) {
			co.setResultFilter(drugFilter);
		}
		co.setName(name);
		return co;
	}

	public static CurrentPatientDrugOrder getPatientCurrentlyActiveOnDrugOrder(String name, ResultFilter drugFilter) {
		return getCurrentDrugOrders(name, drugFilter);
	}

	public static MostRecentObservation getHIVDiagnosisDate(String name, String dateFormat) {

		MostRecentObservation diagnosis = getMostRecent(name,
				gp.getConcept(GlobalPropertiesManagement.HIV_DIAGNOSIS_DATE), dateFormat);

		return diagnosis;
	}

	public static HIVOutcome getHIVOutcomeOnEndDate(String name, String dateFormat) {
		HIVOutcome outcome = new HIVOutcome();
		outcome.setName(name);
		outcome.addParameter(new Parameter("endDate", "endDate", Date.class));

		if (dateFormat != null) {
			outcome.setDateFormat(dateFormat);
		}

		return outcome;
	}

	public static FirstRecordedObservation getFirstRecordedObservation(String name, Concept question,
			ResultFilter filter) {
		FirstRecordedObservation firstRecordedObservation = new FirstRecordedObservation();
		firstRecordedObservation.setName(name);
		firstRecordedObservation.setQuestion(question);
		if (filter != null) {
			firstRecordedObservation.setFilter(filter);
		}
		return firstRecordedObservation;
	}

	public static ObsAfterPeriodOfTimeFromEncounterDate getObsAfterPeriodOfTimeFromEncounterDate(String name,
			Concept question, List<Form> forms, List<EncounterType> encounterTypes, long timeDiff,
			ResultFilter filter) {
		ObsAfterPeriodOfTimeFromEncounterDate obsAfterPeriodOfTimeFromEncounterDate = new ObsAfterPeriodOfTimeFromEncounterDate();
		obsAfterPeriodOfTimeFromEncounterDate.setName(name);
		obsAfterPeriodOfTimeFromEncounterDate.setQuestion(question);
		obsAfterPeriodOfTimeFromEncounterDate.setTimeDiff(timeDiff);
		if (filter != null) {
			obsAfterPeriodOfTimeFromEncounterDate.setFilter(filter);
		}

		if (forms != null) {
			obsAfterPeriodOfTimeFromEncounterDate.setForms(forms);
		}

		if (encounterTypes != null) {
			obsAfterPeriodOfTimeFromEncounterDate.setEncounterTypes(encounterTypes);
		}
		return obsAfterPeriodOfTimeFromEncounterDate;
	}

	public static DatesOfVisitsByStartDateAndEndDate getDatesOfVisitsByStartDateAndEndDate(String name,
			List<EncounterType> encounterTypes, ResultFilter filter) {
		DatesOfVisitsByStartDateAndEndDate dateOfVisits = new DatesOfVisitsByStartDateAndEndDate();
		dateOfVisits.addParameter(new Parameter("startDate", "StartDate", Date.class));
		dateOfVisits.addParameter(new Parameter("endDate", "EndDate", Date.class));
		dateOfVisits.setName(name);
		dateOfVisits.setEncounterTypes(encounterTypes);
		dateOfVisits.setFilter(filter);
		return dateOfVisits;
	}

	public static PatientAddress getPatientAddress(String name, boolean province, boolean district, boolean sector,
			boolean cell, boolean umudugudu) {
		PatientAddress address = new PatientAddress();
		address.setName(name);
		address.setDescription("Address");
		address.setIncludeCountry(false);
		address.setIncludeProvince(province);
		address.setIncludeDistrict(district);
		address.setIncludeSector(sector);
		address.setIncludeCell(cell);
		address.setIncludeUmudugudu(umudugudu);
		return address;
	}

	public static EnrolledInProgram getPatientProgramInfo(String name, Program program, String valueType,
			ResultFilter filter) {
		EnrolledInProgram patientEnrollementDate = new EnrolledInProgram();
		patientEnrollementDate.setName(name);
		patientEnrollementDate.setValueType(valueType);
		patientEnrollementDate.setProgram(program);
		if (filter != null) {
			patientEnrollementDate.setFilter(filter);
		}
		return patientEnrollementDate;
	}

}
