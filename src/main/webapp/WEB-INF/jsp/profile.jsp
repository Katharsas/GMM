<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/tags/all_include.tagf" %>
<% pageContext.setAttribute("newLine", "\n"); %>

<t:all_template>

	<jsp:attribute name="js">
    	<script src="<c:url value="/res/javascript/compiled/profile.bundle.js"/>" type="text/javascript"></script>
    </jsp:attribute>
	<jsp:attribute name="css">
    	<link href="<c:url value="/res/css/compiled/profile.css"/>" media="screen" rel="stylesheet" type="text/css" />
    </jsp:attribute>

    <jsp:body>
    	<div id="changePassword">
			<div class="button pageButton changePasswordButton">
				<fmt:message key="password.change"/>
			</div>
			<div id="changePasswordDialog" class="dialog">
				<div id="changePasswordDialog-error"></div>
				<div>
					<fmt:message key="password.min"/>
				</div>
				<div>
					<fmt:message key="password.current"/>:
				</div>
				<div>
					<input id="changePasswordDialog-old" class="dialog-input" type="password"/>
				</div>
				<div>
					<fmt:message key="password.new"/>:
				</div>
				<div>
					<input id="changePasswordDialog-first" class="dialog-input" type="password"/>
				</div>
				<div>
					<input id="changePasswordDialog-second" class="dialog-input" type="password" />
				</div>
				<div id="changePasswordDialog-ok" class="dialog-button button left">
					<fmt:message key="password.submit"/>
				</div>
				<div id="changePasswordDialog-cancel" class="dialog-button button right">
					<fmt:message key="password.cancel"/>
				</div>
			</div>
		</div>
    </jsp:body>
</t:all_template>