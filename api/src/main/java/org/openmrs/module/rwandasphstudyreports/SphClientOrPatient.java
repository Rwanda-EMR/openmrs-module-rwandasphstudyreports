package org.openmrs.module.rwandasphstudyreports;

import java.util.List;

/**
 * Created by k-joseph on 01/06/2017.
 */
public class SphClientOrPatient {
    private Integer id;
    private String tracnetId;
    private String name;
    private String sex;
    private String birthDate;
    private String dateTestedForHIV;
    private Integer daysSinceHIVToDiagnosis;
    private String telephone;
    private String address;
    private String peerEducator;
    private String peerEducatorTelephone;
    private String contactPerson;
    private String contactPersonTelephone;
    private String hivEnrollmentDate;
    private String registrationDate;
    private String artInitiationDate;
    private List<String> alerts;
    private String type;
    private String currentOrLastRegimen;
    private String returnVisitDate;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTracnetId() {
        return tracnetId;
    }

    public void setTracnetId(String tracnetId) {
        this.tracnetId = tracnetId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getDateTestedForHIV() {
        return dateTestedForHIV;
    }

    public void setDateTestedForHIV(String dateTestedForHIV) {
        this.dateTestedForHIV = dateTestedForHIV;
    }

    public Integer getDaysSinceHIVToDiagnosis() {
        return daysSinceHIVToDiagnosis;
    }

    public void setDaysSinceHIVToDiagnosis(Integer daysSinceHIVToDiagnosis) {
        this.daysSinceHIVToDiagnosis = daysSinceHIVToDiagnosis;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPeerEducator() {
        return peerEducator;
    }

    public void setPeerEducator(String peerEducator) {
        this.peerEducator = peerEducator;
    }

    public String getPeerEducatorTelephone() {
        return peerEducatorTelephone;
    }

    public void setPeerEducatorTelephone(String peerEducatorTelephone) {
        this.peerEducatorTelephone = peerEducatorTelephone;
    }

    public String getHivEnrollmentDate() {
        return hivEnrollmentDate;
    }

    public void setHivEnrollmentDate(String hivEnrollmentDate) {
        this.hivEnrollmentDate = hivEnrollmentDate;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }

    public List<String> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<String> alerts) {
        this.alerts = alerts;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getArtInitiationDate() {
        return artInitiationDate;
    }

    public void setArtInitiationDate(String artInitiationDate) {
        this.artInitiationDate = artInitiationDate;
    }
    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPersonTelephone() {
        return contactPersonTelephone;
    }

    public void setContactPersonTelephone(String contactPersonTelephone) {
        this.contactPersonTelephone = contactPersonTelephone;
    }

    public String getCurrentOrLastRegimen() {
        return currentOrLastRegimen;
    }

    public void setCurrentOrLastRegimen(String currentOrLastRegimen) {
        this.currentOrLastRegimen = currentOrLastRegimen;
    }

    public String getReturnVisitDate() {
		return returnVisitDate;
	}

	public void setReturnVisitDate(String returnVisitDate) {
		this.returnVisitDate = returnVisitDate;
	}

	public enum SphClientOrPatientType {
        CLIENT,
        PATIENT;
    }
}
