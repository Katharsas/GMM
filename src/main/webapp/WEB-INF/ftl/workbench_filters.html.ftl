<#import "/spring.ftl" as s/>
<#import "macros.html.ftl" as m/>

<#-- set path String, set bind to path String, make form available -->
<#assign path="workbench-generalFilterForm">
<@s.bind path=path/>
<#assign filter=.vars[path]>

<form id="generalFilters">
	<div id="generalFilters-body">
		<div class="generalFilters-group left">
			<label>
				<@m.formCheckbox class="generalFilters-notarget" path=path+".assignedToMe"/>
				<@s.message "tasks.filter.assigned"/>
			</label><br>
			<label>
				<@m.formCheckbox class="generalFilters-notarget" path=path+".createdByMe"/>
				<@s.message "tasks.filter.created"/>
			</label><br>
			<label class="generalFilters-space">
				<input type="checkbox"/>.
			</label><br>
			<label>
				<@m.formCheckbox id="generalFilters-all" path=path+".all"/>
				<@s.message "tasks.filter.all"/>
			</label><br>
		</div>
		<div class="generalFilters-group left">
			<#list filter.getTaskStatus() as taskStatus>
				<label>
					<@m.formCheckbox class="generalFilters-all-target"
							path=path+".taskStatus[${taskStatus_index}]"/>
					<@s.message TaskStatus.values()[taskStatus_index].getNameKey() />
				</label><br>
			</#list>
		</div>
		<div class="generalFilters-group left">
			<#list filter.getPriority() as priority>
				<label>
					<@m.formCheckbox class="generalFilters-all-target"
							path=path+".priority[${priority_index}]"/>
					<@s.message TaskPriority.values()[priority_index].getNameKey() />
				</label><br>
			</#list>
		</div>
		<div class="clear"></div>
	</div>
</form>