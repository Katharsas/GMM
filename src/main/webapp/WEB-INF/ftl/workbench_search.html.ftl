<#import "/spring.ftl" as s/>
<#import "macros.html.ftl" as m/>

<#assign path="workbench-searchForm">
<@s.bind path=path/>
<#assign search=.vars[path]>
		

<form id="workbench-searchForm">	
	<div id="workbench-search-wrapper">

		<!-- Hidden -->
		<@m.formSelect path=(path+".easySearch") id="workbench-search-type" style="display:none;">
			<@m.formOption value="true" label="easySearch" />
			<@m.formOption value="false" label="complexSearch"/>
		</@m.formSelect>
		
		<!-- Switch between easy and complex search -->
		<div id="workbench-search-switch" class="button">
			<@s.message "search.complex" />
		</div>
		
		<!-- Easy search -->
		<div id="workbench-search-easy" class="input">
			<@m.formInput path=(path+".easy") class="form-element" />
			
			<div class="workbench-search-submit button">
				<@s.message "search.start" />!
			</div>
		</div>
		
		<!-- Complex search -->
		<div id="workbench-search-complex" class="input">
			<div class="workbench-search-group">
			
				<div class="workbench-search-desc"><@s.message "tasks.title" />:</div>
				<@m.formInput path=(path+".name") class="form-element" />
				
				<div class="workbench-search-desc"><@s.message "tasks.details" />:</div>
				<@m.formInput path=(path+".details") class="form-element" />
				
			</div>
			<div class="workbench-search-group">
			
				<div class="workbench-search-desc"><@s.message "author" />:</div>
				<@m.formInput path=(path+".author") class="form-element" />
				
				<div class="workbench-search-desc"><@s.message "tasks.label" />:</div>
				<@m.formInput path=(path+".label") class="form-element" />
				
			</div>
			<div class="workbench-search-group">
			
				<div class="workbench-search-desc"><@s.message "tasks.assigned" />:</div>
				<@m.formInput path=(path+".assigned") class="form-element" />
				
				<div class="workbench-search-desc"><@s.message "tasks.path" />:</div>
				<@m.formInput path=(path+".path") class="form-element" />
				
			</div>
			<div class="workbench-search-submit button">
				<@s.message "search.start" />!
			</div>
		</div>
		
	</div>
</form>
