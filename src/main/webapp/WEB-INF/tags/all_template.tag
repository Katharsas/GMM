<%@ include file="/WEB-INF/tags/all_include.tagf" %>
<%-- Tag Description --%>
<%@tag description="template page" pageEncoding="UTF-8"%>
<%@attribute name="js" fragment="true" %>
<%@attribute name="css" fragment="true" %>

<%-- Template Start --%>
<html>
	<head>
		<!-- Stuff -->
			<title>GMM - Tasks</title>
			<meta http-equiv="Content-Type" content="text/html" charset="utf-8" />
		<!-- Javascript -->
			<script src="res/javascript/lib/jQuery.js" type="text/javascript"></script>
<!-- 			<script defer src="//code.jquery.com/jquery-1.9.1.min.js" type="text/javascript"></script> -->
<!-- 			<script defer src="//code.jquery.com/jquery-2.1.1.min.js" type="text/javascript"></script> -->
			<script src="res/javascript/lib/jquery.form.min.js" type="text/javascript"></script>
			<script src="res/javascript/default.js" type="text/javascript"></script>
			<script type="text/javascript">
				allVars['adminBanner'] = '${fn:escapeXml(cfn:escapeJS(combinedData.customAdminBanner))}';
			</script>
			<jsp:invoke fragment="js"/>
		<!-- CSS -->
			<link id="css_link" href="res/css/default.css" media="screen" rel="stylesheet" type="text/css" />
			<link id="css_link" href="res/css/dialog.css" media="screen" rel="stylesheet" type="text/css" />
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
				<div class="dialogButton confirmOk button left" onclick="confirmOk()">Confirm</div>
				<div class="dialogButton confirmCancel button right" onclick="hideDialogue()">Cancel</div>
				<div class="clear"></div>
			</div>
			<div id="exceptionDialog" class="dialogContainer">
				<div id="exceptionHeading">
					Internal Server Error 500
				</div>
				<div id="exceptionMessage"></div>
				<div id="exceptionInstructions">
					Please copy text below and send it to somebody reponsible for this app.<br>
					Please also add an description of what you did before receiving this error.<br>
					<br>
					Error Message:
				</div>
				<div id="exceptionStackTraceWrapper">
					<pre id="exceptionStackTrace"></pre>
				</div>
				<div class="dialogButton button" onclick="window.location.reload()">
					Reload Page
				</div>
			</div>
			<div id="bundledMessageDialog" class="dialogContainer">
       			<p>Loading:</p>
       			<p id="conflictMessage"></p>
       			<div id="messageList" class="dialogList"><ul></ul></div>
       			<div id="conflictOptions">
        			<div id="skipButton" class="left dialogButton button" onclick="ajaxChannel.answer('skip');"><fmt:message key="admin.database.skip"/></div>
        			<!-- Load Tasks -->
        			<div id="overwriteTaskButton" class="left dialogButton button" onclick="ajaxChannel.answer('overwrite');"><fmt:message key="admin.database.overwrite"/></div>
        			<div id="addBothTasksButton" class="left dialogButton button" onclick="ajaxChannel.answer('both');"><fmt:message key="admin.database.keepboth"/></div>
        			<!-- Import Assets -->
        			<div id="overwriteTaskAquireDataButton" class="left dialogButton button" onclick="ajaxChannel.answer('overwriteTaskAquireData');">Aquire Data (Overwrites Task)</div>
        			<div id="overwriteTaskDeleteDataButton" class="left dialogButton button" onclick="ajaxChannel.answer('overwriteTaskDeleteData');">Delete Data (Overwrites Task)</div>
        			<div id="aquireDataButton" class="left dialogButton button" onclick="ajaxChannel.answer('aquireData');">Aquire Data</div>
        			<div id="deleteDataButton" class="left dialogButton button" onclick="ajaxChannel.answer('deleteData');">Delete Data</div>
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
				<div class="tab left"><a href="tasks"><fmt:message key="menu.tasks"/><span></span></a></div>
				
				<div class="meta left">
					<spring:eval expression="@config.getProperty('app.name')" /><br/>
					<spring:eval expression="@config.getProperty('app.version')" />
				</div>
				
				<div class="tab right"><a href="<c:url value="j_spring_security_logout"/>">Logout<span></span></a></div>
				<div class="tab right"><a href="profile"><fmt:message key="menu.profile"/><span></span></a></div>
				
				<sec:authorize access="hasRole('ROLE_ADMIN')">
					<div class="tab right"><a href="admin"><fmt:message key="menu.admin"/><span></span></a></div>
				</sec:authorize>
				
				<div class="clear"></div>
			</div>
			<!-- End of Top Tab-Menu -->
			
			<c:if test="${!combinedData.customAdminBanner.equals('')}">
				<div id="customAdminBanner">
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