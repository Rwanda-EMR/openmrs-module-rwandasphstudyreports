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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.ModuleActivator;
import org.openmrs.module.rwandasphstudyreports.reports.*;

/**
 * This class contains the logic that is run every time this module is either
 * started or stopped.
 */
public class RwandaSPHStudyReportsActivator implements ModuleActivator {

	protected Log log = LogFactory.getLog(getClass());

	/**
	 * @see ModuleActivator#willRefreshContext()
	 */
	public void willRefreshContext() {
		log.info("Refreshing Rwanda Reports For CDC Module");
	}

	/**
	 * @see ModuleActivator#contextRefreshed()
	 */
	public void contextRefreshed() {
		log.info("Rwanda Reports For CDC Module refreshed");
	}

	/**
	 * @see ModuleActivator#willStart()
	 */
	public void willStart() {
		log.info("Starting Rwanda Reports For CDC Module");
	}

	/**
	 * @see ModuleActivator#started()
	 */
	public void started() {
		log.info("Rwanda Reports For CDC Module started");
		try {
			if(!"true".equalsIgnoreCase(Context.getAdministrationService().getGlobalProperty(GlobalPropertyConstants.DISABLE_REPORTS))) {
				new HIVPositivePatientsDelayInLinkageToCareReport().setup();
				new PatientsOnARTWithNoClinicalVisitsInLast4MonthsReport().setup();
				new PatientsNotInitiatedOnART().setup();
				new OutStandingBaselineVLReport().setup();
				new OutStandingBaselineCD4Report().setup();
				new VLBasedTreatmentFailureReport().setup();
				new CD4BasedTreatmentFailureReport().setup();
				new PatientsWithNoVLAfter8Months().setup();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see ModuleActivator#willStop()
	 */
	public void willStop() {
		log.info("Stopping Rwanda Reports For CDC Module");
	}

	/**
	 * @see ModuleActivator#stopped()
	 */
	public void stopped() {
		log.info("Rwanda Reports For CDC Module stopped");
	}

}
