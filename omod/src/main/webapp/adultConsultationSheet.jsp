<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>
<openmrs:htmlInclude file="/moduleResources/vcttrac/scripts/jquery.tabletoCSV.modified.js" />
<openmrs:htmlInclude file="/scripts/calendar/calendar.js" />

<input type="button" value="Print" style="float: right;" onclick="printData()"><button id="export" data-export="export" style="float: right;">CSV</button>
<h2><spring:message code="rwandasphstudyreports.adultConsultationSheet"/></h2>

<form method="post">
    <spring:message code="rwandasphstudyreports.startdate"/> <input type="text" id="startDate" size="11" value="${startDate}" name="startDate" onclick="showCalendar(this)" />
    <spring:message code="rwandasphstudyreports.enddate"/> <input type="text" id="endDate" size="11" value="${endDate}" name="endDate" onclick="showCalendar(this)" />
    <spring:message code="rwandasphstudyreports.datesToMatch"/>
    <select multiple name="datesToMatch" id="dates-to-match">
        <option value="test" <c:if test="${testDateMatch}"> selected</c:if>>HIV Test</option>
        <option value="enrollment" <c:if test="${enrollmentDateMatch}"> selected</c:if>>HIV Enrollment</option>
        <option value="initiation" <c:if test="${initiationDateMatch}"> selected</c:if>>ART Initiation</option>
    </select>
    <spring:message code="rwandasphstudyreports.alertsToMatch"/>
    <select multiple name="alerts" id="alerts-to-match">
    	<c:forEach items="${alerts}" var="a">
    		<option value="${a.name}" <c:if test="${fn:containsIgnoreCase(alertsString, a.name)}"> selected</c:if>> ${a.displayName}</option>
    	</c:forEach>
    </select>
    <input type="submit" value="<spring:message code='general.submit'/>"/>
    <input type="button" onclick="window.location.href = 'adultConsultationSheet.form'" value="<spring:message code='rwandasphstudyreports.reset'/>"/>
</form>
<br/>
<br/>
<script type="application/javascript">
    function printData() {
        var divToPrint=document.getElementById("printTable");
        newWin= window.open("");
        newWin.document.write(divToPrint.outerHTML);
        newWin.print();
        newWin.close();
    }
    jQuery(function() {
        jQuery("#export").click(function() {
            jQuery("#printTable").tableToCSV(jQuery("h2").text() + '_startDate: ' + jQuery("#startDate").val() + ' endDate: ' + jQuery("#endDate").val());
        });
    });
</script>

<style type="text/css">
    #dates-to-match {
        height: 4em;
    }
    #alerts-to-match {
        height: 8em;
    }
</style>

<table id="printTable">
    <thead>
    <tr class="evenRow">
        <th>TracNet Id</th>
        <th>Name</th>
        <th>Sex</th>
        <th>Birth Date</th>
        <th>Telephone</th>
        <th>Type</th>
        <th>Address</th>
        <th>Peer Educator</th>
        <th>Contact Person</th>
        <th>Registration Date</th>
        <th>HIV Test Date</th>
        <th>HIV Enrollment Date</th>
        <th>ART Initiation Date</th>
        <th>Regimen</th>
        <th>Alerts</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${clientsAndPatients}" var="cOrP">
        <tr class="evenRow">
            <td>${cOrP.tracnetId}</td>
            <td>${cOrP.name}</td>
            <td>${cOrP.sex}</td>
            <td>${cOrP.birthDate}</td>
            <td>${cOrP.telephone}</td>
            <td>${cOrP.type}</td>
            <td>${cOrP.address}</td>
            <td>${cOrP.peerEducator}<c:if test="${not empty cOrP.peerEducatorTelephone}"> (${cOrP.peerEducatorTelephone})</c:if></td>
            <td>${cOrP.contactPerson}<c:if test="${not empty cOrP.contactPersonTelephone}"> (${cOrP.contactPersonTelephone})</c:if></td>
            <td>${cOrP.registrationDate}</td>
            <td>${cOrP.dateTestedForHIV}</td>
            <td>${cOrP.hivEnrollmentDate}</td>
            <td>${cOrP.artInitiationDate}</td>
            <td>${cOrP.currentOrLastRegimen}</td>
            <td>
                <c:choose>
                    <c:when test="${cOrP.type == 'PATIENT'}">
                        <c:forEach items="${cOrP.alerts}" var="alert">-${alert}<br /></c:forEach>
                    </c:when>
                    <c:otherwise>
                        <spring:message code='rwandasphstudyreports.alerts.notLinked'/>
                    </c:otherwise>
                </c:choose>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>

<%@ include file="/WEB-INF/template/footer.jsp"%>