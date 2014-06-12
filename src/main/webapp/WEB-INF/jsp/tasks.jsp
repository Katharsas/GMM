<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/tags/all_include.tagf" %>
<% pageContext.setAttribute("newLine", "\n"); %>

<t:all_template>

	<jsp:attribute name="js">
		<script src="res/javascript/lib/three.min.js" type="text/javascript"></script>
    	<script src="res/javascript/lib/OrbitControls.js" type="text/javascript"></script>
    	<script src="res/javascript/tasks.js" type="text/javascript"></script>
    	<script src="res/javascript/jqueryFileTree.js" type="text/javascript"></script>
    </jsp:attribute>
	<jsp:attribute name="css">
		<link id="css_link" href="res/css/taskForm.css" media="screen" rel="stylesheet" type="text/css" />
    	<link id="css_link" href="res/css/tasks.css" media="screen" rel="stylesheet" type="text/css" />
    	<link id="css_link" href="res/css/tasks_filters.css" media="screen" rel="stylesheet" type="text/css" />
    	<link id="css_link" href="res/css/tasks_search.css" media="screen" rel="stylesheet" type="text/css" />
    	<link id="css_link" href="res/css/jqueryFileTree.css" media="screen" rel="stylesheet" type="text/css" />
    </jsp:attribute>

    <jsp:body>
		<div class="table">
			<div class="tr">
				<div class="td">
				</div>
				<div class="td">
<!-- New Task Area -->
					<div id="generalArea">
						<div id="newTaskButton" class="button pageButton">
							<fmt:message key="tasks.new"/>
						</div>
						<form:form id="taskForm" method="POST"
								action="tasks/submitTask?tab=${tab}&edit=${edit}"
								commandName="task"
								style="display: none;">
							<t:all_taskForm>
							</t:all_taskForm>
						</form:form>
						<div class="clear"></div>
						<div class="taskButtons">
							<div id="submitTaskButton" class="button pageButton left" style="display: none;">
								Submit Task
							</div>
							<div id="cancelTaskButton" class="button pageButton left" style="display: none;">
								<a href="?tab=${tab}">
									<fmt:message key="tasks.new.cancel"/><span></span>
								</a>
							</div>
							<div class="clear"></div>
						</div>
					</div>
				</div>
			</div>
			<div class="tr">
				<div class="td">
<!-- Side Filter Area -->
					<t:tasks_filters>
					</t:tasks_filters>
				</div>
				<div class="td">
<!-- Task List Area -->
					<t:tasks_lists newLine="${newLine}">
					</t:tasks_lists>
				</div>
			</div>
		</div>
    </jsp:body>
</t:all_template>