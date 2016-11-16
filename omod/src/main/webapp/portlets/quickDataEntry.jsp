<%@ include file="/WEB-INF/template/include.jsp"%>
<openmrs:htmlInclude file="/scripts/calendar/calendar.js" />

<script type="text/javascript">
	jQuery(function() {
		jQuery("#submitEntries").click(
				function(event) {
					jQuery("#formEntries").val(
							JSON.stringify(generateQuickDataEntryToPost()));
					if (jQuery("#formEntries").val() != "")
						jQuery("#entriesForm").submit();
				});
	});

	function generateQuickDataEntryToPost() {
		var entries = [];

		for (var i = 0; i < jQuery(".testName").length; i++) {
			var concept = jQuery(".testName:eq(" + i + ")").attr("id");
			var date = jQuery(".testDate:eq(" + i + ")").val();
			var provider = jQuery(".testProvider:eq(" + i + ")").val();
			var location = jQuery(".testLocation:eq(" + i + ")").val();
			var result = jQuery(".testResult:eq(" + i + ")").val();
			var entry = {
				"conceptId" : concept,
				"date" : date,
				"providerUuid" : provider,
				"locationUuid" : location,
				"result" : result
			};

			if (concept != "" && date != "" && provider != "" && location != ""
					&& result != "")
				entries.push(entry);
		}
		return entries;
	}
</script>

<div class="boxHeader">Quick Data Entry</div>
<div class="box">
	<form method="post" id="entriesForm">
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

				<input type="hidden" name="entries" id="formEntries">
				<c:forEach items="${entries}" var="entry">
					<tr>
						<td><label class="testName" id="${entry.test.conceptId}">${entry.testName}</label>
						</td>
						<td><input type="text" class="testDate date"
							onfocus="showCalendar(this)" /></td>
						<td><select class="testProvider">
								<option></option>
								<c:forEach items="${providers}" var="provider">
									<option value="${provider.uuid}">${provider.name}</option>
								</c:forEach>
						</select></td>
						<td><select class="testLocation">
								<option></option>
								<c:forEach items="${locations}" var="location">
									<option value="${location.uuid}">${location.name}</option>
								</c:forEach>
						</select></td>
						<td><input class="testResult" type="text" /></td>
					</tr>
				</c:forEach>
			<tbody>
		</table>
		<input id="submitEntries" type="button" value="Submit">
	</form>
</div>