package org.openmrs.module.rwandasphstudyreports.web.controller;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandasphstudyreports.CDCAlert;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertyConstants;
import org.openmrs.module.rwandasphstudyreports.api.CDCReportsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by k-joseph on 01/06/2017.
 */
@Controller
public class AdultConsultationSheetController {

    @RequestMapping(value = "/module/rwandasphstudyreports/adultConsultationSheet", method = RequestMethod.GET)
    public void get(ModelMap model) {
        GlobalProperty period = Context.getAdministrationService().getGlobalPropertyObject(GlobalPropertyConstants.MONTHS_ALLOWANCE_FOR_CONSULTATIONSHEET);
        Calendar startDate = Calendar.getInstance();
        Date endDate = new Date();

        if(period != null && StringUtils.isNotBlank(period.getPropertyValue()))
            startDate.add(Calendar.MONTH, - Integer.parseInt(period.getPropertyValue()));

        model.addAttribute("startDate", Context.getDateFormat().format(startDate.getTime()));
        model.addAttribute("endDate", Context.getDateFormat().format(endDate));
        model.addAttribute("clientsAndPatients", Context.getService(CDCReportsService.class).getHIVPositiveClientsOrPatientsForConsultationSheet(startDate.getTime(), endDate, null, null));
        model.addAttribute("testDateMatch", false);
        model.addAttribute("enrollmentDateMatch", false);
        model.addAttribute("initiationDateMatch", false);
        model.addAttribute("alerts", CDCAlert.getAllCDCAlerts());
        model.addAttribute("alertsString", "");
    }

    @RequestMapping(value = "/module/rwandasphstudyreports/adultConsultationSheet", method = RequestMethod.POST)
    public void post(ModelMap model, HttpServletRequest request) {
        String[] selectedDateMatches = request.getParameterValues("datesToMatch");
        String[] selectedAlerts = request.getParameterValues("alerts");

        model.addAttribute("startDate", request.getParameter("startDate"));
        model.addAttribute("endDate", request.getParameter("endDate"));
        model.addAttribute("clientsAndPatients", Context.getService(CDCReportsService.class).getHIVPositiveClientsOrPatientsForConsultationSheet(extractDate(request.getParameter("startDate"), Context.getDateFormat()), extractDate(request.getParameter("endDate"), Context.getDateFormat()), selectedDateMatches, selectedAlerts));
        model.addAttribute("testDateMatch", selectedDateMatches != null && ArrayUtils.contains(selectedDateMatches, "test"));
        model.addAttribute("enrollmentDateMatch", selectedDateMatches != null && ArrayUtils.contains(selectedDateMatches, "enrollment"));
        model.addAttribute("initiationDateMatch", selectedDateMatches != null && ArrayUtils.contains(selectedDateMatches, "initiation"));
        model.addAttribute("alerts", CDCAlert.getAllCDCAlerts());
        model.addAttribute("alertsString", StringUtils.join(selectedAlerts, ", "));
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
