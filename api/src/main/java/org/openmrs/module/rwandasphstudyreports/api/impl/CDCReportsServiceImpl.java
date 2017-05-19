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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.Program;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.mohorderentrybridge.api.MoHOrderEntryBridgeService;
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
import org.openmrs.module.rwandasphstudyreports.GlobalPropertiesManagement;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertyConstants;
import org.openmrs.module.rwandasphstudyreports.QuickDataEntry;
import org.openmrs.module.rwandasphstudyreports.api.CDCReportsService;
import org.openmrs.module.rwandasphstudyreports.api.db.CDCReportsDAO;
import org.openmrs.module.rwandasphstudyreports.reports.BaseSPHReportConfig;

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
				if (BaseSPHReportConfig.PATIENTSWITHNOVLAFTER8MONTHS.equals(ds.getUuid())) {
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
					rr = Context.getService(ReportService.class).saveReportRequest(rr);
				}
				reportRequest = rr;
			}
		}

		return reportRequest;
	}

	private ReportRequest getTodayAdultLostToFollowUpReport() {
		List<ReportDefinition> defs = Context.getService(ReportDefinitionService.class)
				.getDefinitions("PatientsWithNoVLAfter8Months", true);
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

		return req != null ? req.getUuid() : "";
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
	public List<DrugOrder> matchOnlyDrugConceptFromOrders(List<DrugOrder> dOrders, Concept c) {
		List<DrugOrder> orders = new ArrayList<DrugOrder>();

		for (DrugOrder o : dOrders) {
			if (c.getConceptId().equals(o.getConcept().getConceptId()))
				orders.add(o);
		}
		return orders;
	}

	@Override
	public boolean checkIfPatientIsHIVPositive(Patient patient) {
		Concept hiv = Context.getConceptService().getConcept(Integer.parseInt(
				Context.getAdministrationService().getGlobalProperty(GlobalPropertyConstants.HIV_STATUS_CONCEPTID)));
		List<Obs> vLObs = Context.getObsService().getObservationsByPersonAndConcept(patient, hiv);

		sortObsListByObsDateTime(vLObs);
		if (!vLObs.isEmpty()) {// check last status reported instead
			if ("POSITIVE".equals(vLObs.get(0).getValueCoded().getFullySpecifiedName(Locale.ENGLISH).getName()))
				return true;
		}
		return false;
	}

	@Override
	public void sortObsListByObsDateTime(List<Obs> obsList) {
		Collections.sort(obsList, new Comparator<Obs>() {
			public int compare(Obs o1, Obs o2) {
				return o1.getObsDatetime().compareTo(o2.getObsDatetime());
			}
		});
	}

	@Override
	public void sortOrderListByStartDate(List<DrugOrder> arvDrugsOrders) {
		Collections.sort(arvDrugsOrders, new Comparator<DrugOrder>() {
			public int compare(DrugOrder o1, DrugOrder o2) {
				return o1.getEffectiveStartDate().compareTo(o2.getEffectiveStartDate());
			}
		});
	}

	@Override
	public List<Encounter> sortEncountersListByCreationDate(List<Encounter> encs) {
		Collections.sort(encs, new Comparator<Encounter>() {
			public int compare(Encounter o1, Encounter o2) {
				return o1.getDateCreated().compareTo(o2.getDateCreated());
			}
		});
		return encs;
	}

	@Override
	public List<Visit> sortVisitsListByCreationDate(List<Visit> visits) {
		Collections.sort(visits, new Comparator<Visit>() {
			public int compare(Visit o1, Visit o2) {
				return o1.getDateCreated().compareTo(o2.getDateCreated());
			}
		});
		return visits;
	}

	@Override
	public DrugOrder getARTInitiationDrug(Patient patient) {
		List<DrugOrder> arvDrugsOrders = new ArrayList<DrugOrder>();
		List<DrugOrder> drugOrders = Context.getService(MoHOrderEntryBridgeService.class)
				.getDrugOrdersByPatient(patient);

		String otherARVDrugConceptsIds = Context.getAdministrationService()
				.getGlobalProperty(GlobalPropertyConstants.OTHER_ARV_DRUGS_CONCEPTIDS);
		String aRVDrugConceptSetIds = Context.getAdministrationService()
				.getGlobalProperty(GlobalPropertyConstants.ARV_DRUGS_CONCEPTSETID);
		if (StringUtils.isNotBlank(otherARVDrugConceptsIds))
			for (String s : otherARVDrugConceptsIds.split(",")) {
				arvDrugsOrders.addAll(Context.getService(CDCReportsService.class).matchOnlyDrugConceptFromOrders(
						drugOrders, Context.getConceptService().getConcept(Integer.parseInt(s.trim()))));
			}
		if (StringUtils.isNotBlank(aRVDrugConceptSetIds)
				&& Context.getConceptService().getConcept(Integer.parseInt(aRVDrugConceptSetIds)).isSet())
			for (Concept c : Context.getConceptService().getConcept(Integer.parseInt(aRVDrugConceptSetIds))
					.getSetMembers()) {
				arvDrugsOrders.addAll(
						Context.getService(CDCReportsService.class).matchOnlyDrugConceptFromOrders(drugOrders, c));
			}
		Context.getService(CDCReportsService.class).sortOrderListByStartDate(arvDrugsOrders);

		if (!arvDrugsOrders.isEmpty())
			return arvDrugsOrders.get(0);

		return null;
	}

	@Override
	public boolean checkIfPatientIsOnARVMoreThanNMonths(Patient patient, Integer numberOfMonths) {
		if (patient != null && numberOfMonths != null) {
			DrugOrder artInitDrug = getARTInitiationDrug(patient);
			if (artInitDrug != null) {
				Calendar artInit = Calendar.getInstance(Context.getLocale());

				artInit.setTime(artInitDrug.getEffectiveStartDate());
				artInit.add(Calendar.MONTH, numberOfMonths);

				if (artInit.getTime().before(new Date())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * decrease in CD4 of ≥50% from last recorded outcomes.
	 */
	@Override
	public boolean checkForAtleast50PercentDecreaseInCD4(Patient patient) {
		Concept cd4 = Context.getConceptService().getConcept(Integer.parseInt(
				Context.getAdministrationService().getGlobalProperty(GlobalPropertyConstants.CD4_COUNT_CONCEPTID)));

		List<Obs> vLObs = Context.getObsService().getObservationsByPersonAndConcept(patient, cd4);

		sortObsListByObsDateTime(vLObs);

		if (vLObs.size() > 2 && (vLObs.get(vLObs.size() - 1).getValueNumeric() * 100)
				/ vLObs.get(vLObs.size() - 2).getValueNumeric() >= 50) {
			return true;
		}

		return false;
	}
	
	/**
	 * 
	 * @param obsQuestion, defaults to Viral load concept if null
	 * @param nMonths, defaults to 8 if not set
	 * @param program, defaults to HIV program if not set
	 * @param patient, must be set
	 * @return
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean checkIfPatientHasNoObsInLastNMonthsAfterProgramInit(Concept obsQuestion, Integer nMonths, Program program, Patient patient) {
		if(patient != null) {
			Calendar enD = Calendar.getInstance();
			GlobalPropertiesManagement gp = new GlobalPropertiesManagement();
			
			if(nMonths == null)
				nMonths = 8;
			if(program == null)
				program = gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
			if(obsQuestion == null)
				obsQuestion = gp.getConcept(GlobalPropertyConstants.VIRAL_LOAD_CONCEPTID);	
		
			List<Obs> os = Context.getObsService().getObservationsByPersonAndConcept(patient, obsQuestion);
			
			if(os.isEmpty()) {
				return true;
			} else {
				List<PatientProgram> pp = new ArrayList(Context.getProgramWorkflowService().getPatientPrograms(patient, program, null, null, null, null, false));
				
				Collections.sort(pp, new Comparator<PatientProgram>() {
					public int compare(PatientProgram o1, PatientProgram o2) {
						return o1.getDateEnrolled().compareTo(o2.getDateEnrolled());
					}
				});
				sortObsListByObsDateTime(os);
				if(!pp.isEmpty()) {
					enD.setTime(pp.get(0).getDateEnrolled());
					enD.add(Calendar.MONTH, nMonths);
					
					if(os.get(0).getObsDatetime().after(enD.getTime()))
						return false;
				}
			}
		}
		return true;
	}
}