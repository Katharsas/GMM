<#import "/spring.ftl" as s/>

<#macro formCheckbox path attributes...>
	<@s.bind path />
	<input type="hidden" name="_${s.status.expression}" value="false"/>
	<input type="checkbox"
		<#list attributes?keys as attr>
    		${attr}="${attributes[attr]?html}"
  		</#list>
	 	name="${s.status.expression}"
		<#if s.status.value?? && s.status.value?string=="true">checked="checked"</#if>
	<@s.closeTag/>
</#macro>