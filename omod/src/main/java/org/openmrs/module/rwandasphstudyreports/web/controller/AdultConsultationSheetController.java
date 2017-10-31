package org.openmrs.module.rwandasphstudyreports.web.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandasphstudyreports.CDCAlert;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertyConstants;
import org.openmrs.module.rwandasphstudyreports.SphClientOrPatient;
import org.openmrs.module.rwandasphstudyreports.api.CDCReportsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by k-joseph on 01/06/2017.
 */
@Controller
public class AdultConsultationSheetController {

	@RequestMapping(value = "/module/rwandasphstudyreports/adultConsultationSheet", method = RequestMethod.GET)
	public void get(ModelMap model) {
		GlobalProperty period = Context.getAdministrationService()
				.getGlobalPropertyObject(GlobalPropertyConstants.MONTHS_ALLOWANCE_FOR_CONSULTATIONSHEET);
		Calendar startDate = Calendar.getInstance();
		Date endDate = new Date();

		if (period != null && StringUtils.isNotBlank(period.getPropertyValue()))
			startDate.add(Calendar.MONTH, -Integer.parseInt(period.getPropertyValue()));

		modalInit(model, Context.getDateFormat().format(startDate.getTime()),
				Context.getDateFormat().format(endDate), Context.getService(CDCReportsService.class)
						.getHIVPositiveClientsOrPatientsForConsultationSheet(startDate.getTime(), endDate, new String[] {"enrollment"}, null),
				false, true, false, false, CDCAlert.getAllCDCAlerts(), "");
	}

	@RequestMapping(value = "/module/rwandasphstudyreports/adultConsultationSheet", method = RequestMethod.POST)
	public void post(ModelMap model, HttpServletRequest request) {
		String[] selectedDateMatches = request.getParameterValues("datesToMatch");
		String[] selectedAlerts = request.getParameterValues("alerts");

		modalInit(model, request.getParameter("startDate"), request.getParameter("endDate"),
				Context.getService(CDCReportsService.class).getHIVPositiveClientsOrPatientsForConsultationSheet(
						extractDate(request.getParameter("startDate"), Context.getDateFormat()),
						extractDate(request.getParameter("endDate"), Context.getDateFormat()), selectedDateMatches,
						selectedAlerts),
				selectedDateMatches != null && ArrayUtils.contains(selectedDateMatches, "test"),
				selectedDateMatches != null && ArrayUtils.contains(selectedDateMatches, "enrollment"),
				selectedDateMatches != null && ArrayUtils.contains(selectedDateMatches, "initiation"),
				selectedDateMatches != null && ArrayUtils.contains(selectedDateMatches, "returnVisit"),
				CDCAlert.getAllCDCAlerts(), StringUtils.join(selectedAlerts, ", "));
	}

	private void modalInit(ModelMap model, String startDate, String endDate,
			List<SphClientOrPatient> clientsAndPatients, boolean testDateMatch, boolean enrollmentDateMatch, boolean initiationDateMatch,
			boolean returnVisitDateMatch, List<CDCAlert> alerts, String alertsString) {
		model.addAttribute("startDate", startDate);
		model.addAttribute("endDate", endDate);
		model.addAttribute("clientsAndPatients", clientsAndPatients);
		model.addAttribute("testDateMatch", testDateMatch);
		model.addAttribute("enrollmentDateMatch", enrollmentDateMatch);
		model.addAttribute("initiationDateMatch", initiationDateMatch);
		model.addAttribute("alerts", alerts);
		model.addAttribute("alertsString", alertsString);
		model.addAttribute("returnVisitDateMatch", returnVisitDateMatch);
	}

	private Date extractDate(String dateStr, SimpleDateFormat sdf) {
		if (sdf != null && StringUtils.isNotBlank(dateStr)) {
			try {
				return sdf.parse(dateStr);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
