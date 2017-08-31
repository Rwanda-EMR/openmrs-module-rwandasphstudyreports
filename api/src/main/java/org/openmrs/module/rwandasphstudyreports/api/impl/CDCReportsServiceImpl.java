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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptName;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.Person;
import org.openmrs.Program;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.mohorderentrybridge.MoHDrugOrder;
import org.openmrs.module.reporting.definition.DefinitionSummary;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.report.Report;
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
import org.openmrs.module.rwandasphstudyreports.SphClientOrPatient;
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
	public ReportRequest executeAndGetPatientsWithNoVLAfter8MonthsReportRequest() {
		ReportRequest repReq = executeAndGetReportRequest(BaseSPHReportConfig.PATIENTSWITHNOVLAFTER8MONTHS);

		return repReq != null ? repReq : null;
	}

	@Override
	public ReportRequest executeAndGetVLBasedTreatmentFailureReportRequest() {
		ReportRequest repReq = executeAndGetReportRequest(BaseSPHReportConfig.VLBASEDTREATMENTFAILUREREPORT);

		return repReq != null ? repReq : null;
	}

	private ReportRequest executeAndGetReportRequest(String uuid) {
		DefinitionSummary repDefSum = null;
		ReportRequest reportRequest = getTodayReportRequest(uuid);
		Calendar startDate = Calendar.getInstance();

		if (reportRequest == null) {
			List<DefinitionSummary> defs = Context.getService(ReportDefinitionService.class)
					.getAllDefinitionSummaries(false);
			for (DefinitionSummary ds : defs) {
				if (uuid.equals(ds.getUuid())) {
					repDefSum = ds;
					break;
				}
			}
			if (repDefSum != null) {
				ReportRequest rr = Context.getService(ReportService.class).getReportRequestByUuid(repDefSum.getUuid());
				ReportDefinition def = Context.getService(ReportDefinitionService.class)
						.getDefinitionByUuid(repDefSum.getUuid());

				if (rr == null) {
					rr = new ReportRequest(new Mapped<ReportDefinition>(def, null), null,
							new RenderingMode(new DefaultWebRenderer(), "Web", null, 100), Priority.NORMAL, null);

					startDate.setTime(todayMidNight().getTime());
					startDate.add(Calendar.YEAR, -1);
					rr.setStatus(ReportRequest.Status.REQUESTED);
					rr.setPriority(ReportRequest.Priority.NORMAL);
					// TODO fix for PatientsWithNoVLAfter8Months report
					if (!uuid.equals(BaseSPHReportConfig.PATIENTSWITHNOVLAFTER8MONTHS))
						rr.getReportDefinition().addParameterMapping("startDate", startDate.getTime());
					rr.getReportDefinition().addParameterMapping("endDate", todayMidNight().getTime());
					rr = Context.getService(ReportService.class).saveReportRequest(rr);
				}
				reportRequest = rr;
			}
		}

		return reportRequest;
	}

	private ReportRequest getTodayReportRequest(String uuid) {
		ReportDefinition def = Context.getService(ReportDefinitionService.class).getDefinitionByUuid(uuid);
		List<ReportRequest> rrs = null;
		ReportRequest req = null;
		Calendar today = todayMidNight();

		if (def != null) {
			rrs = Context.getService(ReportService.class).getReportRequests(def, today.getTime(), null,
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
		Concept hivPositive = Context.getConceptService().getConcept(Integer.parseInt(
				Context.getAdministrationService().getGlobalProperty(GlobalPropertyConstants.HIV_POSITIVE_CONCEPTID)));

		if (hiv == null)
			hiv = Context.getConceptService().getConcept(2169);
		if (hivPositive == null)
			hivPositive = Context.getConceptService().getConcept(703);

		List<Obs> vLObs = Context.getObsService().getObservationsByPersonAndConcept(patient, hiv);

		sortObsListByObsDateTime(vLObs);
		if (hivPositive != null && vLObs != null && !vLObs.isEmpty()) {// check
																		// last
																		// status
																		// reported
																		// instead
			if (hivPositive.getConceptId()
					.equals(vLObs.get(0).getValueCoded() != null ? vLObs.get(0).getValueCoded().getConceptId() : null))
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
		return getDao().getARTInitiationDrug(patient.getPerson());
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Date getHIVEnrollmentDate(Patient patient) {
		GlobalPropertiesManagement gp = new GlobalPropertiesManagement();
		Program program = gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
		List<PatientProgram> pp = new ArrayList(Context.getProgramWorkflowService().getPatientPrograms(patient, program,
				null, null, null, null, false));

		if (!pp.isEmpty())
			return pp.get(0).getDateEnrolled();
		return null;
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

		if (vLObs.size() > 2 && (vLObs.get(0).getValueNumeric() * 100) / vLObs.get(1).getValueNumeric() >= 50) {
			return true;
		}

		return false;
	}

	/**
	 * 
	 * @param obsQuestion,
	 *            defaults to Viral load concept if null
	 * @param nMonths,
	 *            defaults to 8 if not set
	 * @param program,
	 *            defaults to HIV program if not set
	 * @param patient,
	 *            must be set
	 * @return
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean checkIfPatientHasNoObsInLastNMonthsAfterProgramInit(Concept obsQuestion, Integer nMonths,
			Program program, Patient patient) {
		if (patient != null) {
			Calendar enD = Calendar.getInstance();
			GlobalPropertiesManagement gp = new GlobalPropertiesManagement();

			if (nMonths == null)
				nMonths = 8;
			if (program == null)
				program = gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
			if (obsQuestion == null)
				obsQuestion = gp.getConcept(GlobalPropertyConstants.VIRAL_LOAD_CONCEPTID);

			List<Obs> os = Context.getObsService().getObservationsByPersonAndConcept(patient, obsQuestion);
			List<PatientProgram> pp = new ArrayList(Context.getProgramWorkflowService().getPatientPrograms(patient,
					program, null, null, null, null, false));

			if ((os.isEmpty() && !pp.isEmpty() || (!os.isEmpty() && pp.isEmpty()) || (os.isEmpty() && pp.isEmpty())))
				return true;

			if (!os.isEmpty() && !pp.isEmpty()) {
				sortPatientProgramListByEnrollmentDate(pp);
				sortObsListByObsDateTime(os);
				enD.setTime(pp.get(0).getDateEnrolled());
				enD.add(Calendar.MONTH, nMonths);

				if (os.get(0).getObsDatetime().before(enD.getTime()))
					return true;
			}
		}
		return false;
	}

	private void sortPatientProgramListByEnrollmentDate(List<PatientProgram> pp) {
		Collections.sort(pp, new Comparator<PatientProgram>() {
			public int compare(PatientProgram o1, PatientProgram o2) {
				return o1.getDateEnrolled().compareTo(o2.getDateEnrolled());
			}
		});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public boolean checkIfPatientListedAsBeingAViralLoadTreatmentFailureCase(Patient patient) {
		// TODO withVLDateBetweenStartAndEndDate
		Calendar enD = Calendar.getInstance();
		GlobalPropertiesManagement gp = new GlobalPropertiesManagement();
		Program program = gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
		Concept vl = Context.getConceptService().getConcept(Integer.parseInt(
				Context.getAdministrationService().getGlobalProperty(GlobalPropertyConstants.VIRAL_LOAD_CONCEPTID)));
		List<Obs> vLObs = Context.getObsService().getObservationsByPersonAndConcept(patient, vl);
		List<PatientProgram> pp = new ArrayList(Context.getProgramWorkflowService().getPatientPrograms(patient, program,
				null, null, null, null, false));

		if (!vLObs.isEmpty() && !pp.isEmpty()) {
			sortPatientProgramListByEnrollmentDate(pp);
			sortObsListByObsDateTime(vLObs);

			Obs o = vLObs.get(0);
			PatientProgram p = pp.get(0);

			if (p.getDateEnrolled() != null && o.getValueNumeric() != null && o.getValueNumeric() > 1000) {
				enD.setTime(p.getDateEnrolled());
				enD.add(Calendar.MONTH, 12);

				if (p.getActive(enD.getTime()))
					return true;
			}
		}

		return false;
	}

	@Override
	public Obs createObs(Concept concept, Object value, Date datetime, String accessionNumber) {
		Obs obs = null;

		if (concept != null) {
			obs = new Obs();
			obs.setConcept(concept);
			ConceptDatatype dt = obs.getConcept().getDatatype();
			if (dt.isNumeric()) {
				obs.setValueNumeric(Double.parseDouble(value.toString()));
			} else if (dt.isText()) {
				if (value instanceof Location) {
					Location location = (Location) value;
					obs.setValueText(location.getId().toString() + " - " + location.getName());
				} else if (value instanceof Person) {
					Person person = (Person) value;
					obs.setValueText(person.getId().toString() + " - " + person.getPersonName().toString());
				} else {
					obs.setValueText(value.toString());
				}
			} else if (dt.isCoded()) {
				if (value instanceof Drug) {
					obs.setValueDrug((Drug) value);
					obs.setValueCoded(((Drug) value).getConcept());
				} else if (value instanceof ConceptName) {
					obs.setValueCodedName((ConceptName) value);
					obs.setValueCoded(obs.getValueCodedName().getConcept());
				} else if (value instanceof Concept) {
					obs.setValueCoded((Concept) value);
				}
			} else if (dt.isBoolean()) {
				if (value != null) {
					try {
						obs.setValueAsString(value.toString());
					} catch (ParseException e) {
						throw new IllegalArgumentException("Unable to convert " + value + " to a Boolean Obs value", e);
					}
				}
			} else if (ConceptDatatype.DATE.equals(dt.getHl7Abbreviation())
					|| ConceptDatatype.TIME.equals(dt.getHl7Abbreviation())
					|| ConceptDatatype.DATETIME.equals(dt.getHl7Abbreviation())) {
				Date date = (Date) value;
				obs.setValueDatetime(date);
			} else if ("ZZ".equals(dt.getHl7Abbreviation())) {
				// don't set a value
			} else {
				throw new IllegalArgumentException("concept datatype not yet implemented: " + dt.getName()
						+ " with Hl7 Abbreviation: " + dt.getHl7Abbreviation());
			}
			if (datetime != null)
				obs.setObsDatetime(datetime);
			else
				obs.setObsDatetime(new Date());
			if (accessionNumber != null)
				obs.setAccessionNumber(accessionNumber);
		}
		return obs;
	}

	@Override
	public Obs saveNewObs(Concept concept, Object value, Date datetime, String accessionNumber, Patient patient) {
		Obs o = createObs(concept, value, datetime, accessionNumber);

		if (o != null && patient != null) {
			o.setPerson(patient);
			return Context.getObsService().saveObs(o, null);
		}
		return null;
	}
	
	@Override
	public Obs saveVLBasedTreatmentFailure(Patient patient) {
		String adherenceConceptId = Context.getAdministrationService()
				.getGlobalProperty(GlobalPropertyConstants.ARV_ADHERENCE_OBS_CONCEPTID);

		if (StringUtils.isNotBlank(adherenceConceptId) && patient != null) {
			return saveNewObs(Context.getConceptService().getConcept(Integer.parseInt(adherenceConceptId)), getVLTreatmentFailureAction(patient), null, null, patient);		
		}
		return null;
	}

	@Override
	public void enrollPatientInProgram(Patient patient, Program program, Date enrollmentDate, Date completionDate) {
		PatientProgram p = new PatientProgram();

		p.setPatient(patient);
		p.setProgram(program);
		p.setDateEnrolled(enrollmentDate);
		p.setDateCompleted(completionDate);
		p.setCreator(Context.getAuthenticatedUser());

		Context.getProgramWorkflowService().savePatientProgram(p);
	}

	@Override
	public Report runReport(ReportDefinition reportDef, Date startDate, Date endDate, Location location) {
		ReportRequest request = new ReportRequest(new Mapped<ReportDefinition>(reportDef, null), null,
				new RenderingMode(new DefaultWebRenderer(), "Web", null, 100), Priority.HIGHEST, null);

		if (startDate != null)
			request.getReportDefinition().addParameterMapping("startDate", startDate);
		if (endDate != null)
			request.getReportDefinition().addParameterMapping("endDate", endDate);
		if (location != null)
			request.getReportDefinition().addParameterMapping("location", location);
		request.setStatus(Status.PROCESSING);
		request = Context.getService(ReportService.class).saveReportRequest(request);

		return Context.getService(ReportService.class).runReport(request);
	}

	public List<Patient> getHIVPositivePatientsOnARVTreatment() {
		return getDao().getHIVPositivePatientsOnARVTreatment();
	}

	public List<SphClientOrPatient> getHIVPositiveClientsOrPatientsForConsultationSheet(Date startDate, Date endDate,
			String[] datesToMatch) {
		return getDao().getHIVPositiveClientsOrPatientsForConsultationSheet(startDate, endDate, datesToMatch);
	}

	public List<Patient> getPatientsInHIVProgram(Program program, Date starDate, Date endDate) {
		return getDao().getPatientsInHIVProgram(program, starDate, endDate);
	}

	public boolean matchTestEnrollmentAndArtInitDates(Date testDate, Date hivEnrollmentDate, Date artInitDate,
			String[] datesToMatch, Date startDate, Date endDate) {
		return getDao().matchTestEnrollmentAndArtInitDates(testDate, hivEnrollmentDate, artInitDate, datesToMatch,
				startDate, endDate);
	}

	@Override
	public String getCurrentRegimen(List<MoHDrugOrder> orders) {
		return getDao().getCurrentRegimen(orders);
	}

	@Override
	public boolean vlBasedTreatmentFailure(Patient patient) {
		Concept vl = Context.getConceptService().getConcept(Integer.parseInt(
				Context.getAdministrationService().getGlobalProperty(GlobalPropertyConstants.VIRAL_LOAD_CONCEPTID)));
		DrugOrder artInitDrug = getARTInitiationDrug(patient);
		List<Obs> vLObs = Context.getObsService().getObservationsByPersonAndConcept(patient, vl);

		return artInitDrug != null && checkIfDateIsNMonthsFromNow(artInitDrug.getEffectiveStartDate(), 12)
				&& checkIfPatientIsHIVPositive(patient) && !vLObs.isEmpty() && vLObs.get(0).getValueNumeric() >= 1000;
	}

	@Override
	public boolean checkIfDateIsNMonthsFromNow(Date date, Integer nMonths) {
		if (date != null && nMonths != null) {
			Calendar c = Calendar.getInstance();

			c.setTime(date);
			c.add(Calendar.MONTH, -nMonths);
			return c.getTime().after(date);
		}
		return false;
	}

	@Override
	public boolean cd4BasedTreatmentFailure(Patient patient) {
		return checkIfPatientIsHIVPositive(patient) && checkForAtleast50PercentDecreaseInCD4(patient)
				&& checkIfPatientIsOnARVMoreThanNMonths(patient, 12);
	}

	@Override
	public String getVLTreatmentFailureAction(Patient patient) {
		String adherenceConceptId = Context.getAdministrationService()
				.getGlobalProperty(GlobalPropertyConstants.ARV_ADHERENCE_OBS_CONCEPTID);

		if (patient != null && StringUtils.isNotBlank(adherenceConceptId)) {
			List<Obs> actions = Context.getObsService().getObservationsByPersonAndConcept(patient,
					Context.getConceptService().getConcept(Integer.parseInt(adherenceConceptId)));

			if (!actions.isEmpty())
				return actions.get(0).getValueText();
		}
		return null;
	}
}