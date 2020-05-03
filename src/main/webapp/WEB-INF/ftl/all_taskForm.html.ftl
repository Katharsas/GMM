<#import "/spring.ftl" as s/>
<#import "macros.html.ftl" as m/>

<#assign path="taskForm">
<@s.bind path=path/>
<#assign taskForm=.vars[path]>

<div id="taskForm-group-type" class="taskForm-group">
	<!-- ####################### TYPE -->
	<div id="taskForm-element-type" class="taskForm-element">
		<div class="taskForm-description">
			<@s.message taskForm.type.getTypeKey() />
		</div>
		<div class="taskForm-input input">
			<@m.formSelectEnum path=(path+".type")
				enum = taskForm.type
				class = "taskForm-type-select" />
		</div>
	</div>
	<!-- ####################### ASSET NAME -->
	<div class="taskForm-element taskForm-element-path">
		<div class="taskForm-hint">
			<@s.message "all_taskForm.text" /><br/>
			<@s.message "all_taskForm.text2" /><br/>
		</div>
		<div class="taskForm-description">
			<@s.message "all_taskForm.asset_name" />
		</div>
		<div class="taskForm-input input">
			<@m.formInput path=(path+".assetName") value="" />
		</div>
	</div>
</div>
<div class="taskForm-group">
	<!-- ####################### TITLE -->
	<div class="taskForm-element taskForm-element-name">
		<div class="taskForm-description">
			<@s.message "tasks.title" />
		</div>
		<div class="taskForm-input input">
			<@m.formInput path=(path+".name") class="taskForm-name-input" />
		</div>
	</div>
</div>
<!-- ####################### DETAILS -->
<div class="taskForm-group textArea">
	<div class="taskForm-element textArea">
		<div class="taskForm-description">
			<@s.message "tasks.details"/>
		</div>
		<div class="taskForm-input input">
			<@m.formTextarea path=(path+".details") rows="4" cols="1" />
		</div>
	</div>
</div>
<div class="taskForm-group">
	<!-- ####################### LABEL -->
	<div class="taskForm-element">
		<div class="taskForm-description">
			<@s.message "tasks.label"/>
		</div>
		<div class="taskForm-input input">
			<@m.formInput path=(path+".label") autocomplete="off" />
		</div>
	</div>
	<div class="taskForm-element">
		<div class="taskForm-description">&#160;</div>
		<div class="taskForm-input input">
			<@m.formSelect path=(path+".labelSelect")>
				<@m.formOption value="" label="" />
				<#list taskLabels as singleLabel>
					<@m.formOption value=singleLabel label=singleLabel />
				</#list>
			</@m.formSelect>
		</div>
	</div>
</div>
<div class="taskForm-group">
	<!-- ####################### PRIORITY -->
	<div class="taskForm-element">
		<div class="taskForm-description">
			<@s.message taskForm.priority.getTypeKey() />
		</div>
		<div class="taskForm-input input">
			<@m.formSelectEnum path=(path+".priority")
				enum = taskForm.priority/>
		</div>
	</div>
	<!-- ####################### STATUS -->
	<div class="taskForm-element">
		<div class="taskForm-description">
			<@s.message taskForm.status.typeKey />
		</div>
		<div class="taskForm-input input">
			<@m.formSelectEnum path=(path+".status") enum=taskForm.status />
		</div>
	</div>
	<!-- ####################### ASSIGNED -->
	<div class="taskForm-element">
		<div class="taskForm-description">
			<@s.message "tasks.assigned"/>
		</div>
		<div class="taskForm-input input">
			<@m.formSelect path=(path+".assigned")>
				<@m.formOption value="" label="" />
				<#list users as current>
					<@m.formOption value=current.name  label=current.name />
				</#list>
			</@m.formSelect>
		</div>
	</div>
</div>
