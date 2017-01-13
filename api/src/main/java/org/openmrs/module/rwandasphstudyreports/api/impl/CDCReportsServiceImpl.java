/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.rwandasphstudyreports.api.impl;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.reporting.definition.DefinitionSummary;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.report.ReportRequest;
import org.openmrs.module.reporting.report.ReportRequest.Priority;
import org.openmrs.module.reporting.report.ReportRequest.Status;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.definition.service.ReportDefinitionService;
import org.openmrs.module.reporting.report.renderer.RenderingMode;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.reporting.web.renderers.DefaultWebRenderer;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertyConstants;
import org.openmrs.module.rwandasphstudyreports.QuickDataEntry;
import org.openmrs.module.rwandasphstudyreports.api.CDCReportsService;
import org.openmrs.module.rwandasphstudyreports.api.db.CDCReportsDAO;

/**
 * It is a default implementation of {@link CDCReportsService}.
 */
public class CDCReportsServiceImpl extends BaseOpenmrsService implements CDCReportsService {

	protected final Log log = LogFactory.getLog(this.getClass());

	private CDCReportsDAO dao;

	/**
	 * @param dao
	 *            the dao to set
	 */
	public void setDao(CDCReportsDAO dao) {
		this.dao = dao;
	}

	/**
	 * @return the dao
	 */
	public CDCReportsDAO getDao() {
		return dao;
	}

	public Cohort getAllRwandaAdultsPatients() {
		return dao.getAllRwandaAdultsPatients();
	}

	@Override
	public ReportRequest executeAndGetAdultFollowUpReportRequest() {
		DefinitionSummary lostToFollowUp = null;
		ReportRequest reportRequest = getTodayAdultLostToFollowUpReport();

		if (reportRequest == null) {
			List<DefinitionSummary> defs = Context.getService(ReportDefinitionService.class)
					.getAllDefinitionSummaries(false);
			for (DefinitionSummary ds : defs) {
				if ("HIV-Adult ART Report-Monthly".equals(ds.getName())) {
					lostToFollowUp = ds;
					break;
				}
			}
			if (lostToFollowUp != null) {
				ReportRequest rr = Context.getService(ReportService.class)
						.getReportRequestByUuid(lostToFollowUp.getUuid());
				ReportDefinition def = Context.getService(ReportDefinitionService.class)
						.getDefinitionByUuid(lostToFollowUp.getUuid());

				if (rr == null) {
					rr = new ReportRequest(new Mapped<ReportDefinition>(def, null), null,
							new RenderingMode(new DefaultWebRenderer(), "Web", null, 100), Priority.NORMAL, null);

					rr.setStatus(ReportRequest.Status.REQUESTED);
					rr.setPriority(ReportRequest.Priority.NORMAL);
					rr.getReportDefinition().addParameterMapping("endDate", todayMidNight().getTime());
					rr.getReportDefinition().addParameterMapping("location",
							Context.getLocationService().getLocation(Integer.parseInt(Context.getAdministrationService()
									.getGlobalProperty("mohtracportal.defaultLocationId"))));
					rr = Context.getService(ReportService.class).saveReportRequest(rr);
				}
				reportRequest = rr;
			}
		}

		return reportRequest;
	}

	private ReportRequest getTodayAdultLostToFollowUpReport() {
		List<ReportDefinition> defs = Context.getService(ReportDefinitionService.class)
				.getDefinitions("HIV-Adult ART Report-Monthly", true);
		List<ReportRequest> rrs = null;
		ReportRequest req = null;
		Calendar today = todayMidNight();

		if (defs != null && defs.size() > 0) {
			rrs = Context.getService(ReportService.class).getReportRequests(defs.get(0), today.getTime(), null,
					Status.COMPLETED, Status.REQUESTED, Status.PROCESSING);
			if (rrs != null && rrs.size() > 0)
				req = rrs.get(0);
		}

		return req;
	}

	private Calendar todayMidNight() {
		Calendar today = Calendar.getInstance(Context.getLocale());

		today.set(Calendar.HOUR_OF_DAY, 0);
		today.clear(Calendar.MINUTE);
		today.clear(Calendar.SECOND);
		today.clear(Calendar.MILLISECOND);
		return today;
	}

	@Override
	public String executeAndGetAdultFollowUpReportRequestUuid() {
		ReportRequest req = executeAndGetAdultFollowUpReportRequest();

		return req != null ? executeAndGetAdultFollowUpReportRequest().getUuid() : "";
	}

	@Override
	public Obs saveQuickDataEntry(QuickDataEntry entry, Patient patient, Encounter encounter) {
		Obs obs = new Obs(patient, entry.getTest(), entry.getDateOfExam(), entry.getLocation());

		obs.setEncounter(encounter);
		if ("Numeric".equals(entry.getTestType()))
			obs.setValueNumeric((Double) entry.getResult());
		else if ("Date".equals(entry.getTestType()) || "Datetime".equals(entry.getTestType()))
			obs.setValueDate((Date) entry.getResult());
		else if ("Boolean".equals(entry.getTestType()))
			obs.setValueBoolean((Boolean) entry.getResult());
		else if ("Text".equals(entry.getTestType()))
			try {
				obs.setValueAsString((String) entry.getResult());
			} catch (ParseException e) {
				e.printStackTrace();
			}
		else if ("Coded".equals(entry.getTestType()))
			obs.setValueCoded((Concept) entry.getResult());
		return Context.getObsService().saveObs(obs, "Creating a new observation from quick data entry form");
	}

	@Override
	public Visit getActiveVisit(Patient patient, String visitLocationUuid) {
		List<Visit> activeVisits = Context.getVisitService().getActiveVisitsByPatient(patient);
		if (visitLocationUuid != null) {
			return getVisitBasedOnLocation(visitLocationUuid, activeVisits);
		}
		return activeVisits != null && !activeVisits.isEmpty() ? activeVisits.get(0) : null;
	}

	private Visit getVisitBasedOnLocation(String locationUuid, List<Visit> activeVisits) {
		for (Visit visit : activeVisits) {
			Location visitLocation = visit.getLocation();
			if (visitLocation != null && (visitLocation.getUuid()).equals(locationUuid)) {
				return visit;
			}
		}
		return null;
	}

	@Override
	public boolean checkIfPatientIsHIVPositive(Patient patient) {
		Concept hiv = Context.getConceptService().getConcept(Integer.parseInt(
				Context.getAdministrationService().getGlobalProperty(GlobalPropertyConstants.HIV_STATUS_CONCEPTID)));

		List<Obs> vLObs = Context.getObsService().getObservationsByPersonAndConcept(patient, hiv);

		if (!vLObs.isEmpty()) {
			for (Obs o : vLObs) {
				if ("POSITIVE".equals(o.getValueCoded().getFullySpecifiedName(Locale.ENGLISH).getName()))
					return true;
			}
		}
		return false;
	}
}