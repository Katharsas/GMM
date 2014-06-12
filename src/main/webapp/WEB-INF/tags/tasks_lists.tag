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
		<div class="search listElement">
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
<!-- 					<div class="left">&#160;&#160;</div> -->
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
<!-- 					<div class="clear"></div> -->
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
		<div class="elementCount center">---------------------------------------- 
		<c:out value="${taskList.size()}"/> <fmt:message key="elements"/> 
		----------------------------------------</div>
		<c:forEach items="${taskList}" var="task">
			<div id="${task.getIdLink()}" class="listElement">
				<div class="listElementTop clickable" onclick="switchListElement(this)">
<%-- Priority --%>
					<div class="right elementPriority ${task.getPriority().toString()}">
				    </div>
<%-- ID --%>
					<div class="left elementId elementContent">
						<c:out value="${task.getId()}"/>
				    </div>
<%-- Label --%>
				    <c:if test="${!task.getLabel().equals(\"\")}">
					    <div class="left elementLabel elementContent labelTag">
							<c:out value="${task.getLabel()}"/>
					    </div>
					</c:if>
<%-- Title --%>
					<div class="left elementName elementContent h3">
						<c:out value="${task.getName()}"/>
				    </div>
<%-- Status --%>
				    <div class="right elementStatus elementContent">
				    	<fmt:message key="${task.getTaskStatus().getNameKey()}"/>
				    </div>
<%-- Assigned --%>
				    <div class="right elementAssigned elementContent userTag">
				    	<c:if test="${task.getAssigned()!=null}">
				    		<c:out value="${task.getAssigned().getName()}"/>
						</c:if>
					</div>
			    	<div class="clear"></div>
			    </div>
			    <div class="listElementBody">
<%-- AssetPath --%>
				    <c:if test="${tab.equals('textures') || tab.equals('models')}">
				    	<div class="elementPath elementContent">
				    		File Path: &#160; &#160; ${task.getNewAssetFolder()}
			    		</div>
				    </c:if>
<%-- Details --%>
				    <div class="elementDetails elementContent">
				    	${fn:replace(fn:escapeXml(task.getDetails()), newLine, "<br>")}
				    </div>
<%-- Comments & Comment Form--%>
					<div class="elementComments">
						<c:forEach items="${task.getComments()}" var="comment">
					    	<div id="comment_${comment.getIdLink()}" class="subListElement">
						    	<div class="left subElementAuthor elementContent">
						    		<div class="userTag left">
						    			<c:out value="${comment.getAuthor().getName()}"/>
					    			</div>
						    	</div>
						    	<div class="right commentButton elementContent button commentEditButton" onclick="changeComment('${cfn:escapeJS(comment.getText())}', '${task.getIdLink()}', '${comment.getIdLink()}')">
									Editieren
								</div>
						    	<div class="left subElementText elementContent">
						    		${fn:replace(fn:escapeXml(comment.getText()), newLine, "<br/>")}
						    	</div>
						    	<div class="clear"></div>
						    </div>
						</c:forEach>
					    <form:form class="subListElement commentForm"
					    		method="POST"
					    		action="tasks/submitComment/${task.getIdLink()}?tab=${tab}"
					    		commandName="comment">
					    	<div class="left subElementAuthor">
					    		<div class="button commentButton commentSubmitButton"
					    				onclick="$(this).parents('form.commentForm').submit()">Submit</div>
					    	</div>
					    	<div class="subElementText input">
					    		<textarea class="commentTextArea" rows="2" cols="1" name="text"></textarea>
					    	</div>
					    </form:form>
				    </div>
<%-- TexturePreview --%>
				    <c:if test="${tab.equals('textures')}">
				    	<div class="elementPreview elementContent">
				    		<div class="subPreviewDescriptor left">
				    			<div class="center">Original</div>
				    		</div>
				    		<div class="subPreviewDescriptor right">
				    			<div class="center">Newest</div>
				    		</div>
				    		<div class="subElementPreview left">
					    		<a href="tasks/preview?small=false&amp;ver=original&amp;id=${task.getIdLink()}">
					    			<img src="tasks/preview?small=true&amp;ver=original&amp;id=${task.getIdLink()}" alt="Original Texture Not Avaliable">
					    		</a>
				    		</div>
				    		<div class="subElementPreview right">
				    			<a href="tasks/preview?small=false&amp;ver=newest&id=${task.getIdLink()}">
				    				<img src="tasks/preview?small=true&amp;ver=newest&id=${task.getIdLink()}" alt="New Texture Not Avaliable">
				    			</a>
				    		</div>
				    		<div class="clear"></div>
				    	</div>
<%-- Files --%>
						<div class="elementFiles elementContent">
							<div class="subElementAssets left">
								<div class="subFilesDescriptor">
									Assets
								</div>
								<div id="assetFilesContainer" class="subFilesContainer"></div>
							</div>
							<div class="subElementWip right">
								<div class="subFilesDescriptor">
									Other
								</div>
								<div id="wipFilesContainer" class="subFilesContainer"></div>
							</div>
							<div class="clear"></div>
<%-- File Operations --%>
							<div class="subElementFileOperations">
								<input id="${task.getIdLink()}-upload" type="file" style="display:none;" onchange="uploadFile(this,'${task.getIdLink()}')"/>
								<div class="button subElementButton left" onclick="$('#${task.getIdLink()}-upload').click()">
									Upload
								</div>
<%-- 								<a id="${task.getIdLink()}-download" href="#">Link</a> --%>
								<div class="button subElementButton left" onclick="downloadFile('${task.getIdLink()}')">
									Download
								</div>
								<div class="button subElementButton right" onclick="confirmDeleteFile('${task.getIdLink()}')">
									Delete
								</div>
								<div class="clear"></div>
							</div>
						</div>
					</c:if>
<%-- Footer --%>
				    <div class="listElementBodyFooter">
					    <div class="left commentElement elementButton button" onclick="findSwitchCommentForm(this)">
				    		<fmt:message key="to.comment"/>
				    	</div>
					    <div class="elementAuthorDate right">
				    		<div class=" elementContent right">
				    			<c:out value="${fn:escapeXml(task.getAuthor().getName())}"/><br/>
					    		<c:out value="${task.getFormattedCreationDate()}"/>
				    		</div>
				    		<div class="elementContent right">
				    			<fmt:message key="author"/>:&#160;&#160;<br/>
					    		<fmt:message key="created"/>:&#160;&#160;
				    		</div>
				    	</div>
				    	<div class="right deleteElement elementButton button" onclick="confirmDeleteTask('${task.getIdLink()}','${cfn:escapeJS(task.getName())}')">
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