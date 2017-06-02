package org.openmrs.module.rwandasphstudyreports.web.controller;

import org.openmrs.api.context.Context;
import org.openmrs.module.rwandasphstudyreports.SphClientOrPatient;
import org.openmrs.module.rwandasphstudyreports.api.CDCReportsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * Created by k-joseph on 01/06/2017.
 */
@Controller
public class AdultConsultationSheetController {

    @RequestMapping(value = "/module/rwandasphstudyreports/adultConsultationSheet", method = RequestMethod.GET)
    public void get(ModelMap model) {
        List<SphClientOrPatient> clientsAndPatients = Context.getService(CDCReportsService.class).getHIVPositiveClientsOrPatientsForConsultationSheet();

        model.addAttribute("clientsAndPatients", clientsAndPatients);
    }
}
