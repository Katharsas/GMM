<%@ include file="/WEB-INF/tags/include.tagf" %>
<%-- Tag Description --%>
<%@tag description="template page" pageEncoding="UTF-8"%>

<div class="taskElement left">
<!--PRIORITY------------------------------------- -->
	<div class="taskDescription">Priority:</div>
	<div class="taskInput">
	<form:select path="priority">
		<c:forEach items="${priorities}" var="priority">
			<c:set var="pvalue"><fmt:message key="${priority.getMessageKey()}"/></c:set> 
			<form:option value="${priority}" label="${pvalue}"/>
		</c:forEach>
	</form:select>
	</div>
<!--TITLE------------------------------------------ -->
	<div class="taskDescription">Title:</div>
	<div class="taskInput">
		<form:input path="idName" value=""/>
	</div>
</div>
<div class="taskElement left">
<!--LABEL------------------------------------ -->
	<div class="taskDescription">Label:</div>
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
	<div class="taskDescription">Details:</div>
	<div class="taskInput">
	<form:textarea rows="2" cols="1" path="details"></form:textarea>
	</div>
</div>
<div class="taskElement left">
<!--STATUS-------------------------------------------- -->
	<div class="taskDescription">Status:</div>
	<div class="taskInput">
	<form:select path="status">
		<c:forEach items="${taskStatuses}" var="status">
			<c:set var="svalue"><fmt:message key="${status.getMessageKey()}"/></c:set> 
			<form:option value="${status}" label="${svalue}"/>
		</c:forEach>
	</form:select>
	</div>
<!--ASSIGNED-------------------------------------------- -->
	<div class="taskDescription">Assigned:</div>
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