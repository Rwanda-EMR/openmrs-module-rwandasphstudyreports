package org.openmrs.module.rwandasphstudyreports;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandasphstudyreports.api.CDCReportsService;
import org.openmrs.module.rwandasphstudyreports.sitepackages.SitePackageManager;

public class CDCRulesAlgorithm {
	public List<String> cdcDsRulesAlerts(Patient patient) {
		List<String> alerts = new ArrayList<String>();
		Date enrollmentDate = Context.getService(CDCReportsService.class).getHIVEnrollmentDate(patient);
		
		if(enrollmentDate != null && !Context.getService(CDCReportsService.class).checkIfPatientIsExittedFromCare(patient)) {
			Concept vl = Context.getConceptService().getConcept(Integer.parseInt(
					Context.getAdministrationService().getGlobalProperty(GlobalPropertyConstants.VIRAL_LOAD_CONCEPTID)));
			Concept cd4 = Context.getConceptService().getConcept(Integer.parseInt(
					Context.getAdministrationService().getGlobalProperty(GlobalPropertyConstants.CD4_COUNT_CONCEPTID)));
			@SuppressWarnings("deprecation")
			List<Obs> vLObs = Context.getObsService().getLastNObservations(1, patient, vl, false);
			List<Visit> visits = Context.getVisitService().getVisitsByPatient(patient);
			Date lastVisitDate = null;
			List<Obs> cd4Obs = Context.getObsService().getObservationsByPersonAndConcept(patient, cd4);
			
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
			
			if(SitePackageManager.currentSiteIsPackage2()) {
				if (lastVisitDate != null) {
					if (vLObs.isEmpty() && Context.getService(CDCReportsService.class).checkIfPatientIsInHIVMoreThanNMonths(patient, 8)
							&& Context.getService(CDCReportsService.class).checkIfPatientIsHIVPositiveOrMissingResult(patient)) {
						alerts.add(
								Context.getMessageSourceService().getMessage("rwandasphstudyreports.alerts.orderBaselineVL"));
					}
				}
				if (cd4Obs.isEmpty() && Context.getService(CDCReportsService.class).checkIfPatientIsHIVPositiveOrMissingResult(patient)) {
					alerts.add(Context.getMessageSourceService().getMessage("rwandasphstudyreports.alerts.orderBaselineCD4"));
				}
			}
			
			if(SitePackageManager.currentSiteIsPackage3()) {
				if (Context.getService(CDCReportsService.class).cd4BasedTreatmentFailure(patient)) {
					alerts.add(Context.getMessageSourceService()
							.getMessage("rwandasphstudyreports.alerts.cd4BasedTreatmentFailure"));
				}
				
				if (!vLObs.isEmpty()) {
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
			}
		}
		return alerts;
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
