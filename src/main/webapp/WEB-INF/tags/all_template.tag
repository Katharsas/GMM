<%@ include file="/WEB-INF/tags/all_include.tagf" %>
<%-- Tag Description --%>
<%@tag description="template page" pageEncoding="UTF-8"%>
<%@attribute name="js" fragment="true" %>
<%@attribute name="css" fragment="true" %>

<%-- Template Start --%>
<!DOCTYPE html>
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
				var templateVars = [];
				templateVars['adminBanner'] = '${fn:escapeXml(cfn:escapeJS(customAdminBanner))}';
				<sec:authorize access="hasRole('ROLE_USER')">
					templateVars['isUserLoggedIn'] = ${fn:escapeXml(cfn:escapeJS(isUserLoggedIn))};
					<c:if test="${isUserLoggedIn}">
						templateVars['userIdLink'] = '${fn:escapeXml(cfn:escapeJS(principal.getIdLink()))}';
						templateVars['userName'] = '${fn:escapeXml(cfn:escapeJS(principal.getName()))}';
					</c:if>
				</sec:authorize>
			</script>
			<script src="<c:url value="/res/javascript/lib/bluebird.core.min.js"/>" type="text/javascript"></script>
			<script src="<c:url value="/res/javascript/lib/jquery.js"/>" type="text/javascript"></script>
			<script src="<c:url value="/res/javascript/lib/jquery.form.min.js"/>" type="text/javascript"></script>
			<script src="<c:url value="/res/javascript/lib/jquery-ui.min.js"/>" type="text/javascript"></script>
			<jsp:invoke fragment="js"/>
		<!-- CSS -->
			<link href="<c:url value="/res/css/lib/jquery-ui.min.css"/>" media="screen" rel="stylesheet" type="text/css" />
			<link href="<c:url value="/res/css/lib/jquery-ui.structure.min.css"/>" media="screen" rel="stylesheet" type="text/css" />
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
				<div id="notifications-toggle" class="tab left">
					<div id="notifications-icon" class="left">
						<img class="svg" src="<c:url value="/res/gfx/notification.svg"/>">
					</div>
					<div id="notifications-count" class="left">12</div>
					<div class="clear"></div>
				</div>
				<div class="tab left">
					<a href="<c:url value="/tasks"/>"><fmt:message key="menu.tasks"/><span></span></a>
				</div>
			</sec:authorize>
			<div id="metainfo" class="left">
				<spring:eval expression="@metaConfig.getProperty('app.name')" /><br/>
				<spring:eval expression="@metaConfig.getProperty('app.version')" />
			</div>
			<div id="loading" class="tabmenu-statusIcon left">
				<span class="tabmenu-statusIcon-visual">
					<img class="svg" src="<c:url value="/res/gfx/loading.svg"/>">
				</span>
			</div>
			<div id="assetOperations" class="tabmenu-statusIcon left">
				<span class="tabmenu-statusIcon-visual" title="Asset operations are currently running on server.">
					<img class="svg" src="<c:url value="/res/gfx/file.svg"/>">
				</span>
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
		<c:if test="${isCustomAdminBannerActive}">
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
		
		<!-- 
			###############################################################
			Notifications
			###############################################################
		-->
		<div id="notifications" style="display:none;">
			<div id="notifications-markRead" class="button" style="display: none;">Mark Read</div>
			<div class="notifications-list">
				<div id="notifications-new" class="notifications-container" style="display: none;"></div>
				<div id="notifications-old" class="notifications-container" style="display: none;"></div>
			</div>
			<div id="notifications-clearRead" class="button" style="">Clear Read</div>
   		</div>
		
	</body>
</html>
<%-- Template End --%>