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
package org.openmrs.module.rwandasphstudyreports.api;

import java.util.List;

import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Program;
import org.openmrs.Visit;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.rwandasphstudyreports.QuickDataEntry;
import org.springframework.transaction.annotation.Transactional;

/**
 * This service exposes module's core functionality. It is a Spring managed bean
 * which is configured in moduleApplicationContext.xml.
 * <p>
 * It can be accessed only via Context:<br>
 * <code>
 * Context.getService(CDCReportsService.class).someMethod();
 * </code>
 * 
 * @see org.openmrs.api.context.Context
 */
@Transactional
public interface CDCReportsService extends OpenmrsService {

	/*
	 * Add service methods here
	 * 
	 */
	public Cohort getAllRwandaAdultsPatients();

	String executeAndGetPatientsWithNoVLAfter8MonthsReportRequest();

	Obs saveQuickDataEntry(QuickDataEntry entry, Patient patient, Encounter encounter);

	Visit getActiveVisit(Patient patient, String visitLocationUuid);

	boolean checkIfPatientIsHIVPositive(Patient patient);

	List<DrugOrder> matchOnlyDrugConceptFromOrders(List<DrugOrder> dOrders, Concept c);

	List<Visit> sortVisitsListByCreationDate(List<Visit> visits);

	void sortObsListByObsDateTime(List<Obs> obsList);

	void sortOrderListByStartDate(List<DrugOrder> arvDrugsOrders);

	List<Encounter> sortEncountersListByCreationDate(List<Encounter> encs);

	DrugOrder getARTInitiationDrug(Patient patient);

	boolean checkIfPatientIsOnARVMoreThanNMonths(Patient patient, Integer numberOfMonths);

	boolean checkForAtleast50PercentDecreaseInCD4(Patient patient);

	boolean checkIfPatientHasNoObsInLastNMonthsAfterProgramInit(Concept obsQuestion, Integer nMonths, Program program,
			Patient patient);
	String executeAndGetVLBasedTreatmentFailureReportRequest();

	boolean checkIfPatientListedAsBeingAViralLoadTreatmentFailureCase(Patient patient);
}