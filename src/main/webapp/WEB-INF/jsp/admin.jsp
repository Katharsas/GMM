<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/tags/all_include.tagf" %>

<t:all_template>

	<jsp:attribute name="js">
    	<script src="<c:url value="/res/javascript/admin.js"/>" type="text/javascript"></script>
    	<script src="<c:url value="/res/javascript/all/jqueryFileTree.js"/>" type="text/javascript"></script>
    </jsp:attribute>
	<jsp:attribute name="css">
		<link href="<c:url value="/res/css/compiled/admin.css"/>" media="screen" rel="stylesheet" type="text/css" />
    </jsp:attribute>
    
    
    <jsp:body>
    
    	<div class="groupDescriptor"><fmt:message key="admin.customization"/></div>
        <div class="adminElementGroup">
			<div class="customizationDescription adminElement left"><fmt:message key="admin.htmlCode"/></div>
			<c:if test="${combinedData.isCustomAdminBannerActive()}">
				<div  class="button pageButton right"><a href="admin/deactivateBanner"><fmt:message key="admin.deactivate"/><span></span></a></div>
			</c:if>
			<c:if test="${!combinedData.isCustomAdminBannerActive()}">
				<div  class="button pageButton right"><a href="admin/activateBanner"><fmt:message key="admin.activate"/><span></span></a></div>
			</c:if>
        	<div class="clear"></div>
        	<div class="adminElement">
        		<textarea id="adminBannerTextArea" ></textarea>
        	</div>
        </div>
    
    	<div class="groupDescriptor"><fmt:message key="admin.database"/></div>
        <div class="adminElementGroup">
        	<div id="loadTasks" class="adminElement left">
        	    <div class="adminElement hint"><fmt:message key="admin.database.message"/></div>
        		<div id="taskBackupsContainer" class="adminElement"></div>
        		<br/>
        		<div class="button pageButton left" onclick="loadTasks()">
        			<fmt:message key="admin.database.load"/>
        		</div>
        		<div class="button pageButton left" onclick="deleteFile()">
        			<fmt:message key="admin.database.deletefile"/>
        		</div>
        		<div class="clear"></div>
        	</div>
        	<div id="saveDeleteTasks" class="adminElement left">
        		<div id="saveAllTasksButton" class="button pageButton" onclick="showDialog($('#dialog-saveTasks'))">
        			<fmt:message key="admin.database.saveall"/>
        		</div>
        		<div class="verticalSpace"></div>
        		<div class="button pageButton" onclick="deleteAllTasks()"><fmt:message key="admin.database.deleteall"/></div>
			</div>
			<div class="clear"></div>
        </div>
        
        <div class="groupDescriptor"><fmt:message key="admin.importAssets"/></div>
        <div class="adminElementGroup">
			<div id="originalAssetsContainer" class="adminElement"></div>
			<div id="addTexturesButton" class="left button pageButton" onclick="addAssetPaths(true)"><fmt:message key="admin.database.addtextures"/></div>
			<div id="addMeshesButton" class="left button pageButton" onclick="addAssetPaths(false)"><fmt:message key="admin.database.addmeshes"/></div>
			<div class="clear"></div>
			<div id="selectedPaths" class="adminElement"><ul></ul></div>

			<div id="importTaskForm" class="adminElement">
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
		
		<div class="groupDescriptor">
			<fmt:message key="admin.database.useraccounts"/>
		</div>
		<div class="adminElementGroup">
			<br/>
			<c:forEach items="${users}" var="user">
				<div id="${user.getIdLink()}" class="elementUser">
					<div class="subElementUserEnabled clickable left" onclick="switchUser('${user.getIdLink()}','${cfn:escapeJS(user.getName())}')">
						${user.isEnabled() ? '&#x2611;' : '&#x2610;'}
					</div>
					<div class="subElementUserRole clickable left"  onclick="switchAdmin('${user.getIdLink()}')">
						${user.getRole().equals('ROLE_ADMIN') ? '[ADMIN]' : '&nbsp;'}
					</div>
					<div class="subElementUserName left">
						${fn:escapeXml(user.getName())}
					</div>
					<div class="subElementUserPassword left">
						${user.getPasswordHash()==null ? '(Disabled - Needs Password)' : '&nbsp;'}
					</div>
<%-- 					<div class="button listButton right" onclick="editUserRole('${user.getIdLink()}','${user.getRole()}')"> --%>
<!-- 						Change Role -->
<!-- 					</div> -->
					<div class="button listButton right" onclick="resetPassword('${user.getIdLink()}')">
						<fmt:message key="admin.database.resetpassword"/>
					</div>
					<div class="button listButton right" onclick="editUserName('${user.getIdLink()}','${cfn:escapeJS(user.getName())}')">
						<fmt:message key="admin.database.editname"/>
					</div>
					<div class="clear"></div>
				</div>
			</c:forEach>
			<br/>
			<div class="button pageButton left" onclick="editUserName('new','')"><fmt:message key="admin.database.newuser"/></div>
			<div class="button pageButton right" onclick="saveUsers()"><fmt:message key="admin.database.saveuser"/></div>
			<div class="button pageButton right" onclick="loadUsers()"><fmt:message key="admin.database.loaduser"/></div>
		</div>
			
	</jsp:body>
</t:all_template>
