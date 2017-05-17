package org.openmrs.module.rwandasphstudyreports.reports;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.service.ReportService;
import org.openmrs.module.rwandasphstudyreports.Helper;

public class BaseSPHReportConfig {

	public void deleteReportDefinition(String name) {
		ReportService rs = Context.getService(ReportService.class);

		for (ReportDesign rd : rs.getAllReportDesigns(false)) {
			if (name.equals(rd.getName())) {
				rs.purgeReportDesign(rd);
			}
		}
		Helper.purgeReportDefinition(name);
	}

	/**
	 * @param rd
	 * @param name,
	 *            design and dataset name of the same name
	 * @param excellOutputFileName
	 * @throws IOException
	 */
	public void setupReport(ReportDefinition rd, String name, String excellOutputFileName) throws IOException {
		ReportDesign design = Helper.createRowPerPatientXlsOverviewReportDesign(rd, excellOutputFileName, name, null);
		Properties props = new Properties();

		props.put("repeatingSections", "sheet:1,row:6,dataset:" + name);
		props.put("sortWeight", "5000");
		design.setProperties(props);

		Helper.saveReportDesign(design);
	}

	public ReportDefinition createReportDefinition(String name) {
		ReportDefinition reportDefinition = new ReportDefinition();

		reportDefinition.setName(name);
		reportDefinition.addParameter(new Parameter("startDate", "Start Date", Date.class));
		reportDefinition.addParameter(new Parameter("endDate", "End Date", Date.class));
		/*reportDefinition.addParameter(new Parameter("location", "Health Center", Location.class));
		reportDefinition.setBaseCohortDefinition(Cohorts.createParameterizedLocationCohort("At Location"),
				ParameterizableUtil.createParameterMappings("location=${location}"));
*/
		return reportDefinition;
	}

}
