<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/tags/all_include.tagf" %>

<t:all_template>

	<jsp:attribute name="js">
    	<script src="<c:url value="/res/javascript/compiled/admin.bundle.js"/>" type="text/javascript"></script>
    </jsp:attribute>
	<jsp:attribute name="css">
		<link href="<c:url value="/res/css/compiled/admin.css"/>" media="screen" rel="stylesheet" type="text/css" />
    </jsp:attribute>
    
    
    <jsp:body>
    
    	<div class="admin-groupDesc"><fmt:message key="admin.customization"/></div>
        <div class="admin-group">
			<div id="customizationDescription" class="left"><fmt:message key="admin.htmlCode"/></div>
			<c:if test="${isCustomAdminBannerActive}">
				<div  class="button pageButton right"><a href="admin/deactivateBanner"><fmt:message key="admin.deactivate"/><span></span></a></div>
			</c:if>
			<c:if test="${!isCustomAdminBannerActive}">
				<div  class="button pageButton right"><a href="admin/activateBanner"><fmt:message key="admin.activate"/><span></span></a></div>
			</c:if>
        	<div class="clear"></div>
        	<div>
        		<textarea id="adminBannerTextArea" ></textarea>
        	</div>
        </div>
    
    	<div class="admin-groupDesc"><fmt:message key="admin.database"/></div>
        <div class="admin-group" id="database">
        	<div class="left" id="loadTasks" >
        		<div class="admin-inner">
        			<div class="admin-inner-header hint">
	        	    	<fmt:message key="admin.database.message"/>
	        	    </div>
        			<div id="database-fileTreeContainer" class="admin-inner-main"></div>
        		</div>
        		<div id="database-loadFile" class="button pageButton left">
        			<fmt:message key="admin.database.load"/>
        		</div>
        		<div id="database-deleteFile" class="button pageButton left">
        			<fmt:message key="admin.database.deletefile"/>
        		</div>
        		<div class="clear"></div>
        	</div>
			<div id="save" class="left">
				<div id="database-saveAllAs" class="button pageButton innerButton">
        			<fmt:message key="admin.database.saveallas"/>
        		</div>
        		<div id="database-saveAll" class="button pageButton innerButton">
        			<fmt:message key="admin.database.saveall"/>
        		</div>
        		<div class="verticalSpace"></div>
        		<div id="database-deleteAll" class="button pageButton innerButton">
        			<fmt:message key="admin.database.deleteall"/>
        		</div>
			</div>
			<div class="clear"></div>
        </div>
        
        <div class="admin-groupDesc"><fmt:message key="admin.importAssets"/></div>
        <div class="admin-group" id="assets">
        	<div id="autoImportNewAssets" class="admin-inner admin-inner-main">
        		<label>
        			<input id="autoImportInput" class="clickable" type="checkbox" ${isAutoImportEnabled ? 'checked' : ''}>
        			<fmt:message key="admin.autoImport"/>
        		</label>
        	</div>
        	<div id="importTaskTemplate" class="admin-inner">
        		<div class="admin-inner-header"><fmt:message key="tasks.template"/></div>
				<form:form id="taskTemplate" modelAttribute="taskTemplate" class="admin-inner-main">
					${taskTemplate}
				</form:form>
				<div id="saveTaskTemplateButton" class="left button pageButton innerButton"><fmt:message key="tasks.template.save"/></div>
				<div class="clear"></div>
			</div>
        	<div id="originalAssets" class="admin-inner">
	        	<div class="admin-inner-header">Original assets</div>
				<div class="admin-inner-main fileTreeContainer" id="originalAssets-fileTreeContainer"></div>
			</div>
			<div id="newAssets" class="admin-inner">
				<div class="admin-inner-header">New assets</div>
				<div class="admin-inner-main fileTreeContainer" id="newAssets-fileTreeContainer"></div>
			</div>
			
			<div id="addAssetsButton" class="left button pageButton"><fmt:message key="admin.database.addassets"/></div>
			<div class="clear"></div>
			<div id="selectedPaths" class="admin-inner admin-inner-main"><ul></ul></div>

			<div id="importButtons">
				<div id="importAssetsButton" class="left button pageButton"><fmt:message key="admin.database.importassets"/></div>
				<div id="cancelImportButton" class="left button pageButton"><fmt:message key="admin.database.cancelimport"/></div>
				<div class="clear"></div>
			</div>
		</div>
		
		<div class="admin-groupDesc">
			<fmt:message key="admin.database.useraccounts"/>
		</div>
		<div class="admin-group">
			<div id="admin-users">
				<c:forEach items="${users}" var="user">
					<div id="${user.getIdLink()}" class="admin-user ${user.isEnabled() ? '' : 'disabled'}">
						<div class="admin-user-enabled clickable left">
							${user.isEnabled() ? '&#x2611;' : '&#x2610;'}
						</div>
						<div class="admin-user-role clickable left">
							${user.getRole().equals('ROLE_ADMIN') ? '[ADMIN]' : '&nbsp;'}
						</div>
						<div class="admin-user-name left" data-name="${fn:escapeXml(user.getName())}">
							${fn:escapeXml(user.getName())}
						</div>
						<div class="admin-user-password left">
							${user.getPasswordHash()==null ? '(Disabled - Needs Password)' : '&nbsp;'}
						</div>
						<div class="admin-user-buttonReset button right">
							<fmt:message key="admin.database.resetpassword"/>
						</div>
						<div class="admin-user-buttonRename button right">
							<fmt:message key="admin.database.editname"/>
						</div>
						<div class="clear"></div>
					</div>
				</c:forEach>
				<br/>
			</div>
			<div class="admin-users-new button pageButton left"><fmt:message key="admin.database.newuser"/></div>
			<div class="admin-users-save button pageButton right"><fmt:message key="admin.database.saveuser"/></div>
		</div>
			
	</jsp:body>
</t:all_template>
