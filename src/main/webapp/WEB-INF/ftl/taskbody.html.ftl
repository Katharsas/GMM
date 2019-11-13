<#import "/spring.ftl" as s/>
<#assign newLine = "\n"/>

<div class="task-body" css="display:none;">
    <div class="task-body-content">
		<!-- AssetPath -->
	    <#if !(task.getType().name() == "GENERAL")>
	    	<div class="task-assetPath">
	    		<@s.message "tasks.asset_name"/>: ${task.getAssetName().get()}
    		</div>
	    </#if>
		<!-- Details -->
		<#if task.getDetails()?has_content>
			<div class="task-details">
				${task.getDetails()?esc?markup_string?replace(newLine,'<br>')?no_esc}
			</div>
		</#if>
<!-- Comments & Comment Form-->
		<div class="task-comments">
			<#list task.getComments() as comment>
				<div id="${comment.getIdLink()}" class="task-comment">
			    	<div class="task-comment-author left">
			    		<div class="userTag left">
			    			${comment.getAuthor().getName()}
		    			</div>
			    	</div>
					<#if isUserLoggedIn && comment.getAuthor().getIdLink() == principal.getIdLink()>
				    	<div class="task-comment-editButton right button">
							<img class="buttonIcon svg" src="${request.contextPath}/res/gfx/edit.svg">
							<#--<@s.message "edit"/>-->
						</div>
					</#if>
			    	<div class="task-comment-text left">
			    		${comment.getText()?esc?markup_string?replace(newLine,'<br>')?no_esc}
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
			       <col span="1" class="task-assets-column">
			       <col span="1" class="task-assets-column-seperator">
			       <col span="1" class="task-assets-column">
			    </colgroup>
<!-- Asset File Info -->
			    <tr>
			    	<#if task.originalAssetProperties?has_content>
			    		<#assign asset = task.originalAssetProperties/>
			    		<#assign info = task.originalAssetFileInfo/>
			    		<td class="task-asset-info task-asset-original" 
			    				data-filename="${info.assetFileName}" title="${info.displayPath}">
				    		<span class="left" style="font-weight:bold">Original:</span>
				    		<#if task.getType().name() == 'TEXTURE'>
					    		<span class="right">
					    			${asset.width} x ${asset.height}
					    		</span>
					    		<div class="clear"></div>
					    		<span class="left">${info.assetFileName}</span>
					    		<span class="right">${asset.sizeInKB} KB</span>
				    		</#if>
							<#if task.getType().name() == 'MESH'>
								<span class="right">
					    			${asset.polyCount} &#x25E3;
					    		</span>
					    		<div class="clear"></div>
					    		<span class="left">${info.assetFileName}</span>
					    		<span class="right">${asset.sizeInKB} KB</span>
					    	</#if>
				    	</td>
			    	<#else>
						<#if isUserLoggedIn>
							<td class="task-asset-info center">
								<span><@s.message "asset.original.null"/></span>
							</td>
						<#else><td></td>
						</#if>
			    	</#if>
					<td></td>
					<#if task.newAssetProperties?has_content>
			    		<#assign asset = task.newAssetProperties/>
			    		<#assign info = task.newAssetFolderInfo/>
			    		<td class="task-asset-info task-asset-newest" 
			    				data-filename="${info.assetFileName}" title="${info.displayPath}">
				    		<span class="left" style="font-weight:bold">Newest:</span>
				    		<#if task.getType().name() == 'TEXTURE'>
					    		<span class="right">
					    			${asset.width} x ${asset.height}
					    		</span>
					    		<div class="clear"></div>
					    		<span class="left">${info.assetFileName}</span>
					    		<span class="right">${asset.sizeInKB} KB</span>
					    	</#if>
							<#if task.getType().name() == 'MESH'>
								<span class="right">
					    			${asset.polyCount} &#x25E3;
					    		</span>
					    		<div class="clear"></div>
					    		<span class="left">${info.assetFileName}</span>
					    		<span class="right">${asset.sizeInKB} KB</span>
							</#if>
				    	</td>
			    	<#else>
						<#if isUserLoggedIn>
							<#if task.newAssetFolderInfo?has_content>
								<#assign info = task.newAssetFolderInfo/>
								<#if info.status.isValid()>
									<td class="task-asset-info center">
										<span><@s.message "${info.status.messageKey}"/></span>
									</td>
								<#else><td></td>
								</#if>
							<#else>
								<td class="task-asset-info center">
									<span><@s.message "asset.new.null"/></span>
								</td>
							</#if>
						<#else><td></td>
						</#if>
			    	</#if>
			    </tr>
<!-- Asset File Operations -->
				<#if isUserLoggedIn>
					<tr>
						<#if task.originalAssetProperties?has_content>
							<td class="task-asset-buttons task-asset-original center">
								<div class="action-download task-file-button task-button button">
									Download
								</div>
							</td>
						<#else><td></td>
						</#if>
						<td></td>
						<#if task.newAssetProperties?has_content>
							<td class="task-asset-buttons task-asset-newest">
								<div class="action-download task-file-button task-button button left">
									Download
								</div>
								<input class="action-upload-input" type="file" style="display:none;"/>
								<div class="action-upload task-file-button task-button button left">
									Upload
								</div>
								<div class="action-delete task-file-button task-button button right">
									Delete
								</div>
								<div class="clear"></div>
							</td>
						<#else>
							<#if task.newAssetFolderInfo?has_content>
								<#assign info = task.newAssetFolderInfo/>
								<#if info.status.isValid()>
									<td class="task-asset-buttons task-asset-newest center">
										<input class="action-upload-input" type="file" style="display:none;"/>
										<div class="action-upload task-file-button task-button button">
											Upload
										</div>
									</td>
								<#else><td></td>
								</#if>
							<#else>
								<td class="task-asset-buttons task-asset-newest center">
									<div class="action-folder task-file-button task-button button">
										Create folder
									</div>
								</td>
							</#if>
						</#if>
					</tr>
				</#if>
<!-- 2D Texture Preview -->
		    	<#if task.getType().name() == 'TEXTURE'>
		    	<tr class="task-previews center">
		    		<#if task.originalAssetProperties?has_content>
			    		<td class="task-preview-visual clickable">
			    			<a target="_blank" href="${request.contextPath}/tasks/preview/texture/${task.getLinkKey()}?small=false&ver=original&id=${task.getIdLink()}&nocache=${task.assetName}">
			    				<img class="lazyload" data-src="${request.contextPath}/tasks/preview/texture/${task.getLinkKey()}?small=true&ver=original&id=${task.getIdLink()}&nocache=${task.assetName}">
				    		</a>
				    	</td>
				    <#else><td></td>
			    	</#if>
			    	<td></td>
			    	<#if task.newAssetProperties?has_content>
				    	<td class="task-preview-visual clickable">
				    		<a target="_blank" href="${request.contextPath}/tasks/preview/texture/${task.getLinkKey()}?small=false&ver=newest&id=${task.getIdLink()}&nocache=${task.newestAssetCacheKey}">
				    			<img class="lazyload" data-src="${request.contextPath}/tasks/preview/texture/${task.getLinkKey()}?small=true&ver=newest&id=${task.getIdLink()}&nocache=${task.newestAssetCacheKey}">
				    		</a>
				    	</td>
				    <#else>
				    	<#if task.newAssetFolderInfo?has_content>
				    		<#assign info = task.newAssetFolderInfo/>
				    		<#if !info.status.isValid()>
				    			<td class="task-asset-info center task-asset-invalid">
			    				<span class="error"><@s.message "${info.status.messageKey}"/></span><br>
			    				<#list info.errorPaths as error>
			    					<div class="path">>&nbsp;&nbsp;${error}</div>
			    				</#list>
					    	</td>
					    	<#else><td></td>
				    		</#if>
						<#else><td></td>
			    		</#if>
			    	</#if>
		    	</tr>
		    	</#if>
<!-- 3D Mesh Preview -->
				<#if task.getType().name() == 'MESH'>
					<tr class="task-previews task-preview-3D center" tabindex="-1">
			    		<#if task.originalAssetProperties?has_content>
				    		<td class="task-preview-visual clickable"
				    			data-url="${request.contextPath}/tasks/preview/3Dmodel/${task.getLinkKey()}?ver=original&id=${task.getIdLink()}&nocache=${task.assetName}">
				    			<canvas></canvas>
					    	</td>
					    <#else><td></td>
				    	</#if>
				    	<#if task.originalAssetProperties?has_content || task.newAssetFolderInfo?has_content>
					    	<td class="task-preview-maximize">
					    		<div class="task-preview-maximize-centerer">
					    			<div class="button">
					    				<img class="buttonIcon svg" src="${request.contextPath}/res/gfx/maximize.svg">
					    			</div>
					    		</div>
					    	</td>
					    <#else><td></td>
				    	</#if>
				    	<#if task.newAssetProperties?has_content>
					    	<td class="task-preview-visual clickable"
					    		data-url="${request.contextPath}/tasks/preview/3Dmodel/${task.getLinkKey()}?ver=newest&id=${task.getIdLink()}&nocache=${task.newestAssetCacheKey}">
					    		<canvas></canvas>
					    	</td>
					    <#else>
					    	<#if task.newAssetFolderInfo?has_content>
					    		<#assign info = task.newAssetFolderInfo/>
					    		<#if !info.status.isValid()>
					    			<td class="task-asset-info center task-asset-invalid">
				    				<span class="error"><@s.message "${info.status.messageKey}"/></span><br>
				    				<#list info.errorPaths as error>
				    					<div class="path">>&nbsp;&nbsp;${error}</div>
				    				</#list>
						    	</td>
						    	<#else><td></td>
					    		</#if>
							<#else><td></td>
				    		</#if>
				    	</#if>
				    </tr>
<!-- 3D Mesh Preview Options -->
					<#if task.originalAssetProperties?has_content || task.newAssetProperties?has_content>
					    <tr class="task-preview-options">
				    		<td class="task-preview-renderOptions" colspan="3">
				    			<div class="renderOptionsText left">Shading:</div>
				    			<div class="renderOptionGroup button-group left">
				    				<div class="task-button button left renderOption renderOption-matcap active">Matcap</div>
			    					<div class="task-button button left renderOption renderOption-solid">Solid</div>
				    				<div class="task-button button left renderOption renderOption-none">None</div>
				    				<div class="clear"></div>
				    			</div>
				    			<div class="renderOptionGroup right">
				    				<label class="renderOptionsText renderOption-wire"><input type="checkbox">Wireframe</label>
				    				<!--<label class="renderOptionsText renderOption-shadows"><input type="checkbox">Shadows</label>-->
				    				<label class="renderOptionsText renderOption-rotLight"><input type="checkbox">Rotate Light</label>
				    				<label class="renderOptionsText renderOption-rotCamera"><input type="checkbox">Rotate Camera at speed:</label>
				    				<input class="renderOptionsSpeed renderOption-rotCameraSpeed" type="text" style="width:50px">
				    			</div>
				    			<div class="clear"></div>
				    		</td>
				    	</tr>
<!-- 3D Mesh Preview Textures -->
				    	<tr>
				    		<#if task.originalAssetProperties?has_content>
					    		<td class="task-asset-model-textures">
									<#if isUserLoggedIn>
										<ul>
											<#list task.originalAssetProperties.viewModel.texturesWithoutTasks as textureName>
												<li>
													&bull;<span>${textureName.get()}</span>
												</li>
											</#list>
										</ul>
										<ul class="task-asset-model-textures-tasks">
											<#list task.originalAssetProperties.viewModel.texturesWithTasks as textureTask>
												<li data-id="${textureTask.getIdLink()}">
													&bull;<span class="clickable">${textureTask.assetName.get()}</span>
												</li>
											</#list>
										</ul>
									<#else>
										<#list task.originalAssetProperties.textureNames as textureName>
											<li>
												&bull;<span>${textureName.get()}</span>
											</li>
										</#list>
									</#if>
							    </td>
							<#else><td></td>
							</#if>
							<td></td>
							<#if task.newAssetProperties?has_content>
					    		<td class="task-asset-model-textures">
									<#if isUserLoggedIn>
										<ul>
											<#list task.newAssetProperties.viewModel.texturesWithoutTasks as textureName>
												<li>
													&bull;<span>${textureName.get()}</span>
												</li>
											</#list>
										</ul>
										<ul class="task-asset-model-textures-tasks">
												<#list task.newAssetProperties.viewModel.texturesWithTasks as textureTask>
												<li data-id="${textureTask.getIdLink()}">
													&bull;<span class="clickable">${textureTask.assetName.get()}</span>
												</li>
											</#list>
										</ul>
									<#else>
										<#list task.newAssetProperties.textureNames as textureName>
											<li>
												&bull;<span>${textureName.get()}</span>
											</li>
										</#list>
									</#if>
							    </td>
							<#else><td></td>
							</#if>
				    	</tr>
				    </#if>
				</#if>
<!-- WIP Files -->
				<tr>
					<td class="noborder"></td>
					<td></td>
					<#if task.newAssetFolderInfo?has_content>
				    	<#assign info = task.newAssetFolderInfo/>
				    	<#if info.status.isValid()>
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
										<input class="action-upload-input" type="file" style="display:none;"/>
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
				    	 <#else>
				    	 	<td class="noborder"></td>
				    	</#if>
				    <#else>
				    	<td class="noborder"></td>
				    </#if>
					
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
	    			${task.getAuthor().getName()}<br/>
		    		${task.getFormattedCreationDate(request.locale)}
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