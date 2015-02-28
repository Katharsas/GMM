<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/tags/all_include.tagf" %>
<% pageContext.setAttribute("newLine", "\n"); %>

<t:all_template>

	<jsp:attribute name="js">
		<script src="<c:url value="/res/javascript/lib/three.min.js"/>" type="text/javascript"></script>
    	<script src="<c:url value="/res/javascript/lib/OrbitControls.js"/>" type="text/javascript"></script>
    	<script src="<c:url value="/res/javascript/lib/TweenLite.min.js"/>" type="text/javascript"></script>
    	<script src="<c:url value="/res/javascript/lib/CSSPlugin.min.js"/>" type="text/javascript"></script>
    	
    	<script src="<c:url value="/res/javascript/all/jqueryFileTree.js"/>" type="text/javascript"></script>
    	<script src="<c:url value="/res/javascript/all/queue.js"/>" type="text/javascript"></script>
    	<script src="<c:url value="/res/javascript/all/taskloader.js"/>" type="text/javascript"></script>
    	<script src="<c:url value="/res/javascript/all/taskswitcher.js"/>" type="text/javascript"></script>
    	<script src="<c:url value="/res/javascript/all/tasklisteners.js"/>" type="text/javascript"></script>
    	<script src="<c:url value="/res/javascript/all/sidebarMarkers.js"/>" type="text/javascript"></script>
    	<script src="<c:url value="/res/javascript/tasks/tasksnew.js"/>" type="text/javascript"></script>
    	<script type="text/javascript">
			tasksVars['tab'] = '${fn:escapeXml(cfn:escapeJS(tab))}';
		</script>
    </jsp:attribute>
	<jsp:attribute name="css">
		<link href="<c:url value="/res/css/compiled/tasksnew.css"/>" media="screen" rel="stylesheet" type="text/css" />
    </jsp:attribute>

    <jsp:body>
    	<div id="wrapper">
    		<div id="main">
    			<div id="pinned" class="list">
	    			<div class="list-header">Pinned</div>
	    			<div class="list-body">
	    				<div class="list-element">Task 1</div>
	    				<div class="list-element">Task 2</div>
	    				<div class="list-element">Task 3</div>
	    			</div>
	    		</div>
    			<div id="currentOperation" class="list">
	    			<div class="list-header">%currentOperation%</div>
	    			<div class="list-body">
	    				<div class="list-element">Task 1</div>
	    				<div class="list-element">Task 2</div>
	    			</div>
	    		</div>
    			<div id="workbench" class="list">
	    			<div class="list-header">Workbench</div>
	    			<div id="workbenchMenu">
	    				<div class="workbenchMenu-tab workbenchMenu-tab-active">Load Type</div>
	    				<div class="workbenchMenu-tab">Sort By</div>
	    				<div class="workbenchMenu-tab">Search</div>
	    				<div class="workbenchMenu-tab workbenchMenu-tab-last">More</div>
	    				<div class="clear"></div>
	    			</div>
	    			<div class="workbenchTab">
	    				Tab Content<br>Tab Content
	    			</div>
	    			<div class="list-body">
	    				<div class="list-element">Task 1</div>
	    				<div class="list-element">Task 2</div>
	    				<div class="list-element">Task 3</div>
	    				<div class="list-element">Task 4</div>
	    				<div class="list-element">Task 5</div>
	    				<div class="list-element">Task 3</div>
	    				<div class="list-element">Task 4</div>
	    				<div class="list-element">Task 5</div>
	    				<div class="list-element">Task 1</div>
	    				<div class="list-element">Task 2</div>
	    				<div class="list-element">Task 3</div>
	    				<div class="list-element">Task 4</div>
	    				<div class="list-element">Task 5</div>
	    				<div class="list-element">Task 1</div>
	    				<div class="list-element sidebar-marked" id="test1">Task Important</div>
	    				<div class="list-element">Task 3</div>
	    				<div class="list-element">Task 4</div>
	    				<div class="list-element">Task 5</div>
	    				<div class="list-element">Task 1</div>
	    				<div class="list-element">Task 2</div>
	    				<div class="list-element">Task 3</div>
	    				<div class="list-element">Task 4</div>
	    				<div class="list-element sidebar-marked" id="test2">Task Important 2</div>
	    				<div class="list-element">Task 1</div>
	    				<div class="list-element">Task 2</div>
	    				<div class="list-element">Task 3</div>
	    				<div class="list-element">Task 4</div>
	    				<div class="list-element">Task 5</div>
	    				<div class="list-element">Task 1</div>
	    				<div class="list-element">Task 2</div>
	    				<div class="list-element">Task 3</div>
	    				<div class="list-element">Task 4</div>
	    				<div class="list-element">Task 5</div>
	    				<div class="list-element">Task 1</div>
	    				<div class="list-element">Task 2</div>
	    				<div class="list-element">Task 3</div>
	    				<div class="list-element">Task 4</div>
	    				<div class="list-element">Task 5</div>
	    				<div class="list-element">Task 3</div>
	    				<div class="list-element">Task 4</div>
	    				<div class="list-element">Task 5</div>
	    				<div class="list-element">Task 1</div>
	    				<div class="list-element">Task 2</div>
	    				<div class="list-element">Task 3</div>
	    				<div class="list-element">Task 4</div>
	    				<div class="list-element">Task 5</div>
	    			</div>
	    		</div>
    		</div>
    	</div>
    		
<!-- 		<div class="table"> -->
<!-- 			<div class="tr"> -->
<!-- 				<div class="td leftColumn"> -->
<!-- 				</div> -->
<!-- 				<div class="td rightColumn"> -->
<!-- New Task Area -->
<!-- 					<div id="generalArea"> -->
<!-- 						<div id="newTaskButton" class="button pageButton"> -->
<%-- 							<fmt:message key="tasks.new"/> --%>
<!-- 						</div> -->
<%-- 						<form:form id="taskForm" method="POST" --%>
<%-- 								action="tasks/submitTask?tab=${tab}&edit=${edit}" --%>
<%-- 								commandName="task" --%>
<%-- 								style="display: none;"> --%>
<%-- 							<t:all_taskForm> --%>
<%-- 							</t:all_taskForm> --%>
<%-- 						</form:form> --%>
<!-- 						<div class="clear"></div> -->
<!-- 						<div class="taskButtons"> -->
<!-- 							<div id="submitTaskButton" class="button pageButton left" style="display: none;"> -->
<%-- 								<fmt:message key="tasks.submit"/> --%>
<!-- 							</div> -->
<!-- 							<div id="cancelTaskButton" class="button pageButton left" style="display: none;"> -->
<%-- 								<a href="?tab=${tab}"> --%>
<%-- 									<fmt:message key="tasks.new.cancel"/><span></span> --%>
<!-- 								</a> -->
<!-- 							</div> -->
<!-- 							<div class="clear"></div> -->
<!-- 						</div> -->
<!-- 					</div> -->
<!-- 				</div> -->
<!-- 			</div> -->
<!-- 			<div class="tr"> -->
<!-- 				<div class="td leftColumn"> -->
<!-- Side Filter Area -->
<%-- 					<t:tasks_filters> --%>
<%-- 					</t:tasks_filters> --%>
<!-- 				</div> -->
<!-- 				<div class="td rightColumn"> -->
<!-- Task List Area -->
<%-- 					<t:tasks_lists newLine="${newLine}"> --%>
<%-- 					</t:tasks_lists> --%>
<!-- 				</div> -->
<!-- 			</div> -->
<!-- 		</div> -->
    </jsp:body>
</t:all_template>