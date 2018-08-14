<#include "searchuinew">
[
<#list results as el>
  {
    "title": <@value el "search:title"/>,
    "weightPreFixation": <@value el "search:weightPrefixation"/>,
    "parcellationAtlas": <@value el "search:parcellationAtlas"/>,
    "parcellationRegion": <@value el "search:parcellationRegion"/>,
    "viewer": <@link el "search:brainViewer" "search:link" "search:name"/>,
    "methods":  <@value el "search:methods"/>,
    "files": <@fileDownload el "search:files" "search:absolutePath" "search:name"/>,
    "subject": <@for el "search:subjects" ; subject>
        {
            "children": {
                "genotype": <@value subject "search:genotype"/>,
                "weight": <@value subject "search:weight"/>,
                "age": <@value subject "search:age"/>,
                "subject_name": <@direct_ref "Subject" subject "search:identifier" "search:name" "search:uuid"/>,
                "sex": <@value subject "search:sex"/>,
                "strain": <@value subject "search:strain"/>,
                "species": <@ref "Species" subject "search:species" "search:identifier" "search:name" "search:uuid"/>
            }
        }</@for>,
        "datasets":
        <@group_by el "search:datasets" "search:componentName"; groupName, instances>
        {
            "children": {
                "component": <@direct_print groupName/>,
                "name": <@direct_ref "Dataset" instances "search:identifier" "search:name" "search:uuid"/>
            }
        }
        </@group_by>
  }<#sep>,
</#list>
]
