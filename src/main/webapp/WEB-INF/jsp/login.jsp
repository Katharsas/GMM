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
			<span class="h3">Login</span>
			<form id="loginForm" action="<c:url value="/login"/>" method="POST">
				<fieldset id="login-form-inputs">
				<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
					<div class="input">
						<input type="text" id="username" name="username" autofocus="autofocus"/><br>
					</div>
					<div class="input">
						<input id="login-form-password" type="password" id="password" name="password"><br>
					</div>
					<div id="login-form-submit" class="button">Login</div>
				</fieldset>
				<c:if test="${error}">
					<div id="login-error" class="center">Wrong username or password!<br></div>
				</c:if><c:if test="${not error}"><br></c:if>
			</form><br/><br/><br/><br/>
			<span class="h3">Links</span><br/>
			Visit the development of this software: <a href="http://github.com/Katharsas/GMM">http://github.com/Katharsas/GMM</a>
			<br/>
			Visit the Gothic Reloaded Mod homepage: <a href="http://www.gothic-reloaded-mod.de/">http://www.gothic-reloaded-mod.de/</a>
		</div>
    </jsp:body>
</t:all_template>