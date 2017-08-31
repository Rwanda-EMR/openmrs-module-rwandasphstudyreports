<%@ include file="/WEB-INF/template/include.jsp"%>

<p>${msg}</p>

<c:if test="${vlTreatmentFailure}">
	<form method="post" action="/module/rwandasphstudyreports/portlets/screening">
		<select name="selectedClinicalAction">
			<c:forEach var="action" items="${clinicalActions}">
		    	<option ${action.name == savedClinicalAction ? 'selected="selected"' : ''} value="${action.name}">${action.displayName}</option>
		    </c:forEach>
		</select>			
	</form>
</c:if>
<%@ include file="/WEB-INF/template/footerMinimal.jsp"%>