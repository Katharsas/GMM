<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/tags/all_include.tagf" %>
<% pageContext.setAttribute("newLine", "\n"); %>

<t:all_template>

	<jsp:attribute name="js">
		<script src="<c:url value="/res/javascript/compiled/login.bundle.js"/>" type="text/javascript"></script>
    </jsp:attribute>
	<jsp:attribute name="css">
		<link href="<c:url value="/res/css/compiled/login.css"/>" media="screen" rel="stylesheet" type="text/css" />
    </jsp:attribute>

    <jsp:body>
    	<div id="login">
    		<c:if test="${logout}">
				<div id="login-logoutMessage" class="center">Logout successful!<br></div>
			</c:if>
    	
			<div id="login-welcomeMessage" class="h2">
				Welcome to the <span id="login-gmmName">Gothic Mod Manager</span> !<br>
			</div>
			<span class="h3">Create Account</span>
			<form id="loginForm" action="<c:url value="newaccount/create"/>" method="POST">
				<fieldset id="login-form-inputs">
				<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
					<div class="left">Username:</div>
					<div class="right input">
						<input id="username" type="text" name="username" autofocus="autofocus"/><br>
					</div>
					<div class="clear"></div>
					<div class="left">Password:</div>
					<div class="right input">
						<input id="login-form-password" type="password" name="password"><br>
					</div>
					<div class="clear"></div>
					<div class="left">Token:</div>
					<div class="right input">
						<input id="login-form-token" type="text" name="token"><br>
					</div>
					<div class="clear"></div>
					<div id="login-form-submit" class="button" style="margin-top: 15px">Create</div>
				</fieldset>
				<c:choose>
					<c:when test="${param.wrongToken != null}">
						<div id="login-error" class="center">Invalid token!<br></div>
					</c:when>
					<c:when test="${param.nameTaken != null}">
						<div id="login-error" class="center">Name already taken!<br></div>
					</c:when>
					<c:when test="${param.passwordTooShort != null}">
						<div id="login-error" class="center">Password too short!<br></div>
					</c:when>
					 <c:otherwise><br></c:otherwise>
				</c:choose>
			</form>
		</div>
    </jsp:body>
</t:all_template>