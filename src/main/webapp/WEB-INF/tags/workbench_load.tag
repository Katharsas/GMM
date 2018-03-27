<%@ include file="/WEB-INF/tags/all_include.tagf" %>
<%@tag description="template page" pageEncoding="UTF-8"%>

<div id="workbench-load-wrapper">

	<div id="workbench-load-typeButtons">
		<c:forEach items="${cfn:values('TaskType')}" var="type">
			<div class="button workbench-load-typeButton" data-type="${type.name()}">
				<c:set var="tvalue"><fmt:message key="${type.nameKey}"/></c:set>
				<c:out value="${tvalue}"/>
			</div>
		</c:forEach>
	</div>
	
	
	<form:form id="workbench-loadForm" commandName="workbench-loadForm"
			action="${contextUrl}/tasks/submitLoad" method="POST">
		<div id="workbench-load-formWrapper">
		
			<div class="input">
				Load Operation:
				<form:select class="form-element" path="loadOperation">
					<c:forEach items="${cfn:values('LoadOperation')}" var="operation">
						<form:option value="${operation}" label="${operation.name()}"/>
					</c:forEach>
				</form:select>
			</div>
			
			<div class="input">
				<label>
					<form:checkbox class="form-element" path="reloadOnStartup"/>
					Load on Login
				</label>
				<form:select class="form-element" path="defaultStartupType">
					<c:forEach items="${cfn:values('TaskType')}" var="loadType">
						<c:set var="tvalue"><fmt:message key="${loadType.nameKey}"/></c:set> 
						<form:option value="${loadType.name()}" label="${tvalue}"/>
					</c:forEach>
					<form:option value="NONE" label="NONE"/>
				</form:select>
			</div>
			
		</div>
	</form:form>

</div>