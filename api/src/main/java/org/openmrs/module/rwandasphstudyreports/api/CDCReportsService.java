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

import java.util.Date;
import java.util.List;

import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Program;
import org.openmrs.Visit;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.mohorderentrybridge.MoHDrugOrder;
import org.openmrs.module.reporting.report.Report;
import org.openmrs.module.reporting.report.ReportRequest;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.rwandasphstudyreports.QuickDataEntry;
import org.openmrs.module.rwandasphstudyreports.SphClientOrPatient;
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

	ReportRequest executeAndGetPatientsWithNoVLAfter8MonthsReportRequest();

	Obs saveQuickDataEntry(QuickDataEntry entry, Patient patient, Encounter encounter);

	Visit getActiveVisit(Patient patient, String visitLocationUuid);

	boolean checkIfPatientIsHIVPositiveOrMissingResult(Patient patient);

	List<DrugOrder> matchOnlyDrugConceptFromOrders(List<DrugOrder> dOrders, Concept c);

	List<Visit> sortVisitsListByCreationDate(List<Visit> visits);

	void sortObsListByObsDateTime(List<Obs> obsList);

	void sortOrderListByStartDate(List<DrugOrder> arvDrugsOrders);

	List<Encounter> sortEncountersListByCreationDate(List<Encounter> encs);

	DrugOrder getARTInitiationDrug(Patient patient);

	boolean checkIfPatientIsOnARVMoreThanNMonths(Patient patient, Integer numberOfMonths);

	boolean checkForAtleast50PercentDecreaseInCD4(Patient patient);

	/**
	 *
	 * @param obsQuestion
	 * @param nMonths, defaults to 8
	 * @param program
	 * @param patient
	 * @return
	 */
	boolean checkIfPatientHasNoObsInLastNMonthsAfterProgramInit(Concept obsQuestion, Integer nMonths, Program program,
			Patient patient);
	ReportRequest executeAndGetVLBasedTreatmentFailureReportRequest();

	boolean checkIfPatientListedAsBeingAViralLoadTreatmentFailureCase(Patient patient);

	Obs createObs(Concept concept, Object value, Date datetime, String accessionNumber);

	void enrollPatientInProgram(Patient patient, Program program, Date enrollmentDate, Date completionDate);

	Report runReport(ReportDefinition reportDef, Date startDate, Date endDate, Location location);

	public List<Patient> getHIVPositivePatientsOnARVTreatment();

	public List<SphClientOrPatient> getHIVPositiveClientsOrPatientsForConsultationSheet(Date startDate, Date endDate, String[] datesToMatch);

	public Date getHIVEnrollmentDate(Patient patient);

	public boolean matchTestEnrollmentAndArtInitDates(Date testDate, Date hivEnrollmentDate, Date artInitDate, String[] datesToMatch, Date startDate, Date endDate);

	public String getCurrentRegimen(List<MoHDrugOrder> orders);

	boolean cd4BasedTreatmentFailure(Patient patient);

	boolean checkIfDateIsNMonthsFromNow(Date date, Integer nMonths);

	boolean vlBasedTreatmentFailure(Patient patient);

	String getVLTreatmentFailureAction(Patient patient);

	Obs saveNewObs(Concept concept, Object value, Date datetime, String accessionNumber, Patient patient);

	Obs saveVLBasedTreatmentFailure(Patient patient, String selectedACtionPoint);
	
	public boolean checkIfPatientIsExittedFromCare(Patient p);
}