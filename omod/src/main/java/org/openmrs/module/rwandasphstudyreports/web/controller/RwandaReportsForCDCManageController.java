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
package org.openmrs.module.rwandasphstudyreports.web.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandasphstudyreports.QuickDataEntry;
import org.openmrs.module.rwandasphstudyreports.SetupAdultHIVConsultationSheet;
import org.openmrs.module.rwandasphstudyreports.SetupAdultLateVisitAndCD4Report;
import org.openmrs.module.rwandasphstudyreports.api.CDCReportsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * The main controller.
 */
@Controller
public class RwandaReportsForCDCManageController {

	protected final Log log = LogFactory.getLog(getClass());

	@RequestMapping(value = "/module/rwandasphstudyreports/executeReports", method = RequestMethod.GET)
	public void executeReports(ModelMap model) throws Exception {
	}

	@RequestMapping(value = "/module/rwandasphstudyreports/executeReports", method = RequestMethod.POST)
	public void executeReports(HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (request.getParameter("formAction").equals("aRTMonthly")) {
			new SetupAdultLateVisitAndCD4Report().setup();
		} else if (request.getParameter("formAction").equals("consultSheetSetup")) {
			new SetupAdultHIVConsultationSheet().setup();
		} else if (request.getParameter("formAction").equals("indicatorReport")) {
			// new SetupIDProgramQuarterlyIndicatorReport().setup();
		} else if (request.getParameter("formAction").equals("dataQualityReport")) {
			// new SetupDataQualityIndicatorReport().setup();
		} else if (request.getParameter("formAction").equals("lostToFollowPatiensReport")) {
			// new SetupLostToFollowupPatients().setup();
		}
	}

	@RequestMapping(value = "module/rwandasphstudyreports/reDirectToAdultFollowupReport", method = RequestMethod.GET)
	public String reDirectToAdultFollowupReport() {
		return "redirect:/module/reporting/reports/viewReport.form?uuid="
				+ Context.getService(CDCReportsService.class).executeAndGetAdultFollowUpReportRequestUuid() + "#tabs-2";
	}

	@RequestMapping(value = "/module/rwandasphstudyreports/portlets/quickDataEntry", method = RequestMethod.GET)
	public void quickDataEntry(ModelMap model) throws Exception {
		List<QuickDataEntry> entries = new ArrayList<QuickDataEntry>();

		entries.add(new QuickDataEntry(Context.getConceptService().getConcept(
				Integer.parseInt(Context.getAdministrationService().getGlobalProperty("reports.cd4Concept")))));
		entries.add(new QuickDataEntry(Context.getConceptService().getConcept(
				Integer.parseInt(Context.getAdministrationService().getGlobalProperty("reports.viralLoadConcept")))));
		entries.add(new QuickDataEntry(Context.getConceptService().getConcept(Integer
				.parseInt(Context.getAdministrationService().getGlobalProperty("reports.hivRapidTestConceptId")))));
		model.put("locations", Context.getLocationService().getAllLocations(false));
		model.put("providers", Context.getProviderService().getAllProviders(false));
		model.put("entries", entries);
	}

	@RequestMapping(value = "/module/rwandasphstudyreports/portlets/quickDataEntry", method = RequestMethod.POST)
	public void quickDataEntry(HttpServletRequest request, HttpServletResponse response) throws Exception {

	}
}
