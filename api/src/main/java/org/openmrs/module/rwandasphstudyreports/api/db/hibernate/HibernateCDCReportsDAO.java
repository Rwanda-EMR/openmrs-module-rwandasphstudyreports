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
package org.openmrs.module.rwandasphstudyreports.api.db.hibernate;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.openmrs.Cohort;
import org.openmrs.module.rwandasphstudyreports.api.db.CDCReportsDAO;

/**
 * It is a default implementation of {@link CDCReportsDAO}.
 */
public class HibernateCDCReportsDAO implements CDCReportsDAO {
	protected final Log log = LogFactory.getLog(this.getClass());

	private SessionFactory sessionFactory;

	/**
	 * @param sessionFactory
	 *            the sessionFactory to set
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * @return the sessionFactory
	 */
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	@SuppressWarnings("unchecked")
	public Cohort getAllRwandaAdultsPatients() {

		Query query = sessionFactory.getCurrentSession().createQuery(
				"select distinct p.patient_id from patient p inner join person pp on pp.person_id = p.patient_id where p.voided=0 and pp.voided=0 and (pp.birthdate is null or (select DATEDIFF(NOW(), pp.birthdate) / 365.25) >= 16)");

		Set<Integer> ids = new HashSet<Integer>();
		ids.addAll(query.list());

		return new Cohort("All Rwanda Adults patients", "", ids);
	}
}