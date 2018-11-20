<#include "searchui">
[
<#list results as el>
  {
    "title": <@value el "https://schema.hbp.eu/dataset/search/title"/>,
    "weightPreFixation": <@value el "https://schema.hbp.eu/dataset/search/weightPreFixation"/>,
    "parcellationAtlas": <@value el "https://schema.hbp.eu/dataset/search/parcellationAtlas"/>,
    "parcellationRegion": <@value el "https://schema.hbp.eu/dataset/search/parcellationRegion"/>,
    "viewer": <@link el "https://schema.hbp.eu/dataset/search/brainViewer" "." {"text": "Show "+get_value(el "https://schema.hbp.eu/dataset/search/title")+" in brainviewer"}/>,
    "methods":  <@value el "https://schema.hbp.eu/dataset/search/methods"/>,
    "files": <@fileDownload el "https://schema.hbp.eu/dataset/search/files" "https://schema.hbp.eu/dataset/search/absolutePath" "https://schema.hbp.eu/dataset/search/postFix"/>,
    "subject": <@for el "https://schema.hbp.eu/dataset/search/subjects" ; subject>
        {
    <@meta el "https://schema.hbp.eu/dataset/search/subjects"/>
            "children": {
                "genotype": <@value subject "https://schema.hbp.eu/dataset/search/genotype"/>,
                "weight": <@value subject "https://schema.hbp.eu/dataset/search/weight"/>,
                "age": <@value subject "https://schema.hbp.eu/dataset/search/age"/>,
                "sex": <@value subject "https://schema.hbp.eu/dataset/search/sex"/>,
                "subject_name": <@direct_ref "Subject" subject "https://schema.hbp.eu/dataset/search/identifier" "https://schema.hbp.eu/dataset/search/postFix" "https://schema.hbp.eu/dataset/search/uuid"/>,
                "strain": <@value subject "https://schema.hbp.eu/dataset/search/strain"/>,
                "species": <@ref "Species" subject "https://schema.hbp.eu/dataset/search/species" "https://schema.hbp.eu/dataset/search/identifier" "https://schema.hbp.eu/dataset/search/postFix" "https://schema.hbp.eu/dataset/search/uuid"/>
            }
        }</@for>,
        "datasets":<@for el "https://schema.hbp.eu/dataset/search/datasets" ; datasetGrp>
        {
    <@meta el "https://schema.hbp.eu/dataset/search/datasets"/>
            "children": {
                "component": <@value datasetGrp "https://schema.hbp.eu/dataset/search/componentName"/>,
                "postFix": <@ref "Dataset" datasetGrp "https://schema.hbp.eu/dataset/search/instances" "https://schema.hbp.eu/dataset/search/identifier" "https://schema.hbp.eu/dataset/search/postFix" "https://schema.hbp.eu/dataset/search/uuid"/>
            }
        }
</@for>
  }<#sep>,
</#list>
]
