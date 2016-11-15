<%@ include file="/WEB-INF/template/include.jsp"%>
<openmrs:htmlInclude file="/scripts/calendar/calendar.js" />

<div class="boxHeader">
	Quick Data Entry
</div>
<div class="box">
	<table>
		<thead>
			<tr>
				<th>Test</th>
				<th>Date Of Exam</th>
				<th>Provider</th>
				<th>Location</th>
				<th>Result</th>
			</tr>
		</thead>
		<tbody>
			<c:forEach items="${entries}" var="entry">
				<tr id="${entry.test.conceptId}">
					<td>
						<label class="testName" value="${entry.test.conceptId}">${entry.testName}</label>
					</td>
					<td>
						<input type="text" class="testDate date" onfocus="showCalendar(this)" />
					</td>
					<td>
						<select class="testProvider">
							<option></option>
							<c:forEach items="${providers}" var="provider">
								<option value="${provider.uuid}">${provider.name}</option>
							</c:forEach>
						</select>
					</td>
					<td>
						<select class="testLocation">
							<option></option>
							<c:forEach items="${locations}" var="location">
								<option value="${location.uuid}">${location.name}</option>
							</c:forEach>
						</select>
					</td>
					<td>
						<input class="testResult" type="text"/>
					</td>
				</tr>
			</c:forEach>
		<tbody>
	</table>
</div>