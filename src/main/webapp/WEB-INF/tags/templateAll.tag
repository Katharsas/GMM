<%@ include file="/WEB-INF/tags/include.tagf" %>
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
			<script src="res/javascript/jQuery.js" type="text/javascript"></script>
			<script src="res/javascript/default.js" type="text/javascript"></script>
			<jsp:invoke fragment="js"/>
		<!-- CSS -->
			<link id="css_link" href="res/css/default.css" media="screen" rel="stylesheet" type="text/css" />
			<link id="css_link" href="res/css/dialog.css" media="screen" rel="stylesheet" type="text/css" />
			<jsp:invoke fragment="css"/>
	</head>
	
	<div id="overlay"></div>
	<div id="confirmDialog" class="dialogContainer">
		<p id="confirmDialogMessage" class="center"></p>
		<div id="confirmDialogTextInputWrapper">
			<input id="confirmDialogTextInput" class="center"/>
		</div>
		<div class="dialogButton keyEnter Button left" onclick="confirmOk()">Confirm</div>
		<div class="dialogButton Button right" onclick="hideDialogue()">Cancel</div>
		<div class="clear"></div>
	</div>
	
	<body class="noPage">
		<div id="wrap">
		
			<!-- No Javascript Warning -->
			<!-- TODO: Test -->
			<noscript>
				<div class="warning">
					<p><fmt:message key="noscript"/><br/></p>
				</div>
			</noscript>
			<!-- End of No Javascript Warning -->
		
			<!-- Top Tab-Menu -->
			<div id="top" class="pageTabmenu tabmenu inactivePage h2">
				<div class="tab left"><a href="tasks"><fmt:message key="menu.tasks"/><span></span></a></div>
<%-- 				<div class="tab left"><a href="notifications"><fmt:message key="menu.notifications"/><span></span></a></div> --%>
<%-- 				<div class="tab left"><a href="files"><fmt:message key="menu.files"/><span></span></a></div> --%>
				<div class="tab right"><a href="<c:url value="j_spring_security_logout"/>">Logout<span></span></a></div>
<%-- 				<div class="tab right"><a href="options"><fmt:message key="menu.options"/><span></span></a></div> --%>
				<div class="tab right"><a href="admin"><fmt:message key="menu.admin"/><span></span></a></div>
				<div class="clear"></div>
			</div>
			<!-- End of Top Tab-Menu -->
			
		
			<!-- Body of Task Tab -->
			<div id="main" class="pageTabbody tabbody activePage">
				<jsp:doBody/>
			</div>
			<!-- End of body of Task Tab -->
			
		</div>
	</body>
</html>
<%-- Template End --%>