<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/tags/all_include.tagf" %>
<% pageContext.setAttribute("newLine", "\n"); %>

<t:all_template>

	<jsp:attribute name="js">
		<script type="text/javascript">
    		var tasksHTML = {};
    		tasksHTML['tab'] = '${fn:escapeXml(cfn:escapeJS(tab))}';
		</script>
		<script src="<c:url value="/res/javascript/compiled/three.bundle.js"/>" type="text/javascript"></script>
    	<script src="<c:url value="/res/javascript/lib/OrbitControls.js"/>" type="text/javascript"></script>
    	<script src="<c:url value="/res/javascript/lib/TweenLite.min.js"/>" type="text/javascript"></script>
    	<script src="<c:url value="/res/javascript/lib/CSSPlugin.min.js"/>" type="text/javascript"></script>
    	<script src="<c:url value="/res/javascript/compiled/tasks.bundle.js"/>" type="text/javascript"></script>
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
						modelAttribute="taskForm"
						style="display: none;">
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
    			<div id="pinned" class="list">
	    			<div class="list-header"><fmt:message key="tasks.pinned"/></div>
	    			<div class="list-body" tabindex="-1">
	    			</div>
	    		</div>
    			<!-- <div id="currentOperation" class="list">
	    			<div class="list-header">%currentOperation%</div>
	    			<div class="list-body">
	    				<div class="task">
	    					<div class="task-header">Task 1</div>
	    				</div>
	    			</div>
	    		</div> -->
	    		
    			<div id="workbench" class="list">
	    			<div class="list-header"><fmt:message key="tasks.workbench"/></div>
	    			
	    			<!-- 
						workbench tabs
						###############################################
					-->
	    			
	    			<div id="workbench-menu">
	    				<c:set var="tabId" value="load"/>
	    				<div class="workbench-menu-tab"  data-tabid="${tabId}"><fmt:message key="workbench.menu.tab.${tabId}"/></div>
	    				<c:set var="tabId" value="sort"/>
	    				<div class="workbench-menu-tab"  data-tabid="${tabId}"><fmt:message key="workbench.menu.tab.${tabId}"/></div>
	    				<c:set var="tabId" value="search"/>
	    				<div class="workbench-menu-tab"  data-tabid="${tabId}"><fmt:message key="workbench.menu.tab.${tabId}"/></div>
	    				<c:set var="tabId" value="filter"/>
	    				<div class="workbench-menu-tab"  data-tabid="${tabId}"><fmt:message key="workbench.menu.tab.${tabId}"/></div>
						<sec:authorize access="hasRole('ROLE_ADMIN')">
							<c:set var="tabId" value="admin"/>
	    					<div class="workbench-menu-tab"  data-tabid="${tabId}"><fmt:message key="workbench.menu.tab.${tabId}"/></div>
	    				</sec:authorize>
	    				<div class="clear"></div>
	    			</div>
	    			<div id="workbench-tabs">
	    				<c:set var="tabId" value="load"/>
		    			<div id="workbench-tab-${tabId}" class="workbench-tab" data-tabid="${tabId}">
		    			</div>
		    			<c:set var="tabId" value="sort"/>
		    			<div id="workbench-tab-${tabId}" class="workbench-tab" data-tabid="${tabId}">
		    				<t:workbench_sort>
		    				</t:workbench_sort>
		    			</div>
		    			<c:set var="tabId" value="search"/>
		    			<div id="workbench-tab-${tabId}" class="workbench-tab" data-tabid="${tabId}">
		    			</div>
		    			<c:set var="tabId" value="filter"/>
		    			<div id="workbench-tab-${tabId}" class="workbench-tab" data-tabid="${tabId}">
		    			</div>
		    			<sec:authorize access="hasRole('ROLE_ADMIN')">
			    			<c:set var="tabId" value="admin"/>
			    			<div id="workbench-tab-${tabId}" class="workbench-tab" data-tabid="${tabId}">
			    				<t:workbench_admin>
			    				</t:workbench_admin>
			    			</div>
		    			</sec:authorize>
	    			</div>
	    			<!-- 
						workbench list
						###############################################
					-->
	    			
	    			<div class="list-body" tabindex="-1">
	    				<div class="list-count center">
							<span></span> <fmt:message key="elements"/> 
						</div>
	    			</div>
	    		</div>
	    		
    		</div>
    	</div>
    </jsp:body>
</t:all_template>