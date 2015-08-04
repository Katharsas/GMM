<#import "/spring.ftl" as s/>
<#assign newLine = "\n"/>

<div class="task-body" css="display:none;">
    <div class="task-body-content">
		<!-- AssetPath -->
	    <#if !(task.getType().name() == "GENERAL")>
	    	<div class="task-assetPath">
	    		File Path: &#160; &#160; ${task.getAssetPath().toString()?html}
    		</div>
	    </#if>
		<!-- Details -->
		<div class="task-details">
			${task.getDetails()?html?replace(newLine,'<br>')}
				    	
<#-- Add 'r' argument for regex replace, use ?json_string or ?js_string or ?js_script to escape JS stuff -->
				    
		</div>
<!-- Comments & Comment Form-->
		<div class="task-comments">
			<#list task.getComments() as comment>
				<div id="${comment.getIdLink()}" class="task-comment">
			    	<div class="task-comment-author left">
			    		<div class="userTag left">
			    			${comment.getAuthor().getName()?html}
		    			</div>
			    	</div>
					<#if isUserLoggedIn && comment.getAuthor().getIdLink() == principal.getIdLink()>
				    	<div class="task-comment-editButton right button">
							<img class="buttonIcon svg" src="${request.contextPath}/res/gfx/edit.svg">
							<#--<@s.message "edit"/>-->
						</div>
					</#if>
			    	<div class="task-comment-text left">
			    		${comment.getText()?html?replace(newLine,'<br>')}
			    	</div>
			    	<div class="clear"></div>
			    </div>
			</#list>
			<#if isUserLoggedIn>
				<form class="task-comments-form task-comment" style="display:none;">
			    	<@s.bind "commentForm"/>
			    	<div class="task-comment-author left">
			    		<div class="task-comment-form-submitButton button">Submit</div>
			    	</div>
			    	<div class="task-comment-text input">
			    		<textarea class="task-comments-form-textArea" rows="2" cols="1" name="text"></textarea>
			    	</div>
			    </form>
			</#if>
	    </div>
<!-- TexturePreview -->
	    <#if task.getType().name() == 'TEXTURE'>
<!-- 	    	<div> -->
		    	<table class="task-preview">
	    			<colgroup>
				       <col span="1" style="width: 49%;">
				       <col span="1" style="width: 2%;">
				       <col span="1" style="width: 49%;">
				    </colgroup>
					<tr class="task-preview-buttons">
				    	<#if task.originalAsset?has_content>
				    		<#assign asset = task.originalAsset/>
					    	<td class="task-preview-button-original task-button button">
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
					    	<td class="task-preview-button-newest task-button button">
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
			    	<tr class="task-preview-images center">
			    		<#if task.originalAsset?has_content>
			    			<#assign asset = task.originalAsset/>
				    		<td class="task-preview-image clickable">
				    			<a href="${request.contextPath}/tasks/preview?small=false&ver=original&id=${task.getIdLink()}">
				    				<img src="${request.contextPath}/tasks/preview?small=true&amp;ver=original&amp;id=${task.getIdLink()}">
					    		</a>
					    	</td>
					    <#else><td></td>
				    	</#if>
				    	<td></td>
				    	<#if task.newestAsset?has_content>
				    		<#assign asset = task.newestAsset/>
					    	<td class="task-preview-image clickable">
					    		<a href="${request.contextPath}/tasks/preview?small=false&amp;ver=newest&id=${task.getIdLink()}&nocache=${task.getNewestAssetNocache()}">
					    			<img src="${request.contextPath}/tasks/preview?small=true&amp;ver=newest&amp;id=${task.getIdLink()}&nocache=${task.getNewestAssetNocache()}">
					    		</a>
					    	</td>
					    <#else><td></td>
				    	</#if>
			    	</tr>
				</table>
<!-- 			</div> -->
	    </#if>
<!-- Files -->
		<#if !(task.getType().name() == 'GENERAL')>
			<div class="task-files">
				<#if isUserLoggedIn>
					<div class="task-files-assets left">
						<div class="task-files-description">
							Assets
						</div>
						<div class="task-files-assets-tree"></div>
					</div>
					<div class="task-files-other right">
						<div class="task-files-description">
							Other
						</div>
						<div class="task-files-other-tree"></div>
					</div>
					<div class="clear"></div>
<!-- File Operations -->
					<div class="task-files-operations">
						<#if isUserLoggedIn>
							<input class="task-files-uploadInput" type="file" style="display:none;"/>
							<div class="task-files-button-upload button left">
								Upload
							</div>
						</#if>
						<div class="task-files-button-download button left">
							Download
						</div>
						<#if isUserLoggedIn>
						<div class="task-files-button-delete button right">
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
			    <div class="task-operations-switchComment left task-button button">
		    		<img class="buttonIcon svg" src="${request.contextPath}/res/gfx/bubble.svg">
		    		<@s.message "to.comment"/>
		    	</div>
		    	<div class="left task-button button">
			    	<a href="${request.contextPath}/public/link/${task.getId()}/${task.getLinkKey()}">
		    			<img class="buttonIcon svg" src="${request.contextPath}/res/gfx/link.svg">
			    		Link <span></span>
			    	</a>
			    </div>
		    </#if>
		    <#if !isUserLoggedIn>
		    	<div class="left task-button button">
			    	<a href="${request.contextPath}/public/login">Login<span></span></a>
			    </div>
		    </#if>
		    <div class="task-authorDate right">
	    		<div class="right">
	    			${task.getAuthor().getName()?html}<br/>
		    		${task.getFormattedCreationDate()?html}
	    		</div>
	    		<div class="right">
	    			<@s.message "author"/>:&#160;&#160;<br/>
		    		<@s.message "created"/>:&#160;&#160;
	    		</div>
	    	</div>
	    	<#if isUserLoggedIn>
		    	<div class="task-operations-deleteTask right task-button button">
		    		<img class="buttonIcon svg" src="${request.contextPath}/res/gfx/delete.svg">
		    		<@s.message "delete"/>
		    	</div>
		    	<div class="right task-button button">
					<a href="${request.contextPath}/tasks?edit=${task.getIdLink()}">
						<img class="buttonIcon svg" src="${request.contextPath}/res/gfx/edit.svg">
						<@s.message "edit"/><span></span>
					</a>
		    	</div>
		    </#if>
	    	<div class="clear"></div>
	    </div>
	    <div class="task-operations-extended">
	    	<!-- Extended Operations  -->
	    </div>
   	</div>
</div>