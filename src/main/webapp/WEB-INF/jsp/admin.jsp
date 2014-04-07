<%@ include file="/WEB-INF/tags/include.tagf" %>
<% pageContext.setAttribute("newLine", "\n"); %>

<t:templateAll>

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
        	    <div class="adminElement hint">Select a file to load tasks from:</div>
        		<div id="taskBackupsContainer" class="adminElement"></div>
        		<br/>
        		<div class="button pageButton" onclick="loadTasks()"><fmt:message key="admin.database.load"/></div>
        	</div>
        	<div id="saveDeleteTasks" class="adminElement left">
        		<div id="saveAllTasksButton" class="button pageButton" onclick="saveAllTasks()"><fmt:message key="admin.database.saveall"/></div>
<!--         		<div id="saveAllTasksDialog" class="dialogContainer">test</div> -->
        		<div class="verticalSpace"></div>
        		<div class="button pageButton" onclick="deleteAllTasks()"><fmt:message key="admin.database.deleteall"/></div>
			</div>
			<div class="clear"></div>
        </div>
        
        <div class="groupDescriptor">Import Assets (WIP)</div>
        <div class="adminElementGroup">
			<div id="fileTreeContainer" class="adminElement"></div>
			<div id="addTexturesButton" class="left button pageButton" onclick="addAssetPaths(true)">Add Textures</div>
			<div id="addMeshesButton" class="left button pageButton" onclick="addAssetPaths(false)">Add 3D Meshes</div>
			<div class="clear"></div>
			<div id="selectedPaths" class="adminElement"><ul></ul></div>

			<div id="importTaskForm">
				<form:form id="taskForm" method="POST" action="/GMM/admin/import/submit" commandName="task">
					<div style="width:100%;">
						<t:all_taskForm>
						</t:all_taskForm>
					</div>
				</form:form>
				
				<div class="clear"></div>
				<div id="importButtons">
				<div id="importTexturesButton" class="left button pageButton" onclick="importTextures()">Import Textures</div>
				<div id="importMeshesButton" class="left button pageButton" onclick="importMeshes()">Import 3D Meshes</div>
				<div id="cancelImportButton" class="left button pageButton" onclick="cancelImport()">Cancel Import</div>
				<div class="clear"></div>
			</div>
			</div>
		</div>
	</jsp:body>
</t:templateAll>
