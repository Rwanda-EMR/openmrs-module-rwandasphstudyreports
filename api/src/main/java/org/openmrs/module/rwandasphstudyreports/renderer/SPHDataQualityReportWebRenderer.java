package org.openmrs.module.rwandasphstudyreports.renderer;

import org.openmrs.annotation.Handler;
import org.openmrs.module.reporting.report.definition.ReportDefinition;

/**
 * Renderer for Data Quality report
 *
 */
@Handler
public class SPHDataQualityReportWebRenderer extends AbstractRwandaWebRenderer {

	@Override
    public String getLabel() {
    	return "DQ-Data Quality HIV/TB Report By Site";
    }

	@Override
	public String getLinkUrl(ReportDefinition arg0) {
		return "module/rwandasphstudyreports/renderDataQualityDataSet.form";
	}
	
	public String getDataSetNameToCheck() {
		return "DQ-Data Quality HIV/TB Report By Site Data Set";

	}

}