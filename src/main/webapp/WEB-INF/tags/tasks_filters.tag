<%@ include file="/WEB-INF/tags/include.tagf" %>
<%-- Tag Description --%>
<%@tag description="template page" pageEncoding="UTF-8"%>

<%-- Template Start --%>
<div id="filters">
	<form:form class="generalFilters" method="POST" action="/GMM/tasks/submitFilter?tab=${tab}&edit=${edit}" commandName="generalFilter">
		<div id="switchGeneralFilterBody" class="filterGroup clickable" onclick="switchGeneralFilters()">
			&#160;&#62;&#62;
		</div>
		<div id="generalFilterBody">
			<div class="filterBorder"></div>
			<div class="filterGroup center h3">
				<fmt:message key="tasks.filter.general"/>
			</div>
			<div class="filterBorder"></div>
			<div class="filterGroup">
				<label>
					<form:checkbox class="generalFiltersFormElement" path="assignedToMe" />
					<fmt:message key="tasks.filter.assigned"/>
				</label><br>
				<label>
					<form:checkbox class="generalFiltersFormElement" path="createdByMe" />
					<fmt:message key="tasks.filter.created"/>
				</label><br>
			</div>
			<div class="filterBorder"></div>
			<div class="filterGroup">
				<label>
					<form:checkbox id="generalFiltersAllCheckbox" path="all" />
					<fmt:message key="tasks.filter.all"/>
				</label><br>
			</div>
			<div class="filterBorder"></div>
			<div class="filterGroup">
				<c:forEach items="${generalFilter.getTaskStatus()}" var="taskStatus" varStatus="count">
					<label>
						<form:checkbox class="generalFiltersFormElement generalFiltersAllCheckBoxTarget" path="taskStatus[${count.index}]" />
						<fmt:message key="${taskStatuses[count.index].getMessageKey()}"/>
					</label><br>
				</c:forEach>
			</div>
			<div class="filterBorder"></div>
			<div class="filterGroup">
				<c:forEach items="${generalFilter.getPriority()}" var="priority" varStatus="count">
					<label>
						<form:checkbox class="generalFiltersFormElement generalFiltersAllCheckBoxTarget" path="priority[${count.index}]" />
						<fmt:message key="${priorities[count.index].getMessageKey()}"/>
					</label><br>
				</c:forEach>
			</div>
			<div id="generalFiltersInvisible" class="filterGroup center">
				<form:checkbox id="generalFiltersHidden" path="hidden" />
				<input type="submit" value="Apply Filter">
			</div>
		</div>
	</form:form>

	<div class="specificFilters right">
		<div id="switchSpecificFilterBody" class="filterGroup clickable" onclick="switchSpecificFilters()">
				&#60;&#60;
		</div>
		<div id="specificFilterBody">
			<div class="filterBorder"></div>
			<div class="filterGroup center h3">
				Specific
			</div>
			<div class="filterBorder"></div>
			<div class="filterGroup">
				<label><input type="checkbox" name="status">Done</label><br>
				<label><input type="checkbox" name="status">Spacered</label><br>
				<label><input type="checkbox" name="status">Rigged</label><br>
				<label><input type="checkbox" name="status">Textured</label><br>
				<label><input type="checkbox" name="status">Modelled</label><br>
				<label><input type="checkbox" name="status">Unworked</label><br>
			</div>
			<div class="filterBorder"></div>
			<div class="filterGroup">
				...<br>
			</div>
		</div>
	</div>
</div>