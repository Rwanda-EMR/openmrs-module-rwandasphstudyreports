package org.openmrs.module.rwandasphstudyreports.web.controller;

import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertiesManagement;
import org.openmrs.module.rwandasphstudyreports.VLTreatmentFailureAction;
import org.openmrs.module.rwandasphstudyreports.api.CDCReportsService;
import org.openmrs.web.WebConstants;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ScreeningController {

	@RequestMapping(value = "/module/rwandasphstudyreports/portlets/screening", method = RequestMethod.GET)
	public void screening(ModelMap model, @RequestParam(required = true, value = "patientId") Integer patientId) {
		if (!Context.isAuthenticated() || patientId == null) {
			return;
		}
		Patient p = Context.getPatientService().getPatient(patientId);
		
		setUpModel(model, p);
	}
	
	@RequestMapping(value = "/module/rwandasphstudyreports/portlets/screening", method = RequestMethod.POST)
	public void saveScreening(ModelMap model, @RequestParam(required = true, value = "patientId") Integer patientId, HttpServletRequest request) {
		if (!Context.isAuthenticated() || patientId == null) {
			return;
		}
		Patient p = Context.getPatientService().getPatient(patientId);
		String selectedACtionPoint = request.getParameter("selectedClinicalAction");
		
		if (p != null && StringUtils.isNotBlank(selectedACtionPoint)) {
			Obs o = Context.getService(CDCReportsService.class).saveVLBasedTreatmentFailure(p, selectedACtionPoint);
			
			if(o != null)
			request.getSession().setAttribute(WebConstants.OPENMRS_MSG_ATTR,
					Context.getMessageSourceService().getMessage("rwandasphstudyreports.screening.success"));
		}
		setUpModel(model, p);
	}

	private void setUpModel(ModelMap model, Patient p) {
		if (!Context.getService(CDCReportsService.class).checkIfPatientIsExittedFromCare(p)) {
			if (p != null && Context.getService(CDCReportsService.class).vlBasedTreatmentFailure(p)) {
				model.addAttribute("vlTreatmentFailure", true);
				model.addAttribute("msg",
						Context.getMessageSourceService().getMessage("rwandasphstudyreports.screening.yes"));
				model.addAttribute("clinicalActions", VLTreatmentFailureAction.getAllVLTreatmentFailureActions());
				model.addAttribute("savedClinicalAction",
						Context.getService(CDCReportsService.class).getVLTreatmentFailureAction(p));
			} else {
				model.addAttribute("vlTreatmentFailure", false);
				model.addAttribute("msg",
						Context.getMessageSourceService().getMessage("rwandasphstudyreports.screening.no"));
			} 
		} else {
			model.addAttribute("vlTreatmentFailure", false);
			model.addAttribute("msg", "Patient exited care");
		}
	}
}
