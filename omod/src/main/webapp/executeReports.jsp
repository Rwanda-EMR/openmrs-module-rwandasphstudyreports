<%@ include file="/WEB-INF/template/include.jsp" %>

<%@ include file="/WEB-INF/template/header.jsp" %>

<h2>Execute/Setup The Three CDC Study Reports</h2>

<form method="post">
	<input id="form-action" name="formAction" type="hidden" value="">
	<input type="Submit" value="ART Monthly" onclick="jQuery('#form-action').val('aRTMonthly')">
	<input type="Submit" value="Consult Sheet Setup" onclick="jQuery('#form-action').val('consultSheetSetup')">
	<input disabled type="Submit" value="Indicator Report" onclick="jQuery('#form-action').val('indicatorReport')">
</form>

<%@ include file="/WEB-INF/template/footer.jsp" %>