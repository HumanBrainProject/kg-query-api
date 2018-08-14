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

<#macro for instance propertyName distinctPropertyName="">
    <#local instances = flatten(instance propertyName distinctPropertyName)>
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

<#macro value instance propertyName distinctPropertyName="">
    <#local instances = flatten(instance propertyName distinctPropertyName)>
    <@direct_value instances/>
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

<#macro ref target instance propertyName identifierPropertyName labelPropertyName uuidPropertyName distinctPropertyName="">
    <#local instances = flatten(instance propertyName distinctPropertyName)>
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
                <#if i?has_content && i[normalize(identifierPropertyName)]?has_content && i[normalize(labelPropertyName)]?has_content>
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
        <#elseif instance?has_content && instance[normalize(identifierPropertyName)]?has_content>
            {
                "reference": "${target}/${instance[normalize(identifierPropertyName)]}",
            <#if instance[normalize(uuidPropertyName)]?has_content>"uuid": "${instance[normalize(uuidPropertyName)]}",</#if>
                "value": "${instance[normalize(labelPropertyName)]?json_string}"
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
                <#if i?has_content && i[normalize(url)]?has_content && _get_label(i label)?has_content>
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
            <#if instance[normalize(url)]?has_content && _get_label(instance label)?has_content>
            {
                "url": "${urlbase}${instance[normalize(url)]}",
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
    <@link instance path url label "https://kg-dev.humanbrainproject.org/api/export?container=" "###HBP Knowledge Graph Data Platform Citation Requirements\nThis text is provided to describe the requirements for citing data found via HBP Knowledge Graph Data Platform (KG): [https://www.humanbrainproject.eu/explore-the-brain/search](https://www.humanbrainproject.eu/explore-the-brain/search). It is meant to provide a more human-readable form of key parts of the KG Terms of Service, but in the event of disagreement between the [KG Terms of Service](https://www.humanbrainproject.eu/en/explore-the-brain/search-terms-of-use/) and these Citation Requirements, the former is to be taken as authoritative.\n\n####Dataset licensing\nAll datasets in the KG have explicit licensing conditions attached. The license is typically one of the Creative Commons licenses. You must follow the licensing conditions attached to the dataset, including all restrictions on commercial use, requirements for attribution or requirements to share-alike.\n\n####HBP Knowledge Graph citation policy\n\nIf you use of the software and datasets found via the KG, the KG Pyxus API or KG REST API in your scientific publication you **must** follow the following citation policy:\n\n1) Cite the Site: \"HBP Knowledge Graph Data Platform: <https://www.humanbrainproject.eu/explore-the-brain/search>\"\n\n2) For a dataset is released under a Creative Commons license which includes \"Attribution\", please cite:   \n>a) The primary publication listed under the dataset.\n\n>b) The dataset DOI, if using a collection of single datasets.\n\n>c) The parent project DOI, if using multiple dataset from the same project\n\n>d) In cases where a primary publication is not provided, and only in such cases, the names of the Data Contributors should be cited (Data provided by Contributor 1, Contributor 2, ..., and Contributor N) in addition to the citation of the Site and the DOI for the data.\n\n  3) For software, you must cite software as defined in the software's respective citation policy. If you can not find a citation or acknowledgement policy for the software, please use the opensource repository link as the citation link.\n\nFailure to cite data or software used in a publication or presentation constitutes scientific misconduct.   Failure to cite data or software used in a scientific publication must be corrected by issuing an official Erratum and correction of the given article if it is discovered post-publication.\n\n####[Acknowledgement policy](https://www.humanbrainproject.eu/en/explore-the-brain/search-terms-of-use/#acknowledgements)\n\n####Final thoughts\nCitations of datasets are essential for encouraging researchers to release their data through the KG or other scientific data sharing platforms. Your citation may help them to get their next job or next grant and will ultimately encourage researchers to produce and release more useful open data. Make science more reproducible and more efficient."/>
</#macro>


<#macro link instance path url label urlbase="" detail="" distinctPropertyName="">
    <#local instances = flatten(instance path distinctPropertyName)>
    <#if instances?has_content>
        <@direct_link instances url label urlbase detail/>
    <#else>
    null
    </#if>
</#macro>

<#macro print instance propertyName distinctPropertyName="" separator=" ">
    <#local instances = flatten(instance propertyName distinctPropertyName)>
    <#if instances?has_content>
        <@direct_print instances separator/>
    </#if>
</#macro>

<#macro direct_print value separator=" ">
    <#if value?has_content>
        ${get_direct_print_value(value separator)}
    </#if>
</#macro>

<#function get_value instance propertyName distinctPropertyName="" separator=" ">
    <#local instances = flatten(instance propertyName distinctPropertyName)>
    <#if instances?has_content>
        <#return get_direct_value(instances separator)>
    <#else>
        <#return ""/>
    </#if>
</#function>

<#function get_print_value instance propertyName distinctPropertyName="" separator=" ">
    <#local instances = flatten(instance propertyName distinctPropertyName)>
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
                <#return value[0]/>
            <#else>
                <#local result = ""/>
                <#list value as v>
                    <#local result = result+v+separator>
                </#list>
                <#return result[0..result?length-1]/>
            </#if>
        <#else>
            <#return value/>
        </#if>
    <#else>
        <#return ""/>
    </#if>
</#function>

<#function _get_label instance labelProperty>
    <#if labelProperty?is_hash>
        <#return labelProperty.text>
    <#else>
        <#return instance[normalize(labelProperty)]>
    </#if>
</#function>

<#function flatten instance path distinctPropertyName="">
    <#assign normalizedPath = normalizeLookupPath(path)/>
    <#if distinctPropertyName!="">
        <#local keys=[]/>
        <#local result=[]/>
        <#list normalizedPath as propertyNames>
            <#local flat= _do_flatten(instance propertyNames)/>
            <#list flat as e>
                <#if e[normalize(distinctPropertyName)]?has_content && !keys?seq_contains(e[normalize(distinctPropertyName)])>
                    <#local keys=keys+[e[normalize(distinctPropertyName)]]/>
                    <#local result=result+[e]/>
                </#if>
            </#list>
        </#list>
        <#return result>
    <#else>
        <#if instance?has_content>
            <#if normalizedPath?size==1>
                <#return _do_flatten(instance normalizedPath[0])>
            <#else>
                <#local result=[]/>
                <#list normalizedPath as propertyNames>
                    <#local result=result+_do_flatten(instance propertyNames)/>
                </#list>
                <#return result>
            </#if>
        <#else>
            <#return []>
        </#if>
    </#if>
</#function>

<#function _do_flatten instance normalizedPropertyNames i=0>
    <#if instance?has_content>
        <#if instance?is_sequence>
            <#local result = []>
            <#list instance as el>
                <#local result = result+_do_flatten(el normalizedPropertyNames i)>
            </#list>
            <#return result>
        <#else>
            <#if i<normalizedPropertyNames?size-1 && normalizedPropertyNames[i]?has_content && instance[normalizedPropertyNames[i]]?has_content>
                <#return _do_flatten(instance[normalizedPropertyNames[i]] normalizedPropertyNames i+1)>
            <#elseif instance[normalizedPropertyNames[i]]?has_content>
                <#if instance[normalizedPropertyNames[i]]?is_sequence>
                    <#return instance[normalizedPropertyNames[i]]>
                <#else>
                    <#return [instance[normalizedPropertyNames[i]]]>
                </#if>
            <#else>
                <#return []>
            </#if>
        </#if>
    </#if>
    <#return []>
</#function>

<#function filter instances propertyName propertyValue>
    <#local result = []>
    <#list instances as instance>
        <#if instance[normalize(propertyName)]?has_content && instance[normalize(propertyName)]?seq_contains(propertyValue)>
            <#local result = result + [instance]>
        </#if>
    </#list>
    <#return result>
</#function>

<#function normalize propertyName>
    <#if propertyName?has_content>
        <#return propertyName?replace(":", "_")>
    <#else>
        <#return "">
    </#if>
</#function>



<#function normalizeLookupPath path>
    <#if path?has_content>
        <#local result = []>
        <#if path?is_sequence>
            <#local first_level = path>
        <#else>
            <#local first_level = [path]>
        </#if>
        <#list first_level as f>
            <#if f?is_sequence>
                <#local second_level = []>
                <#list f as s>
                    <#local second_level = second_level+[normalize(s)]>
                </#list>
            <#else>
                <#local second_level = [normalize(f)]>
            </#if>
            <#local result = result+[second_level]>
        </#list>
        <#return result/>
    <#else>
        <#return null/>
    </#if>
</#function>


<#macro group_by target instancesProperty groupingProperty>
    <#local uniqueGroupValues = []>
    <#if target[normalize(instancesProperty)]?has_content>
        [
        <#list target[normalize(instancesProperty)] as i>
            <#if i[normalize(groupingProperty)]?has_content>
                <#if i[normalize(groupingProperty)]?is_sequence>
                    <#list i[normalize(groupingProperty)] as p>
                        <#if !uniqueGroupValues?seq_contains(p)>
                            <#local uniqueGroupValues = uniqueGroupValues+[p]>
                        </#if>
                    </#list>
                <#else>
                    <#local uniqueGroupValues = uniqueGroupValues+i[normalize(groupingProperty)]>
                </#if>
            </#if>
        </#list>
        <#local hasInstancesByGroupValue = false/>
        <#if uniqueGroupValues?has_content>
            <#list uniqueGroupValues as groupName>
                <#local groupInstances = filter(target[normalize(instancesProperty)] groupingProperty groupName)>
                <#if groupInstances?has_content>
                    <#local hasInstancesByGroupValue = true/>
                    <#nested groupName, groupInstances>
                </#if>
                <#sep>,
            </#list>
        </#if>
        <#local instancesWithoutGroup = filter(target[normalize(instancesProperty)] groupingProperty "")>
        <#if instancesWithoutGroup?has_content>
            <#if hasInstancesByGroupValue>,</#if>
            <#nested "", instancesWithoutGroup>
        </#if>
        ]
    <#else>
    null
    </#if>
</#macro>