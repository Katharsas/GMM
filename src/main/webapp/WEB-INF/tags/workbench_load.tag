<%@ include file="/WEB-INF/tags/all_include.tagf" %>
<%@tag description="template page" pageEncoding="UTF-8"%>


<c:forEach items="${cfn:values('TaskType')}" var="type">
	<div class="button left" onclick="workbench.load('${type.name()}')">
		<c:set var="tvalue"><fmt:message key="${type.nameKey}"/></c:set> 
		<c:out value="${tvalue}"/>
	</div>
</c:forEach>
<div class="clear"></div>

<form:form id="workbench-loadForm" class="left" commandName="workbench-loadForm"
		action="${contextUrl}/tasks/submitLoad" method="POST">
	<div>
		Load Operation
		<form:select class="form-element" path="loadOperation">
			<c:forEach items="${cfn:values('LoadOperation')}" var="operation">
				<form:option value="${operation}" label="${operation.name()}"/>
			</c:forEach>
		</form:select>
	</div>
	
	
	<div>
		<form:checkbox class="form-element" path="reloadOnStartup"/>
		Load on startup
			
		<form:select class="form-element left" path="defaultStartupType">
			<c:forEach items="${cfn:values('TaskType')}" var="loadType">
				<c:set var="tvalue"><fmt:message key="${loadType.nameKey}"/></c:set> 
				<form:option value="${loadType.name()}" label="${tvalue}"/>
			</c:forEach>
			<form:option value="NONE" label="NONE"/>
		</form:select>
	</div>
</form:form>