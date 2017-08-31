package org.openmrs.module.rwandasphstudyreports.extension.html;

import org.openmrs.module.web.extension.PatientDashboardTabExt;

public class ScreeningTab extends PatientDashboardTabExt {

	@Override
	public String getPortletUrl() {
		return "screening";
	}

	@Override
	public String getRequiredPrivilege() {
		return null;
	}

	@Override
	public String getTabId() {
		return "screeningTab";
	}

	@Override
	public String getTabName() {
		return "Screening TF";
	}
}
