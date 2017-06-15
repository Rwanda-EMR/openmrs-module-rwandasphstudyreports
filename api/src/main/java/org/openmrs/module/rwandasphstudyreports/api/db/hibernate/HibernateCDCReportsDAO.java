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

import org.apache.commons.beanutils.converters.IntegerArrayConverter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.mohorderentrybridge.api.MoHOrderEntryBridgeService;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientProperty;
import org.openmrs.module.rwandasphstudyreports.CDCRulesAlgorithm;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertiesManagement;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertyConstants;
import org.openmrs.module.rwandasphstudyreports.SphClientOrPatient;
import org.openmrs.module.rwandasphstudyreports.api.CDCReportsService;
import org.openmrs.module.rwandasphstudyreports.api.db.CDCReportsDAO;
import org.openmrs.module.vcttrac.VCTClient;
import org.openmrs.module.vcttrac.service.VCTModuleService;

import java.util.*;

import static org.openmrs.api.context.Context.getProgramWorkflowService;

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
		List<DrugOrder> drugOrders = Context.getService(MoHOrderEntryBridgeService.class).getDrugOrdersByPatient(new Patient(person));

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
	public List<Patient> getHIVPositivePatientsOnARVTreatment() {
		String hivStatus = Context.getAdministrationService().getGlobalProperty(GlobalPropertyConstants.HIV_STATUS_CONCEPTID);
		String hivPositive = Context.getAdministrationService().getGlobalProperty(GlobalPropertyConstants.HIV_POSITIVE_CONCEPTID);
		List<Person> obsList = getSessionFactory().getCurrentSession().createCriteria(Obs.class)
				.add(Restrictions.eq("concept", StringUtils.isNotBlank(hivStatus) ? Context.getConceptService().getConcept(Integer.parseInt(hivStatus)) : Context.getConceptService().getConcept(2169)))
						.add(Restrictions.eq("valueCoded", StringUtils.isNotBlank(hivPositive) ? Context.getConceptService().getConcept(Integer.parseInt(hivPositive)) : Context.getConceptService().getConcept(703)))
				.setProjection(Projections.distinct(Projections.property("person"))).setFetchMode("Order", FetchMode.JOIN)
				.add(Restrictions.in("concept", getHIVDrugsConcepts())).list();

		List<Patient> pList = new ArrayList<Patient>();

		for(Person p : obsList)
			pList.add(new Patient(p));

		return pList;
	}

	private List<Concept> getHIVDrugsConcepts() {
		List<Concept> l = new ArrayList<Concept>();
		String arv = Context.getAdministrationService().getGlobalProperty(GlobalPropertyConstants.ARV_DRUGS_CONCEPTSETID);
		String arvs = Context.getAdministrationService().getGlobalProperty(GlobalPropertyConstants.ARV_CONCEPT_IDS);

		if(StringUtils.isNotBlank(arv))
			l.add(Context.getConceptService().getConcept(Integer.parseInt(arv.trim())));
		if(StringUtils.isNotBlank(arvs)) {
			for (String s : arvs.split(",")) {
				if(StringUtils.isNotBlank(s))
					l.add(Context.getConceptService().getConcept(Integer.parseInt(s.trim())));
			}
		}
		return l;
	}

	@Override
	public List<Patient> getPatientsInHIVProgram(Program program) {
		List<Patient> patients = new ArrayList<Patient>();
		String m = Context.getAdministrationService().getGlobalProperty(GlobalPropertyConstants.MONTHS_ALLOWANCE_TOENROLLMENT);
		Integer monthsAllowance = StringUtils.isNotBlank(m) ? Integer.parseInt(m) : 6;
		Calendar c = Calendar.getInstance();

		c.add(Calendar.MONTH, -monthsAllowance);
		for(PatientProgram p : Context.getProgramWorkflowService().getPatientPrograms(null, program, c.getTime(), null, null, null, false)) {
			if(!patients.contains(p.getPatient()))
				patients.add(p.getPatient());
		}

		return patients;
	}

	@Override
	public List<SphClientOrPatient> getHIVPositiveClientsOrPatientsForConsultationSheet() {
		List<VCTClient> clientList = Context.getService(VCTModuleService.class).getVCTClientsWaitingForHIVProgramEnrollment();
		List<Patient> patientList = getPatientsInHIVProgram(new GlobalPropertiesManagement().getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM));
		List<SphClientOrPatient> uiClients = new ArrayList<SphClientOrPatient>();
		String adultAge = Context.getAdministrationService().getGlobalProperty("reports.adultStartingAge");
		String clientIds = "";

		for(VCTClient c : clientList) {
			clientIds = convertAndAddClientOrPatient(uiClients, clientIds, c.getClient());
		}
		for(Patient p : patientList) {
			clientIds = convertAndAddClientOrPatient(uiClients, clientIds, p.getPerson());
		}

		return uiClients;
	}



	private String convertAndAddClientOrPatient(List<SphClientOrPatient> uiClients, String clientIds, Person p) {
		SphClientOrPatient client = convertPersonIntoSphClientOrPatient(p);
		if(client != null && clientIds.indexOf("," + client.getId()) < 0) {
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

	private SphClientOrPatient convertPersonIntoSphClientOrPatient(Person person) {
		String query = "SELECT COUNT(patient_id) from patient where patient_id = " + person.getPersonId();
		int numberOfClient = Integer.valueOf("" + getSessionFactory().getCurrentSession().createSQLQuery(query).uniqueResult());
		Date testDate = checkIfPersonIsHIVPositive(person);
		PersonAttributeType tel = Context.getPersonService().getPersonAttributeTypeByName("Phone Number");
		PersonAttributeType peerEduc = Context.getPersonService().getPersonAttributeTypeByName("Peer Educator's Name");
		PersonAttributeType peerEducTel = Context.getPersonService().getPersonAttributeTypeByName("Peer Educator's Phone Number");
		Date hivEnrollmentDate = checkIfPersonIsEnrolledInHIVProgram(person);
		Date artInitDate = getArtInitiationDate(person);
		String adultAge = Context.getAdministrationService().getGlobalProperty("reports.adultStartingAge");
		Calendar adult = Calendar.getInstance();
		adult.add(Calendar.YEAR, StringUtils.isNotBlank(adultAge) ? - Integer.parseInt(adultAge) : -16);

		resetTimes(adult);
		if (person != null && testDate != null && person.getBirthdate().before(adult.getTime())) {
			SphClientOrPatient c = new SphClientOrPatient();

			if(numberOfClient == 0)
				c.setType(SphClientOrPatient.SphClientOrPatientType.CLIENT.name());
			else
				c.setType(SphClientOrPatient.SphClientOrPatientType.PATIENT.name());
			c.setAddress(person.getPersonAddress() != null ? getFormattedAddress(person.getPersonAddress()) : "");
			c.setBirthDate(person.getBirthdate() != null ? c.sdf.format(person.getBirthdate()) : "");
			c.setId(person.getPersonId());
			c.setName(person.getPersonName() != null ? person.getPersonName().getFullName() : "");
			c.setDateTestedForHIV(c.sdf.format(testDate));
			c.setPeerEducator(person.getAttribute(peerEduc) != null ? person.getAttribute(peerEduc).getValue() : "");
			c.setPeerEducatorTelephone(person.getAttribute(peerEducTel) != null ? person.getAttribute(peerEducTel).getValue() : "");
			c.setSex(person.getGender());
			c.setTelephone(person.getAttribute(tel) != null ? person.getAttribute(tel).getValue() : "");
			c.setRegistrationDate(c.sdf.format(person.getDateCreated()));
			c.setHivEnrollmentDate(hivEnrollmentDate != null ? c.sdf.format(hivEnrollmentDate) : "");
			c.setDateTestedForHIV(c.sdf.format(testDate));
			c.setArtInitiationDate(artInitDate != null ? c.sdf.format(artInitDate) : "");
			c.setAlerts(new CDCRulesAlgorithm().cdcDsRulesAlerts(new Patient(person)));

			return c ;
		}
		return null;
	}

	private String getFormattedAddress(PersonAddress pa) {
		return pa.getAddress1() + ", " + pa.getCityVillage() + ", " + pa.getStateProvince() + ", " + pa.getCountry();
	}

	private Date getArtInitiationDate(Person p) {
		DrugOrder artInit = getARTInitiationDrug(p);

		return artInit != null ? (artInit.getEffectiveStartDate() != null ? artInit.getEffectiveStartDate() : artInit.getScheduledDate()) : null;
	}

	private Date checkIfPersonIsEnrolledInHIVProgram(Person person) {
		String hivProg = Context.getAdministrationService().getGlobalProperty("reports.adulthivprogramname");
		Program program = getProgramWorkflowService().getProgramByName(StringUtils.isNotBlank(hivProg) ? hivProg : "HIV Program");
		List<PatientProgram> pp = getProgramWorkflowService().getPatientPrograms(new Patient(person), program, null, null, null, null, false);

		if(!pp.isEmpty())
			return pp.get(0).getDateEnrolled();
		return null;
	}

	/*
     * return date when HIV was tested
     */
	private Date checkIfPersonIsHIVPositive(Person person) {
		String hivConcept = Context.getAdministrationService().getGlobalProperty("reports.hivRapidTestConceptId");
		String hivPositiveConcept = Context.getAdministrationService().getGlobalProperty("rwandasphstudyreports.hivPositiveConceptId");

		for(Obs o: Context.getObsService().getObservationsByPersonAndConcept(person, StringUtils.isNotBlank(hivConcept) ? Context.getConceptService().getConcept(Integer.parseInt(hivConcept)) : Context.getConceptService().getConcept(2169))) {
			if(o.getValueCoded() != null && o.getValueCoded().getConceptId().equals(StringUtils.isNotBlank(hivPositiveConcept) ? Integer.parseInt(hivPositiveConcept) : 703))
				return o.getObsDatetime() != null ? o.getObsDatetime() : o.getDateCreated();
		}

		return null;
	}
}