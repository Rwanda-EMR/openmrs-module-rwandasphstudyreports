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
package org.openmrs.module.rwandasphstudyreports.api.db;

import org.openmrs.*;
import org.openmrs.module.mohorderentrybridge.MoHDrugOrder;
import org.openmrs.module.rwandasphstudyreports.SphClientOrPatient;
import org.openmrs.module.rwandasphstudyreports.api.CDCReportsService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Database methods for {@link CDCReportsService}.
 */
public interface CDCReportsDAO {

	/*
	 * Add DAO methods here
	 */

	public Cohort getAllRwandaAdultsPatients();

	DrugOrder getARTInitiationDrug(Person patient);

	public List<Patient> getHIVPositivePatientsOnARVTreatment();

	public List<SphClientOrPatient> getHIVPositiveClientsOrPatientsForConsultationSheet(Date startDate, Date endDate, String[] datesToMatch);

	public List<Patient> getPatientsInHIVProgram(Program program, Date starDate, Date endDate);

	public boolean matchTestEnrollmentAndArtInitDates(Date testDate, Date hivEnrollmentDate, Date artInitDate, String[] datesToMatch, Date startDate, Date endDate);

	public String getCurrentRegimen(List<MoHDrugOrder> orders);
	
	public List<DrugOrder> matchOnlyDrugConceptFromOrders(List<DrugOrder> dOrders, Concept c);

	boolean checkForAtleast50PercentDecreaseInCD4(Patient patient);
}