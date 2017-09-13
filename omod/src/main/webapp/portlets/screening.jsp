<%@ include file="/WEB-INF/template/include.jsp"%>

<p>${msg}</p>

<c:if test="${vlTreatmentFailure}">
	<form method="post">
		<select name="selectedClinicalAction">
			<option></option>
			<c:forEach var="action" items="${clinicalActions}">
				<option ${action.name == savedClinicalAction ? 'selected="selected"' : ''} value="${action.name}">${action.displayName}</option>
		    </c:forEach>
		</select>
		<input type="submit" value="Submit">			
	</form>
</c:if>
<%@ include file="/WEB-INF/template/footerMinimal.jsp"%>