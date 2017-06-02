package org.openmrs.module.rwandasphstudyreports;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by k-joseph on 01/06/2017.
 */
public class SphClientOrPatient {
    public SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    private Integer id;
    private String name;
    private String sex;
    private String birthDate;
    private String dateTestedForHIV;
    private Integer daysSinceHIVToDiagnosis;
    private String telephone;
    private String address;
    private String peerEducator;
    private String peerEducatorTelephone;
    private String hivEnrollmentDate;
    private String registrationDate;
    private String hivTestDate;
    private String artInitiationDate;
    private List<String> alerts;

    private String type;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getHivTestDate() {
        return hivTestDate;
    }

    public void setHivTestDate(String hivTestDate) {
        this.hivTestDate = hivTestDate;
    }

    public String getArtInitiationDate() {
        return artInitiationDate;
    }

    public void setArtInitiationDate(String artInitiationDate) {
        this.artInitiationDate = artInitiationDate;
    }

    public enum SphClientOrPatientType {
        CLIENT,
        PATIENT;
    }
}
