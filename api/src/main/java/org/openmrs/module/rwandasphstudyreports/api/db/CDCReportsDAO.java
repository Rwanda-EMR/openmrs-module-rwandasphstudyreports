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
import org.openmrs.module.rwandasphstudyreports.SphClientOrPatient;
import org.openmrs.module.rwandasphstudyreports.api.CDCReportsService;

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

	public List<SphClientOrPatient> getHIVPositiveClientsOrPatientsForConsultationSheet();

	public List<Patient> getPatientsInHIVProgram(Program program);
}