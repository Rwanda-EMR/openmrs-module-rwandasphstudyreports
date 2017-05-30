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

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.Program;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.dataset.DataSetRow;
import org.openmrs.module.reporting.report.Report;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.definition.service.ReportDefinitionService;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertiesManagement;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertyConstants;
import org.openmrs.module.rwandasphstudyreports.reports.BaseSPHReportConfig;
import org.openmrs.module.rwandasphstudyreports.reports.PatientsWithNoVLAfter8Months;
import org.openmrs.test.BaseModuleContextSensitiveTest;

/**
 * Tests {@link CDCReportsService}.
 */
public class CDCReportsServiceTest extends BaseModuleContextSensitiveTest {

	CDCReportsService service;

	ReportDefinitionService reportDefinitionService;

	GlobalPropertiesManagement gp;

	PatientService patientService;

	ObsService obsService;

	Concept cd4CountConcept;

	Concept viralLoadConcept;

	EncounterType adultFollowUpEncounterType;

	Concept hivStatusConcept;

	Concept reasonForExitingCareConcept;

	Concept transferOutConcept;

	Concept hivPositive;

	Program hivProgram;

	ProgramWorkflowService programService;

	UserService userService;

	@Before
	public void setup() {
		try {
			executeDataSet("RwandaSPHStudyReportsDataset.xml");

			gp = new GlobalPropertiesManagement();
			service = Context.getService(CDCReportsService.class);
			reportDefinitionService = Context.getService(ReportDefinitionService.class);
			patientService = Context.getPatientService();
			obsService = Context.getObsService();
			programService = Context.getProgramWorkflowService();
			userService = Context.getUserService();
			hivProgram = gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
			cd4CountConcept = gp.getConcept(GlobalPropertyConstants.CD4_COUNT_CONCEPTID);
			viralLoadConcept = gp.getConcept(GlobalPropertyConstants.VIRAL_LOAD_CONCEPTID);
			adultFollowUpEncounterType = gp.getEncounterType(GlobalPropertyConstants.ADULT_FOLLOWUP_ENCOUNTER_TYPEID);
			hivStatusConcept = gp.getConcept(GlobalPropertyConstants.HIV_STATUS_CONCEPTID);
			reasonForExitingCareConcept = gp.getConcept(GlobalPropertiesManagement.REASON_FOR_EXITING_CARE);
			transferOutConcept = gp.getConcept(GlobalPropertiesManagement.TRASNFERED_OUT);
			hivPositive = gp.getConcept(GlobalPropertyConstants.HIV_POSITIVE_CONCEPTID);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Ignore
	public void patientsWithNoVLAfter8Months_Report_Test() {
		/*
		 * adultPatientsCohort, hivPositive, noVL8MonthsAfterEnrollmentIntoHIV
		 */
		PatientsWithNoVLAfter8Months report1 = new PatientsWithNoVLAfter8Months();
		try {
			report1.setup();
			
			ReportDefinition rep1 = reportDefinitionService
					.getDefinitionByUuid(BaseSPHReportConfig.PATIENTSWITHNOVLAFTER8MONTHS);
			Report r11 = service.runReport(rep1, null, new Date(), null);
			Patient p432 = patientService.getPatient(432);
			Calendar age30 = Calendar.getInstance();
			Calendar hivEnrollment = Calendar.getInstance();
			Calendar hivPos = Calendar.getInstance();
			Calendar eightMonthsAfterHivEnrollment = Calendar.getInstance();
			DataSetRow row11 = r11.getReportData().getDataSets().get("PatientsWithNoVLAfter8Months").iterator().next();
			Collection<Integer> hivPatients = programService.patientsInProgram(hivProgram, null, null);

			Assert.assertNotNull(row11);

			for (Integer ip : hivPatients) {
				for (PatientProgram pp : programService.getPatientPrograms(patientService.getPatient(ip)))
					programService.purgePatientProgram(pp);
			}

			Report r12 = service.runReport(rep1, null, new Date(), null);
			DataSetRow row12 = r12.getReportData().getDataSets().get("PatientsWithNoVLAfter8Months").iterator().next();

			Assert.assertNotNull(rep1);
			Assert.assertNull(row12);

			hivPos.add(Calendar.MONTH, -12);
			hivEnrollment.add(Calendar.MONTH, -10);
			eightMonthsAfterHivEnrollment.setTime(hivEnrollment.getTime());
			eightMonthsAfterHivEnrollment.add(Calendar.MONTH, 8);
			age30.add(Calendar.YEAR, -30);
			p432.setBirthdate(age30.getTime());

			patientService.savePatient(p432);
			patientService.unvoidPatient(p432);

			Assert.assertNotNull(Context.getService(ReportDefinitionService.class));
			Assert.assertNotNull(p432);

			for (Obs o : obsService.getObservationsByPerson(p432))
				obsService.purgeObs(o);

			Obs hivPositiveObs = service.createObs(hivStatusConcept, hivPositive, hivPos.getTime(), null);

			obsService.saveObs(hivPositiveObs, null);
			service.enrollPatientInProgram(p432, hivProgram, hivEnrollment.getTime(), null);

			Report r13 = service.runReport(rep1, null, new Date(), null);
			DataSetRow row13 = r13.getReportData().getDataSets().get("PatientsWithNoVLAfter8Months").iterator().next();

			Assert.assertNotNull(row13);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void test_checkIfPatientHasNoObsInLastNMonthsAfterProgramInit() {
		Patient patient = patientService.getPatient(10432);

		Assert.assertTrue(service.checkIfPatientHasNoObsInLastNMonthsAfterProgramInit(viralLoadConcept, 8, hivProgram, patient));

		Calendar age30 = Calendar.getInstance();
		Calendar hivEnrollment = Calendar.getInstance();
		Calendar hivPos = Calendar.getInstance();
		Calendar eightMonthsAfterHivEnrollment = Calendar.getInstance();
		Collection<Integer> hivPatients = programService.patientsInProgram(hivProgram, null, null);

		for (Integer ip : hivPatients) {
			for (PatientProgram pp : programService.getPatientPrograms(patientService.getPatient(ip)))
				programService.purgePatientProgram(pp);
		}

		hivPos.add(Calendar.MONTH, -12);
		hivEnrollment.add(Calendar.MONTH, -10);
		eightMonthsAfterHivEnrollment.setTime(hivEnrollment.getTime());
		eightMonthsAfterHivEnrollment.add(Calendar.MONTH, 8);
		age30.add(Calendar.YEAR, -30);
		patient.setBirthdate(age30.getTime());
		for (Obs o : obsService.getObservationsByPerson(patient))
			obsService.purgeObs(o);

		Obs hivPositiveObs = service.createObs(hivStatusConcept, hivPositive, hivPos.getTime(), null);
		Obs vlObs = service.createObs(viralLoadConcept, 900.0, eightMonthsAfterHivEnrollment.getTime(), null);

		hivPositiveObs.setCreator(userService.getUser(1));
		hivPositiveObs.setPerson(patient);
		vlObs.setPerson(patient);
		vlObs.setCreator(userService.getUser(1));
		obsService.saveObs(hivPositiveObs, null);
		obsService.saveObs(vlObs, null);
		service.enrollPatientInProgram(patient, hivProgram, hivEnrollment.getTime(), null);

		patientService.unvoidPatient(patient);
		patientService.savePatient(patient);

		Assert.assertFalse(service.checkIfPatientHasNoObsInLastNMonthsAfterProgramInit(viralLoadConcept, 8, hivProgram, patient));
	}
}
