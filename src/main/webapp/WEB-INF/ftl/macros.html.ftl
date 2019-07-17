<#import "/spring.ftl" as s/>


<#--
	@author Jan Mothes

	Create form elements that are bonded to Spring form classes.
	Common parameters:
	=> "path" is a String that holds the java expression that would reference
			the field that will be bonded to the input element.
	=> "attributes..." is optional and allows the caller to specify any additional
			html attributes directly on the macro element.
-->


<#--
	Create a checkbox input.
	@param path - Corresponding form path to boolean type.
-->
<#macro formCheckbox path attributes...>
	<@s.bind path />
	<input type="hidden" name="_${s.status.expression}" value="false"/>
	<input type="checkbox"
		<#list attributes?keys as attr>
    		${attr}="${attributes[attr]}"
  		</#list>
	 	name="${s.status.expression}"
		<#if s.status.value?? && s.status.value?string=="true">checked="checked"</#if>
	>
</#macro>


<#--
	Create a simple text input.
	@param path - Corresponding form path to String type.
-->
<#macro formInput path attributes...>
	<#assign attrString="">
	<#list attributes?keys as attr>
   		<#assign attrString = attrString+attr+"="+attributes[attr]+" " >
 	</#list>
	<@s.formInput path=path attributes="${attrString}"/>
</#macro>


<#--
	Create a textarea input.
	@param path - Corresponding form path to String type.
-->
<#macro formTextarea path attributes...>
	<#assign attrString="">
	<#list attributes?keys as attr>
   		<#assign attrString = attrString+attr+"="+attributes[attr]+" " >
 	</#list>
	<@s.formTextarea path=path attributes="${attrString}"/>
</#macro>


<#--
	Create a select with options from an enum, requires the enum to have the method
	"public String getNameKey()", which is used as the message key for the option label.
	@param path - Corresponding form path to enum type.
	@param enum - Selected enum.
-->
<#macro formSelectEnum path enum attributes...>
	<@s.bind path />
	<select name="${s.status.expression}"
		<#list attributes?keys as attr>
			${attr}="${attributes[attr]}"
		</#list>
	>
	<#list enum.values() as currentEnum>
		<#assign message><@s.message currentEnum.getNameKey() /></#assign>
		<@formOption
			value = currentEnum.name()
			label = message />
	</#list>
	<#nested>
	</select>
</#macro>


<#--
	Create a select tag for selection from options.
	@param path - Corresponding select path to String or enum type.
-->
<#macro formSelect path attributes...>
	<@s.bind path />
	<select name="${s.status.expression}"
		<#list attributes?keys as attr>
			${attr}="${attributes[attr]}"
		</#list>
	>
		<#nested>
	</select>
</#macro>


<#--
	Create an option tag for a selection.
	@param value - The value the selection will have if this option is chosen.
	@param label - The text this option will show to the user.
-->
<#macro formOption value label attributes...>
	<option value="${value}"
		<#list attributes?keys as attr>
			${attr}="${attributes[attr]}"
		</#list>
		<@s.checkSelected value/>
	>
	${label}
	<#nested>
	</option>
</#macro>
