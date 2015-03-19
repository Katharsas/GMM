<%@ include file="/WEB-INF/tags/all_include.tagf" %>
<%@tag description="template page" pageEncoding="UTF-8"%>

<div id="workbench-sort-formWrapper">
	<form:form id="workbench-sortForm" commandName="workbench-sortForm"
			action="${contextUrl}/tasks/submitSort" method="POST">
		
			<div id="workbench-sort-primary">
				<div class="workbench-sort-element">
					<fmt:message key="search.first"/>
				</div>
				<div class="workbench-sort-element input">
					<form:select path="sortByPrimary">
						<c:forEach items="${cfn:values('TaskSortAttribute')}" var="sort">
							<c:set var="svalue"><fmt:message key="${sort.nameKey}"/></c:set> 
							<form:option value="${sort}" label="${svalue}"/>
						</c:forEach>
					</form:select>
				</div>
				<div class="workbench-sort-element">
					<fmt:message key="tasks.down"/>
					<form:checkbox path="sortDownPrimary"/>
				</div>
			</div>	
			
			<div id="workbench-sort-secondary">
				<div class="workbench-sort-element">
					<fmt:message key="search.seconde"/>
				</div>
				<div class="workbench-sort-element input">
					<form:select path="sortBySecondary">
						<c:forEach items="${cfn:values('TaskSortAttribute')}" var="sort">
							<c:set var="svalue"><fmt:message key="${sort.nameKey}"/></c:set> 
							<form:option value="${sort}" label="${svalue}"/>
						</c:forEach>
					</form:select>
				</div>
				<div class="workbench-sort-element">
					<fmt:message key="tasks.down"/>
					<form:checkbox path="sortDownSecondary"/>
				</div>
			</div>
			
	</form:form>
</div>