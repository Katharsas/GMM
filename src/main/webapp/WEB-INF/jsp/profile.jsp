<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/tags/all_include.tagf" %>
<% pageContext.setAttribute("newLine", "\n"); %>

<t:all_template>

	<jsp:attribute name="js">
    	<script src="<c:url value="/res/javascript/profile.js"/>" type="text/javascript"></script>
    </jsp:attribute>
	<jsp:attribute name="css">
    	<link href="<c:url value="/res/css/profile.css"/>" media="screen" rel="stylesheet" type="text/css" />
    </jsp:attribute>

    <jsp:body>
    	<div id="changePassword">
			<div class="button pageButton" onclick="showChangePassword()"><fmt:message key="password.change"/></div>
			<div id="changePasswordDialog" class="dialogContainer">
				<div id="passwordError"></div>
				<div><fmt:message key="password.min"/></div>
				<div class="inputDescriptor">
					<fmt:message key="password.current"/>:
				</div>
				<div class="inputWrapper">
					<input type="password" id="oldPassword"/>
				</div>
				<div class="inputDescriptor">
					<fmt:message key="password.new"/>:
				</div>
				<div class="inputWrapper">
					<input type="password" id="newPassword1"/>
				</div>
				<div class="inputWrapper">
					<input type="password" id="newPassword2"/>
				</div>
				<div class="button pageButton left" onclick="changePassword()"><fmt:message key="password.submit"/></div>
				<div class="button pageButton right" onclick="hideDialogue()"><fmt:message key="password.cancel"/></div>
			</div>
		</div>
    </jsp:body>
</t:all_template>