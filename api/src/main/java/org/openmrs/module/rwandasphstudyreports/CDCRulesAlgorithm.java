package org.openmrs.module.rwandasphstudyreports;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandasphstudyreports.api.CDCReportsService;

public class CDCRulesAlgorithm {
	public List<String> cdcDsRulesAlerts(Patient patient) {
		List<String> alerts = new ArrayList<String>();
		Concept vl = Context.getConceptService().getConcept(Integer.parseInt(
				Context.getAdministrationService().getGlobalProperty(GlobalPropertyConstants.VIRAL_LOAD_CONCEPTID)));
		Concept cd4 = Context.getConceptService().getConcept(Integer.parseInt(
				Context.getAdministrationService().getGlobalProperty(GlobalPropertyConstants.CD4_COUNT_CONCEPTID)));
		List<Obs> vLObs = Context.getObsService().getLastNObservations(1, patient, vl, false);
		List<Visit> visits = Context.getVisitService().getVisitsByPatient(patient);
		Date lastVisitDate = null;
		DrugOrder artInitDrug = Context.getService(CDCReportsService.class).getARTInitiationDrug(patient);
		List<Obs> cd4Obs = Context.getObsService().getObservationsByPersonAndConcept(patient, cd4);
		boolean patientsWithNoVLAfter8Months = false;
		Date enrollmentDate = Context.getService(CDCReportsService.class).getHIVEnrollmentDate(patient);
		boolean vlAfter6MonthsFromEnrollment = checkIfNoObsNMonthsFromDate(vLObs, 6, enrollmentDate);

		visits = Context.getService(CDCReportsService.class).sortVisitsListByCreationDate(visits);
		//Context.getService(CDCReportsService.class).sortObsListByObsDateTime(vLObs);

		if (visits.isEmpty()) {
			List<Encounter> encs = Context.getEncounterService().getEncountersByPatient(patient);
			encs = Context.getService(CDCReportsService.class).sortEncountersListByCreationDate(encs);
			if (!encs.isEmpty())
				lastVisitDate = (encs.get(0).getEncounterDatetime() != null) ? encs.get(0).getEncounterDatetime()
						: new Date();
		} else {
			lastVisitDate = visits.get(0).getDateCreated();
		}
		if (lastVisitDate != null) {
			if (vLObs.isEmpty()
					&& Context.getService(CDCReportsService.class).checkIfPatientIsOnARVMoreThanNMonths(patient, 8)
					&& Context.getService(CDCReportsService.class).checkIfPatientIsHIVPositiveOrMissingResult(patient)) {
				alerts.add(
						Context.getMessageSourceService().getMessage("rwandasphstudyreports.alerts.orderBaselineVL"));
			}
		}
		if (cd4Obs.isEmpty() && Context.getService(CDCReportsService.class).checkIfPatientIsHIVPositiveOrMissingResult(patient)) {
			alerts.add(Context.getMessageSourceService().getMessage("rwandasphstudyreports.alerts.orderBaselineCD4"));
		}

		if (Context.getService(CDCReportsService.class).cd4BasedTreatmentFailure(patient)) {
			alerts.add(Context.getMessageSourceService()
					.getMessage("rwandasphstudyreports.alerts.cd4BasedTreatmentFailure"));
		}

		if (artInitDrug != null
				&& Context.getService(CDCReportsService.class)
						.checkIfDateIsNMonthsFromNow(artInitDrug.getEffectiveStartDate(), 6)
				&& Context.getService(CDCReportsService.class).checkIfPatientIsHIVPositiveOrMissingResult(patient)
				&& vlAfter6MonthsFromEnrollment && Context.getService(CDCReportsService.class)
						.checkIfPatientHasNoObsInLastNMonthsAfterProgramInit(null, null, null, patient)) {
			alerts.add(Context.getMessageSourceService()
					.getMessage("rwandasphstudyreports.alerts.patientsWithNoVLAfter8Months"));
			patientsWithNoVLAfter8Months = true;
		}
		if (artInitDrug != null
				&& Context.getService(CDCReportsService.class)
						.checkIfDateIsNMonthsFromNow(artInitDrug.getEffectiveStartDate(), 6)
				&& !patientsWithNoVLAfter8Months
				&& Context.getService(CDCReportsService.class).checkIfPatientIsHIVPositiveOrMissingResult(patient)
				&& Context.getService(CDCReportsService.class).checkIfPatientHasNoObsInLastNMonthsAfterProgramInit(null,
						6, null, patient))
			alerts.add(Context.getMessageSourceService()
					.getMessage("rwandasphstudyreports.alerts.patientsWithNoVLAfter6Months"));
		/*if (artInitDrug != null
				&& Context.getService(CDCReportsService.class)
						.checkIfDateIsNMonthsFromNow(artInitDrug.getEffectiveStartDate(), 6)
				&& Context.getService(CDCReportsService.class)
						.checkIfPatientListedAsBeingAViralLoadTreatmentFailureCase(patient))
			alerts.add(Context.getMessageSourceService()
					.getMessage("rwandasphstudyreports.alerts.patientsPartOfVLTreatmentFailureList"));
		*/if (!vLObs.isEmpty()) {
			Date vLDate = vLObs.get(0).getObsDatetime();
			Calendar vLCalendar = Calendar.getInstance(Context.getLocale());
			Calendar currentVisitDate = Calendar.getInstance(Context.getLocale());

			if (vLDate != null) {
				vLCalendar.setTime(vLDate);
				currentVisitDate.setTime(getCurrentVisitEndDate(patient));

				currentVisitDate.add(Calendar.MONTH, -12);
				if (vLCalendar.before(currentVisitDate)) {
					alerts.add(
							Context.getMessageSourceService().getMessage("rwandasphstudyreports.alerts.orderRepeatVL"));
				}
			}
			if (Context.getService(CDCReportsService.class).vlBasedTreatmentFailure(patient)) {
				alerts.add(Context.getMessageSourceService()
						.getMessage("rwandasphstudyreports.alerts.vlBasedTreatmentFailure"));
			}
		}

		return alerts;
	}

	private boolean checkIfNoObsNMonthsFromDate(List<Obs> obs, Integer nMonths, Date date) {
		if (!obs.isEmpty()) {
			Calendar c = Calendar.getInstance();

			c.setTime(date);
			c.add(Calendar.MONTH, nMonths);
			c.set(Calendar.HOUR, 00);
			c.set(Calendar.MINUTE, 00);
			c.set(Calendar.SECOND, 00);
			c.set(Calendar.MILLISECOND, 00);

			for (Obs o : obs) {
				if (o.getObsDatetime() != null && o.getObsDatetime().before(c.getTime()))
					return false;
			}
		}
		return true;
	}

	public static Date getCurrentVisitEndDate(Patient patient) {
		Visit activeVisit = Context.getService(CDCReportsService.class).getActiveVisit(patient, null);
		List<Encounter> encs = Context.getEncounterService().getEncountersByPatient(patient);
		encs = Context.getService(CDCReportsService.class).sortEncountersListByCreationDate(encs);

		if (activeVisit != null && activeVisit.getStopDatetime() != null
				&& activeVisit.getStopDatetime().before(new Date())) {
			return activeVisit.getStopDatetime();
		} else {
			if (!encs.isEmpty())
				return encs.get(0).getEncounterDatetime() != null ? encs.get(0).getEncounterDatetime()
						: encs.get(0).getDateCreated();
			else
				return new Date();
		}
	}
}
