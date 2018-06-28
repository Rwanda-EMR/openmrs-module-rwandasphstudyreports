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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jettison.json.JSONArray;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.report.ReportRequest;
import org.openmrs.module.rwandasphstudyreports.QuickDataEntry;
import org.openmrs.module.rwandasphstudyreports.api.CDCReportsService;
import org.openmrs.module.rwandasphstudyreports.reports.BaseSPHReportConfig;
import org.openmrs.module.rwandasphstudyreports.sitepackages.SitePackageManager;
import org.openmrs.web.WebConstants;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

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
			//new SetupAdultLateVisitAndCD4Report().setup();
		}
	}

	private String redirectToReport(ReportRequest reportRequest) {
		String uuid = reportRequest != null ? reportRequest.getUuid() : "";

		return "redirect:/module/reporting/reports/reportHistoryOpen.form?uuid=" + uuid;
	}

	@RequestMapping(value = "module/rwandasphstudyreports/reDirectToOutStandingBaselineCD4Report", method = RequestMethod.GET)
	public String reDirectToOutStandingBaselineCD4Report() {
		if(SitePackageManager.currentSiteIsPackage2()) {
			ReportRequest reportRequest = Context.getService(CDCReportsService.class).executeAndGetReportRequest(BaseSPHReportConfig.OUTSTANDINGBASELINECD4REPORT);
			return redirectToReport(reportRequest);
		} else {
			return "redirect:/index.htm";
		}
	}
	
	@RequestMapping(value = "module/rwandasphstudyreports/reDirectToOutStandingBaselineVLReport", method = RequestMethod.GET)
	public String reDirectToOutStandingBaselineVLReport() {
		if(SitePackageManager.currentSiteIsPackage2()) {
			ReportRequest reportRequest = Context.getService(CDCReportsService.class).executeAndGetReportRequest(BaseSPHReportConfig.OUTSTANDINGBASELINEVLREPORT);
			return redirectToReport(reportRequest);
		} else {
			return "redirect:/index.htm";
		}
	}

	@RequestMapping(value = "module/rwandasphstudyreports/reDirectToVLBasedTreatmentFailureReport", method = RequestMethod.GET)
	public String reDirectToVLBasedTreatmentFailureReport() {
		if(SitePackageManager.currentSiteIsPackage3()) {
			ReportRequest reportRequest = Context.getService(CDCReportsService.class).executeAndGetVLBasedTreatmentFailureReportRequest();
			return redirectToReport(reportRequest);
		} else {
			return "redirect:/index.htm";
		}
	}

	@RequestMapping(value = "module/rwandasphstudyreports/reDirectToCD4BasedTreatmentFailureReport", method = RequestMethod.GET)
	public String reDirectToCD4BasedTreatmentFailureReport() {
		if(SitePackageManager.currentSiteIsPackage3()) {
			ReportRequest reportRequest = Context.getService(CDCReportsService.class).executeAndGetReportRequest(BaseSPHReportConfig.CD4BASEDTREATMENTFAILUREREPORT);
			return redirectToReport(reportRequest);
		} else {
			return "redirect:/index.htm";
		}
	}
	
	@RequestMapping(value = "module/rwandasphstudyreports/reDirectToPatientsOnARTWithNoClinicalVisitsInLast4MonthsReport", method = RequestMethod.GET)
	public String reDirectToPatientsOnARTWithNoClinicalVisitsInLast4MonthsReport() {
		ReportRequest reportRequest = Context.getService(CDCReportsService.class).executeAndGetReportRequest(BaseSPHReportConfig.PATIENTSONARTWITHNOCLINICALVISITSINLAST4MONTHSREPORT);
		return redirectToReport(reportRequest);
	}

	@RequestMapping(value = "module/rwandasphstudyreports/reDirectToPatientsNotInitiatedOnARTReport", method = RequestMethod.GET)
	public String reDirectToPatientsNotInitiatedOnARTReport() {
		ReportRequest reportRequest = Context.getService(CDCReportsService.class).executeAndGetReportRequest(BaseSPHReportConfig.PATIENTSNOTINITIATEDONART);
		return redirectToReport(reportRequest);
	}
	
	@RequestMapping(value = "module/rwandasphstudyreports/reDirectToHIVPositivePatientsDelayInLinkageToCareReport", method = RequestMethod.GET)
	public String reDirectToHIVPositivePatientsDelayInLinkageToCareReport() {
		ReportRequest reportRequest = Context.getService(CDCReportsService.class).executeAndGetReportRequest(BaseSPHReportConfig.HIVPOSITIVEPATIENTSDELAYINLINKAGETOCAREREPORT);
		return redirectToReport(reportRequest);
	}
	
	@RequestMapping(value = "/module/rwandasphstudyreports/portlets/quickDataEntry", method = RequestMethod.GET)
	public void quickDataEntry(ModelMap model) throws Exception {
		initialiseQuickDataEntries(model);
	}

	private void initialiseQuickDataEntries(ModelMap model) {
		List<QuickDataEntry> entries = new ArrayList<QuickDataEntry>();
		String otherQuickEntryIds = Context.getAdministrationService()
				.getGlobalProperty("reports.otherQuickDataEntryConceptIds");

		entries.add(new QuickDataEntry(Context.getConceptService().getConcept(
				Integer.parseInt(Context.getAdministrationService().getGlobalProperty("reports.cd4Concept")))));
		entries.add(new QuickDataEntry(Context.getConceptService().getConcept(
				Integer.parseInt(Context.getAdministrationService().getGlobalProperty("reports.viralLoadConcept")))));
		entries.add(new QuickDataEntry(Context.getConceptService().getConcept(Integer
				.parseInt(Context.getAdministrationService().getGlobalProperty("reports.hivRapidTestConceptId")))));
		if (StringUtils.isNotBlank(otherQuickEntryIds)) {
			String[] ids = otherQuickEntryIds.split(",");

			for (int i = 0; i < ids.length; i++) {
				String id = ids[i].replaceAll("\\s", "");

				if (StringUtils.isNotBlank(id))
					entries.add(new QuickDataEntry(Context.getConceptService().getConcept(Integer.parseInt(id))));
			}
		}

		model.put("locations", Context.getLocationService().getAllLocations(false));
		model.put("providers", Context.getProviderService().getAllProviders(false));
		model.put("entries", entries);
	}

	@RequestMapping(value = "/module/rwandasphstudyreports/portlets/quickDataEntry", method = RequestMethod.POST)
	public void quickDataEntry(ModelMap model, HttpServletRequest request, HttpServletResponse response,
			@RequestParam(value = "entries", required = false) String entries,
			@RequestParam(required = true, value = "patientId") Integer patientId) throws Exception {
		Patient patient = Context.getPatientService().getPatient(patientId);
		JSONArray quickEntries = new JSONArray(entries);

		for (int i = 0; i < quickEntries.length(); i++) {
			QuickDataEntry entry = new QuickDataEntry(quickEntries.getJSONObject(i));
			// TODO pass real encounter and probably attach to a visit,
			// initially have created a quick data entry form attached to it
			Encounter encounter = null;
			Context.getService(CDCReportsService.class).saveQuickDataEntry(entry, patient, encounter);
		}
		initialiseQuickDataEntries(model);
		request.getSession().setAttribute(WebConstants.OPENMRS_MSG_ATTR, "Successfully saved quick data entry results");
	}
}
