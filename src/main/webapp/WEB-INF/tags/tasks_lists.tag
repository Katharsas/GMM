<%@ include file="/WEB-INF/tags/all_include.tagf" %>
<%-- Tag Description --%>
<%@tag description="template page" pageEncoding="UTF-8"%>

<%-- Template Start --%>
<%@ attribute name="newLine" required="true" %>

<div id="lists">
	<div id="listsTop" class="subTabmenu tabmenu inactiveSubpage h3">
		<div class="tab left"><a href="tasks?tab=general"><fmt:message key="tasks.menu.general"/><span></span></a></div>
		<div class="tab left"><a href="tasks?tab=textures"><fmt:message key="tasks.menu.textures"/><span></span></a></div>
		<div class="tab right"><a href="tasks?tab=models"><fmt:message key="tasks.menu.models"/><span></span></a></div>
		<div class="clear"></div>
	</div>
	<div id="listsMain" class="subTabbody tabbody activeSubpage">
	
<%-- ########################## Searching ########################### --%>
	
		<div class="search listElement">
			<form:form id="searchForm" method="POST" action="tasks/submitSearch?tab=${tab}" modelAttribute="search">
				
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
					
<!-- 					Title -->
						<div class="searchDescription"><fmt:message key="tasks.title"/>:</div>
						<form:input class="searchInputField" path="name" value=""/>
						
<!-- 					Details -->
						<div class="searchDescription"><fmt:message key="tasks.details"/>:</div>
						<form:input class="searchInputField" path="details" value=""/>
						
<!-- 					Author -->
						<div class="searchDescription left"><fmt:message key="author"/>:</div>
						<form:input class="searchInputField" path="author" value=""/>
					</div>
					<div class="complexSearchRight">
					
<!-- 					Description -->
						<div class="searchDescription"><fmt:message key="tasks.label"/>:</div>
						<form:input class="searchInputField" path="label" value=""/>
						<div class="clear"></div>

<!-- 					Assigned -->
						<div class="searchDescription"><fmt:message key="tasks.assigned"/>:</div>
						<form:input class="searchInputField" path="assigned" value=""/>
						<div class="clear"></div>
	
						<div class="submitSearchButton button pageButton right">
							<fmt:message key="search.start"/>!
						</div>
					</div>
				</div>
			</form:form>
		</div>
		<div class="clear"></div>
		
<%-- ########################## Sorting ########################### --%>
		
		<div class="sort listElement">
			<form:form id="sortForm" method="POST" action="tasks/submitSort?tab=${tab}" modelAttribute="sort">
				<div class="left">
				<fmt:message key="search.first"/><span></span>
				</div>
				<div id="primarySort" class="left">
					<form:select class="sortFormElement" path="sortByPrimary">
						<c:forEach items="${cfn:values('TaskSortAttribute')}" var="sort">
							<c:set var="svalue"><fmt:message key="${sort.nameKey}"/></c:set> 
							<form:option value="${sort}" label="${svalue}"/>
						</c:forEach>
					</form:select>
					<fmt:message key="tasks.down"/><form:checkbox class="sortFormElement" path="sortDownPrimary"/>
				</div>
				<div class="clear"></div>
				<div class="left">
					<fmt:message key="search.seconde"/><span></span> 
				</div>
				<div id="secondarySort" class="left">
					<form:select class="sortFormElement" path="sortBySecondary">
						<c:forEach items="${cfn:values('TaskSortAttribute')}" var="sort">
							<c:set var="svalue"><fmt:message key="${sort.nameKey}"/></c:set> 
							<form:option value="${sort}" label="${svalue}"/>
						</c:forEach>
					</form:select>
					<fmt:message key="tasks.down"/><form:checkbox class="sortFormElement" path="sortDownSecondary"/>
				</div>
			</form:form>
		</div>
		
<%-- ########################## Task List ########################### --%>

		<div class="elementCount center">
			---------------------------------------- 
			<c:out value="${taskList.size()}"/> <fmt:message key="elements"/> 
			----------------------------------------
		</div>
	</div>
</div>