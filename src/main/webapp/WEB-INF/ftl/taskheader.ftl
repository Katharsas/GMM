<#import "/spring.ftl" as s/>


			<div id="${task.getIdLink()}" class="list-element">
				<div class="task-header clickable" onclick="switchListElement(this)">
<!-- Priority -->
					<div class="right elementPriority ${task.getPriority().toString()}">
				    </div>
<!-- ID -->
					<div class="left elementId elementContent">
						${task.getId()}
				    </div>
<!-- Label -->
				    <#if task.getLabel()?has_content>
					    <div class="left elementLabel elementContent labelTag">
							${task.getLabel()}
					    </div>
					</#if>
<!-- Title -->
					<div class="left elementName elementContent h3">
						${task.getName()}
				    </div>
<!-- Status -->
				    <div class="right elementStatus elementContent">
				    	<@s.message "${task.getTaskStatus().getNameKey()}"/>
				    </div>
<!-- Assigned -->
				    <div class="right elementAssigned elementContent userTag">
						<#if task.getAssigned()??>
				    		${task.getAssigned().getName()}
				    	</#if>
					</div>
			    	<div class="clear"></div>
			    </div>
			</div>