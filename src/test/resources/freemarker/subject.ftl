<#include "searchuinew">
[
<#list results as el>
  {
    "title": <@value el "search:title"/>,
    "species": <@ref "Species" el "search:species" "search:identifier" "search:postFix" "search:uuid"/>,
    "sex": <@value el "search:sex"/>,
    "age": <@value el "search:age"/>,
    "agecategory": <@value el "search:agecategory"/>,
    "weight": <@value el "search:weight"/>,
    "strain": <@value el "search:strain"/>,
    "genotype": <@value el "search:genotype"/>,
    "samples": <@ref "Sample" el "search:samples" "search:identifier" "search:postFix" "search:uuid"/>,
    "datasets": <@for el "search:datasets" ; datasetGrp>
        {
            "children": {
                "component": <@value datasetGrp "search:componentName"/>,
                "postFix": <@ref "Dataset" datasetGrp "search:instances" "search:identifier" "search:postFix" "search:uuid"/>
            }
        }
    </@for>
  }<#sep>,
</#list>
]
