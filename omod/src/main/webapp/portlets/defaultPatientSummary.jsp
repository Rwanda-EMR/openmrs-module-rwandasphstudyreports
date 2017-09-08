<%@ include file="/WEB-INF/template/include.jsp"%>

<openmrs:htmlInclude
	file="/moduleResources/rwandasphstudyreports/patientsummary.css" />


<script type="text/javascript">
	function printPatientSummary() {
		jQuery("#patientDashboardHeader, #openmrs_dwr_error, #navList, #userBar, #patientTabs, #footer").addClass("noprint");
		
		var restorepage = document.body.innerHTML;
		var htmlToPrint = '<style type="text/css">@media print {.noprint {display:none;}}</style>'
			+ restorepage;
	    
	    document.body.innerHTML = htmlToPrint;
	    window.print();
	    document.body.innerHTML = restorepage;
	    
	    jQuery("#patientDashboardHeader, #openmrs_dwr_error, #navList, #userBar, #patientTabs, #footer").removeClass("noprint");
	}
	
	jQuery(function() {
		jQuery("#defaultPatientSummaryTabIDTab").insertBefore(jQuery("#patientOverviewTab"));
		jQuery("#DataEntryTabIdTab").insertAfter(jQuery("#patientOverviewTab"));
		jQuery("#formEntryTab").insertAfter(jQuery("#DataEntryTabIdTab"));
		jQuery("#defaultPatientSummaryTabIDTab").trigger("click");

		//external fixes
		jQuery("#patientHeaderRegimen").text("${currentRegimen}");
		if(jQuery("#patientActions").text().toLowerCase().indexOf('resume care') > 0) {
			jQuery(".boxHeader").css({"background-color": "#bbbbbb", "border" : "1px solid #555555"});
			jQuery(".box").css({"border" : "1px solid #555555"})
			jQuery("#patientHeader").css({"background-color": "#b82619"});
		}
	});
</script>

<br />

<div id="patientsummarymodule">
<div class="header">
<table width="100%" cellpadding="0" cellspacing="0" border="0">
	<tr height="27px">
		<td valign="top" colspan="2" id="backAndPrintButtons">
		<table width="100%" cellpadding="0" cellspacing="0" border="0">
			<tr>
				<td class="nowrap"><h3>${patient.personName}</h3> <span id="died"><c:if
					test="${patient.dead == 'true'}">(Dead)</c:if></span>
				</td>
				<td class="noprint" align="right" valign="top">
					<input disabled style="position: relative; top: -5px;" type="button" value="<spring:message code='general.back' />" onclick="" />
					<input style="position: relative; top: -5px;" type="button" value="<spring:message code='rwandasphstudyreports.print' />" onclick="printPatientSummary();" />
				</td>
			</tr>
			<tr>
				<td colspan="2">
				<hr>
				</td>
			</tr>
		</table>
		</td>
	</tr>
	<tr>
		<td valign="top">
		<table cellspacing="0" cellpadding="0" border="0">
			<tr>
				<td align="right"><spring:message code="Patient.gender" /></td>
				<td><b> <c:if test="${patient.gender == 'M'}">
					<spring:message code="Patient.gender.male" />
				</c:if> <c:if test="${patient.gender == 'F'}">
					<spring:message code="Patient.gender.female" />
				</c:if> </b></td>
			</tr>
			<tr>
				<td align="right"><spring:message code="Person.age" /></td>
				<td><b><c:choose>
					<c:when test="${patient.age <= maxage}">
												${agestring}
											</c:when>
					<c:otherwise>
												${patient.age} <spring:message code="Person.age.years" />
					</c:otherwise>
				</c:choose> </b> (<c:if test="${patient.birthdateEstimated}">~</c:if><openmrs:formatDate
					date="${patient.birthdate}" type="medium" />)</td>
			</tr>
			<c:forEach var="patientProgram" items="${programs}">
				<c:if test="${patientProgram.program!=null}">
					<tr>
						<td class="nowrap" align="right">${patientProgram.program.name}</td>
						<td>
							<c:choose>
								<c:when test="${patientProgram.dateEnrolled!=null}">
									<i>Enrolled on <openmrs:formatDate date="${patientProgram.dateEnrolled}" type="medium" /></i>
								</c:when>
							</c:choose>
						</td>
						<td colspan="3">
							<c:choose>
								<c:when test="${patientProgram.dateCompleted!=null}">
									<i>Completed on <openmrs:formatDate date="${patientProgram.dateCompleted}" type="medium" /></i>
								</c:when>
								<c:otherwise>
									<b>
										<c:forEach var="patientState" items="${patientProgram.currentStates}">
											${patientState.state.concept.name.name}<br/>
										</c:forEach>
									</b>
								</c:otherwise>
							</c:choose>
						</td>
					</tr>
				</c:if>
			</c:forEach>
			</tr>			<c:if test="${not empty lastencounter.timeago}">
				<tr>
					<td align="right"><spring:message
						code="rwandasphstudyreports.lastvisit" /></td>
					<td><b>${lastencounter.timeago}</b> (<openmrs:formatDate
						date="${lastencounter.datetime}" type="medium" />)</td>
				</tr>
				<tr>
					<td align="right"></td>
					<td><b>${lastencounter.encounterType}</b>
						by <b>${lastencounter.provider.personName}</b> @ <b>${lastencounter.location}</b> </td>
				</tr>
			</c:if>
			
			
			
			
		</table>
		</td>
		<td valign="top" align="right">
		</td>

	</tr>
</table>
</div>

<div class="section">
<table width="100%" cellpadding="0" cellspacing="0" border="0">
	<tr>
		<td class="th"><spring:message code="rwandasphstudyreports.alerts" /></td>
	</tr>
	<c:if test="${not empty cdcAlerts}">
		<c:forEach var="cdcAlert" items="${cdcAlerts}">
			<tr>
				<td class="alert" style="color:red">${cdcAlert}</td>
			</tr>
		</c:forEach>
	</c:if>
	<tr>
		<td class="alert"><c:forEach var="alertconcept"
			items="${alertconcepts}">
			<openmrs:concept conceptId="${alertconcept}" var="concept"
				nameVar="name" numericVar="num">
				<c:if test="${not empty alerts[alertconcept]}">
							No <c:choose>
						<c:when test="${!empty concept.name.shortName}">
									${concept.name.shortName}
								</c:when>
						<c:otherwise>
									${concept.name}
								</c:otherwise>
					</c:choose> ${alerts[alertconcept]} <br />
				</c:if>
			</openmrs:concept>
		</c:forEach> <c:if test="${empty accompFound && patient.age <= maxage}">
			<spring:message code="rwandasphstudyreports.noaccomp" />
			<br />
		</c:if></td>
	</tr>

</table>
<br/>

<c:if test="${not empty drugorders}">
<table width="100%" cellpadding="0" cellspacing="1" border="0">
		<tr>
			<td class="th"><spring:message code="rwandasphstudyreports.drugorders" /></td>
			<td class="th"><spring:message code="rwandasphstudyreports.dose" /></td>
			<td class="th"><spring:message code="rwandasphstudyreports.frequency" /></td>
			<td class="th"><spring:message code="rwandasphstudyreports.startdate" /></td>
			<td class="th"><spring:message code="rwandasphstudyreports.stopdate" /></td>
			<td class="th"><spring:message code="rwandasphstudyreports.order.action" /></td>
			<td class="th"><spring:message code="rwandasphstudyreports.reason" /></td>
		</tr>
		<c:forEach items="${drugorders}" var="drug">
			<c:if test="${drug.drugOrder.voided == false}">
				<tr>
					<td class="nowrap">${not empty drug.drugOrder.drug ? drug.drugOrder.drug.name : drug.drugOrder.concept.name.name} <c:if test="${drug.isActive == false}"> (stopped)</c:if></td>
					<td class="nowrap">${drug.drugOrder.dose} ${drug.doseUnitsName}</td>
					<td class="nowrap">${drug.frequency}</td>
					<td class="nowrap"><openmrs:formatDate
						date="${drug.startDate}" type="medium"/></td>
					<td class="nowrap"><openmrs:formatDate
						date="${drug.stopDate}" type="medium"/></td>
					<td>${not empty drug.drugOrder.action ? drug.drugOrder.action : ""}</td>
					<td>${drug.orderReason}</td>
				</tr>
			</c:if>
		</c:forEach>
	</table>
</c:if>
<br/>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
	<tr>
		<td class="th"><spring:message code="rwandasphstudyreports.comments" /></td>
	</tr>
	<tr>
		<td>
		<c:if test="${empty lastencounter.timeago}">
			<spring:message code="rwandasphstudyreports.nolastvisit" />
			<br />
		</c:if> 
		<c:if test="${empty adverse['recent'] && empty adverse['old']}">
			<spring:message code="rwandasphstudyreports.noadverse" />
			<br />
		</c:if> 
		<c:if test="${empty infections['recent'] && empty infections['old']}">
			<spring:message code="rwandasphstudyreports.noinfections" />
			<br />
		</c:if> 
		<c:if test="${empty symptoms['recent'] && empty symptoms['old']}">
			<spring:message code="rwandasphstudyreports.nosymptoms" />
			<br />
		</c:if> 
		<c:if test="${empty prevdiags['recent'] && empty prevdiags['old']}">
			<spring:message code="rwandasphstudyreports.nodiagnoses" />
			<br />
		</c:if> 
		<c:if test="${empty drugorders}">
			<spring:message code="rwandasphstudyreports.nodrugs" />
			<br />
		</c:if> 
		<c:if
			test="${empty vitals['recent'] && empty vitals['old'] && patient.age <= maxage}">
			<spring:message code="rwandasphstudyreports.novitals" />
			<br />
		</c:if> 
		<c:if test="${empty labdata['recent'] && empty labdata['old']}">
			<spring:message code="rwandasphstudyreports.nolabdata" />
			<br />
		</c:if> 
		<c:if test="${empty notes}">
			<spring:message code="rwandasphstudyreports.noclinicalnotes" />
			<br />
		</c:if></td>
	</tr>

</table>
</div>

<c:if test="${not empty adverse['recent'] || not empty adverse['old']}">
	<div class="section">
	<table width="100%" cellpadding="1" cellspacing="1" border="0">
		<tr>
			<td class="th"><spring:message code="rwandasphstudyreports.adverse" /></td>
			<td class="th"><spring:message code="rwandasphstudyreports.date" /></td>
			<td class="th"><spring:message code="rwandasphstudyreports.comments" /></td>
		</tr>
		<openmrs:forEachObs obs="${adverse['recent']}" var="ob" descending="true">
			<tr>
				<td><!-- ${ob.obsId} --><b>${ob.valueAsString[locale]}</b></td>
				<td class="nowrap"><openmrs:formatDate date="${ob.obsDatetime}"
					type="medium" /></td>
				<td>${ob.comment}</td>
			</tr>
		</openmrs:forEachObs>
		<openmrs:forEachObs obs="${adverse['old']}" var="ob" descending="true">
			<tr>
				<td><!-- ${ob.obsId} -->${ob.valueAsString[locale]}</td>
				<td class="nowrap"><openmrs:formatDate date="${ob.obsDatetime}"
					type="medium" /></td>
				<td>${ob.comment}</td>
			</tr>
		</openmrs:forEachObs>
	</table>
	</div>
</c:if>

<c:if test="${not empty infections['recent'] || not empty infections['old']}">
	<div class="section">
	<table width="100%" cellpadding="1" cellspacing="1" border="0">
		<tr>
			<td class="th"><spring:message code="rwandasphstudyreports.infections" /></td>
			<td class="th"><spring:message code="rwandasphstudyreports.date" /></td>
		</tr>
		<openmrs:forEachObs obs="${infections['recent']}" var="ob" descending="true">
			<tr>
				<td><!-- ${ob.obsId} --><b>${ob.valueAsString[locale]}</b></td>
				<td class="nowrap"><openmrs:formatDate date="${ob.obsDatetime}"
					type="medium" /></td>
			</tr>
		</openmrs:forEachObs>
		<openmrs:forEachObs obs="${infections['old']}" var="ob" descending="true">
			<tr>
				<td><!-- ${ob.obsId} -->${ob.valueAsString[locale]}</td>
				<td class="nowrap"><openmrs:formatDate date="${ob.obsDatetime}"
					type="medium" /></td>
			</tr>
		</openmrs:forEachObs>
	</table>
	</div>
</c:if>

<c:if test="${not empty symptoms['recent'] || not empty symptoms['old']}">
	<div class="section">
	<table width="100%" cellpadding="1" cellspacing="1" border="0">
		<tr>
			<td class="th"><spring:message code="rwandasphstudyreports.symptoms" /></td>
			<td class="th"><spring:message code="rwandasphstudyreports.date" /></td>
		</tr>
		<openmrs:forEachObs obs="${symptoms['recent']}" var="ob" descending="true">
			<tr>
				<td><!-- ${ob.obsId} --><b>${ob.valueAsString[locale]}</b></td>
				<td class="nowrap"><openmrs:formatDate date="${ob.obsDatetime}"
					type="medium" /></td>
			</tr>
		</openmrs:forEachObs>
		<openmrs:forEachObs obs="${symptoms['old']}" var="ob" descending="true">
			<tr>
				<td><!-- ${ob.obsId} -->${ob.valueAsString[locale]}</td>
				<td class="nowrap"><openmrs:formatDate date="${ob.obsDatetime}"
					type="medium" /></td>
			</tr>
		</openmrs:forEachObs>
	</table>
	</div>
</c:if>

<c:if test="${not empty prevdiags['recent'] || not empty prevdiags['old']}">
	<div class="section">
	<table width="100%" cellpadding="1" cellspacing="1" border="0">
		<tr>
			<td class="th"><spring:message code="rwandasphstudyreports.diagnosis" /></td>
			<td class="th"><spring:message code="rwandasphstudyreports.date" /></td>
		</tr>
		<openmrs:forEachObs obs="${prevdiags['recent']}" var="ob"
			descending="true">
			<tr>
				<td><!-- ${ob.obsId} --><b>${ob.valueAsString[locale]}</b></td>
				<td class="nowrap"><openmrs:formatDate date="${ob.obsDatetime}"
					type="medium" /></td>
			</tr>
		</openmrs:forEachObs>
		<openmrs:forEachObs obs="${prevdiags['old']}" var="ob"
			descending="true">
			<tr>
				<td><!-- ${ob.obsId} -->${ob.valueAsString[locale]}</td>
				<td class="nowrap"><openmrs:formatDate date="${ob.obsDatetime}"
					type="medium" /></td>
			</tr>
		</openmrs:forEachObs>
	</table>
	</div>
</c:if>

<c:if test="${not empty vitals['recent'] || not empty vitals['old']}">
	<div class="section">
	<table width="100%" cellpadding="1" cellspacing="1" border="0">
		<tr>
			<td class="th"><spring:message code="rwandasphstudyreports.vitals" /></td>
			<td class="th"><spring:message code="rwandasphstudyreports.result" /></td>
			<td class="th"><spring:message code="rwandasphstudyreports.date" /></td>
			<td class="th"><spring:message code="rwandasphstudyreports.comments" /></td>
		</tr>
		<openmrs:forEachObs obs="${vitals['recent']}" var="ob"
			descending="true">
			<tr>
				<td class="nowrap">
					<!-- ${ob.obsId} -->
					<b><c:choose>
					<c:when test="${!empty ob.concept.name.shortName}">
										${ob.concept.name.shortName}
									</c:when>
					<c:otherwise>
										${ob.concept.name}
									</c:otherwise>
				</c:choose></b></td>
				<td class="nowrap"><b>${ob.valueNumeric}</b></td>
				<td class="nowrap"><openmrs:formatDate date="${ob.obsDatetime}"
					type="medium" /></td>
				<td>${ob.comment}</td>
			</tr>
		</openmrs:forEachObs>
		<openmrs:forEachObs obs="${vitals['old']}" var="ob" descending="true">
			<tr>
				<td class="nowrap">
				<!-- ${ob.obsId} -->
				<c:choose>
					<c:when test="${!empty ob.concept.name.shortName}">
										${ob.concept.name.shortName}
									</c:when>
					<c:otherwise>
										${ob.concept.name}
									</c:otherwise>
				</c:choose></td>
				<td class="nowrap">${ob.valueNumeric}</td>
				<td class="nowrap"><openmrs:formatDate date="${ob.obsDatetime}"
					type="medium" /></td>
				<td>${ob.comment}</td>
			</tr>
		</openmrs:forEachObs>
	</table>
	</div>
</c:if> 
<c:if test="${not empty labdata['recent'] || not empty labdata['old']}">

<style>
#labTestTable th { width: 200px; } 
</style>

	<div class="section">
	<table width="100%" cellpadding="1" cellspacing="1" border="0">
		<tr>
			<td class="th"><spring:message code="rwandasphstudyreports.labtests" /></td>
		</tr>
		<tr>
			<td width="100%">		
				<openmrs:obsTable
					observations="${labdata['all']}"
					concepts="name:CD4 COUNT|name:WEIGHT (KG)|set:name:LABORATORY EXAMINATIONS CONSTRUCT"
					conceptLink="${pageContext.request.contextPath}/admin/observations/personObs.form?personId=${patient.patientId}&"
					id="labTestTable"
					showEmptyConcepts="false"
					showConceptHeader="true"
					showDateHeader="true"
					orientation="verticle"
					sort="asc"
					combineEqualResults="true"
					limit="25"
				/>
			</td>
		</tr>
		
	</table>
	</div>
</c:if>

<div class="section">
<table width="100%" cellpadding="1" cellspacing="1" border="0">
	<tr>
		<td class="th"><spring:message code="rwandasphstudyreports.graphs" /></td>
	</tr>
	<tr>
		<td>
			<c:forEach var="graphconcept" items="${graphconcepts}">
				<img
					src="${pageContext.request.contextPath}/showGraphServlet?patientId=${patient.patientId}&conceptId=${graphconcept}&width=200&height=200&minRange=<c:out value="${graphdata['floor'][graphconcept]}" default="0.0"/>&maxRange=<c:out value="${graphdata['ceiling'][graphconcept]}" default="200.0"/>" width="200" height="200" />
			</c:forEach>
		</td>
	</tr>
</table>
</div>
<c:if test="${not empty notes}">
	<div class="section">
	<table width="100%" cellpadding="1" cellspacing="1" border="0">
		<tr>
			<td class="th"><spring:message
				code="rwandasphstudyreports.clinicalnotes" /></td>
			<td class="th"><spring:message code="rwandasphstudyreports.date" /></td>
			<td class="th"><spring:message code="rwandasphstudyreports.clinician" /></td>
		</tr>
		
		<openmrs:forEachObs obs="${notes}" var="ob" descending="true">
			<tr>
				<td>
					<b>${ob.concept.name}</b><br />
					<div class="indent">${notesText[ob]}</div>
				</td>
				<td class="nowrap"><openmrs:formatDate date="${ob.obsDatetime}"
					type="medium" /></td>
				<td class="nowrap">${ob.encounter.provider.personName}</td>
			</tr>
		</openmrs:forEachObs>
	</table>
	</div>
</c:if></div>

<%@ include file="/WEB-INF/template/footerMinimal.jsp"%>