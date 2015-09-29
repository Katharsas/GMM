<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/tags/all_include.tagf" %>
<% pageContext.setAttribute("newLine", "\n"); %>

<t:all_template>

	<jsp:attribute name="js">
		<script src="<c:url value="/res/javascript/lib/three.min.js"/>" type="text/javascript"></script>
    	<script src="<c:url value="/res/javascript/lib/OrbitControls.js"/>" type="text/javascript"></script>
    	<script src="<c:url value="/res/javascript/lib/TweenLite.min.js"/>" type="text/javascript"></script>
    	<script src="<c:url value="/res/javascript/lib/CSSPlugin.min.js"/>" type="text/javascript"></script>
    	<script src="<c:url value="/res/javascript/compiled/links.bundle.js"/>" type="text/javascript"></script>
    </jsp:attribute>
	<jsp:attribute name="css">
		<link href="<c:url value="/res/css/compiled/tasks.css"/>" media="screen" rel="stylesheet" type="text/css" />
    	<link href="<c:url value="/res/css/compiled/links.css"/>" media="screen" rel="stylesheet" type="text/css" />
    </jsp:attribute>

    <jsp:body>
<!-- 		<div id="taskList" class="subTabbody tabbody activeSubpage"> -->
<!-- 		</div> -->
		<div id="taskList" class="list list-body">
		</div>
    </jsp:body>
</t:all_template>