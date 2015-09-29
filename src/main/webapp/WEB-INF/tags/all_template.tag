<%@ include file="/WEB-INF/tags/all_include.tagf" %>
<%-- Tag Description --%>
<%@tag description="template page" pageEncoding="UTF-8"%>
<%@attribute name="js" fragment="true" %>
<%@attribute name="css" fragment="true" %>

<%-- Template Start --%>
<html>
	<head>
		<!-- Stuff -->
			<title><fmt:message key="all_template.GMMtasks"/></title>
			<meta http-equiv="Content-Type" content="text/html" charset="utf-8" />
			<meta name="_csrf" content="${_csrf.token}"/>
			<!-- default header name is X-CSRF-TOKEN -->
			<meta name="_csrf_header" content="${_csrf.headerName}"/>
		<!-- Javascript -->
			<script type="text/javascript">
				var contextUrl = '${contextUrl}';
				var allVars = [];
				allVars['adminBanner'] = '${fn:escapeXml(cfn:escapeJS(combinedData.customAdminBanner))}';
			</script>
			<script src="<c:url value="/res/javascript/lib/jquery-2.1.1.js"/>" type="text/javascript"></script>
<!-- 			<script src="//code.jquery.com/jquery-2.1.1.min.js" type="text/javascript"></script> -->
			<script src="<c:url value="/res/javascript/lib/jquery.form.min.js"/>" type="text/javascript"></script>
			<jsp:invoke fragment="js"/>
		<!-- CSS -->
			<link href="<c:url value="/res/css/compiled/all_template.css"/>" media="screen" rel="stylesheet" type="text/css" />
			<jsp:invoke fragment="css"/>
	</head>
	
	<body>
		<div id="page-background"></div>
			
		<!-- 
			###############################################################
			Overlay & Dialogs
			###############################################################
		-->
		<div id="overlay"></div>
		<%@ include file="/WEB-INF/tags/all_dialogs.tagf" %>
		
		<!-- 
			###############################################################
			No Javascript Warning
			###############################################################
		-->
		<noscript>
			<div class="blocker">
				<div class="warning">
					<p><fmt:message key="noscript"/><br/></p>
				</div>
			</div>
		</noscript>
		
		<!-- 
			###############################################################
			Top Menu
			###############################################################
		-->
		<div id="page-tabmenu" class="tabmenu h3">
			<sec:authorize access="hasRole('ROLE_USER')">
				<div class="tab left">
					<a href="<c:url value="/tasks"/>"><fmt:message key="menu.tasks"/><span></span></a>
				</div>
			</sec:authorize>
			<div id="metainfo" class="left">
				<spring:eval expression="@config.getProperty('app.name')" /><br/>
				<spring:eval expression="@config.getProperty('app.version')" />
			</div>
			<sec:authorize access="isAnonymous()">
				<div class="tab right">
					<a href="<c:url value="/login"/>">Login<span></span></a>
				</div>
			</sec:authorize>
			<sec:authorize access="hasRole('ROLE_USER')">
				<div class="tab right">
					<a id="logout" class="clickable"><fmt:message key="all_template.logout"/><span></span></a>
				</div>
				<div class="tab right">
					<a href="<c:url value="/profile"/>"><fmt:message key="menu.profile"/><span></span></a>
				</div>
			</sec:authorize>
			<sec:authorize access="hasRole('ROLE_ADMIN')">
				<div class="tab right">
					<a href="<c:url value="/admin"/>"><fmt:message key="menu.admin"/><span></span></a>
				</div>
			</sec:authorize>
			
			<div class="clear"></div>
		</div>
		<div id ="page-tabmenu-spacer"></div>
		
		<!-- 
			###############################################################
			Custom Admin Banner
			###############################################################
		-->
		<c:if test="${combinedData.isCustomAdminBannerActive()}">
			<div id="customAdminBanner" class="center">
			</div>
		</c:if>
	
		<!-- 
			###############################################################
			Dynamic Body
			###############################################################
		-->
		<div id="page">
			<jsp:doBody/>
		</div>
		
	</body>
</html>
<%-- Template End --%>