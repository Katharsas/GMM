<%@ include file="/WEB-INF/tags/all_include.tagf" %>
<%@tag description="template page" pageEncoding="UTF-8"%>


<form:form id="searchForm" method="POST" action="tasks/submitSearch?tab=${tab}" commandName="search">
				
	<form:select id="searchTypeSelect" path="easySearch">
		<form:option value="true" label="easySearch"/>
		<form:option value="false" label="complexSearch"/>
		</form:select>
		<div class="easySearch">
			<div class="right switchSearchButton button pageButton" onclick="switchSearchType()">
				<fmt:message key="search.complex"/>
			</div>
			<form:input class="searchInputField left" path="easy" value=""/>
			<div class="submitSearchButton button pageButton left"><fmt:message key="search.start"/>!</div>
		</div>
				
		<div class="complexSearch">
			<div class="clear"></div>
			<div class="right switchSearchButton button pageButton" onclick="switchSearchType()">
				<fmt:message key="search.easy"/>
			</div>
			<div class="clear"></div>
			<div class="complexSearchLeft">
			
				<!-- Title -->
				<div class="searchDescription"><fmt:message key="tasks.title"/>:</div>
				<form:input class="searchInputField" path="name" value=""/>
				
				<!-- Details -->
				<div class="searchDescription"><fmt:message key="tasks.details"/>:</div>
				<form:input class="searchInputField" path="details" value=""/>
				
				<!-- Author -->
				<div class="searchDescription left"><fmt:message key="author"/>:</div>
				<form:input class="searchInputField" path="author" value=""/>
			</div>
			<div class="complexSearchRight">
			
			<!-- Description -->
			<div class="searchDescription"><fmt:message key="tasks.label"/>:</div>
			<form:input class="searchInputField" path="label" value=""/>
			<div class="clear"></div>

			<!-- Assigned -->
			<div class="searchDescription"><fmt:message key="tasks.assigned"/>:</div>
			<form:input class="searchInputField" path="assigned" value=""/>
			<div class="clear"></div>

			<div class="submitSearchButton button pageButton right">
				<fmt:message key="search.start"/>!
			</div>
		</div>
	</div>
</form:form>