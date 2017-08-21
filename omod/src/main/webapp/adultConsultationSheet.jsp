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
    <select id="dates-to-match" multiple name="datesToMatch">
        <option value="test" <c:if test="${testDateMatch}"> selected</c:if>>HIV Test</option>
        <option value="enrollment" <c:if test="${enrollmentDateMatch}"> selected</c:if>>HIV Enrollment</option>
        <option value="initiation" <c:if test="${initiationDateMatch}"> selected</c:if>>ART Initiation</option>
    </select>
    <input type="submit" value="<spring:message code='general.submit'/>"/>
    <input type="button" id="dates-to-match-reset" onclick="" value="<spring:message code='rwandasphstudyreports.reset'/>"/>
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
        jQuery("#dates-to-match-reset").click(function () {
            //jQuery("#dates-to-match").children().removeAttr('selected');
            window.location.href = 'adultConsultationSheet.form';
        })
    });
</script>

<style type="text/css">
    select[multiple] {
        max-height: 4em;
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
            <td>${cOrP.peerEducator}<c:if test=""> (${cOrP.peerEducatorTelephone})</c:if></td>
            <td>${cOrP.contactPerson}  ${cOrP.contactPersonTelephone}</td>
            <td>${cOrP.dateTestedForHIV}</td>
            <td>${cOrP.registrationDate}</td>
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