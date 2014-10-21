<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/tags/all_include.tagf" %>
<% pageContext.setAttribute("newLine", "\n"); %>

<t:all_template>

	<jsp:attribute name="js">
    	<script src="res/javascript/admin.js" type="text/javascript"></script>
    	<script src="res/javascript/jqueryFileTree.js" type="text/javascript"></script>
    </jsp:attribute>
	<jsp:attribute name="css">
		<link id="css_link" href="res/css/taskForm.css" media="screen" rel="stylesheet" type="text/css" />
    	<link id="css_link" href="res/css/admin.css" media="screen" rel="stylesheet" type="text/css" />
    	<link id="css_link" href="res/css/jqueryFileTree.css" media="screen" rel="stylesheet" type="text/css" />
    </jsp:attribute>
    
    
    <jsp:body>
    	<div class="groupDescriptor"><fmt:message key="admin.database"/></div>
        <div class="adminElementGroup">
        	<div id="loadTasks" class="adminElement left">
        	    <div class="adminElement hint"><fmt:message key="admin.database.message"/></div>
        		<div id="taskBackupsContainer" class="adminElement"></div>
        		<br/>
        		<div class="button pageButton left" onclick="taskLoader = new ResponseBundleHandler();taskLoader.start();">
        			<fmt:message key="admin.database.load"/>
        		</div>
        		<div class="button pageButton left" onclick="deleteFile()">
        			<fmt:message key="admin.database.deletefile"/>
        		</div>
        		<div class="clear"></div>
        		<div id="loadTasksDialog" class="dialogContainer">
        			<p>Loading tasks:</p>
        			<p id="conflictMessage"></p>
        			<div id="loadedTasks" class="dialogList"><ul></ul></div>
        			<div id="conflictOptions">
	        			<div id="skipTaskButton" class="left dialogButton button" onclick="taskLoader.answer('skip');"><fmt:message key="admin.database.skip"/></div>
	        			<div id="overwriteTaskButton" class="left dialogButton button" onclick="taskLoader.answer('overwrite');"><fmt:message key="admin.database.overwrite"/></div>
	        			<div id="addBothTasksButton" class="left dialogButton button" onclick="taskLoader.answer('both');"><fmt:message key="admin.database.keepboth"/></div>
	        			<div class="clear"></div>
	        			<label id="doForAllCheckbox">
	        				<input type="checkbox" name="doForAll" value="doForAll"><fmt:message key="admin.database.dothesame"/><br>
        				</label>
        			</div>
        			<div id="finishLoadingButton" class="dialogButton button" onclick="taskLoader.finish()"><fmt:message key="admin.database.finish"/></div>
        		</div>
        	</div>
        	<div id="saveDeleteTasks" class="adminElement left">
        		<div id="saveAllTasksButton" class="button pageButton" onclick="showDialogue('#saveAllTasksDialog')"><fmt:message key="admin.database.saveall"/></div>
        		<div id="saveAllTasksDialog" class="dialogContainer">
        			<form id="saveAllTasksForm" method="POST" action="admin/save">
        				<fmt:message key="admin.database.filename"/><br/><br/>
        				<input name="name" value="manual/example"/>.xml
        			</form><br/>
        			<div class="dialogButton button left" onclick="saveAllTasks()"><fmt:message key="admin.database.save"/></div>
        			<div class="dialogButton button right" onclick="hideDialogue()"><fmt:message key="admin.database.cancel"/></div>
        			<div class="clear"></div>
        		</div>
        		<div class="verticalSpace"></div>
        		<div class="button pageButton" onclick="deleteAllTasks()"><fmt:message key="admin.database.deleteall"/></div>
			</div>
			<div class="clear"></div>
        </div>
        
        <div class="groupDescriptor">Import Assets (WIP)</div>
        <div class="adminElementGroup">
			<div id="originalAssetsContainer" class="adminElement"></div>
			<div id="addTexturesButton" class="left button pageButton" onclick="addAssetPaths(true)"><fmt:message key="admin.database.addtextures"/></div>
			<div id="addMeshesButton" class="left button pageButton" onclick="addAssetPaths(false)"><fmt:message key="admin.database.addmeshes"/></div>
			<div class="clear"></div>
			<div id="selectedPaths" class="adminElement"><ul></ul></div>

			<div id="importTaskForm">
				<form:form id="taskForm" method="POST"
						action="admin/importAssets"
						commandName="task">
					<div style="width:100%;">
						<t:all_taskForm>
						</t:all_taskForm>
					</div>
				</form:form>
				
				<div class="clear"></div>
				<div id="importButtons">
					<div id="importTexturesButton" class="left button pageButton" onclick="importAssets('true')"><fmt:message key="admin.database.texturesimport"/></div>
					<div id="importMeshesButton" class="left button pageButton" onclick="importAssets('false')"><fmt:message key="admin.database.meshesimport"/></div>
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
					<div class="subElementUserEnabled button left" onclick="switchUser('${user.getIdLink()}','${cfn:escapeJS(user.getName())}')">
						${user.isEnabled() ? '&#x2611;' : '&#x2610;'}
					</div>
					<div class="subElementUserRole button left"  onclick="switchAdmin('${user.getIdLink()}')">
						${user.getRole().equals('ROLE_ADMIN') ? '[ADMIN]' : '&nbsp;'}
					</div>
					<div class="subElementUserName left">
						${fn:escapeXml(user.getName())}
					</div>
					<div class="subElementUserPassword left">
						${user.getPasswordHash()==null ? '(Disabled - Needs Password)' : '&nbsp;'}
					</div>
<%-- 					<div class="button listButton right" onclick="switchUser('${user.getIdLink()}','${user.getName()}')"> --%>
<%-- 						${user.isEnabled() ? 'Disable' : 'Enable'} --%>
<!-- 					</div> -->
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
