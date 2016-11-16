package org.openmrs.module.rwandasphstudyreports;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;

public class QuickDataEntry {

	private Concept test;

	private Date dateOfExam;

	private Provider provider;

	private Location location;

	private Double result;

	@SuppressWarnings("unused")
	private String testName;

	public String getTestName() {
		if (getTest() != null && getTest().getName() != null)
			return getTest().getName().getName();
		return null;
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
			String result = json.getString("result");
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

			setDateOfExam(sdf.parse(date));
			setTest(Context.getConceptService().getConcept(Integer.parseInt(conceptId)));
			setProvider(Context.getProviderService().getProviderByUuid(providerUuid));
			setLocation(Context.getLocationService().getLocationByUuid(locationUuid));
			setResult(Double.parseDouble(result));
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

	public Double getResult() {
		return result;
	}

	public void setResult(Double result) {
		this.result = result;
	}

}
