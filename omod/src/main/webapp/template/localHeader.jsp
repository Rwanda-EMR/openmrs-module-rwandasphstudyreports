<spring:htmlEscape defaultHtmlEscape="true" />
<ul id="menu">
	<li class="first"><a
		href="${pageContext.request.contextPath}/admin"><spring:message
				code="admin.title.short" /></a></li>

	<li
		<c:if test='<%= request.getRequestURI().contains("/executeReports") %>'>class="active"</c:if>>
		<a href="${pageContext.request.contextPath}/module/rwandasphstudyreports/executeReports.list">Execute Reports</a>
	</li>
	<!-- Add further links here -->
</ul>
<h2>
	<spring:message code="rwandasphstudyreports.title" />
</h2>
