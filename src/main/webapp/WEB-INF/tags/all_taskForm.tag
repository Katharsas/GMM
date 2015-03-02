<%@ include file="/WEB-INF/tags/all_include.tagf" %>
<%-- Tag Description --%>
<%@tag description="template page" pageEncoding="UTF-8"%>

<div class="taskForm-group">
	<!-- ####################### PRIORITY -->
	<div class="taskForm-element">
		<div class="taskForm-description"><fmt:message key="${task.priority.typeKey}"/>:</div>
		<div class="taskForm-input input">
		<form:select path="priority">
			<c:forEach items="${task.priority.values()}" var="priority">
				<c:set var="pvalue">
					<fmt:message key="${priority.nameKey}"/>
				</c:set> 
				<form:option value="${priority}" label="${pvalue}"/>
			</c:forEach>
		</form:select>
		</div>
	</div>
	<!-- ####################### TITLE -->
	<div class="taskForm-element">
		<div class="taskForm-description"><fmt:message key="tasks.title"/>:</div>
		<div class="taskForm-input input">
			<form:input path="name" value=""/>
		</div>
	</div>
</div>
<div class="taskForm-group">
	<!-- ####################### LABEL -->
	<div class="taskForm-element">
		<div class="taskForm-description"><fmt:message key="tasks.label"/>:</div>
		<div class="taskForm-input input">
			<form:input path="label" value="" autocomplete="off"/>
		</div>
	</div>
	<div class="taskForm-element">
		<div class="taskForm-description">&#160;</div>
		<div class="taskForm-input input">
			<form:select path="labelSelect"> 
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
</div>
<!-- ####################### DETAILS -->
<div class="taskForm-group textArea">
	<div class="taskForm-element">
		<div class="taskForm-description"><fmt:message key="tasks.details"/>:</div>
		<div class="taskForm-input input">
		<form:textarea rows="4" cols="1" path="details"></form:textarea>
		</div>
	</div>
</div>
<div class="taskForm-group">
	<!-- ####################### STATUS -->
	<div class="taskForm-element">
		<div class="taskForm-description"><fmt:message key="${task.status.typeKey}"/>:</div>
		<div class="taskForm-input input">
		<form:select path="status">
			<c:forEach items="${task.status.values()}" var="status">
				<c:set var="svalue"><fmt:message key="${status.nameKey}"/></c:set> 
				<form:option value="${status}" label="${svalue}"/>
			</c:forEach>
		</form:select>
		</div>
	</div>
	<!-- ####################### ASSIGNED -->
	<div class="taskForm-element">
		<div class="taskForm-description"><fmt:message key="tasks.assigned"/>:</div>
		<div class="taskForm-input input">
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
</div>
<div class="taskForm-group">
	<!-- ####################### TYPE -->
	<div class="taskForm-element">
		<div class="taskForm-description"><fmt:message key="${task.type.typeKey}"/>:</div>
		<div class="taskForm-input input">
			<form:select path="type">
				<c:forEach items="${task.type.values()}" var="type">
					<c:set var="tvalue"><fmt:message key="${type.nameKey}"/></c:set> 
					<form:option value="${type}" label="${tvalue}"/>
				</c:forEach>
			</form:select>
		</div>
	</div>
	<!-- ####################### PATH -->
	<div class="taskForm-element">
		<div class="taskForm-hint">
			<fmt:message key="all_taskForm.text"/><br/>
			<fmt:message key="all_taskForm.text2"/><br/><br/>
			<fmt:message key="all_taskForm.text3"/>
		</div>
		<div class="taskForm-description"><fmt:message key="all_taskForm.file"/></div>
		<div class="taskForm-input input">
			<form:input path="assetPath" value=""/>
		</div>
	</div>
</div>