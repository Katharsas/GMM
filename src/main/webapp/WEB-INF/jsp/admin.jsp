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
			<div class="admin-inner customizationDescription left"><fmt:message key="admin.htmlCode"/></div>
			<c:if test="${combinedData.isCustomAdminBannerActive()}">
				<div  class="button pageButton right"><a href="admin/deactivateBanner"><fmt:message key="admin.deactivate"/><span></span></a></div>
			</c:if>
			<c:if test="${!combinedData.isCustomAdminBannerActive()}">
				<div  class="button pageButton right"><a href="admin/activateBanner"><fmt:message key="admin.activate"/><span></span></a></div>
			</c:if>
        	<div class="clear"></div>
        	<div class="admin-inner">
        		<textarea id="adminBannerTextArea" ></textarea>
        	</div>
        </div>
    
    	<div class="admin-groupDesc"><fmt:message key="admin.database"/></div>
        <div class="admin-group" id="database">
        	<div class="admin-inner left" id="loadTasks" >
        	    <div class="admin-inner hint"><fmt:message key="admin.database.message"/></div>
        		<div id="database-fileTreeContainer" class="admin-inner"></div>
        		<br/>
        		<div id="database-loadFile" class="button pageButton left">
        			<fmt:message key="admin.database.load"/>
        		</div>
        		<div id="database-deleteFile" class="button pageButton left">
        			<fmt:message key="admin.database.deletefile"/>
        		</div>
        		<div class="clear"></div>
        	</div>
        	<div id="save" class="admin-inner left">
        		<div id="database-saveAll" class="button pageButton">
        			<fmt:message key="admin.database.saveall"/>
        		</div>
        		<div class="verticalSpace"></div>
        		<div id="database-deleteAll" class="button pageButton"><fmt:message key="admin.database.deleteall"/></div>
			</div>
			<div class="clear"></div>
        </div>
        
        <div class="admin-groupDesc"><fmt:message key="admin.importAssets"/></div>
        <div class="admin-group" id="assets">
			<div class="admin-inner" id="assets-fileTreeContainer"></div>
			<div id="addTexturesButton" class="left button pageButton" onclick="addAssetPaths(true)"><fmt:message key="admin.database.addtextures"/></div>
			<div id="addMeshesButton" class="left button pageButton" onclick="addAssetPaths(false)"><fmt:message key="admin.database.addmeshes"/></div>
			<div class="clear"></div>
			<div id="selectedPaths" class="admin-inner"><ul></ul></div>

			<div id="importTaskForm" class="admin-inner">
				<form:form id="taskForm" commandName="taskForm">
					<t:all_taskForm>
					</t:all_taskForm>
				</form:form>
				
				<div class="clear"></div>
				<div id="importButtons">
					<div id="importTexturesButton" class="left button pageButton" onclick="importAssets('textures')"><fmt:message key="admin.database.texturesimport"/></div>
					<div id="importMeshesButton" class="left button pageButton" onclick="importAssets('models')"><fmt:message key="admin.database.meshesimport"/></div>
					<div id="cancelImportButton" class="left button pageButton" onclick="cancelImport()"><fmt:message key="admin.database.cancelimport"/></div>
					<div class="clear"></div>
				</div>
			</div>
		</div>
		
		<div class="admin-groupDesc">
			<fmt:message key="admin.database.useraccounts"/>
		</div>
		<div class="admin-group">
			<br/>
			<div class="admin-inner" id="admin-users">
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
			<div class="admin-users-load button pageButton right"><fmt:message key="admin.database.loaduser"/></div>
		</div>
			
	</jsp:body>
</t:all_template>
