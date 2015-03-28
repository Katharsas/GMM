<%@ include file="/WEB-INF/tags/all_include.tagf" %>
<%@tag description="template page" pageEncoding="UTF-8"%>

		

<form:form id="workbench-searchForm" commandName="workbench-searchForm">	
	<div id="workbench-search-wrapper">

		<%-- Hidden --%>
		<form:select style="display:none;" id="workbench-search-type" path="easySearch">
			<form:option value="true" label="easySearch"/>
			<form:option value="false" label="complexSearch"/>
		</form:select>
		
		<!-- Switch between easy and complex search -->
		<div id="workbench-search-switch" class="button">
			<fmt:message key="search.complex"/>
		</div>
		
		<!-- Easy search -->
		<div id="workbench-search-easy" class="input">
			<form:input class="form-element" path="easy" value=""/>
			
			<div class="workbench-search-submit button">
				<fmt:message key="search.start"/>!
			</div>
		</div>
		
		<!-- Complex search -->
		<div id="workbench-search-complex" class="input">
			<div class="workbench-search-group">
			
				<div class="workbench-search-desc"><fmt:message key="tasks.title"/>:</div>
				<form:input class="form-element" path="name" value=""/>
				
				<div class="workbench-search-desc"><fmt:message key="tasks.details"/>:</div>
				<form:input class="form-element" path="details" value=""/>
				
			</div>
			<div class="workbench-search-group">
			
				<div class="workbench-search-desc"><fmt:message key="author"/>:</div>
				<form:input class="form-element" path="author" value=""/>
				
				<div class="workbench-search-desc"><fmt:message key="tasks.label"/>:</div>
				<form:input class="form-element" path="label" value=""/>
				
			</div>
			<div class="workbench-search-group">
			
				<div class="workbench-search-desc"><fmt:message key="tasks.assigned"/>:</div>
				<form:input class="form-element" path="assigned" value=""/>
				
				<div class="workbench-search-desc"><fmt:message key="tasks.path"/>:</div>
				<form:input class="form-element" path="path" value=""/>
				
			</div>
			<div class="workbench-search-submit button">
				<fmt:message key="search.start"/>!
			</div>
		</div>
		
	</div>
</form:form>
