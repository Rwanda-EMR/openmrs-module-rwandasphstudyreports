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
package org.openmrs.module.rwandasphstudyreports;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class CDCAlert {

	private String name;

	private String localeCode;

	private String displayName;
	
	public String getLocaleCode() {
		return localeCode;
	}

	public String getDisplayName() {
		return displayName;
	}
	
	public String getName() {
		return name;
	}

	CDCAlert(CDCAlerts a) {
		this.name = a.name();
		this.localeCode = a.getLocaleCode();
		this.displayName = a.getDisplayName();
	}

	public enum CDCAlerts {
		orderBaselineCD4("rwandasphstudyreports.alerts.orderBaselineCD4", "Order Baseline CD4"),
		orderRepeatVL("rwandasphstudyreports.alerts.orderRepeatVL", "Order Annual Viral Load"),
		orderBaselineVL("rwandasphstudyreports.alerts.orderBaselineVL", "Order Baseline Viral Load"),
		cd4BasedTreatmentFailure("rwandasphstudyreports.alerts.cd4BasedTreatmentFailure", "CD4 Based Treatment Failure"),
		vlBasedTreatmentFailure("rwandasphstudyreports.alerts.vlBasedTreatmentFailure", "Viral Load Based Treatment Failure"),
		patientsWithNoVLAfter8Months("rwandasphstudyreports.alerts.patientsWithNoVLAfter8Months", "No Viral Load After 8months from Enrollment");

		private String localeCode;

		private String displayName;

		CDCAlerts(String localeCode, String displayName) {
			this.localeCode = localeCode;
			this.displayName = displayName;
		}

		public String getLocaleCode() {
			return localeCode;
		}

		public String getDisplayName() {
			return displayName;
		}
	}

	public static List<CDCAlert> getAllCDCAlerts() {
		List<CDCAlert> reps = new ArrayList<CDCAlert>();

		for (CDCAlerts r : new ArrayList<CDCAlerts>(EnumSet.allOf(CDCAlerts.class))) {
			reps.add(new CDCAlert(r));
		}

		return reps;
	}
}