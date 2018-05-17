<#import "/spring.ftl" as s/>
<#import "macros.html.ftl" as m/>

<#-- set path String, set bind to path String, make form available -->
<#assign path="workbench-loadForm">
<@s.bind path=path/>
<#assign load=.vars[path]>

<div id="workbench-load-wrapper">

	<div id="workbench-load-typeButtons">
		<#list TaskType.values() as type>
			<div class="button workbench-load-typeButton" data-type="${type.name()}">
				<@s.message type.getNameKey() />
			</div>
		</#list>
	</div>
	
	
	<form id="workbench-loadForm">
		<div id="workbench-load-formWrapper">
		
			<div class="input">
				Load Operation:
				<@m.formSelect class="form-element" path=(path+".loadOperation")>
					<#list LoadOperation.values() as operation>
						<@m.formOption value=operation label=operation.name() />
					</#list>
				</@m.formSelect>
			</div>
			
			<div class="input">
				<label>
					<@m.formCheckbox class="form-element" path=path+".reloadOnStartup"/>
					Load on Login
				</label>
				<@m.formSelectEnum class="form-element" path=(path+".defaultStartupType")
					enum=TaskType/>
			</div>
			
		</div>
	</form>

</div>