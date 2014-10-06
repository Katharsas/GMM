			    <#--
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
								<c:if test="${comment.getAuthor().getName().equals(pageContext.request.userPrincipal.name)}">
							    	<div class="right commentButton elementContent button commentEditButton"
							    			onclick="changeComment('${cfn:escapeJS(comment.getText())}', '${task.getIdLink()}', '${comment.getIdLink()}')">
										Editieren
									</div>
								</c:if>
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
					    	<table>
				    			<colgroup>
							       <col span="1" style="width: 49%;">
							       <col span="1" style="width: 2%;">
							       <col span="1" style="width: 49%;">
							    </colgroup>
							    <c:set value="${task.originalAsset}" var="asset"/>
								<tr class="subPreviewDescriptor">
							    	<c:if test="${!(empty asset)}">
								    	<td class="subPreviewHalf leftHalf elementButton button"
								    			onclick="downloadFromPreview('${task.getIdLink()}', 'original')">
								    		<span class="left" style="font-weight:bold">Original:</span>
								    		<span class="right">
								    			${asset.width} x ${asset.height}
								    		</span>
								    		<div class="clear"></div>
								    		<span class="left">${asset.path.fileName}</span>
								    		<span class="right">${asset.sizeInKB} KB</span>
								    	</td>
							    	</c:if>
							    	<c:if test="${empty asset}"><td></td></c:if>
							    	<td></td>
							    	<c:set value="${task.newestAsset}" var="asset"/>
							    	<c:if test="${!(empty asset)}">
								    	<td class="subPreviewHalf leftHalf elementButton button"
								    			onclick="downloadFromPreview('${task.getIdLink()}', 'newest')">
								    		<span class="left" style="font-weight:bold">Newest:</span>
								    		<span class="right">
								    			${asset.width} x ${asset.height}
								    		</span>
								    		<div class="clear"></div>
								    		<span class="left">${asset.path.fileName}</span>
								    		<span class="right">${asset.sizeInKB} KB</span>
								    	</td>
							    	</c:if>
							    	<c:if test="${empty asset}"><td></td></c:if>
						    	</tr>
						    	<tr class="subPreviewImage center">
						    		<c:if test="${!(empty task.originalAsset)}">
							    		<td class="subPreviewHalf leftHalf">
							    			<a href="tasks/preview?small=false&amp;ver=original&id=${task.getIdLink()}">
							    				<img src="tasks/preview?small=true&amp;ver=original&amp;id=${task.getIdLink()}">
								    		</a>
								    	</td>
								    </c:if>
								    <c:if test="${empty task.originalAsset}"><td></td></c:if>
							    	<td></td>
							    	<c:if test="${!(empty task.newestAsset)}">
								    	<td class="subPreviewHalf rightHalf clickable">
								    		<a href="tasks/preview?small=false&amp;ver=newest&id=${task.getIdLink()}">
								    			<img src="tasks/preview?small=true&amp;ver=newest&amp;id=${task.getIdLink()}">
								    		</a>
								    	</td>
								    </c:if>
								    <c:if test="${empty task.newestAsset}"><td></td></c:if>
						    	</tr>
							</table>
						</div>
				    </c:if>
<%-- Files --%>
					<c:if test="${tab.equals('textures') || tab.equals('models')}">
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
			    </div>-->