package org.openmrs.module.rwandasphstudyreports.extension.html;

import org.openmrs.module.web.extension.PatientDashboardTabExt;

public class PatientSummaryDashboardTab extends PatientDashboardTabExt {

	@Override
	public String getPortletUrl() {
		return "defaultPatientSummary";
	}

	@Override
	public String getRequiredPrivilege() {
		return null;
	}

	@Override
	public String getTabId() {
		return "defaultPatientSummaryTabID";
	}

	@Override
	public String getTabName() {
		return "Patient Summary";
	}

}
