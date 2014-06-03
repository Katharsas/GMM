<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/tags/all_include.tagf" %>
<% pageContext.setAttribute("newLine", "\n"); %>

<t:all_template>

	<jsp:attribute name="js">
    	<script src="res/javascript/profile.js" type="text/javascript"></script>
    </jsp:attribute>
	<jsp:attribute name="css">
    	<link id="css_link" href="res/css/profile.css" media="screen" rel="stylesheet" type="text/css" />
    </jsp:attribute>

    <jsp:body>
    	<div id="changePassword">
			<div class="button pageButton" onclick="showChangePassword()">Change Password</div>
			<div id="changePasswordDialog" class="dialogContainer">
				<div id="passwordError"></div>
				<div>Password must contain at least 8 letters.</div>
				<div class="inputDescriptor">
					Current password:
				</div>
				<div class="inputWrapper">
					<input type="password" id="oldPassword"/>
				</div>
				<div class="inputDescriptor">
					New password:
				</div>
				<div class="inputWrapper">
					<input type="password" id="newPassword1"/>
				</div>
				<div class="inputWrapper">
					<input type="password" id="newPassword2"/>
				</div>
				<div class="button pageButton left" onclick="changePassword()">Submit</div>
				<div class="button pageButton right" onclick="hideDialogue()">Cancel</div>
			</div>
		</div>
    </jsp:body>
</t:all_template>