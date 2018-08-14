<#include "searchuinew">
[
<#list results as el>
  {
    "title": <@value el "search:title"/>,
    "species": <@ref "Species" el "search:species" "search:identifier" "search:name" "search:uuid"/>,
    "sex": <@value el "search:sex"/>,
    "age": <@value el "search:age"/>,
    "agecategory": <@value el "search:agecategory"/>,
    "weight": <@value el "search:weight"/>,
    "strain": <@value el "search:strain"/>,
    "genotype": <@value el "search:genotype"/>,
    "samples": <@ref "Sample" el "search:samples" "search:identifier" "search:name" "search:uuid"/>,
    "datasets":
    <@group_by el "search:datasets" "search:componentName"; groupName, instances>
        {
            "children": {
                "component": <@direct_value groupName/>,
                "name": <@direct_ref "Dataset" instances "search:identifier" "search:name" "search:uuid"/>
            }
        }
    </@group_by>
  }<#sep>,
</#list>
]
