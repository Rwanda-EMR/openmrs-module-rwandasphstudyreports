package org.openmrs.module.rwandasphstudyreports;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.ModuleMustStartException;
import org.openmrs.util.DatabaseUpdateException;
import org.openmrs.util.InputRequiredException;
import org.openmrs.util.OpenmrsUtil;

public class OpenMRSCoreDevelopment {
	public static void main(String[] args) {
		File propsFile = new File(OpenmrsUtil.getApplicationDataDirectory(), "openmrs-runtime.properties");
		Properties props = new Properties();
		OpenmrsUtil.loadProperties(props, propsFile);
		try {
			Context.startup("jdbc:mysql://localhost:3306/openmrs_up?autoReconnect=true", "root", "jesus", props);
			Context.openSession();
			Context.authenticate("admin", "Admin123");
			List<Patient> patients = Context.getPatientService().getPatients("John");
			for (Patient patient : patients) {
				System.out.println(
						"Found patient with name " + patient.getPersonName() + " and uuid: " + patient.getUuid());
			}
		} catch (ModuleMustStartException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DatabaseUpdateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InputRequiredException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			Context.closeSession();
		}
	}
}