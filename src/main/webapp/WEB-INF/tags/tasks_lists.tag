<%@ include file="/WEB-INF/tags/include.tagf" %>
<%-- Tag Description --%>
<%@tag description="template page" pageEncoding="UTF-8"%>

<%-- Template Start --%>
<%@ attribute name="newLine" required="true" %>

<div id="lists">
	<div id="listsTop" class="subTabmenu tabmenu inactiveSubpage h2">
		<div class="tab left"><a href="tasks/reset?tab=general&edit=${edit}"><fmt:message key="tasks.menu.general"/><span></span></a></div>
		<div class="tab left"><a href="tasks/reset?tab=textures&edit=${edit}"><fmt:message key="tasks.menu.textures"/><span></span></a></div>
		<div class="tab middle"><a href="tasks/reset?tab=models&edit=${edit}"><fmt:message key="tasks.menu.models"/><span></span></a></div>
		<div class="clear"></div>
	</div>
	<div id="listsMain" class="subTabbody tabbody activeSubpage">
		<div class="search listElement">
		<form:form id="searchForm" method="POST" action="/GMM/tasks/submitSearch?tab=${tab}&edit=${edit}" commandName="search">
			<div class="right switchSearchButton button pageButton" onclick="switchSearchType()">
				Complex Search
			</div>
			<form:select id="searchTypeSelect" path="easySearch">
				<form:option value="true" label="easySearch"/>
				<form:option value="false" label="complexSearch"/>
			</form:select>
			<div class="easySearch">
				<form:input class="searchInputField left" path="easy" value=""/>
				<div class="left">&#160;&#160;</div>
			</div>
			<div class="complexSearch">
				<div class="clear"></div>
				<div class="searchDescription left">Label:</div>
				<form:input class="searchInputField left" path="label" value=""/>
				<div class="searchDescription left">&#160;&#160;Name:</div>
				<form:input class="searchInputField left" path="name" value=""/>
				<div class="clear"></div>
				<div class="searchDescription left">Details:</div>
				<form:input class="searchInputField left" path="details" value=""/>
				<div class="searchDescription left">&#160;&#160;Author:</div>
				<form:input class="searchInputField left" path="author" value=""/>
				<div class="clear"></div>
				<div class="searchDescription left">Assigned:</div>
				<form:input class="searchInputField left" path="assigned" value=""/>
				<div class="searchDescription left">&#160;</div>
			</div>
			<div class="submitSearchButton button pageButton left">Start Search!</div>
			<div class="clear"></div>
			</form:form>
		</div>
		<c:forEach items="${taskList}" var="task">
			<div id="task_${task.getIdLink()}" class="listElement">
				<div class="listElementTop clickable" onclick="switchListElement(this)">
					<div class="right elementPriority ${task.getPriority().toString()}">
				    </div>
				    <c:if test="${!task.getLabel().equals(\"\")}">
					    <div class="left elementLabel elementContent">
							[<c:out value="${task.getLabel()}"/>]
					    </div>
					</c:if>
					<div class="left elementName elementContent h3">
						<c:out value="${task.getName()}"/>
				    </div>
				    <div class="right elementStatus elementContent">
				    	<fmt:message key="${task.getTaskStatus().getMessageKey()}"/>
				    </div>
				    <div class="right elementAssigned elementContent">
				    	<c:if test="${!(task.getAssigned()==null)}">
				    		<c:out value="${task.getAssigned().getName()}"/>&#160;&#160;-&#160;&#160;
						</c:if>
					</div>
			    	<div class="clear"></div>
			    </div>
			    <div class="listElementBody">
				    <div class="elementDetails elementContent">
				    	${fn:replace(fn:escapeXml(task.getDetails()), newLine, "<br>")}
				    </div>
				    <div class="elementComments">
					    <c:forEach items="${task.getComments()}" var="comment">
					    <div id="comment_${comment.getIdLink()}" class="subListElement">
					    	<div class="left subElementAuthor elementContent">
					    		<c:out value="${comment.getAuthor().getName()}"/>
					    	</div>
					    	<div class="subElementText elementContent">
					    		${fn:replace(fn:escapeXml(comment.getText()), newLine, "<br/>")}
					    	</div>
					    	<div class="clear"></div>
					    </div>
					    </c:forEach>
					    <form:form class="commentInput input" method="POST" action="/GMM/tasks/submitComment?tab=${tab}&edit=${edit}&editComment=${task.getIdLink()}" commandName="comment">
					    	<div class="left subElementAuthor">
					    		<input type="submit" value="Submit">
					    	</div>
					    	<div class="subElementText">
					    		<form:textarea rows="2" cols="1" path="text"></form:textarea>
					    	</div>
					    </form:form>
				    </div>
				    <div class="elementDelete">
				    	<div class="left deleteQuestion">
				    		<fmt:message key="task.deleteQuestion"/>
				    	</div>
						<div class="right button deleteButton">
							<a href="/GMM/tasks/deleteTask?tab=${tab}&edit=${edit}&delete=${task.getIdLink()}">
								<fmt:message key="ok"/><span></span>
							</a>
						</div>
						<div class="clear"></div>
					</div>
				    <div class="listElementBodyFooter">
					    <div class="left commentElement elementButton button" onclick="switchCommentInput(this)">
				    		<fmt:message key="to.comment"/>
				    	</div>
					    <div class="right elementAuthorDate">
				    		<div class=" elementContent right">
				    			<c:out value="${task.getAuthor().getName()}"/><br/>
					    		<c:out value="${task.getCreationDate().toString()}"/>
				    		</div>
				    		<div class="elementContent right">
				    			<fmt:message key="author"/>:&#160;&#160;<br/>
					    		<fmt:message key="tasks.list.created"/>:&#160;&#160;
				    		</div>
				    	</div>
				    	<div class="right deleteElement elementButton button" onclick="switchDeleteQuestion(this)">
				    		<fmt:message key="delete"/>
				    	</div>
				    	<div class="right editElement elementButton button">
				    		<a href="?tab=${tab}&edit=${task.getIdLink()}"><fmt:message key="edit"/><span></span></a>
				    	</div>
				    	<div class="clear"></div>
			    	</div>
			    </div>
			</div>
		</c:forEach>
	</div>
</div>