<#import "/spring.ftl" as s/>
<#assign newLine = "\n"/>

<div class="task-body" css="display:none;">
    <div class="task-body-content">
		<!-- AssetPath -->
	    <#if !(task.getType().name() == "GENERAL")>
	    	<div class="task-assetPath">
	    		<@s.message "tasks.asset_name"/>: ${task.getAssetName().get()?html}
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
<!-- Asset Files -->
		<#if !(task.getType().name() == 'GENERAL')>
	    	<div class="task-assets">
	    	<table>
    			<colgroup>
			       <col span="1" style="width: 49%;">
			       <col span="1" style="width: 2%;">
			       <col span="1" style="width: 49%;">
			    </colgroup>
<!-- Asset File Info -->
			    <tr>
			    	<#if task.originalAsset?has_content>
			    		<#assign asset = task.originalAsset/>
			    		<td class="task-asset-info task-asset-original" data-filename="${asset.filename?html}">
				    		<span class="left" style="font-weight:bold">Original:</span>
				    		<#if task.getType().name() == 'TEXTURE'>
					    		<span class="right">
					    			${asset.width} x ${asset.height}
					    		</span>
					    		<div class="clear"></div>
					    		<span class="left">${asset.filename?html}</span>
					    		<span class="right">${asset.sizeInKB} KB</span>
				    		</#if>
							<#if task.getType().name() == 'MESH'>
								<span class="right">
					    			${asset.polyCount} &#x25E3;
					    		</span>
					    		<div class="clear"></div>
					    		<span class="left">${asset.filename?html}</span>
					    		<span class="right">${asset.sizeInKB} KB</span>
					    	</#if>
				    	</td>
			    	<#else>
			    	<td class="task-asset-info center">
			    		<span>Original asset does not exist.</span>
			    	</td>
			    	</#if>
					<td></td>
					<#if task.newAsset?has_content>
			    		<#assign asset = task.newAsset/>
			    		<td class="task-asset-info task-asset-newest" data-filename="${asset.filename?html}">
				    		<span class="left" style="font-weight:bold">Newest:</span>
				    		<#if task.getType().name() == 'TEXTURE'>
					    		<span class="right">
					    			${asset.width} x ${asset.height}
					    		</span>
					    		<div class="clear"></div>
					    		<span class="left">${asset.filename?html}</span>
					    		<span class="right">${asset.sizeInKB} KB</span>
					    	</#if>
							<#if task.getType().name() == 'MESH'>
								<span class="right">
					    			${asset.polyCount} &#x25E3;
					    		</span>
					    		<div class="clear"></div>
					    		<span class="left">${asset.filename?html}</span>
					    		<span class="right">${asset.sizeInKB} KB</span>
							</#if>
				    	</td>
			    	<#else>
			    	<td class="task-asset-info center">
			    		<span>New asset does not exist.</span>
			    	</td>
			    	</#if>
			    </tr>
<!-- Asset File Operations -->
				<tr>
			    	<#if task.originalAsset?has_content>
			    		<#assign asset = task.originalAsset/>
				    	<td class="task-asset-buttons task-asset-original center">
				    		<div class="action-download task-file-button task-button button">
				    			Download
				    		</div>
				    	</td>
			    	<#else><td></td>
			    	</#if>
			    	<td></td>
			    	<#if task.newAsset?has_content>
			    	<td class="task-asset-buttons task-asset-newest">
			    		<div class="action-download task-file-button task-button button left">
			    			Download
			    		</div>
			    		<div class="action-upload task-file-button task-button button left">
			    			Upload
			    		</div>
			    		<div class="action-delete task-file-button task-button button right">
			    			Delete
			    		</div>
			    		<div class="clear"></div>
			    	</td>
			    	<#else>
			    	<td class="task-asset-buttons task-asset-newest center">
			    		<div class="action-upload task-file-button task-button button">
			    			Upload
			    		</div>
			    	</td>
			    	</#if>
		    	</tr>
<!-- 2D Texture Preview -->
		    	<#if task.getType().name() == 'TEXTURE'>
		    	<tr class="task-previews center">
		    		<#if task.originalAsset?has_content>
		    			<#assign asset = task.originalAsset/>
			    		<td class="task-preview-visual clickable">
			    			<a href="${request.contextPath}/tasks/preview/texture?small=false&ver=original&id=${task.getIdLink()}">
			    				<img src="${request.contextPath}/tasks/preview/texture?small=true&ver=original&id=${task.getIdLink()}">
				    		</a>
				    	</td>
				    <#else><td></td>
			    	</#if>
			    	<td></td>
			    	<#if task.newAsset?has_content>
			    		<#assign asset = task.newAsset/>
				    	<td class="task-preview-visual clickable">
				    		<a href="${request.contextPath}/tasks/preview/texture?small=false&ver=newest&id=${task.getIdLink()}&nocache=${task.getNewestAssetNocache()}">
				    			<img src="${request.contextPath}/tasks/preview/texture?small=true&ver=newest&id=${task.getIdLink()}&nocache=${task.getNewestAssetNocache()}">
				    		</a>
				    	</td>
				    <#else><td></td>
			    	</#if>
		    	</tr>
		    	</#if>
<!-- 3D Mesh Preview -->
				<#if task.getType().name() == 'MESH'>
					<tr class="task-previews task-preview-3D center">
			    		<#if task.originalAsset?has_content>
			    			<#assign asset = task.originalAsset/>
				    		<td class="task-preview-visual clickable"
				    			data-url="${request.contextPath}/tasks/preview/3Dmodel?ver=original&id=${task.getIdLink()}">
				    			<a href="${request.contextPath}/tasks/preview/3Dmodel/full?ver=original&id=${task.getIdLink()}">
				    				<canvas></canvas>
					    		</a>
					    	</td>
					    <#else><td></td>
				    	</#if>
				    	<td></td>
				    	<#if task.newAsset?has_content>
				    		<#assign asset = task.newAsset/>
					    	<td class="task-preview-visual clickable"
					    		data-url="${request.contextPath}/tasks/preview/3Dmodel?ver=newest&id=${task.getIdLink()}&nocache=${task.getNewestAssetNocache()}">
					    		<a href="${request.contextPath}/tasks/preview/3Dmodel/full?ver=newest&id=${task.getIdLink()}&nocache=${task.getNewestAssetNocache()}">
					    			<canvas></canvas>
					    		</a>
					    	</td>
					    <#else><td></td>
				    	</#if>
				    </tr>
<!-- 3D Mesh Preview Options -->
				    <tr>
			    		<td class="task-preview-renderOptions" colspan="3">
			    			<div class="renderOptionsText left">Rendering:</div>
			    			<div class="renderOptionGroup button-group left">
		    					<div class="task-button button left renderOption-solid active">Solid</div>
			    				<div class="task-button button left renderOption-wire">Wireframe</div>
			    				<div class="clear"></div>
			    			</div>
			    			<div class="renderOptionGroup right">
			    				<label class="renderOptionsText renderOption-shadows"><input type="checkbox">Shadows</label>
			    				<label class="renderOptionsText renderOption-rotLight"><input type="checkbox">Rotate Light</label>
			    				<label class="renderOptionsText renderOption-rotCamera"><input type="checkbox">Rotate Camera at speed:</label>
			    				<input class="renderOptionsSpeed renderOption-rotCameraSpeed" type="text" style="width:50px">
			    			</div>
			    			<div class="clear"></div>
			    		</td>
			    	</tr>
<!-- 3D Mesh Preview Textures -->
			    	<tr>
			    		<#if task.originalAsset?has_content>
				    		<td class="task-asset-model-textures">
				    			<ul>
				    				<#list task.originalAsset.textureNames as textureName>
						    			<li>${textureName?html}</li>
									</#list>
				    			</ul>
						    </td>
						<#else><td></td>
						</#if>
						<td></td>
						<#if task.newAsset?has_content>
				    		<td class="task-asset-model-textures">
				    			<ul>
							    	<#list task.newAsset.textureNames as textureName>
							    		<li>${textureName?html}</li>
									</#list>
								</ul>
						    </td>
						<#else><td></td>
						</#if>
			    	</tr>
				</#if>
<!-- WIP Files -->
				<tr>
					<td class="noborder"></td>
					<td></td>
					<td class="task-files-wip">
						<#if isUserLoggedIn>
							<div>
								<div class="task-files-description">
									WIP Files:
								</div>
								<div class="task-files-wip-tree"></div>
							</div>
							<div class="clear"></div>
							<div class="task-files-wip-operations">
								<div class="action-download task-file-button task-button button left">
									Download
								</div>
								<input class="task-files-uploadInput" type="file" style="display:none;"/>
								<div class="action-upload task-file-button task-button button left">
									Upload
								</div>
								<div class="action-delete task-file-button task-button button right">
									Delete
								</div>
								<div class="clear"></div>
							</div>
						<#else>
							You must login to see any files! Use Login button below.
						</#if>
					</td>
				</tr>
			</table>
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
		    	<div class="task-operations-unpin left task-button button">
		    		Unpin
		    	</div>
	    		<div class="task-operations-pin left task-button button">
		    		Pin
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
		    	<div class="task-operations-editTask right task-button button">
					<img class="buttonIcon svg" src="${request.contextPath}/res/gfx/edit.svg">
					<@s.message "edit"/><span></span>
		    	</div>
		    </#if>
	    	<div class="clear"></div>
	    </div>
	    <div class="task-operations-extended">
	    	<!-- Extended Operations  -->
	    </div>
   	</div>
</div>