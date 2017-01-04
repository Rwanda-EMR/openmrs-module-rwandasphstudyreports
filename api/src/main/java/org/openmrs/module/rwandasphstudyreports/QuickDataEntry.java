package org.openmrs.module.rwandasphstudyreports;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.Location;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;

public class QuickDataEntry {

	private Concept test;

	private Date dateOfExam;

	private Provider provider;

	private Location location;

	private Object result;

	private String testType;

	private List<String> codedAnswers;

	private String testName;

	public String getTestName() {
		if (getTest() != null && getTest().getName() != null)
			return getTest().getName().getName();
		return testName;
	}

	public QuickDataEntry(Concept test) {
		this.test = test;
	}

	public QuickDataEntry(JSONObject json) {
		try {
			String conceptId = json.getString("conceptId");
			String date = json.getString("date");
			String providerUuid = json.getString("providerUuid");
			String locationUuid = json.getString("locationUuid");
			Object result = ("Boolean".equals(getTestType())) ? json.getBoolean("result") : json.getString("result");
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

			setDateOfExam(sdf.parse(date));
			setTest(Context.getConceptService().getConcept(Integer.parseInt(conceptId)));
			setProvider(Context.getProviderService().getProviderByUuid(providerUuid));
			setLocation(Context.getLocationService().getLocationByUuid(locationUuid));

			if ("Numeric".equals(getTestType()))
				setResult(Double.parseDouble((String) result));
			else if ("Date".equals(getTestType()) || "Datetime".equals(getTestType()))
				setResult(sdf.parse((String) result));
			else if ("Boolean".equals(getTestType()))
				setResult((Boolean) result);
			else if ("Text".equals(getTestType()))
				setResult(result);
			else if ("Coded".equals(getTestType())) {
				setResult(Context.getConceptService().getConcept(
						((String) result).substring(((String) result).indexOf("(") + 1, ((String) result).indexOf(")"))));
			}

		} catch (ParseException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public Concept getTest() {
		return test;
	}

	public void setTest(Concept test) {
		this.test = test;
	}

	public Date getDateOfExam() {
		return dateOfExam;
	}

	public void setDateOfExam(Date dateOfExam) {
		this.dateOfExam = dateOfExam;
	}

	public Provider getProvider() {
		return provider;
	}

	public void setProvider(Provider provider) {
		this.provider = provider;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	/*
	 * Can be one of these; Boolean ,Coded ,Complex ,Date ,Datetime ,Document
	 * ,N/A ,Numeric ,Rule ,Structured Numeric ,Text ,Time
	 */
	public String getTestType() {
		if (getTest() != null && getTest().getDatatype() != null)
			return getTest().getDatatype().getName();
		return testType;
	}

	public List<String> getCodedAnswers() {
		if (StringUtils.isNotBlank(getTestType()) && getTestType().equals("Coded")
				&& !getTest().getAnswers().isEmpty()) {
			List<String> answers = new ArrayList<String>();

			for (ConceptAnswer answer : getTest().getAnswers()) {
				answers.add(answer.getAnswerConcept().getName().getName() + " ("
						+ answer.getAnswerConcept().getConceptId() + ")");
			}

			return answers;
		}
		return codedAnswers;
	}

}
