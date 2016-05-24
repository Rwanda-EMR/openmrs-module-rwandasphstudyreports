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
package org.openmrs.module.rwandareportsforcdc.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandareportsforcdc.SetupAdultHIVConsultationSheet;
import org.openmrs.module.rwandareportsforcdc.SetupAdultLateVisitAndCD4Report;
import org.openmrs.module.rwandareportsforcdc.SetupIDProgramQuarterlyIndicatorReport;
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

	@RequestMapping(value = "/module/rwandareportsforcdc/manage", method = RequestMethod.GET)
	public void manage(ModelMap model) {
		model.addAttribute("user", Context.getAuthenticatedUser());
	}

	@RequestMapping(value = "/module/rwandareportsforcdc/executeReports", method = RequestMethod.GET)
	public void executeReports(ModelMap model) throws Exception {
	}
	
	@RequestMapping(value = "/module/rwandareportsforcdc/executeReports", method = RequestMethod.POST)
	public void executeReports(HttpServletRequest request, HttpServletResponse response) throws Exception {
		if(request.getParameter("formAction").equals("aRTMonthly")) {
			new SetupAdultLateVisitAndCD4Report().setup();
		} else if(request.getParameter("formAction").equals("consultSheetSetup")) {
			new SetupAdultHIVConsultationSheet().setup();
		} else if(request.getParameter("formAction").equals("indicatorReport")) {
			new SetupIDProgramQuarterlyIndicatorReport().setup();
		}
	}
}
