package org.openmrs.module.rwandasphstudyreports;

import java.util.Date;

import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Provider;

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
