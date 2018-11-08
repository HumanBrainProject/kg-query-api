<#macro field parent>
	<#list parent?keys as k>
			"${k}": {
				"properties": {
		<#if parent[k]["children"]??>
					"children": {
						"type": "nested",
						"properties": {
			<@field parent[k]["children"]/>
            			}
					},
		</#if>
		<@value parent[k]/>
				}
			}
		<#sep>,
	</#list>
</#macro>


<#macro value el>
	"value": {
	<#if el["https://schema.hbp.eu/search_ui/type"]?has_content>
		<#local type = el["https://schema.hbp.eu/search_ui/type"]/>
	<#else>
		<#local type = "text"/>
	</#if>
    "type": "${type}"
	<#if type=="text">
        ,
        "fields": {
            "keyword": {
                "type": "keyword"
            }
        }
	</#if>
    }
</#macro>

{
	"${apiName}": {
		"_all": {
			"enabled": true
		},
		"properties": {
<#list results as r>

			"@timestamp": {
				"type": "date"
			},
	<@field r/>
</#list>

		}
		}
}