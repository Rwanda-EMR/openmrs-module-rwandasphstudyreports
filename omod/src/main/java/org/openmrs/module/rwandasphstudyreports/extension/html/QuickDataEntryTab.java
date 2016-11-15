package org.openmrs.module.rwandasphstudyreports.extension.html;

import org.openmrs.module.web.extension.PatientDashboardTabExt;

public class QuickDataEntryTab extends PatientDashboardTabExt {

	@Override
	public String getPortletUrl() {
		return "quickDataEntry";
	}

	@Override
	public String getRequiredPrivilege() {
		return null;
	}

	@Override
	public String getTabId() {
		return "quickDataEntryTabId";
	}

	@Override
	public String getTabName() {
		return "Quick Data Entry";
	}

}
