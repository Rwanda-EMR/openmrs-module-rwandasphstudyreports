<%@ include file="/WEB-INF/view/module/reporting/include.jsp"%>
<%@ include file="/WEB-INF/view/module/reporting/includeScripts.jsp"%>
<%@ include file="/WEB-INF/template/header.jsp"%>

<style type="text/css">
    .alt { background-color: #E0ECF8; }
    .hover { background-color: #DED; }
    .althover { background-color: #EFE; }        
</style>

<script type="text/javascript">
$(document).ready(function(){
	$('.display tr:even').addClass('alt');
	$('.display tr:even').hover(
			function(){$(this).addClass('hover')},
			function(){$(this).removeClass('hover')}
	);	
	$('.display tr:odd').hover(
			function(){$(this).addClass('althover')},
			function(){$(this).removeClass('althover')}
	);
});
</script>


<!-- Start break table scripts-->

<style type="text/css">
        #page { margin: 0px; } 
        #cohortHeader { padding-left: 5px; background-color: #003366; height: 2em; border-bottom: 1px solid black;
        vertical-align:middle; width: 100%; 
        line-height:2em; font-size: 1.5em; font-weight: bold; color: white; }         
        .cohortResultsColumn { width: 100%; height: auto; text-align: left; margin: 0px; padding-left: 5px; padding-top: 5px;border-collapse: collapse;border: 1px solid black; } 
        #accordion { width: 100%; } 
        table { width: 100%; } 
        .profileImage { width: 75px; height: auto; }
        #cohort-details-table_wrapper { width: 75%; } 
        #cohort-details-table { border: 0px; } 
</style>

<script type="text/javascript" charset="utf-8">
$(document).ready(function() {
        $('.dataset-preview-table').dataTable( {
                "bPaginate": true,
                //"sPaginationType": "full_numbers",
                "iDisplayLength": 5,
                "bLengthChange": false,
                "bFilter": true,
                "bSort": true,
                "bInfo": true,
                "bAutoWidth": true,
                "oLanguage": {
                        "oPaginate": {
                                "sPrevious": "<spring:message code="general.previous" javaScriptEscape="true"/>",
                                "sNext": "<spring:message code="general.next" javaScriptEscape="true"/>",
                        },
                        "sInfo": "<spring:message code="SearchResults.viewing" javaScriptEscape="false"/> _START_ - _END_ <spring:message code="SearchResults.of" javaScriptEscape="false"/> _TOTAL_ ",
                        "sSearch": "<spring:message code="general.search" javaScriptEscape="false"/>"
                }                
                
                //"sDom": '<"top"i>rt<"bottom"flp<"clear">'
        } );       
        
        $("#show-columns").click(function(event){ 
                // eventually will show/hide appropriate columns based on selected dataset definition
        });

} );
</script>
<script type="text/javascript">
$(function() {

        $('#cohort-breakdown-table').dataTable( {
                "bPaginate": false,
                "bLengthChange": false,
                "bFilter": false,
                "bSort": false,
                "bInfo": false,
                "bAutoWidth": false
        } );
        
        $("#accordion").accordion();
        $('#cohort-tabs').tabs();
        $('#cohort-tabs').show();        

        $('#cohort-details-table').dataTable(
                         {
                                "iDisplayLength": 5,                                 
                                "bPaginate": true,
                                "bLengthChange": false,
                                "bFilter": true,
                                "bSort": false,
                                "bInfo": true,
                                "bAutoWidth": true
                                } 
        );

});
</script>

<style>
       input,select { font-size: medium; } 
       legend { font-size: large; } 
       label { font-size: medium; font-weight: bold; } 
       .desc { display: block; }
</style>



<!-- End break table scripts -->

<%--
  This page assumes a ReportData object in the session as the attribute '__openmrs_report_data'
--%>

<c:set var="__openmrs_hide_report_link" value="true" />
<c:set var="dataSetMaps" value="${__openmrs_report_data.dataSets}" />
<c:set var="mapDataSet" value="${dataSetMaps['defaultDataSet'].data}"/> 
<c:set var="mapDataSet" value="${dataSetMaps['defaultDataSetGlobal'].data}"/> 
<c:set var="mapDataSet" value="${dataSetMaps['defaultDataSetncd'].data}"/> 
<openmrs:portlet url="currentReportHeader" moduleId="reporting" parameters="showDiscardButton=true"/>

<div id="page" style="display:block;">
	<div id="container">
		
		<!-- INDICATOR REPORT DSD -->
		<div id="portal">
		<c:forEach var="cohort" items="${dQRList}" varStatus="cohortNum">
		<c:set var="patientsNumInOneDataSet" value="${fn:length(cohort.dataSet.rows)}" />
		<c:set var="patientsNumInAllDataSets" value="${patientsNumInAllDataSets + patientsNumInOneDataSet}" />				
		</c:forEach>
		 <c:forEach var="cohort" items="${dQRList}" varStatus="count" begin="1" end="1">
		<c:if test="${!empty cohort.dataSet}">
		<br/><h3>There are ${patientsNumInAllDataSets} total data problems</h3> <br/><br />
		</c:if>
		</c:forEach> 
		<c:forEach var="cohortResults" items="${dQRList}" varStatus="loopTimes">
		 <c:if test="${!empty cohortResults.dataSet}">
		 
		<div class="cohortResultsColumn">				
			
					<h3>${cohortResults.selectedColumn.name}: ${cohortResults.selectedColumn.label} (${fn:length(cohortResults.dataSet.rows)} <spring:message code="Patient.header"/>)</h3>
        
		 <c:if test="${!empty cohortResults.dataSet}">
						<table class="display">
							<thead>
														
								<tr>
									<c:forEach var="column" items="${cohortResults.dataSet.metaData.columns}" varStatus="varStatus">				
										<c:if test="${column.label!='patientId'}">
										<th>
									<br /><br />
											${column.label}
										</th>
										</c:if>
									</c:forEach>
								</tr>
							</thead>
							<tbody>
									<c:set var="patientsNumInOneDataSet" value="${fn:length(cohortResults.dataSet.rows)}" />
									<c:choose>
									<c:when test='${patientsNumInOneDataSet > "0"}'>
									
									<c:forEach var="dataSetRow" items="${cohortResults.dataSet.rows}" varStatus="varStatus">
										<c:set var="patId" value="${dataSetRow.columnValuesByKey['patientId']}"/>
										<tr>
											<c:forEach var="column" items="${cohortResults.dataSet.metaData.columns}" varStatus="varStatus">
												<c:if test="${column.label!='patientId'}">
												<td>
													<c:if test="${!empty patId}"><a href="${pageContext.request.contextPath}/patientDashboard.form?patientId=${patId}" target="_blank"></c:if>
														${dataSetRow.columnValues[column]}
													<c:if test="${!empty patId}"></a></c:if>
												</td>
												</c:if>
											</c:forEach>										
										</tr>
									</c:forEach>
									</c:when>
									<c:otherwise>
									<tr> <td colspan="5" align="center">
							<table class="display">
							<thead><tr></tr></thead>
						    </table>
                                      No matching records found </td></tr>	
									</c:otherwise>
								</c:choose>			
							</tbody>
							<tfoot>
							</tfoot>
						</table>
					</c:if>
		
								
			</div><!-- column -->
			</c:if>
			<c:if test="${empty cohortResults.dataSet}"></c:if>
			
			
			</c:forEach>
		</div><!-- portal -->
		
		 
		<!-- ENCOUNTER DATA PROBLEM -->
		<div id="portal">
		<c:forEach var="cohort" items="${dQRListenc}" varStatus="cohortNum">
		<c:set var="patientsNumInOneDataSetenc" value="${fn:length(cohort.encounters)}" />
		<c:set var="patientsNumInAllDataSetsenc" value="${patientsNumInOneDataSetenc + patientsNumInOneDataSetenc}" />				
		</c:forEach>

		<c:forEach var="cohort" items="${dQRList}" varStatus="count" begin="1" end="1">
		<c:if test="${!empty cohort.dataSet}">
		<br/><h3>There are ${patientsNumInOneDataSetenc} total encounters problems</h3> <br/><br />
		</c:if>
		</c:forEach> 
		
		 <c:forEach var="cohortResults" items="${dQRListenc}" varStatus="loopTimes">
		 
			<div class="cohortResultsColumn">				
			<c:if test="${!empty cohortResults.selectedEncounter}">
		
					<h3>${cohortResults.selectedEncounter}: ${cohortResults.selectedEncounter} (${fn:length(cohortResults.encounters)} encounters)</h3>
        
		 <c:if test="${!empty cohortResults.encounters}">
						<table class="display">
							<thead>
							<tr>
									<th>encounterId</th>
									<th>EncounterDate</th>
									<th>patientId</th>
									<th>givenName</th>
									<th>familyName</th>
									<th>age</th>
									<th>gender</th>
									
								</tr>
							</thead>
							<tbody>
					<c:forEach var="encdsd" items="${cohortResults.encounters}" varStatus="encNum">
					
							<tr>
	<td><c:if test="${!empty encdsd.encounterId }"><a href="${pageContext.request.contextPath}/admin/encounters/encounter.form?encounterId=${encdsd.encounterId}" target="_blank">${encdsd.encounterId}</a></c:if></td>
	<td><c:if test="${!empty encdsd.encounterId }"><a href="${pageContext.request.contextPath}/admin/encounters/encounter.form?encounterId=${encdsd.encounterId}" target="_blank">${encdsd.encounterDatetime}</a></c:if></td>
	<td><c:if test="${encdsd.patient.patientIdentifier.identifierType.name=='IMB ID'}"><a href="${pageContext.request.contextPath}/admin/encounters/encounter.form?encounterId=${encdsd.encounterId}" target="_blank">${encdsd.patient.patientIdentifier}</a></c:if>
	<c:if test="${encdsd.patient.patientIdentifier.identifierType.name=='IMB Primary Care Registration ID'}"><a href="${pageContext.request.contextPath}/admin/encounters/encounter.form?encounterId=${encdsd.encounterId}" target="_blank">${encdsd.patient.patientIdentifier}</a></c:if>
	</td>
	
	<td><c:if test="${!empty encdsd.encounterId }"><a href="${pageContext.request.contextPath}/admin/encounters/encounter.form?encounterId=${encdsd.encounterId}" target="_blank">${encdsd.patient.givenName}</a></c:if></td>
	<td><c:if test="${!empty encdsd.encounterId }"><a href="${pageContext.request.contextPath}/admin/encounters/encounter.form?encounterId=${encdsd.encounterId}" target="_blank">${encdsd.patient.familyName}</a></c:if></td>
	<td><c:if test="${!empty encdsd.encounterId }"><a href="${pageContext.request.contextPath}/admin/encounters/encounter.form?encounterId=${encdsd.encounterId}" target="_blank">${encdsd.patient.age}</a></c:if></td>
	<td><c:if test="${!empty encdsd.encounterId }"><a href="${pageContext.request.contextPath}/admin/encounters/encounter.form?encounterId=${encdsd.encounterId}" target="_blank">${encdsd.patient.gender}</a></c:if></td>
	                   
	                       
							</tr>
							
							</c:forEach>
							</tbody>
							<tfoot>
							</tfoot>
						</table>
					</c:if>
		
			</c:if>
			<c:if test="${empty cohortResults.selectedEncounter}"></c:if>			
			</div><!-- column --> 
			
			</c:forEach>
		
		</div>
		
		<br/>
		
		 <!-- INDICATOR DATASET FOR NCD -->
		
	 	 <div id="portal">
		<c:forEach var="cohort" items="${dQRListncd}" varStatus="cohortNum">
		<c:set var="patientsNumInOneDataSetncd" value="${fn:length(cohort.dataSet.rows)}" />
		<c:set var="patientsNumInAllDataSetsncd" value="${patientsNumInAllDataSetsncd + patientsNumInOneDataSetncd}" />				
		</c:forEach>
		
		<c:forEach var="cohort" items="${dQRListncd}" varStatus="count" begin="1" end="1">
		<c:if test="${!empty cohort.dataSet}">
		<h3>There are ${patientsNumInAllDataSetsncd} total data problems for NCD</h3> <br/><br />
		</c:if>
		</c:forEach> 
		
		 <c:forEach var="cohortResults" items="${dQRListncd}" varStatus="loopTimes">
		  <c:if test="${!empty cohortResults.dataSet}">
			<div class="cohortResultsColumn">				
			
		
					<h3>${cohortResults.selectedColumn.name}: ${cohortResults.selectedColumn.label} (${fn:length(cohortResults.dataSet.rows)} <spring:message code="Patient.header"/>)</h3>
        
		 <c:if test="${!empty cohortResults.dataSet}">
						<table class="display">
							<thead>
														
								<tr>
									<c:forEach var="column" items="${cohortResults.dataSet.metaData.columns}" varStatus="varStatus">				
										<c:if test="${column.label!='patientId'}">
										<th>
									<br /><br />
											${column.label}
										</th>
										</c:if>
									</c:forEach>
								</tr>
							</thead>
							<tbody>
									<c:set var="patientsNumInOneDataSetncd" value="${fn:length(cohortResults.dataSet.rows)}" />
									<c:choose>
									<c:when test='${patientsNumInOneDataSetncd > "0"}'>
									
									<c:forEach var="dataSetRow" items="${cohortResults.dataSet.rows}" varStatus="varStatus">
										<c:set var="patId" value="${dataSetRow.columnValuesByKey['patientId']}"/>
										<tr>
											<c:forEach var="column" items="${cohortResults.dataSet.metaData.columns}" varStatus="varStatus">
												<c:if test="${column.label!='patientId'}">
												<td>
													<c:if test="${!empty patId}"><a href="${pageContext.request.contextPath}/patientDashboard.form?patientId=${patId}" target="_blank"></c:if>
														${dataSetRow.columnValues[column]}
													<c:if test="${!empty patId}"></a></c:if>
												</td>
												</c:if>
											</c:forEach>										
										</tr>
									</c:forEach>
									</c:when>
									<c:otherwise>
									<tr> <td colspan="5" align="center">
							<table class="display">
							<thead><tr></tr></thead>
						    </table>
                                      No matching records found </td></tr>	
									</c:otherwise>
								</c:choose>			
							</tbody>
							<tfoot>
							</tfoot>
						</table>
					</c:if>
		
								
			</div><!-- column --> 
			</c:if>
			<c:if test="${empty cohortResults.dataSet}"></c:if>
			</c:forEach>
		</div>   
	</div>
</div>


<%@ include file="/WEB-INF/template/footer.jsp"%>