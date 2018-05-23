<#import "/spring.ftl" as s/>

<div id="${task.getIdLink()}" class="list-element task collapsed">
	<div class="task-header clickable">
		<div class="task-header-left">
			<div class="task-id">
				${task.getId()}
			</div>
			<div class="task-label labelTag">
		    	<#if task.getLabel()?has_content>
			    	${task.getLabel()}
				</#if>
			</div>
			<div class="task-name">
				${task.getName()}
		    </div>
		</div>
	    <div class="task-header-right">
	    	<#if task.getAssigned()??>
			    <div class="task-assigned userTag">
			    	${task.getAssigned().getName()}
			    </div>
			</#if>
		    <div class="task-status">
		    	<@s.message task.getTaskStatus().getNameKey() />
		    </div>
			<div class="task-priority ${task.getPriority().toString()}">
				&nbsp;
			</div>
	    </div>
    </div>
</div>