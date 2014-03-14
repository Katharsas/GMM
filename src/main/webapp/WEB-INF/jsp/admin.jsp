<%@ include file="/WEB-INF/jsp/include.jsp" %>
<%-- <%@ include file="/WEB-INF/jsp/jqueryFileTree.jsp" %> --%>

<t:templateAll>

	<jsp:attribute name="js">
    	<script src="/res/javascript/admin.js" type="text/javascript"></script>
    	<script src="/res/javascript/jqueryFileTree.js" type="text/javascript"></script>
    </jsp:attribute>
	<jsp:attribute name="css">
    	<link id="css_link" href="/res/css/admin.css" media="screen" rel="stylesheet" type="text/css" />
    	<link id="css_link" href="/res/GMM/css/jqueryFileTree.css" media="screen" rel="stylesheet" type="text/css" />
    </jsp:attribute>
    
    
    <jsp:body>
        <div>
        	<div class="button pageButton" onclick="saveTasks()">save Tasks</div>
        	<div class="button pageButton" onclick="loadTasks()">load Tasks</div>
        	<div id="fileTreeContainer" style="background-color:white;margin:40px;">
        	</div>
        </div>
        
        
    </jsp:body>
</t:templateAll>
