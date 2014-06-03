<%@ include file="/WEB-INF/tags/all_include.tagf" %>
<%-- Tag Description --%>
<%@tag description="template page" pageEncoding="UTF-8"%>

<div class="taskElement left">
<!--PRIORITY------------------------------------- -->
	<div class="taskDescription"><fmt:message key="${task.priority.typeKey}"/>:</div>
	<div class="taskInput">
	<form:select path="priority">
		<c:forEach items="${task.priority.values()}" var="priority">
			<c:set var="pvalue">
				<fmt:message key="${priority.nameKey}"/>
			</c:set> 
			<form:option value="${priority}" label="${pvalue}"/>
		</c:forEach>
	</form:select>
	</div>
<!--TITLE------------------------------------------ -->
	<div class="taskDescription"><fmt:message key="tasks.title"/>:</div>
	<div class="taskInput">
		<form:input path="idName" value=""/>
	</div>
</div>
<div class="taskElement left">
<!--LABEL------------------------------------ -->
	<div class="taskDescription"><fmt:message key="tasks.label"/>:</div>
	<div class="taskInput">
		<form:input id="labelInput" path="label" value=""/>
	</div>
	<div class="taskDescription">&#160;</div>
	<div class="taskInput">
		<form:select id="labelSelect" path="labelSelect"> 
			<form:option value="" label=""/>
			<c:forEach items="${taskLabels}" var="singleLabel">
				<c:choose>
					<c:when test="${singleLabel.equals(label)}">
						<form:option value="${singleLabel}" label="${singleLabel}" selected="selected"/>
					</c:when>
					<c:otherwise>
						<form:option value="${singleLabel}" label="${singleLabel}" />
					</c:otherwise>
				</c:choose>
			</c:forEach>
		</form:select>
	</div>
</div>
<div class="taskElement left">
<!--DETAILS----------------------------------------- -->
	<div class="taskDescription"><fmt:message key="tasks.details"/>:</div>
	<div class="taskInput">
	<form:textarea rows="2" cols="1" path="details"></form:textarea>
	</div>
</div>
<div class="taskElement left">
<!--STATUS-------------------------------------------- -->
	<div class="taskDescription"><fmt:message key="${task.status.typeKey}"/>:</div>
	<div class="taskInput">
	<form:select path="status">
		<c:forEach items="${task.status.values()}" var="status">
			<c:set var="svalue"><fmt:message key="${status.nameKey}"/></c:set> 
			<form:option value="${status}" label="${svalue}"/>
		</c:forEach>
	</form:select>
	</div>
<!--ASSIGNED-------------------------------------------- -->
	<div class="taskDescription"><fmt:message key="tasks.assigned"/>:</div>
	<div class="taskInput">
		<form:select path="assigned">
			<form:option value="" label=""/>
			<c:forEach items="${users}" var="user">
				<c:choose>
					<c:when test="${user.getName().equals(assigned)}">
						<form:option value="${user.getName()}" label="${user.getName()}" selected="selected"/>
					</c:when>
					<c:otherwise>
						<form:option value="${user.getName()}" label="${user.getName()}" />
					</c:otherwise>
				</c:choose>
			</c:forEach>
		</form:select>
	</div>
</div>
<div class="taskElement left">
<!--TYPE-------------------------------------------- -->
	<div class="taskDescription"><fmt:message key="${task.type.typeKey}"/>:</div>
	<div class="taskInput">
		<form:select path="type">
			<c:forEach items="${task.type.values()}" var="type">
				<c:set var="tvalue"><fmt:message key="${type.nameKey}"/></c:set> 
				<form:option value="${type}" label="${tvalue}"/>
			</c:forEach>
		</form:select>
	</div>
<!--TYPE-------------------------------------------- -->
	<div class="taskDescription">Path:</div>
	<div class="taskInput">
		<form:input path="assetPath" value=""/>
	</div>
</div>