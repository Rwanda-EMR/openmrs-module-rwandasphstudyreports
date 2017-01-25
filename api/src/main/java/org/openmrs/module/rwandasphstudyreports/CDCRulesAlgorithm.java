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

		List<Obs> vLObs = Context.getObsService().getObservationsByPersonAndConcept(patient, vl);
		List<Visit> visits = Context.getVisitService().getVisitsByPatient(patient);
		Date lastVisitDate = null;
		DrugOrder artInitDrug = Context.getService(CDCReportsService.class).getARTInitiationDrug(patient);
		List<Obs> cd4Obs = getOnlyObsWithDatetimeMoreThanNMonthsAfterObsDate(
				Context.getObsService().getObservationsByPersonAndConcept(patient, cd4),
				artInitDrug != null ? artInitDrug.getEffectiveStartDate() : null, 2);

		visits = Context.getService(CDCReportsService.class).sortVisitsListByCreationDate(visits);
		Context.getService(CDCReportsService.class).sortObsListByObsDateTime(vLObs);

		if (!vLObs.isEmpty()) {
			Date vLDate = vLObs.get(vLObs.size() - 1).getObsDatetime();
			Calendar vLCalendar = Calendar.getInstance(Context.getLocale());
			Calendar activeVisitLastYearEndsAt = Calendar.getInstance(Context.getLocale());
			Calendar activeVisitLast6MonthsEndsAt = Calendar.getInstance(Context.getLocale());

			if (vLDate != null) {
				vLCalendar.setTime(vLDate);
				activeVisitLastYearEndsAt.setTime(getCurrentVisitEndDate(patient));

				activeVisitLastYearEndsAt.add(Calendar.YEAR, -1);
				activeVisitLast6MonthsEndsAt.add(Calendar.MONTH, -6);
				if (vLCalendar.before(activeVisitLastYearEndsAt)) {
					alerts.add(
							Context.getMessageSourceService().getMessage("rwandasphstudyreports.alerts.orderRepeatVL"));
				}

			}
			if (Context.getService(CDCReportsService.class).checkIfPatientIsHIVPositive(patient)
					&& Context.getService(CDCReportsService.class).checkIfPatientIsOnARVMoreThanNMonths(patient, 12)
					&& vLObs.get(vLObs.size() - 1).getValueNumeric() > 1000) {
				alerts.add(Context.getMessageSourceService()
						.getMessage("rwandasphstudyreports.alerts.vlBasedTreatmentFailure"));
			}

		}

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
			Calendar lastRecordedVisit = Calendar.getInstance(Context.getLocale());

			lastRecordedVisit.setTime(lastVisitDate);
			lastRecordedVisit.add(Calendar.MONTH, 8);

			if (getOnlyObsWithDatetimeMoreThanNMonthsAfterObsDate(vLObs, lastRecordedVisit.getTime(), 8).isEmpty()
					&& Context.getService(CDCReportsService.class).checkIfPatientIsHIVPositive(patient)) {
				alerts.add(
						Context.getMessageSourceService().getMessage("rwandasphstudyreports.alerts.orderBaselineVL"));
			}
		}
		if (cd4Obs.isEmpty() && Context.getService(CDCReportsService.class).checkIfPatientIsHIVPositive(patient)) {
			alerts.add(Context.getMessageSourceService().getMessage("rwandasphstudyreports.alerts.orderBaselineCD4"));
		}

		if (Context.getService(CDCReportsService.class).checkIfPatientIsHIVPositive(patient)
				&& Context.getService(CDCReportsService.class).checkForAtleast50PercentDecreaseInCD4(patient)
				&& Context.getService(CDCReportsService.class).checkIfPatientIsOnARVMoreThanNMonths(patient, 12)) {
			alerts.add(Context.getMessageSourceService()
					.getMessage("rwandasphstudyreports.alerts.cd4BasedTreatmentFailure"));
		}

		return alerts;
	}

	private Date getCurrentVisitEndDate(Patient patient) {
		Visit activeVisit = Context.getService(CDCReportsService.class).getActiveVisit(patient, null);
		List<Encounter> encs = Context.getEncounterService().getEncountersByPatient(patient);
		encs = Context.getService(CDCReportsService.class).sortEncountersListByCreationDate(encs);

		if (activeVisit != null && activeVisit.getStopDatetime().before(new Date())) {
			return activeVisit.getStopDatetime();
		} else {
			if (!encs.isEmpty())
				return encs.get(0).getEncounterDatetime() != null ? encs.get(0).getEncounterDatetime()
						: encs.get(0).getDateCreated();
			else
				return new Date();
		}
	}

	private List<Obs> getOnlyObsWithDatetimeMoreThanNMonthsAfterObsDate(List<Obs> obsList, Date startingDate,
			Integer months) {
		List<Obs> matchedObs = new ArrayList<Obs>();

		if (obsList.size() > 0 && startingDate != null && months != null) {
			Calendar d = Calendar.getInstance(Context.getLocale());

			d.setTime(startingDate);
			d.add(Calendar.MONTH, months);
			for (Obs o : obsList) {
				if (o.getObsDatetime().after(d.getTime())) {
					matchedObs.add(o);
				}
			}
		}
		return matchedObs;
	}
}
