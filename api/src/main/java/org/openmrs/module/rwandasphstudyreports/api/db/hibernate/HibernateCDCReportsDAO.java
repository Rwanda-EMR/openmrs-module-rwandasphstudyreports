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
package org.openmrs.module.rwandasphstudyreports.api.db.hibernate;

import static org.openmrs.api.context.Context.getProgramWorkflowService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.GlobalProperty;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PatientProgram;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttributeType;
import org.openmrs.Program;
import org.openmrs.api.context.Context;
import org.openmrs.module.mohorderentrybridge.MoHDrugOrder;
import org.openmrs.module.mohorderentrybridge.api.MoHOrderEntryBridgeService;
import org.openmrs.module.rwandasphstudyreports.CDCRulesAlgorithm;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertiesManagement;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertyConstants;
import org.openmrs.module.rwandasphstudyreports.SphClientOrPatient;
import org.openmrs.module.rwandasphstudyreports.api.CDCReportsService;
import org.openmrs.module.rwandasphstudyreports.api.db.CDCReportsDAO;
import org.openmrs.module.vcttrac.VCTClient;
import org.openmrs.module.vcttrac.service.VCTModuleService;
import org.openmrs.module.vcttrac.util.VCTConfigurationUtil;

/**
 * It is a default implementation of {@link CDCReportsDAO}.
 */
public class HibernateCDCReportsDAO implements CDCReportsDAO {
	protected final Log log = LogFactory.getLog(this.getClass());

	private SessionFactory sessionFactory;

	/**
	 * @param sessionFactory
	 *            the sessionFactory to set
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * @return the sessionFactory
	 */
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	@SuppressWarnings("unchecked")
	public Cohort getAllRwandaAdultsPatients() {
		Query query = sessionFactory.getCurrentSession().createQuery(
				"select distinct p.patient_id from patient p inner join person pp on pp.person_id = p.patient_id where p.voided=0 and pp.voided=0 and (pp.birthdate is null or (select DATEDIFF(NOW(), pp.birthdate) / 365.25) >= 16)");

		Set<Integer> ids = new HashSet<Integer>();
		ids.addAll(query.list());

		return new Cohort("All Rwanda Adults patients", "", ids);
	}

	@Override
	public DrugOrder getARTInitiationDrug(Person person) {
		List<DrugOrder> arvDrugsOrders = new ArrayList<DrugOrder>();
		List<DrugOrder> drugOrders = Context.getService(MoHOrderEntryBridgeService.class)
				.getDrugOrdersByPatient(new Patient(person));

		String otherARVDrugConceptsIds = Context.getAdministrationService()
				.getGlobalProperty(GlobalPropertyConstants.OTHER_ARV_DRUGS_CONCEPTIDS);
		String aRVDrugConceptSetIds = Context.getAdministrationService()
				.getGlobalProperty(GlobalPropertyConstants.ARV_DRUGS_CONCEPTSETID);
		if (StringUtils.isNotBlank(otherARVDrugConceptsIds))
			for (String s : otherARVDrugConceptsIds.split(",")) {
				arvDrugsOrders.addAll(matchOnlyDrugConceptFromOrders(drugOrders,
						Context.getConceptService().getConcept(Integer.parseInt(s.trim()))));
			}
		if (StringUtils.isNotBlank(aRVDrugConceptSetIds)
				&& Context.getConceptService().getConcept(Integer.parseInt(aRVDrugConceptSetIds)).isSet())
			for (Concept c : Context.getConceptService().getConcept(Integer.parseInt(aRVDrugConceptSetIds))
					.getSetMembers()) {
				arvDrugsOrders.addAll(matchOnlyDrugConceptFromOrders(drugOrders, c));
			}
		Context.getService(CDCReportsService.class).sortOrderListByStartDate(arvDrugsOrders);

		if (!arvDrugsOrders.isEmpty())
			return arvDrugsOrders.get(0);

		return null;
	}

	@Override
	public List<Patient> getHIVPositivePatientsOnARVTreatment() {
		String hivStatus = Context.getAdministrationService()
				.getGlobalProperty(GlobalPropertyConstants.HIV_STATUS_CONCEPTID);
		String hivPositive = Context.getAdministrationService()
				.getGlobalProperty(GlobalPropertyConstants.HIV_POSITIVE_CONCEPTID);
		List<Person> obsList = getSessionFactory().getCurrentSession().createCriteria(Obs.class).add(Restrictions.eq(
				"concept",
				StringUtils.isNotBlank(hivStatus) ? Context.getConceptService().getConcept(Integer.parseInt(hivStatus))
						: Context.getConceptService().getConcept(2169)))
				.add(Restrictions.eq("valueCoded",
						StringUtils.isNotBlank(hivPositive)
								? Context.getConceptService().getConcept(Integer.parseInt(hivPositive))
								: Context.getConceptService().getConcept(703)))
				.setProjection(Projections.distinct(Projections.property("person")))
				.setFetchMode("Order", FetchMode.JOIN).add(Restrictions.in("concept", getHIVDrugsConcepts())).list();

		List<Patient> pList = new ArrayList<Patient>();

		for (Person p : obsList)
			pList.add(new Patient(p));

		return pList;
	}

	private List<Concept> getHIVDrugsConcepts() {
		List<Concept> l = new ArrayList<Concept>();
		String arv = Context.getAdministrationService()
				.getGlobalProperty(GlobalPropertyConstants.ARV_DRUGS_CONCEPTSETID);
		String arvs = Context.getAdministrationService().getGlobalProperty(GlobalPropertyConstants.ARV_CONCEPT_IDS);

		if (StringUtils.isNotBlank(arv))
			l.add(Context.getConceptService().getConcept(Integer.parseInt(arv.trim())));
		if (StringUtils.isNotBlank(arvs)) {
			for (String s : arvs.split(",")) {
				if (StringUtils.isNotBlank(s))
					l.add(Context.getConceptService().getConcept(Integer.parseInt(s.trim())));
			}
		}
		return l;
	}

	@Override
	public List<Patient> getPatientsInHIVProgram(Program program, Date startDate, Date endDate) {
		List<Patient> patients = new ArrayList<Patient>();

		if(startDate == null && endDate == null) {
			GlobalProperty period = Context.getAdministrationService()
					.getGlobalPropertyObject(GlobalPropertyConstants.MONTHS_ALLOWANCE_FOR_CONSULTATIONSHEET);
			Calendar sd = Calendar.getInstance();
			endDate = new Date();

			if (period != null && StringUtils.isNotBlank(period.getPropertyValue()))
				sd.add(Calendar.MONTH, -Integer.parseInt(period.getPropertyValue()));
			startDate = sd.getTime();
		}
			
		for (PatientProgram p : Context.getProgramWorkflowService().getPatientPrograms(null, program, startDate, endDate,
				null, null, false)) {
			if (!patients.contains(p.getPatient()))
				patients.add(p.getPatient());
		}

		return patients;
	}

	@Override
	public List<SphClientOrPatient> getHIVPositiveClientsOrPatientsForConsultationSheet(Date startDate, Date endDate,
			String[] datesToMatch, String[] alerts) {
		List<VCTClient> clientList = Context.getService(VCTModuleService.class)
				.getVCTClientsWaitingForHIVProgramEnrollment();
		List<Patient> patientList = getPatientsInHIVProgram(
				new GlobalPropertiesManagement().getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM), datesToMatch != null && ArrayUtils.contains(datesToMatch, "enrollment") ? startDate : null,
						datesToMatch != null && ArrayUtils.contains(datesToMatch, "enrollment") ? endDate : null);
		List<SphClientOrPatient> uiClients = new ArrayList<SphClientOrPatient>();
		String clientIds = "";

		for (Patient p : patientList) {
			if (!p.isVoided())
				clientIds = convertAndAddClientOrPatient(uiClients, clientIds, p, startDate, endDate, datesToMatch,
						alerts);
		}

		for (VCTClient c : clientList) {
			if (!c.isVoided() && !c.getClient().isVoided())
				clientIds = convertAndAddClientOrPatient(uiClients, clientIds, c, startDate, endDate, datesToMatch,
						alerts);
		}

		return uiClients;
	}

	private String convertAndAddClientOrPatient(List<SphClientOrPatient> uiClients, String clientIds, Object p,
			Date startDate, Date endDate, String[] datesToMatch, String[] alerts) {
		SphClientOrPatient client = convertPersonIntoSphClientOrPatient(p, startDate, endDate, datesToMatch, alerts);
		if (client != null && clientIds.indexOf("," + client.getId()) < 0) {
			uiClients.add(client);
			clientIds += "," + client.getId();
		}
		return clientIds;
	}

	private void resetTimes(Calendar c) {
		c.set(Calendar.HOUR, 00);
		c.set(Calendar.MINUTE, 00);
		c.set(Calendar.SECOND, 00);
		c.set(Calendar.MILLISECOND, 00);
	}

	private SphClientOrPatient convertPersonIntoSphClientOrPatient(Object p, Date startDate, Date endDate,
			String[] datesToMatch, String[] alerts) {
		Person person = null;
		SimpleDateFormat sdf = Context.getDateFormat();
		Patient patient = null;

		if (p instanceof Patient) {
			person = ((Patient) p).getPerson();
			patient = (Patient) p;
		} else if (p instanceof VCTClient) {
			person = ((VCTClient) p).getClient();
		}
		if (person != null) {
			Date testDate = checkIfPersonIsHIVPositive(person);
			PersonAttributeType tel = Context.getPersonService().getPersonAttributeTypeByName("Phone Number");
			PersonAttributeType peerEduc = Context.getPersonService()
					.getPersonAttributeTypeByName("Peer Educator's Name");
			PersonAttributeType peerEducTel = Context.getPersonService()
					.getPersonAttributeTypeByName("Peer Educator's Phone Number");
			PersonAttributeType contactPerson = Context.getPersonService()
					.getPersonAttributeTypeByName("Contact Person's Name");
			PersonAttributeType contactPersonTel = Context.getPersonService()
					.getPersonAttributeTypeByName("Contact Person's Phone Number");
			Date hivEnrollmentDate = checkIfPersonIsEnrolledInHIVProgram(person);
			Date artInitDate = getArtInitiationDate(person);
			String adultAge = Context.getAdministrationService().getGlobalProperty("reports.adultStartingAge");
			Calendar adult = Calendar.getInstance();
			PatientIdentifierType tracnetIdType = new GlobalPropertiesManagement()
					.getPatientIdentifier(GlobalPropertiesManagement.TRACNET_IDENTIFIER);
			String rVisitDC = Context.getAdministrationService()
					.getGlobalProperty(GlobalPropertyConstants.RETURN_VISIT_CONCEPTID);
			List<Obs> returnVisitDate = StringUtils.isNotBlank(rVisitDC) ? Context.getObsService().getLastNObservations(
					1, person, Context.getConceptService().getConcept(Integer.parseInt(rVisitDC)), false) : null;
			List<Obs> savedTestDate = Context.getObsService().getLastNObservations(1, person,
					Context.getConceptService().getConcept(VCTConfigurationUtil.getHivTestDateConceptId()), false);

			if (person.getBirthdate().before(adult.getTime())) {
				adult.add(Calendar.YEAR, StringUtils.isNotBlank(adultAge) ? -Integer.parseInt(adultAge) : -16);
				resetTimes(adult);
				String hivTestDate = testDate != null ? sdf.format(testDate) : (savedTestDate != null && !savedTestDate.isEmpty() ? Context.getDateFormat().format(savedTestDate.get(0).getValueDatetime()) : "");

				if (savedTestDate != null && !savedTestDate.isEmpty() && testDate != null) {
					hivTestDate = savedTestDate != null && !savedTestDate.isEmpty()
							? Context.getDateFormat().format(savedTestDate.get(0).getValueDatetime())
							: "" + testDate != null ? " (" + sdf.format(testDate) + ")" : "";
				}
				if (matchTestEnrollmentArtInitAndReturnVisitDates(
						savedTestDate != null && !savedTestDate.isEmpty()
								&& savedTestDate.get(0).getValueDatetime() != null
										? savedTestDate.get(0).getValueDatetime() : testDate,
						hivEnrollmentDate, artInitDate,
						returnVisitDate != null && returnVisitDate.size() > 0
								&& returnVisitDate.get(0).getValueDatetime() != null
										? returnVisitDate.get(0).getValueDatetime() : null,
						datesToMatch, startDate, endDate)) {
					SphClientOrPatient c = new SphClientOrPatient();

					if (p instanceof VCTClient) {
						c.setType(SphClientOrPatient.SphClientOrPatientType.CLIENT.name());
						c.setRegistrationDate(((VCTClient) p).getDateOfRegistration() + " ("
								+ sdf.format(person.getDateCreated()) + ")");
					} else {
						c.setType(SphClientOrPatient.SphClientOrPatientType.PATIENT.name());
						c.setRegistrationDate(sdf.format(person.getDateCreated()));
						patient = Context.getPatientService().getPatient(patient.getPatientId());
					}
					patient = patient == null ? new Patient(person) : patient;

					List<String> patientAlerts = new CDCRulesAlgorithm().cdcDsRulesAlerts(patient);

					c.setAlerts(patientAlerts);
					if ((c.getAlerts() != null && !c.getAlerts().isEmpty() && matchAlerts(alerts, patientAlerts))
							|| SphClientOrPatient.SphClientOrPatientType.CLIENT.name().equals(c.getType())) {
						c.setAddress(person.getPersonAddress() != null ? getFormattedAddress(person.getPersonAddress())
								: "");
						c.setBirthDate(person.getBirthdate() != null ? sdf.format(person.getBirthdate()) : "");
						c.setId(person.getPersonId());
						if (tracnetIdType != null && patient.getPatientIdentifier(tracnetIdType) != null) {
							c.setTracnetId(patient.getPatientIdentifier(tracnetIdType).getIdentifier());
						}
						c.setDateTestedForHIV(hivTestDate);
						c.setName(person.getPersonName() != null ? person.getPersonName().getFullName() : "");
						c.setPeerEducator(
								person.getAttribute(peerEduc) != null ? person.getAttribute(peerEduc).getValue() : "");
						c.setPeerEducatorTelephone(person.getAttribute(peerEducTel) != null
								? person.getAttribute(peerEducTel).getValue() : "");
						c.setContactPerson(person.getAttribute(contactPerson) != null
								? person.getAttribute(contactPerson).getValue() : "");
						c.setContactPersonTelephone(person.getAttribute(contactPersonTel) != null
								? person.getAttribute(contactPersonTel).getValue() : "");
						c.setSex(person.getGender());
						c.setTelephone(person.getAttribute(tel) != null ? person.getAttribute(tel).getValue() : "");
						c.setHivEnrollmentDate(hivEnrollmentDate != null ? sdf.format(hivEnrollmentDate) : "");
						c.setReturnVisitDate(returnVisitDate != null && returnVisitDate.size() > 0
								&& returnVisitDate.get(0).getValueDatetime() != null
										? sdf.format(returnVisitDate.get(0).getValueDatetime()) : "");
						c.setArtInitiationDate(artInitDate != null ? sdf.format(artInitDate) : "");
						c.setCurrentOrLastRegimen(getCurrentRegimen(Context.getService(MoHOrderEntryBridgeService.class)
								.getMoHDrugOrdersByPatient(patient)));

						return c;
					}
				}
			}
		}
		return null;
	}

	private boolean matchAlerts(String[] alertsToMatch, List<String> patientAlerts) {
		if (alertsToMatch != null && patientAlerts != null && alertsToMatch.length > 0 && patientAlerts.size() > 0) {
			List<String> localizedSelectedAlerts = new ArrayList<String>();

			for (String a : alertsToMatch) {
				localizedSelectedAlerts
						.add(Context.getMessageSourceService().getMessage("rwandasphstudyreports.alerts." + a));
			}
			if (!patientAlerts.containsAll(localizedSelectedAlerts))
				return false;
		}
		return true;
	}

	@Override
	public boolean matchTestEnrollmentArtInitAndReturnVisitDates(Date testDate, Date hivEnrollmentDate,
			Date artInitDate, Date returnVisitDate, String[] datesToMatch, Date startDate, Date endDate) {
		boolean matched = false;

		if (startDate != null && endDate != null) {
			if (datesToMatch != null && ArrayUtils.isNotEmpty(datesToMatch)) {
				for (int i = 0; i < datesToMatch.length; i++) {
					String d = datesToMatch[i];

					if (StringUtils.isNotBlank(d)) {
						if ((d.equals("test")
								&& (testDate != null && (testDate.equals(startDate) || testDate.after(startDate))
										&& (testDate.equals(endDate) || testDate.before(endDate))))
								|| (d.equals("enrollment") && (hivEnrollmentDate != null
										&& (hivEnrollmentDate.equals(startDate) || hivEnrollmentDate.after(startDate))
										&& (hivEnrollmentDate.equals(endDate) || hivEnrollmentDate.before(endDate))))
								|| (d.equals("initiation") && (artInitDate != null
										&& (artInitDate.equals(startDate) || artInitDate.after(startDate))
										&& (artInitDate.equals(endDate) || artInitDate.before(endDate))))
								|| (d.equals("returnVisit") && (returnVisitDate != null
										&& (returnVisitDate.equals(startDate) || returnVisitDate.after(startDate))
										&& (returnVisitDate.equals(endDate) || returnVisitDate.before(endDate))))) {
							matched = matched || i == 0 ? true : false;
						} else
							matched = false;
					} else {
						matched = false;
					}
				}
			} else
				matched = true;
		}
		return matched;
	}

	private String getFormattedAddress(PersonAddress pa) {
		return pa.getAddress1() + ", " + pa.getCityVillage() + ", " + pa.getStateProvince() + ", " + pa.getCountry();
	}

	private Date getArtInitiationDate(Person p) {
		DrugOrder artInit = getARTInitiationDrug(p);

		return artInit != null ? (artInit.getEffectiveStartDate() != null ? artInit.getEffectiveStartDate()
				: artInit.getScheduledDate()) : null;
	}

	private Date checkIfPersonIsEnrolledInHIVProgram(Person person) {
		String hivProg = Context.getAdministrationService().getGlobalProperty("reports.adulthivprogramname");
		Program program = getProgramWorkflowService()
				.getProgramByName(StringUtils.isNotBlank(hivProg) ? hivProg : "HIV Program");
		List<PatientProgram> pp = getProgramWorkflowService().getPatientPrograms(new Patient(person), program, null,
				null, null, null, false);

		if (!pp.isEmpty())
			return pp.get(0).getDateEnrolled();
		return null;
	}

	/*
	 * return date when HIV was tested
	 */
	private Date checkIfPersonIsHIVPositive(Person person) {
		String hivConcept = Context.getAdministrationService().getGlobalProperty("reports.hivRapidTestConceptId");
		String hivPositiveConcept = Context.getAdministrationService()
				.getGlobalProperty("rwandasphstudyreports.hivPositiveConceptId");

		for (Obs o : Context.getObsService().getObservationsByPersonAndConcept(person,
				StringUtils.isNotBlank(hivConcept)
						? Context.getConceptService().getConcept(Integer.parseInt(hivConcept))
						: Context.getConceptService().getConcept(2169))) {
			if (o.getValueCoded() != null && o.getValueCoded().getConceptId()
					.equals(StringUtils.isNotBlank(hivPositiveConcept) ? Integer.parseInt(hivPositiveConcept) : 703))
				return o.getObsDatetime() != null ? o.getObsDatetime() : o.getDateCreated();
		}

		return null;
	}

	@Override
	public String getCurrentRegimen(List<MoHDrugOrder> orders) {
		List<String> o = new ArrayList<String>();
		Date today = Calendar.getInstance(Context.getLocale()).getTime();

		Collections.sort(orders, new Comparator<MoHDrugOrder>() {
			public int compare(MoHDrugOrder o1, MoHDrugOrder o2) {
				return o1.getDrugOrder().getDateCreated().compareTo(o2.getDrugOrder().getDateCreated());
			}
		});
		for (MoHDrugOrder ord : orders) {
			if (ord.getIsActive() && ord.getStartDate().before(today)
					&& ((ord.getStopDate() != null && ord.getStopDate().after(today)) || ord.getStopDate() == null)) {
				if (ord.getDrugOrder() != null && ord.getDrugOrder().getDrug() != null)
					o.add(ord.getDrugOrder().getDrug().getDisplayName());
			}
		}

		return StringUtils.join(o, ", ");
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
	public boolean checkForAtleast50PercentDecreaseInCD4(Patient patient) {
		Concept cd4 = Context.getConceptService().getConcept(Integer.parseInt(
				Context.getAdministrationService().getGlobalProperty(GlobalPropertyConstants.CD4_COUNT_CONCEPTID)));

		List<Obs> cd4Obs = Context.getObsService().getLastNObservations(1, patient, cd4, false);
		Obs o = getMaximumObs(patient, cd4);

		if (o != null
				&& (((o.getValueNumeric() - cd4Obs.get(0).getValueNumeric()) * 100) / o.getValueNumeric()) >= 50) {
			return true;
		}

		return false;
	}

	private Obs getMaximumObs(Patient patient, Concept concept) {
		if (patient != null && concept != null) {
			Integer obsId = (Integer) sessionFactory.getCurrentSession()
					.createSQLQuery("select obs_id from obs where concept_id = " + concept.getConceptId()
							+ " and person_id = " + patient.getPersonId() + " order by value_numeric desc limit 1")
					.uniqueResult();
			return obsId != null ? Context.getObsService().getObs(obsId) : null;
		}
		return null;
	}
}