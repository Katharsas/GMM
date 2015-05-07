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
    	<script src="<c:url value="/res/javascript/tasks/tasks.js"/>" type="text/javascript"></script>
    	<script type="text/javascript">
			tasksVars['tab'] = '${fn:escapeXml(cfn:escapeJS(tab))}';
		</script>
    </jsp:attribute>
	<jsp:attribute name="css">
		<link href="<c:url value="/res/css/compiled/tasks.css"/>" media="screen" rel="stylesheet" type="text/css" />
    </jsp:attribute>

    <jsp:body>
    	<div id="wrapper">
    		<div id="main">
    			
    			<!-- 
					task form
					###############################################
				-->
				<div id="newTaskButton" class="button">
					<fmt:message key="tasks.new"/>
				</div>
				<form:form id="taskForm" method="POST"
						action="${contextUrl}/tasks/submitTask?edit=${edit}"
						commandName="taskForm"
						style="display: none;">
					<t:all_taskForm>
					</t:all_taskForm>
				</form:form>
				<div class="clear"></div>
				<div class="taskForm-buttons">
					<div id="submitTaskButton" class="button left" style="display: none;">
						<fmt:message key="tasks.submit"/>
					</div>
					<div id="cancelTaskButton" class="button left" style="display: none;">
						<fmt:message key="tasks.new.cancel"/>
					</div>
					<div class="clear"></div>
				</div>
    			
    			<!-- 
					pinned & current operation
					###############################################
				-->
    			<!-- <div id="pinned" class="list">
	    			<div class="list-header">Pinned</div>
	    			<div class="list-body">
	    				<div class="task">
	    					<div class="task-header">Task 1</div>
	    				</div>
	    			</div>
	    		</div>
    			<div id="currentOperation" class="list">
	    			<div class="list-header">%currentOperation%</div>
	    			<div class="list-body">
	    				<div class="task">
	    					<div class="task-header">Task 1</div>
	    				</div>
	    			</div>
	    		</div> -->
	    		
    			<div id="workbench" class="list">
	    			<div class="list-header">Workbench</div>
	    			
	    			<!-- 
						workbench tabs
						###############################################
					-->
	    			
	    			<div id="workbench-menu">
	    				<div class="workbench-menu-tab">Load</div>
	    				<div class="workbench-menu-tab">Sort</div>
	    				<div class="workbench-menu-tab">Search</div>
	    				<div class="workbench-menu-tab workbench-menu-tab-last">Filter</div>
<!-- 	    				<div class="workbench-menu-tab">More</div> -->
<!-- 	    				<div class="workbench-menu-tab">Admin</div> -->
	    				<div class="clear"></div>
	    			</div>
	    			<div id="workbench-tabs">
		    			<div id="workbench-tab-load" class="workbench-tab">
		    				<t:workbench_load>
		    				</t:workbench_load>
		    			</div>
		    			<div id="workbench-tab-sort" class="workbench-tab">
		    				<t:workbench_sort>
		    				</t:workbench_sort>
		    			</div>
		    			<div id="workbench-tab-search" class="workbench-tab">
		    				<t:workbench_search>
		    				</t:workbench_search>
		    			</div>
		    			<div id="workbench-tab-filter" class="workbench-tab">
		    				${workbench_filters}
		    			</div>
		    			<div id="workbench-tab-more" class="workbench-tab">
		    				[] Link tasks currently visible in workbench<br>
		    				[] Edit tasks currently visible in workbench
		    			</div>
		    			<div id="workbench-tab-admin" class="workbench-tab">
		    				[] Save tasks currently visible in workbench<br>
		    				[] Delete tasks currently visible in workbench
		    			</div>
	    			</div>
	    			<!-- 
						workbench list
						###############################################
					-->
	    			
	    			<div class="list-body">
	    				<div class="list-count center">
							<span></span> <fmt:message key="elements"/> 
						</div>
	    			</div>
	    		</div>
	    		
    		</div>
    	</div>
    </jsp:body>
</t:all_template>