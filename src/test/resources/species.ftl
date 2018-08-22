<#include "searchuinew">
[
<#list results as el>
	{
		"datasets": <@ref "Dataset" el "search:datasets" "search:identifier" "search:name" "search:uuid"/>,
		"title": <@value el "search:title"/>
	}
	<#sep>,
</#list>
]