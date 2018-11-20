<#include "searchuinew">
[
<#list results as el>
	{
		"datasets": <@ref "Dataset" el "https://schema.hbp.eu/dataset/search/datasets" "https://schema.hbp.eu/dataset/search/identifier" "https://schema.hbp.eu/dataset/search/postFix" "https://schema.hbp.eu/dataset/search/uuid"/>,
		"title": <@value el "https://schema.hbp.eu/dataset/search/title"/>
	}
	<#sep>,
</#list>
]