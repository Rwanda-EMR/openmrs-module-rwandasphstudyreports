package org.openmrs.module.rwandasphstudyreports;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Obs;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.rowperpatientreports.patientdata.definition.CustomCalculation;
import org.openmrs.module.rowperpatientreports.patientdata.result.AllObservationValuesResult;
import org.openmrs.module.rowperpatientreports.patientdata.result.DateValueResult;
import org.openmrs.module.rowperpatientreports.patientdata.result.ObservationResult;
import org.openmrs.module.rowperpatientreports.patientdata.result.PatientAttributeResult;
import org.openmrs.module.rowperpatientreports.patientdata.result.PatientDataResult;

public class HIVAdultAlerts implements CustomCalculation {

	protected Log log = LogFactory.getLog(this.getClass());

	public PatientDataResult calculateResult(List<PatientDataResult> results, EvaluationContext context) {

		PatientAttributeResult alert = new PatientAttributeResult(null, null);

		//ProgramWorkflowState state = (ProgramWorkflowState) context.getParameterValue("state");

		StringBuffer alerts = new StringBuffer();

		double height = 0;
		double weight = 0;

		for (PatientDataResult result : results) {

			if (result.getName().equals("CD4Test")) {
				AllObservationValuesResult cd4 = (AllObservationValuesResult) result;

				if (cd4.getValue() != null) {
					int decline = calculateDecline(cd4.getValue());

					/*if (decline > 50
							&& (state.toString().contains("GROUP") || state.toString().contains("FOLLOWING"))) {
						alerts.append("CD4 decline(");
						alerts.append(decline);
						alerts.append(").\n");
					}*/

					Obs lastCd4 = null;

					if (cd4.getValue().size() > 0) {
						lastCd4 = cd4.getValue().get(cd4.getValue().size() - 1);
					}

					if (lastCd4 == null) {
						alerts.append("No CD4 recorded.\n");
					} else {
						Date dateCd4 = lastCd4.getObsDatetime();
						Date date = Calendar.getInstance().getTime();

						int diff = calculateMonthsDifference(date, dateCd4);

						if (diff > 12) {
							alerts.append("Very late CD4(" + diff + " months ago).\n");
						/*} else if ((diff > 6) && state.toString().contains("FOLLOWING")) {
							alerts.append("Late CD4(" + diff + " months ago).\n");
						}

						if (state.toString().contains("FOLLOWING") && lastCd4.getValueNumeric() != null
								&& lastCd4.getValueNumeric() < 500) {
							alerts.append("Eligible for Treatment.\n");
						}*/
					}
				}
			}
			if (result.getName().equals("viralLoadTest")) {
				AllObservationValuesResult viraload = (AllObservationValuesResult) result;

				if (viraload.getValue() != null) {
					Obs lastviraload = null;

					if (viraload.getValue().size() > 0) {
						lastviraload = viraload.getValue().get(viraload.getValue().size() - 1);
					}

					/*if (state.toString().contains("GROUP") && (lastviraload == null)) {
						alerts.append("No VL recorded.\n");
					} else {*/
						try {
							Date dateVl = lastviraload.getObsDatetime();
							Date date = Calendar.getInstance().getTime();

							int diff = calculateMonthsDifference(date, dateVl);

							/*if (state.toString().contains("GROUP")) {
								if (diff > 12) {
									alerts.append("Late VL(" + diff + " months ago).\n");
								}

								if (lastviraload.getValueNumeric() != null && lastviraload.getValueNumeric() > 1000) {
									alerts.append("VL Failure " + lastviraload.getValueNumeric() + ".\n");
								}
							}*/
						} catch (Exception e) {
						}
					}
				}
			}
			if (result.getName().equals("weightObs")) {
				AllObservationValuesResult wt = (AllObservationValuesResult) result;

				if (wt.getValue() != null) {
					int decline = calculatePercentageDecline(wt.getValue());

					if (decline > 5) {
						alerts.append("WT decline(");
						alerts.append(decline);
						alerts.append("%, ");
						int kilosLost = calculateDecline(wt.getValue());
						alerts.append(kilosLost);
						alerts.append("kg)\n");
					}

					if (wt.getValue().size() > 0) {
						weight = wt.getValue().get(wt.getValue().size() - 1).getValueNumeric();
					}
				}

				if (wt.getValue() == null || wt.getValue().size() == 0) {
					alerts.append("No weight recorded.\n");
				}
			}

			if (result.getName().equals("RecentHeight")) {
				ObservationResult heightOb = (ObservationResult) result;

				if (heightOb.getValue() == null || heightOb.getValue().trim().length() == 0) {
					alerts.append("No height recorded.\n");
				} else {
					height = Double.parseDouble(heightOb.getValue());

				}
			}

			if (result.getName().equals("lastEncInMonth")) {
				DateValueResult encinmonths = (DateValueResult) result;
				if (encinmonths.getValue() != null) {
					Date dateVl = encinmonths.getDateOfObservation();
					Date date = Calendar.getInstance().getTime();
					int diff = calculateMonthsDifference(date, dateVl);
					if (diff > 12) {
						alerts.append("LTFU determine status.\n");
					}
				}
			}

			if (result.getName().equals("IO") && result.getValue() != null) {
				alerts.append("OI reported last visit: " + result.getValue() + "\n");
			}

			if (result.getName().equals("SideEffects") && result.getValue() != null) {
				alerts.append("Side effects reported last visit: " + result.getValue() + "\n");
			}
		}

		if (height > 0 && weight > 0) {
			double bmi = weight / (height / 100 * height / 100);
			int decimalPlace = 1;
			BigDecimal bd = new BigDecimal(Double.toString(bmi));
			bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);

			if (bmi < 16) {
				alerts.append("Very low BMI (" + bd.doubleValue() + ").\n");
			} else if (bmi < 18.5) {
				alerts.append("Low BMI (" + bd.doubleValue() + ").\n");
			}

		}

		alert.setValue(alerts.toString().trim());
		return alert;
	}

	private int calculateMonthsDifference(Date observation, Date startingDate) {
		int diff = 0;

		Calendar obsDate = Calendar.getInstance();
		obsDate.setTime(observation);

		Calendar startDate = Calendar.getInstance();
		startDate.setTime(startingDate);

		// find out if there is any difference in years first
		diff = obsDate.get(Calendar.YEAR) - startDate.get(Calendar.YEAR);
		diff = diff * 12;

		int monthDiff = obsDate.get(Calendar.MONTH) - startDate.get(Calendar.MONTH);
		diff = diff + monthDiff;

		return diff;
	}

	private int calculateDecline(List<Obs> obs) {
		Obs lastOb = null;
		Obs nextToLastOb = null;

		if (obs.size() > 0) {
			lastOb = obs.get(obs.size() - 1);
		}

		if (obs.size() > 1) {
			nextToLastOb = obs.get(obs.size() - 2);
		}

		if (lastOb != null && nextToLastOb != null) {
			Double firstVal = lastOb.getValueNumeric();
			Double nextToLastVal = nextToLastOb.getValueNumeric();

			if (firstVal != null && nextToLastVal != null) {
				double decline = nextToLastVal - firstVal;

				if (decline > 0) {
					return (int) decline;
				}
			}
		}

		return 0;
	}

	private int calculatePercentageDecline(List<Obs> obs) {
		Obs lastOb = null;
		Obs nextToLastOb = null;

		if (obs.size() > 0) {
			lastOb = obs.get(obs.size() - 1);
		}

		if (obs.size() > 1) {
			nextToLastOb = obs.get(obs.size() - 2);
		}

		if (lastOb != null && nextToLastOb != null) {
			Double firstVal = lastOb.getValueNumeric();
			Double nextToLastVal = nextToLastOb.getValueNumeric();

			if (firstVal != null && nextToLastVal != null) {
				double decline = 100 - ((firstVal / nextToLastVal) * 100);

				if (decline > 0) {
					return (int) decline;
				}
			}
		}

		return 0;
	}
}
