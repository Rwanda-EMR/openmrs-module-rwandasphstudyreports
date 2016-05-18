<spring:htmlEscape defaultHtmlEscape="true" />
<ul id="menu">
	<li class="first"><a
		href="${pageContext.request.contextPath}/admin"><spring:message
				code="admin.title.short" /></a></li>

	<li
		<c:if test='<%= request.getRequestURI().contains("/manage") %>'>class="active"</c:if>>
		<a
		href="${pageContext.request.contextPath}/module/rwandareportsforcdc/manage.form"><spring:message
				code="rwandareportsforcdc.manage" /></a>
	</li>
	
	<li
		<c:if test='<%= request.getRequestURI().contains("/executeReports") %>'>class="active"</c:if>>
		<a href="${pageContext.request.contextPath}/module/rwandareportsforcdc/executeReports.list">Execute Reports</a>
	</li>
	<!-- Add further links here -->
</ul>
<h2>
	<spring:message code="rwandareportsforcdc.title" />
</h2>
