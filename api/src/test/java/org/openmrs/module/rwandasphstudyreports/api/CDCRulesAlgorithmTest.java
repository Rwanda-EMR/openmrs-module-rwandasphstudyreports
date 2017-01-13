package org.openmrs.module.rwandasphstudyreports.api;

import java.util.Properties;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.rwandasphstudyreports.CDCRulesAlgorithm;
import org.openmrs.test.BaseModuleContextSensitiveTest;

@Ignore
public class CDCRulesAlgorithmTest extends BaseModuleContextSensitiveTest {

	@Override
	public Boolean useInMemoryDatabase() {
		return false;
	}

	/**
	 * @return MS Note: use port 3306 as standard, 5538 for sandbox 5.5 mysql
	 *         environment
	 */
	@Override
	public Properties getRuntimeProperties() {
		Properties p = super.getRuntimeProperties();
		p.setProperty("connection.url",
				"jdbc:mysql:mxj://127.0.0.1:3316/openmrs?autoReconnect=true&sessionVariables=storage_engine=InnoDB&useUnicode=true&characterEncoding=UTF-8"
						+ "&zeroDateTimeBehavior=convertToNull&server.initialize-user=true&createDatabaseIfNotExist=true&server.basedir=database&server.datadir=database/data&server.collation-server=utf8_general_ci"
						+ "&server.character-set-server=utf8&server.max_allowed_packet=96M&server.socket=/tmp/openmrs.sock");
		p.setProperty("connection.username", "openmrs");
		p.setProperty("connection.password", "2cn~oyZTqD^Y");
		p.setProperty("junit.username", "test");
		p.setProperty("junit.password", "Password123");
		return p;
	}

	@Before
	public void setupForTest() throws Exception {
		if (!Context.isSessionOpen()) {
			Context.openSession();
		}
		Context.clearSession();
		authenticate();
	}

	@Override
	public void deleteAllData() throws Exception {
	}

	@Test
	public void test_cdcDsRulesAlerts() {
		CDCRulesAlgorithm alg = new CDCRulesAlgorithm();
		Patient patient = Context.getPatientService().getPatient(7011);

		System.out.println(alg.cdcDsRulesAlerts(patient));
	}
}
