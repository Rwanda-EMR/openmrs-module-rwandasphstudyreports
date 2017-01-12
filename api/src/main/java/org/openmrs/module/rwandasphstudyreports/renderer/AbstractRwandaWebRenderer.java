package org.openmrs.module.rwandasphstudyreports.renderer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.renderer.RenderingMode;
import org.openmrs.module.reporting.web.renderers.AbstractWebReportRenderer;

/**
 * Renderer for Data Quality report
 *
 */
public abstract class AbstractRwandaWebRenderer extends AbstractWebReportRenderer {

	protected final Log log = LogFactory.getLog(getClass());

	/**
	 * This should contain the display name for the report output that the user
	 * will choose in the UI
	 */
	public abstract String getLabel();

	/**
	 * This should be set to the name of the dataset that a report must contain
	 * in order for this renderer to be enabled for this report.
	 */
	public abstract String getDataSetNameToCheck();

	@Override
	public boolean canRender(ReportDefinition reportDefinition) {
		return !getRenderingModes(reportDefinition).isEmpty();

	}

	/**
	 * @see org.openmrs.module.reporting.report.renderer.ReportRenderer#getRenderingModes(org.openmrs.module.reporting.report.definition.ReportDefinition)
	 */
	@Override
	public Collection<RenderingMode> getRenderingModes(ReportDefinition definition) {
		List<RenderingMode> ret = new ArrayList<RenderingMode>();
		for (Map.Entry<String, Mapped<? extends DataSetDefinition>> e : definition.getDataSetDefinitions().entrySet()) {
			String name = e.getKey();
			DataSetDefinition def = e.getValue().getParameterizable();
			if (getDataSetNameToCheck() != null && getDataSetNameToCheck().equals(def.getName())) {
				ret.add(new RenderingMode(this, this.getLabel(), name, Integer.MAX_VALUE - 5));
			}
		}
		return ret;
	}
}
