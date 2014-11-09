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
		<!-- Javascript -->
			<script src="<c:url value="/res/javascript/lib/jquery-2.1.1.js"/>" type="text/javascript"></script>
<!-- 			<script src="//code.jquery.com/jquery-2.1.1.min.js" type="text/javascript"></script> -->
			<script src="<c:url value="/res/javascript/lib/jquery.form.min.js"/>" type="text/javascript"></script>
			<script src="<c:url value="/res/javascript/default.js"/>" type="text/javascript"></script>
			<script type="text/javascript">
				allVars['adminBanner'] = '${fn:escapeXml(cfn:escapeJS(combinedData.customAdminBanner))}';
				allVars['contextPath'] = '${pageContext.request.contextPath}';
			</script>
			<jsp:invoke fragment="js"/>
		<!-- CSS -->
			<link href="<c:url value="/res/css/all/default.css"/>" media="screen" rel="stylesheet" type="text/css" />
			<link href="<c:url value="/res/css/all/dialog.css"/>" media="screen" rel="stylesheet" type="text/css" />
			<jsp:invoke fragment="css"/>
	</head>
	
	<body class="noPage">
		<div id="wrap">
	
			<div id="overlay"></div>
			<div id="confirmDialog" class="dialogContainer">
				<p id="confirmDialogMessage" class="center"></p>
				<div id="confirmDialogTextInputWrapper">
					<input id="confirmDialogTextInput" class="center"/>
				</div>
				<textarea id="confirmDialogTextArea">
				</textarea>
				<div class="dialogButton confirmOk button left" onclick="confirmOk()"><fmt:message key="all_template.confirm"/></div>
				<div class="dialogButton confirmCancel button right" onclick="hideDialogue()"><fmt:message key="all_template.cancel"/></div>
				<div class="clear"></div>
			</div>
			<div id="exceptionDialog" class="dialogContainer">
				<div id="exceptionHeading">
					<fmt:message key="all_template.error"/>
				</div>
				<div id="exceptionMessage"></div>
				<div id="exceptionInstructions">
					<fmt:message key="all_template.errortext"/><br>
					<fmt:message key="all_template.errortext2"/><br>
					<br>
					<fmt:message key="all_template.errormessage"/>
				</div>
				<div id="exceptionStackTraceWrapper">
					<pre id="exceptionStackTrace"></pre>
				</div>
				<div class="dialogButton button" onclick="window.location.reload()">
					<fmt:message key="all_template.reload"/>
				</div>
			</div>
			<div id="bundledMessageDialog" class="dialogContainer">
       			<p><fmt:message key="all_template.loading"/></p>
       			<p id="conflictMessage"></p>
       			<div id="messageList" class="dialogList"><ul></ul></div>
       			<div id="conflictOptions">
        			<div id="skipButton" class="left dialogButton button" onclick="ajaxChannel.answer('skip');"><fmt:message key="admin.database.skip"/></div>
        			<!-- Load Tasks -->
        			<div id="overwriteTaskButton" class="left dialogButton button" onclick="ajaxChannel.answer('overwrite');"><fmt:message key="admin.database.overwrite"/></div>
        			<div id="addBothTasksButton" class="left dialogButton button" onclick="ajaxChannel.answer('both');"><fmt:message key="admin.database.keepboth"/></div>
        			<!-- Import Assets -->
        			<div id="overwriteTaskAquireDataButton" class="left dialogButton button" onclick="ajaxChannel.answer('overwriteTaskAquireData');"><fmt:message key="all_template.acquire"/></div>
        			<div id="overwriteTaskDeleteDataButton" class="left dialogButton button" onclick="ajaxChannel.answer('overwriteTaskDeleteData');"><fmt:message key="all_template.delete"/></div>
        			<div id="aquireDataButton" class="left dialogButton button" onclick="ajaxChannel.answer('aquireData');"><fmt:message key="all_template.acquire"/></div>
        			<div id="deleteDataButton" class="left dialogButton button" onclick="ajaxChannel.answer('deleteData');"><fmt:message key="all_template.delete"/></div>
        			<div class="clear"></div>
        			<label id="doForAllCheckbox">
        				<input type="checkbox" name="doForAll" value="doForAll"><fmt:message key="admin.database.dothesame"/><br>
       				</label>
       			</div>
       			<div id="finishLoadingButton" class="dialogButton button" onclick="ajaxChannel.finish()"><fmt:message key="admin.database.finish"/></div>
       		</div>
		
		
			<!-- No Javascript Warning -->
			<!-- TODO: Test -->
			<noscript>
				<div class="blocker">
					<div class="warning">
						<p><fmt:message key="noscript"/><br/></p>
					</div>
				</div>
			</noscript>
			<!-- End of No Javascript Warning -->
		
			<!-- Top Tab-Menu -->
			<div id="top" class="pageTabmenu tabmenu inactivePage h3">
				<sec:authorize access="hasRole('ROLE_USER')">
					<div class="tab left">
						<a href="<c:url value="/tasks"/>"><fmt:message key="menu.tasks"/><span></span></a>
					</div>
				</sec:authorize>
				<div class="meta left">
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
						<a href="<c:url value="/logout"/>"><fmt:message key="all_template.logout"/><span></span></a>
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
			<div id ="menuSpacer"></div>
			<!-- End of Top Tab-Menu -->
			
			<c:if test="${combinedData.isCustomAdminBannerActive()}">
				<div id="customAdminBanner" class="center">
				</div>
			</c:if>
		
			<!-- Body of Task Tab -->
			<div id="main" class="pageTabbody tabbody activePage">
				<jsp:doBody/>
			</div>
			<!-- End of body of Task Tab -->
			
		</div>
		
	</body>
</html>
<%-- Template End --%>