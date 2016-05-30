package org.openmrs.module.rwandasphstudyreports;

import java.util.List;

import org.openmrs.Program;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.report.definition.ReportDefinition;

public interface PatientSummaryManager {
		/**
		 * @return the uuid of the Report
		 */
		String getUuid();

		/**
		 * @return the key of the Report
		 */
		String getKey();

		/**
		 * @return the name of the Report
		 */
		String getName();

		/**
		 * @return the description of the Report
		 */
		String getDescription();

		/**
		 * @return the programs this patient summary is for
		 */
		List<Program> getRequiredPrograms();

		/**
		 * @return the parameters of the Report
		 */
		List<Parameter> getParameters();

		/**
		 * @return the privilege required to view or evaluate this report
		 */
		String getRequiredPrivilege();

		/**
		 * @return a ReportDefinition that may be persisted or run
		 */
		ReportDefinition constructReportDefinition();
}
