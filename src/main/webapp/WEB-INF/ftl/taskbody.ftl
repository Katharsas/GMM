<#import "/spring.ftl" as s/>
<#assign newLine = "\n"/>

			<div class="task-body" css="display:none;">
			    <div class="task-body-content">
<!-- AssetPath -->
				    <#if !(task.getType().name() == "GENERAL")>
				    	<div class="elementPath elementContent">
				    		File Path: &#160; &#160; ${task.getAssetPath().toString()?html}
			    		</div>
				    </#if>
<!-- Details -->
				    <div class="elementDetails elementContent">
				    	${task.getDetails()?html?replace(newLine,'<br>')}
				    	
<#-- Add 'r' argument for regex replace, use ?json_string or ?js_string or ?js_script to escape JS stuff -->
				    
				    </div>
<!-- Comments & Comment Form-->
					<div class="elementComments">
						<#list task.getComments() as comment>
							<div id="comment_${comment.getIdLink()}" class="subListElement">
						    	<div class="left subElementAuthor elementContent">
						    		<div class="userTag left">
						    			${comment.getAuthor().getName()?html}
					    			</div>
						    	</div>
								<#if isUserLoggedIn && comment.getAuthor().getIdLink() == principal.getIdLink()>
							    	<div class="right commentButton elementContent button commentEditButton"
							    			onclick="changeComment('${comment.getText()?js_string?html}', '${task.getIdLink()}', '${comment.getIdLink()}')">
										<img class="buttonIcon svg" src="${request.contextPath}/res/gfx/edit.svg">
										<#--<@s.message "edit"/>-->
									</div>
								</#if>
						    	<div class="left subElementText elementContent">
						    		${comment.getText()?html?replace(newLine,'<br>')}
						    	</div>
						    	<div class="clear"></div>
						    </div>
						</#list>
						<#if isUserLoggedIn>
							<form class="subListElement commentForm"
						    		method="POST"
						    		action="${request.contextPath}/tasks/submitComment/${task.getIdLink()}">
						    	<@s.bind "comment"/>
						    	<div class="left subElementAuthor">
						    		<div class="button commentButton commentSubmitButton"
						    				onclick="$(this).parents('form.commentForm').submit()">Submit</div>
						    	</div>
						    	<div class="subElementText input">
						    		<textarea class="commentTextArea" rows="2" cols="1" name="text"></textarea>
						    	</div>
						    </form>
						</#if>
				    </div>
<!-- TexturePreview -->
				    <#if task.getType().name() == 'TEXTURE'>
				    	<div class="elementPreview elementContent">
					    	<table>
				    			<colgroup>
							       <col span="1" style="width: 49%;">
							       <col span="1" style="width: 2%;">
							       <col span="1" style="width: 49%;">
							    </colgroup>
								<tr class="subPreviewDescriptor">
							    	<#if task.originalAsset?has_content>
							    		<#assign asset = task.originalAsset/>
								    	<td class="subPreviewHalf leftHalf elementButton button"
								    			onclick="downloadFromPreview('${task.getIdLink()}', 'original')">
								    		<span class="left" style="font-weight:bold">Original:</span>
								    		<span class="right">
								    			${asset.width} x ${asset.height}
								    		</span>
								    		<div class="clear"></div>
								    		<span class="left">${asset.fileName?html}</span>
								    		<span class="right">${asset.sizeInKB} KB</span>
								    	</td>
							    	<#else><td></td>
							    	</#if>
							    	<td></td>
							    	<#if task.newestAsset?has_content>
							    		<#assign asset = task.newestAsset/>
								    	<td class="subPreviewHalf leftHalf elementButton button"
								    			onclick="downloadFromPreview('${task.getIdLink()}', 'newest')">
								    		<span class="left" style="font-weight:bold">Newest:</span>
								    		<span class="right">
								    			${asset.width} x ${asset.height}
								    		</span>
								    		<div class="clear"></div>
								    		<span class="left">${asset.fileName?html}</span>
								    		<span class="right">${asset.sizeInKB} KB</span>
								    	</td>
							    	<#else><td></td>
							    	</#if>
						    	</tr>
						    	<tr class="subPreviewImage center">
						    		<#if task.originalAsset?has_content>
						    			<#assign asset = task.originalAsset/>
							    		<td class="subPreviewHalf leftHalf">
							    			<a href="${request.contextPath}/tasks/preview?small=false&amp;ver=original&id=${task.getIdLink()}">
							    				<img src="${request.contextPath}/tasks/preview?small=true&amp;ver=original&amp;id=${task.getIdLink()}">
								    		</a>
								    	</td>
								    <#else><td></td>
							    	</#if>
							    	<td></td>
							    	<#if task.newestAsset?has_content>
							    		<#assign asset = task.newestAsset/>
								    	<td class="subPreviewHalf rightHalf clickable">
								    		<a href="${request.contextPath}/tasks/preview?small=false&amp;ver=newest&id=${task.getIdLink()}">
								    			<img src="${request.contextPath}/tasks/preview?small=true&amp;ver=newest&amp;id=${task.getIdLink()}">
								    		</a>
								    	</td>
								    <#else><td></td>
							    	</#if>
						    	</tr>
							</table>
						</div>
				    </#if>
<!-- Files -->
					<#if !(task.getType().name() == 'GENERAL')>
						<div class="elementFiles elementContent">
							<#if isUserLoggedIn>
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
<!-- File Operations -->
								<div class="subElementFileOperations">
									<#if isUserLoggedIn>
										<input id="${task.getIdLink()}-upload" type="file" style="display:none;" onchange="uploadFile(this,'${task.getIdLink()}')"/>
										<div class="button subElementButton left" onclick="$('#${task.getIdLink()}-upload').click()">
											Upload
										</div>
									</#if>
	<#-- 								<a id="${task.getIdLink()}-download" href="#">Link</a> -->
									<div class="button subElementButton left" onclick="downloadFile('${task.getIdLink()}')">
										Download
									</div>
									<#if isUserLoggedIn>
									<div class="button subElementButton right" onclick="confirmDeleteFile('${task.getIdLink()}')">
										Delete
									</div>
									</#if>
									<div class="clear"></div>
								</div>
							</#if>
							<#if !isUserLoggedIn>
								You must login to see any files! Use Login button below.
							</#if>
						</div>
					</#if>
				</div>
<!-- Footer -->
			    <div class="task-body-footer">
			    	<div class="task-operations">
				    	<#if isUserLoggedIn>
						    <div class="left elementButton button" onclick="findSwitchCommentForm(this)">
					    		<img class="buttonIcon svg" src="${request.contextPath}/res/gfx/bubble.svg">
					    		<@s.message "to.comment"/>
					    	</div>
					    	<div class="left elementButton button">
						    	<a href="${request.contextPath}/public/linkTasks/${task.getId()}/${task.getLinkKey()}">
					    			<img class="buttonIcon svg" src="${request.contextPath}/res/gfx/link.svg">
						    		Link <span></span>
						    	</a>
						    </div>
					    </#if>
					    <#if !isUserLoggedIn>
					    	<div class="left elementButton button">
						    	<a href="${request.contextPath}/public/login">Login<span></span></a>
						    </div>
					    </#if>
					    <div class="elementAuthorDate right">
				    		<div class=" elementContent right">
				    			${task.getAuthor().getName()?html}<br/>
					    		${task.getFormattedCreationDate()?html}
				    		</div>
				    		<div class="elementContent right">
				    			<@s.message "author"/>:&#160;&#160;<br/>
					    		<@s.message "created"/>:&#160;&#160;
				    		</div>
				    	</div>
				    	<#if isUserLoggedIn>
					    	<div class="right elementButton button" onclick="deleteTask('${task.getIdLink()}','${task.getName()?js_string?html}')">
					    		<img class="buttonIcon svg" src="${request.contextPath}/res/gfx/delete.svg">
					    		<@s.message "delete"/>
					    	</div>
					    	<div class="right elementButton button">
								<a href="${request.contextPath}/tasks?edit=${task.getIdLink()}">
									<img class="buttonIcon svg" src="${request.contextPath}/res/gfx/edit.svg">
									<@s.message "edit"/><span></span>
								</a>
					    	</div>
					    </#if>
				    	<div class="clear"></div>
				    </div>
				    <div class="task-operations-extended">
				    	Extended Operations
				    </div>
		    	</div>
			</div>