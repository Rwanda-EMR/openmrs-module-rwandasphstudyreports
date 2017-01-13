package org.openmrs.module.rwandasphstudyreports;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.mohorderentrybridge.api.MoHOrderEntryBridgeService;
import org.openmrs.module.rwandasphstudyreports.api.CDCReportsService;

public class CDCRulesAlgorithm {
	public List<String> cdcDsRulesAlerts(Patient patient) {
		List<String> alerts = new ArrayList<String>();
		Concept vl = Context.getConceptService().getConcept(Integer.parseInt(
				Context.getAdministrationService().getGlobalProperty(GlobalPropertyConstants.VIRAL_LOAD_CONCEPTID)));
		Concept cd4 = Context.getConceptService().getConcept(Integer.parseInt(
				Context.getAdministrationService().getGlobalProperty(GlobalPropertyConstants.CD4_COUNT_CONCEPTID)));

		List<Obs> vLObs = Context.getObsService().getObservationsByPersonAndConcept(patient, vl);
		List<Obs> cd4Obs = getOnlyObsWithDatetimeMoreThan2MonthsAfterStartingDate(
				Context.getObsService().getObservationsByPersonAndConcept(patient, cd4),
				getDateWhenARTWasInitiated(patient));

		sortObsListByObsDateTime(vLObs);

		if (!vLObs.isEmpty()) {
			Date vLDate = vLObs.get(vLObs.size() - 1).getObsDatetime();
			Calendar vLCalendar = Calendar.getInstance(Context.getLocale());
			Calendar activeVisitLastYearEndsAt = Calendar.getInstance(Context.getLocale());
			Calendar activeVisitLast6MonthsEndsAt = Calendar.getInstance(Context.getLocale());

			if (vLDate != null) {
				vLCalendar.setTime(vLDate);
				activeVisitLastYearEndsAt.setTime(getCurrentVisitEndDate(patient));

				activeVisitLastYearEndsAt.add(Calendar.YEAR, -1);
				activeVisitLast6MonthsEndsAt.add(Calendar.MONTH, -6);
				if (vLCalendar.before(activeVisitLastYearEndsAt)) {
					alerts.add(
							Context.getMessageSourceService().getMessage("rwandasphstudyreports.alerts.orderRepeatVL"));
				}

			}
		}
		if (cd4Obs.isEmpty() && Context.getService(CDCReportsService.class).checkIfPatientIsHIVPositive(patient)) {
			alerts.add(Context.getMessageSourceService().getMessage("rwandasphstudyreports.alerts.orderBaselineCD4"));
		}

		return alerts;
	}

	private Date getCurrentVisitEndDate(Patient patient) {
		Visit activeVisit = Context.getService(CDCReportsService.class).getActiveVisit(patient, null);

		return activeVisit != null && activeVisit.getStopDatetime().before(new Date()) ? activeVisit.getStopDatetime()
				: new Date();
	}

	private void sortObsListByObsDateTime(List<Obs> obsList) {
		Collections.sort(obsList, new Comparator<Obs>() {
			public int compare(Obs o1, Obs o2) {
				return o1.getObsDatetime().compareTo(o2.getObsDatetime());
			}
		});
	}

	private void sortOrderListByStartDate(List<DrugOrder> arvDrugsOrders) {
		Collections.sort(arvDrugsOrders, new Comparator<DrugOrder>() {
			public int compare(DrugOrder o1, DrugOrder o2) {
				return o1.getEffectiveStartDate().compareTo(o2.getEffectiveStartDate());
			}
		});
	}

	private List<Obs> getOnlyObsWithDatetimeMoreThan2MonthsAfterStartingDate(List<Obs> obsList, Date startingDate) {
		List<Obs> matchedObs = new ArrayList<Obs>();

		if (obsList.size() > 0 && startingDate != null) {
			Calendar d = Calendar.getInstance(Context.getLocale());

			d.setTime(startingDate);
			d.add(Calendar.MONTH, 2);
			for (Obs o : obsList) {
				if (o.getObsDatetime().after(d.getTime())) {
					matchedObs.add(o);
				}
			}
		}
		return matchedObs;
	}

	private Date getDateWhenARTWasInitiated(Patient patient) {
		List<DrugOrder> arvDrugsOrders = new ArrayList<DrugOrder>();
		List<DrugOrder> drugOrders = Context.getService(MoHOrderEntryBridgeService.class)
				.getDrugOrdersByPatient(patient);

		String otherARVDrugConceptsIds = Context.getAdministrationService()
				.getGlobalProperty(GlobalPropertyConstants.OTHER_ARV_DRUGS_CONCEPTIDS);
		String aRVDrugConceptSetIds = Context.getAdministrationService()
				.getGlobalProperty(GlobalPropertyConstants.ARV_DRUGS_CONCEPTSETID);
		if (StringUtils.isNotBlank(otherARVDrugConceptsIds))
			for (String s : otherARVDrugConceptsIds.split(",")) {
				arvDrugsOrders.addAll(matchOnlyDrugConceptFromOrders(drugOrders,
						Context.getConceptService().getConcept(Integer.parseInt(s.trim()))));
			}
		if (StringUtils.isNotBlank(aRVDrugConceptSetIds)
				&& Context.getConceptService().getConcept(Integer.parseInt(aRVDrugConceptSetIds)).isSet())
			for (Concept c : Context.getConceptService().getConcept(Integer.parseInt(aRVDrugConceptSetIds))
					.getSetMembers()) {
				arvDrugsOrders.addAll(matchOnlyDrugConceptFromOrders(drugOrders, c));
			}
		sortOrderListByStartDate(arvDrugsOrders);

		if (!arvDrugsOrders.isEmpty())
			return arvDrugsOrders.get(0).getEffectiveStartDate();

		return null;
	}

	private List<DrugOrder> matchOnlyDrugConceptFromOrders(List<DrugOrder> dOrders, Concept c) {
		List<DrugOrder> orders = new ArrayList<DrugOrder>();

		for (DrugOrder o : dOrders) {
			if (c.getConceptId().equals(o.getConcept().getConceptId()))
				orders.add(o);
		}
		return orders;
	}
}
