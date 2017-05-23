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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.definition.service.ReportDefinitionService;
import org.openmrs.module.rwandasphstudyreports.reports.BaseSPHReportConfig;
import org.openmrs.module.rwandasphstudyreports.reports.PatientsWithNoVLAfter8Months;
import org.openmrs.test.BaseModuleContextSensitiveTest;


/**
 * Tests {@link ${CDCReportsService}}.
 */
public class CDCReportsServiceTest extends BaseModuleContextSensitiveTest {
	
	CDCReportsService service;

	@Before
	public void setup() {
		try {
			service = Context.getService(CDCReportsService.class);
			executeDataSet("RwandaSPHStudyReportsDataset.xml");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void patientsWithNoVLAfter8Months_Report_Test() {
		Assert.assertNotNull(Context.getService(ReportDefinitionService.class));
		
		PatientsWithNoVLAfter8Months report = new PatientsWithNoVLAfter8Months();
		try {
			report.setup();
			ReportDefinition rep1 = Context.getService(ReportDefinitionService.class).getDefinitionByUuid(BaseSPHReportConfig.PATIENTSWITHNOVLAFTER8MONTHS);
			
			Assert.assertNotNull(rep1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
