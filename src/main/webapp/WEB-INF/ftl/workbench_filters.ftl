<#import "/spring.ftl" as s/>
<#import "macros.ftl" as m/>

<div id="filters">
	<form id="generalFilters"
			action="${request.contextPath}/tasks/submitFilter" method="POST">
		<#-- set path String, set bind to path String, make form available -->
		<#assign path="workbench-generalFilterForm">
		<@s.bind path=path/>
		<#assign filter=.vars[path]>
		
		<div id="switchGeneralFilterBody" class="filterGroup clickable" onclick="switchGeneralFilters()">
			&#160;&#62;&#62;
		</div>
		<div id="generalFilterBody">
			<div class="filterBorder"></div>
			<div class="filterGroup center h3">
				<@s.message "tasks.filter.general"/>
			</div>
			<div class="filterBorder"></div>
			<div class="filterGroup">
				<label>
					<@m.formCheckbox class="generalFiltersFormElement" path=path+".assignedToMe"/>
					<@s.message "tasks.filter.assigned"/>
				</label><br>
				<label>
					<@m.formCheckbox class="generalFiltersFormElement" path=path+".createdByMe"/>
					<@s.message "tasks.filter.created"/>
				</label><br>
			</div>
			<div class="filterBorder"></div>
			<div class="filterGroup">
				<label>
					<@m.formCheckbox id="generalFiltersAllCheckbox" path=path+".all"/>
					<@s.message "tasks.filter.all"/>
				</label><br>
			</div>
			<div class="filterBorder"></div>
			<div class="filterGroup">
				<#list filter.getTaskStatus() as taskStatus>
					<label>
						<@m.formCheckbox class="generalFiltersFormElement generalFiltersAllCheckBoxTarget"
								path=path+".taskStatus[${taskStatus_index}]"/>
						<@s.message "${TaskStatus.values()[taskStatus_index].getNameKey()}"/>
					</label><br>
				</#list>
			</div>
			<div class="filterBorder"></div>
			<div class="filterGroup">
				<#list filter.getPriority() as priority>
					<label>
						<@m.formCheckbox class="generalFiltersFormElement generalFiltersAllCheckBoxTarget"
								path=path+".priority[${priority_index}]"/>
						<@s.message "${TaskPriority.values()[priority_index].getNameKey()}"/>
					</label><br>
				</#list>
			</div>
			<div id="generalFiltersInvisible" class="filterGroup center">
				<input type="checkbox" id="generalFiltersHidden" name="hidden">
				<#-- <input type="submit" value="Apply Filter"> -->
			</div>
		</div>
	</form:form>
</div>