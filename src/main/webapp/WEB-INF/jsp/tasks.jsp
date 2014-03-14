<%@ include file="/WEB-INF/jsp/include.jsp" %>

<t:templateAll>

	<jsp:attribute name="js">
    	<script src="/res/javascript/tasks.js" type="text/javascript"></script>
    </jsp:attribute>
	<jsp:attribute name="css">
    	<link id="css_link" href="/res/css/tasks.css" media="screen" rel="stylesheet" type="text/css" />
    </jsp:attribute>

    <jsp:body>
		<div class="table">
			<div class="tr">
				<div class="td">
				</div>
				<div class="td">
					<t:tasks_generalArea>
					</t:tasks_generalArea>
				</div>
			</div>
			<div class="tr">
				<div class="td">
					<t:tasks_filters>
					</t:tasks_filters>
				</div>
				<div class="td">
					<t:tasks_lists newLine="${newLine}">
					</t:tasks_lists>
				</div>
			</div>
		</div>
    </jsp:body>
</t:templateAll>