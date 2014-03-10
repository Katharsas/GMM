<%@ include file="/WEB-INF/jsp/include.jsp" %>
<%-- <%@ include file="/WEB-INF/jsp/jqueryFileTree.jsp" %> --%>

<t:templateAll>

	<jsp:attribute name="js">
    	<script src="/GMM/javascript/admin.js" type="text/javascript"></script>
    	<script src="/GMM/javascript/jQuery.js" type="text/javascript"></script>
    	<script src="/GMM/javascript/jqueryFileTree.js" type="text/javascript"></script>
    </jsp:attribute>
	<jsp:attribute name="css">
    	<link id="css_link" href="/GMM/css/admin.css" media="screen" rel="stylesheet" type="text/css" />
    	<link id="css_link" href="/GMM/css/jqueryFileTree.css" media="screen" rel="stylesheet" type="text/css" />
    </jsp:attribute>
    
    
    <jsp:body>
    	<div class="groupDescriptor">DataBase Operations</div>
        <div class="adminElementGroup">
        	<div class="button pageButton" onclick="saveTasks()">save Tasks</div>
        	<div class="button pageButton" onclick="loadTasks()">load Tasks</div>
        	<div class="button pageButton" onclick="deleteTasks()">delete Tasks</div>
        </div>
        <div class="groupDescriptor">Import Assets</div>
        <div class="adminElementGroup">
        	<div id="fileTreeContainer" style="background-color:white;margin:40px;"></div>
        </div>
        
    </jsp:body>
</t:templateAll>
