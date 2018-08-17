<#ftl output_format="JSON">

<#macro direct_for instances>
    <#if instances?has_content>
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
    <#if instances?has_content>
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
    <@direct_value _value(instance propertyName)/>
</#macro>

<#macro direct_value instance>
    <#if instance?has_content>
        <#if instance?is_sequence>
            <#if instance?size==1>
                {"value": <@direct_print instance[0]/>}
            <#else>
                [
            <#list instance as el>
                {"value": <@direct_print el/>}
                <#sep>,
            </#list>
                ]
            </#if>
        <#else>
            {"value": <@direct_print instance/>}
        </#if>
    <#else>null</#if>
</#macro>

<#macro ref target instance propertyName identifierPropertyName labelPropertyName uuidPropertyName>
    <#local instances = _value(instance propertyName)>
    <#if instances?has_content>
        <@direct_ref target instances identifierPropertyName labelPropertyName uuidPropertyName/>
    <#else>null
    </#if>
</#macro>

<#macro direct_ref target instance identifierPropertyName labelPropertyName uuidPropertyName>
    <#if target?has_content>
        <#if instance?is_sequence>
            <#local hasValidElements=false/>
            <#list instance as i>
                <#if i?has_content && i[identifierPropertyName]?has_content && i[labelPropertyName]?has_content>
                    <#local hasValidElements=true/>
                </#if>
            </#list>
            <#if hasValidElements>
                <@direct_for instance; i>
                    <@direct_ref target i identifierPropertyName labelPropertyName uuidPropertyName/>
                </@direct_for>
            <#else>
                null
            </#if>
        <#elseif instance?has_content && instance[identifierPropertyName]?has_content && !instance[identifierPropertyName]?is_hash>
            {
                "reference": "${target}/${instance[identifierPropertyName]}",
            <#if instance[uuidPropertyName]?has_content>"uuid": "${instance[uuidPropertyName]}",</#if>
                "value": "${instance[labelPropertyName]?json_string}"
            }
        <#else>
            null
        </#if>
    <#else>
        null
    </#if>
</#macro>

<#macro direct_link instance url label urlbase="" detail="" >
    <#if instance?has_content>
        <#if instance?is_sequence>
            <#local hasValidElements=false/>
            <#list instance as i>
                <#if i?has_content && (url=="." || i[url]?has_content) && _get_label(i label)?has_content>
                    <#local hasValidElements=true/>
                </#if>
            </#list>

            <#if hasValidElements>
                <@direct_for instance; i>
                    <@direct_link i url label urlbase detail/>
                </@direct_for>
            <#else>
                    null
            </#if>
        <#else>
            <#if ((url=="." && !instance?is_hash) || (instance[url]?has_content && !instance[url]?is_hash)) && _get_label(instance label)?has_content>
            {
                <#if url==".">
                    "url": "${urlbase}${instance}",
                <#else>
                    "url": "${urlbase}${instance[url]}",
                </#if>
                <#if detail!="">"detail": "${detail?json_string}",</#if>
                "value": "${_get_label(instance label)}"
            }
            </#if>
        </#if>
    <#else>
        null
    </#if>
</#macro>

<#macro fileDownload instance path url label>
    <@link instance path url label "https://kg-dev.humanbrainproject.org/api/export?container=" "###HBP Knowledge Graph Data Platform Citation Requirements"/>
</#macro>


<#macro link instance propertyName url label urlbase="" detail="">
    <#local instances = _value(instance propertyName)>
    <#if instances?has_content>
        <@direct_link instances url label urlbase detail/>
    <#else>
    null
    </#if>
</#macro>

<#macro print instance propertyName separator=" ">
    <#local instances = _value(instance propertyName)>
    <#if instances?has_content>
        <@direct_print instances separator/>
    </#if>
</#macro>

<#macro meta instance propertyName>
</#macro>

<#macro direct_print value separator=" ">
    <#if value?has_content>
        ${get_direct_print_value(value separator)}
    </#if>
</#macro>

<#function get_value instance propertyName separator=" ">
    <#local instances = _value(instance propertyName)>
    <#if instances?has_content>
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
        <#elseif !v?is_hash && !v?is_sequence>
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
                <#return value[0]/>
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
    <#if instance?has_content && propertyName?has_content  && instance[propertyName]?has_content>
        <#if instance[propertyName]?is_sequence>
            <#return instance[propertyName]/>
        <#else>
            <#return [instance[propertyName]]/>
        </#if>
    </#if>
    <#return []/>
</#function>
