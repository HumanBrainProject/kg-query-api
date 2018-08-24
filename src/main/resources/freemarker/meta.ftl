<#ftl output_format="JSON">

<#macro direct_for instances>
    <#if instances??>
        <#if instances?is_sequence>
            <#if instances?size==1>
                <#nested instances[0]>
            <#else>
             [
                <#list instances as p>
                    <#nested p>
                    <#sep>,
                </#list>
             ]
            </#if>
        <#else>
            <#nested instances>
        </#if>
    <#else>
        null
    </#if>
</#macro>

<#macro for instance propertyName>
    <#local instances = _value(instance propertyName)>
    <#if instances??>
        <#if instances?is_sequence>
            <#if instances?size==1>
                <#nested instances[0]>
            <#else>
            [
                <#list instances as p>
                    <#nested p>
                    <#sep>,
                </#list>
            ]
            </#if>
        <#else>
            <#nested instances>
        </#if>
    <#else>
    null
    </#if>
</#macro>

<#macro value instance propertyName>
    <@_direct_value _value(instance propertyName)/>
</#macro>


<#macro direct_value instance>
    <#if !instance?is_string>
        <@_direct_value instance/>
    </#if>
</#macro>

<#macro _direct_value instance>
    <#if instance??>
        <#if instance?is_sequence>
            <#if instance?size==1>
                <@_direct_value instance[0]/>
            <#else>
            [
                <#list instance as el>
                    <@_direct_value el/>
                    <#sep>,
                </#list>
            ]
            </#if>
        <#elseif instance?is_hash>
        {
            <#local keysOfDirectValues = []/>
            <#list instance?keys as key>
                <#if !instance[key]?is_hash>
                    <#local keysOfDirectValues = keysOfDirectValues+[key]/>
                </#if>
            </#list>
            <#list keysOfDirectValues as key>
                "${key}": <@_direct_value instance[key]/>
                <#sep>,
            </#list>        }
        <#elseif instance?is_string>
        	"${instance}"
        <#else>
            ${instance?c}
        </#if>
    <#else>null</#if>
</#macro>

<#macro meta instance propertyName wrap=false trailingComma=false>
    <#local instances = _value(instance propertyName)>
    <#if instances?is_hash>
        <#if wrap>{</#if>
        <#local keys = []/>
        <#list instances?keys as key>
            <#if !instance[key]?is_enumerable && !instance[key]?is_hash>
                <#local keys=keys+[key]/>
            </#if>
        </#list>
        <#list keys as key>
            "${key}": <@_direct_value instances[key]/>
        <#sep>,
        </#list>
        <#if wrap>}<#elseif trailingComma>,</#if>
    <#else>
        <#list instances as instance>
            <#if instance?is_hash>
                <#if wrap>{</#if>
                <#local keys = []/>
                <#list instance?keys as key>
                    <#if !instance[key]?is_enumerable && !instance[key]?is_hash>
                        <#local keys=keys+[key]/>
                    </#if>
                </#list>
                <#list keys as key>
                        "${key}": <@_direct_value instance[key]/>
                    <#sep>,
                </#list>
                <#if wrap>}<#else>,</#if>
            </#if>
        </#list>
    </#if>
</#macro>

<#macro ref target instance propertyName identifierPropertyName labelPropertyName uuidPropertyName>
    <#local instances = _value(instance propertyName)>
    <@value instance propertyName/>
</#macro>

<#macro direct_ref target instance identifierPropertyName labelPropertyName uuidPropertyName>
    <@value instance labelPropertyName/>
</#macro>

<#macro direct_link instance url label urlbase="" detail="" >
    <@direct_value instance/>
</#macro>

<#macro fileDownload instance path url label>
    <@value instance path/>
</#macro>

<#macro link instance propertyName url label urlbase="" detail="">
    <@value instance propertyName/>
</#macro>

<#macro print instance propertyName separator=" ">
    <@value instance propertyName/>
</#macro>

<#macro direct_print value separator=" ">
    <@direct_value instance/>
</#macro>

<#function get_value instance propertyName separator=" ">
    <#local instances = _value(instance propertyName)>
    <#if instances?has_content && !instances?is_hash >
        <#return get_direct_value(instances separator)>
    <#else>
        <#return ""/>
    </#if>
</#function>

<#function get_print_value instance propertyName distinctPropertyName="" separator=" ">
    <#local instances = _value(instance propertyName)>
    <#if instances?has_content>
        <#return get_direct_print_value(instances separator)/>
    <#else>
        <#return ""/>
    </#if>
</#function>

<#function get_direct_print_value value separator=" ">
    <#local v = get_direct_value(value separator)>
    <#if v?has_content>
        <#if v?is_string>
            <#return "\""+v?json_string+"\""/>
        <#else>
            <#return v/>
        </#if>
    <#else>
        <#return ""/>
    </#if>
</#function>


<#function get_direct_value value separator=" ">
    <#if value?has_content>
        <#if value?is_sequence>
            <#if value?size==1>
                <#return get_direct_value(value[0])/>
            <#else>
                <#local result = ""/>
                <#list value as v>
                    <#local result = result+v+separator>
                </#list>
                <#return result[0..result?length-1]/>
            </#if>
        <#elseif !value?is_hash>
            <#return value/>
        <#else>
            <#return ""/>
        </#if>
    <#else>
        <#return ""/>
    </#if>
</#function>

<#function _get_label instance labelProperty>
    <#if labelProperty?is_hash>
        <#return labelProperty.text>
    <#else>
        <#return instance[labelProperty]>
    </#if>
</#function>

<#function _value instance propertyName>
    <#if instance?has_content && propertyName?has_content>
        <#if instance?is_sequence>
            <#local result=[]/>
            <#list instance as i>
                <#local result = result+_value(i, propertyName)/>
            </#list>
            <#return result/>
        <#elseif instance[propertyName]?has_content>
            <#if instance[propertyName]?is_sequence>
                <#return instance[propertyName]/>
            <#else>
                <#return [instance[propertyName]]/>
            </#if>
        </#if>
    </#if>
    <#return []/>
</#function>