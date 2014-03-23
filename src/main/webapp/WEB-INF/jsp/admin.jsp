<%@ include file="/WEB-INF/jsp/include.jsp" %>

<t:templateAll>

	<jsp:attribute name="js">
    	<script src="res/javascript/admin.js" type="text/javascript"></script>
    	<script src="res/javascript/jqueryFileTree.js" type="text/javascript"></script>
    </jsp:attribute>
	<jsp:attribute name="css">
    	<link id="css_link" href="res/css/admin.css" media="screen" rel="stylesheet" type="text/css" />
    	<link id="css_link" href="res/css/jqueryFileTree.css" media="screen" rel="stylesheet" type="text/css" />
    </jsp:attribute>
    
    
    <jsp:body>
    	<div class="groupDescriptor"><fmt:message key="admin.database"/></div>
        <div class="adminElementGroup">
        	<div class="button pageButton left" onclick="saveTasks()"><fmt:message key="admin.database.save"/></div>
        	<div class="button pageButton left" onclick="loadTasks()"><fmt:message key="admin.database.load"/></div>
        	<div class="button pageButton left" onclick="deleteTasks()"><fmt:message key="admin.database.delete"/></div>
        	<div class="clear"></div>
        	<div class="hint"><fmt:message key="admin.database.hint"/></div>
        </div>
        
        <div class="groupDescriptor">Import Assets (WIP)</div>
        <div class="adminElementGroup">
			<div id="fileTreeContainer"></div>
			<div class="left button pageButton" onclick="addAssetPaths(true)">Scan for Textures</div>
			<div class="left button pageButton" onclick="addAssetPaths(false)">Scan for 3D Meshes</div>
			<div class="clear"></div>
			<div id="selectedPaths">
				<ul>
				</ul>
			</div>
			<div id="importButtons">
				<div id="importTexturesButton" class="left button pageButton" onclick="importTextures()">Import Textures</div>
				<div id="importMeshesButton" class="left button pageButton" onclick="importMeshes()">Import 3D Meshes</div>
				<div id="cancelImportButton" class="left button pageButton" onclick="cancelImport()">Cancel Import</div>
				<div class="clear"></div>
			</div>
		</div>
	</jsp:body>
</t:templateAll>
