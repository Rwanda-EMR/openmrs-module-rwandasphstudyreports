<%@ include file="/WEB-INF/template/include.jsp" %>
<%@ include file="/WEB-INF/template/header.jsp" %>
<%@ include file="template/localHeader.jsp"%>

<h4><spring:message code="rwandasphstudyreports.executeReports"/></h4>

<form method="post">
	<input id="form-action" name="formAction" type="hidden" value="">
	<input type="Submit" value="ART Monthly" onclick="jQuery('#form-action').val('aRTMonthly')">
	<input type="Submit" value="Consult Sheet Setup" onclick="jQuery('#form-action').val('consultSheetSetup')">
	<input disabled type="Submit" value="Indicator Report" onclick="jQuery('#form-action').val('indicatorReport')">
	<input disabled type="Submit" value="Data Quality Report" onclick="jQuery('#form-action').val('dataQualityReport')">
	<input disabled type="Submit" value="Lost to Followup Patients report" onclick="jQuery('#form-action').val('lostToFollowPatiensReport')">
</form>

<%@ include file="/WEB-INF/template/footer.jsp" %>