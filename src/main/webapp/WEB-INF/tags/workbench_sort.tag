<%@ include file="/WEB-INF/tags/all_include.tagf" %>
<%@tag description="template page" pageEncoding="UTF-8"%>


<form:form id="sortForm" commandName="workbench-sortForm"
		action="${contextUrl}/tasks/submitSort" method="POST">
		
	<div class="left">
	<fmt:message key="search.first"/><span></span>
	</div>
	<div id="primarySort" class="left">
		<form:select class="sortFormElement" path="sortByPrimary">
			<c:forEach items="${cfn:values('TaskSortAttribute')}" var="sort">
				<c:set var="svalue"><fmt:message key="${sort.nameKey}"/></c:set> 
				<form:option value="${sort}" label="${svalue}"/>
			</c:forEach>
		</form:select>
		<fmt:message key="tasks.down"/><form:checkbox class="sortFormElement" path="sortDownPrimary"/>
	</div>
	<div class="clear"></div>
	<div class="left">
		<fmt:message key="search.seconde"/><span></span> 
	</div>
	<div id="secondarySort" class="left">
		<form:select class="sortFormElement" path="sortBySecondary">
			<c:forEach items="${cfn:values('TaskSortAttribute')}" var="sort">
				<c:set var="svalue"><fmt:message key="${sort.nameKey}"/></c:set> 
				<form:option value="${sort}" label="${svalue}"/>
			</c:forEach>
		</form:select>
		<fmt:message key="tasks.down"/><form:checkbox class="sortFormElement" path="sortDownSecondary"/>
	</div>
</form:form>