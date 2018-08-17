<#list results as el>
<#list el.spec_fields as f>
<#list f?keys as k>
   <#if f.spec_fieldname??>
    ${f.spec_fieldname.@id}

   </#if>
</#list>
</#list>
</#list>