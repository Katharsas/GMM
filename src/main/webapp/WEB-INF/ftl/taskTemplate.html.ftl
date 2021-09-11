<#import "/spring.ftl" as s/>
<#import "macros.html.ftl" as m/>

<#assign path="taskTemplate">
<@s.bind path=path/>
<#assign taskForm=.vars[path]>

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
	<!-- ####################### STATUS NO NEW ASSET -->
	<div class="taskForm-element">
		<div class="taskForm-description">
			<@s.message "tasks.template.status.noasset" />
		</div>
        <div class="taskForm-input input">&#160;</div><!-- This is a layout hack to give description a full line -->
	</div>
    <div class="taskForm-element">
		<div class="taskForm-input input">
			<@m.formSelectEnum path=(path+".statusNoNewAsset") enum=taskForm.statusNoNewAsset />
		</div>
	</div>
    <!-- ####################### STATUS NEW ASSET -->
	<div class="taskForm-element">
		<div class="taskForm-description">
			<@s.message "tasks.template.status.withasset" />
		</div>
		<div class="taskForm-input input">&#160;</div><!-- This is a layout hack to give description a full line -->
	</div>
    <div class="taskForm-element">
        <div class="taskForm-input input">
            <@m.formSelectEnum path=(path+".statusWithNewAsset") enum=taskForm.statusWithNewAsset />
        </div>
    </div>
	
</div>
