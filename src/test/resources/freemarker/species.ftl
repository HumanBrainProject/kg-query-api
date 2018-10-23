<#include "searchuinew">
[
<#list results as el>
	{
		"datasets": <@ref "Dataset" el "http://schema.hbp.eu/dataset/search/datasets" "http://schema.hbp.eu/dataset/search/identifier" "http://schema.hbp.eu/dataset/search/postFix" "http://schema.hbp.eu/dataset/search/uuid"/>,
		"title": <@value el "http://schema.hbp.eu/dataset/search/title"/>
	}
	<#sep>,
</#list>
]