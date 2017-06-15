<%@ include file="/WEB-INF/template/include.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>
<openmrs:htmlInclude file="/moduleResources/vcttrac/scripts/jquery.tabletoCSV.modified.js" />

<input type="button" value="Print" style="float: right;" onclick="printData()"><button id="export" data-export="export" style="float: right;">CSV</button>
<h2><spring:message code="rwandasphstudyreports.adultConsultationSheet"/></h2>

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
            jQuery("#printTable").tableToCSV(jQuery("h2").text());
        });
    });
</script>

<table id="printTable">
    <thead>
    <tr class="evenRow">
        <th>Person Id</th>
        <th>Name</th>
        <th>Sex</th>
        <th>BirthDate</th>
        <th>HIV Test Date</th>
        <th>Telephone</th>
        <th>Type</th>
        <th>Address</th>
        <th>Peer Educator</th>
        <th>Peer Educator Telephone</th>
        <th>HIV Enrollment Date</th>
        <th>Registration Date</th>
        <th>ART Initiation Date</th>
        <th>Alerts</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${clientsAndPatients}" var="cOrP">
        <tr class="evenRow">
            <td>${cOrP.id}</td>
            <td>${cOrP.name}</td>
            <td>${cOrP.sex}</td>
            <td>${cOrP.birthDate}</td>
            <td>${cOrP.dateTestedForHIV}</td>
            <td>${cOrP.telephone}</td>
            <td>${cOrP.type}</td>
            <td>${cOrP.address}</td>
            <td>${cOrP.peerEducator}</td>
            <td>${cOrP.peerEducatorTelephone}</td>
            <td>${cOrP.hivEnrollmentDate}</td>
            <td>${cOrP.registrationDate}</td>
            <td>${cOrP.artInitiationDate}</td>
            <td>
                <c:forEach items="${cOrP.alerts}" var="alert">-&gt;${alert}<br /></c:forEach>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>

<%@ include file="/WEB-INF/template/footer.jsp"%>