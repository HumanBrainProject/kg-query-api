<#include "searchui">
[
<#list results as el>
  {
    "title": <@value el "http://schema.hbp.eu/dataset/search/title"/>,
    "weightPreFixation": <@value el "http://schema.hbp.eu/dataset/search/weightPreFixation"/>,
    "parcellationAtlas": <@value el "http://schema.hbp.eu/dataset/search/parcellationAtlas"/>,
    "parcellationRegion": <@value el "http://schema.hbp.eu/dataset/search/parcellationRegion"/>,
    "viewer": <@link el "http://schema.hbp.eu/dataset/search/brainViewer" "." {"text": "Show "+get_value(el "http://schema.hbp.eu/dataset/search/title")+" in brainviewer"}/>,
    "methods":  <@value el "http://schema.hbp.eu/dataset/search/methods"/>,
    "files": <@fileDownload el "http://schema.hbp.eu/dataset/search/files" "http://schema.hbp.eu/dataset/search/absolutePath" "http://schema.hbp.eu/dataset/search/postFix"/>,
    "subject": <@for el "http://schema.hbp.eu/dataset/search/subjects" ; subject>
        {
    <@meta el "http://schema.hbp.eu/dataset/search/subjects"/>
            "children": {
                "genotype": <@value subject "http://schema.hbp.eu/dataset/search/genotype"/>,
                "weight": <@value subject "http://schema.hbp.eu/dataset/search/weight"/>,
                "age": <@value subject "http://schema.hbp.eu/dataset/search/age"/>,
                "sex": <@value subject "http://schema.hbp.eu/dataset/search/sex"/>,
                "subject_name": <@direct_ref "Subject" subject "http://schema.hbp.eu/dataset/search/identifier" "http://schema.hbp.eu/dataset/search/postFix" "http://schema.hbp.eu/dataset/search/uuid"/>,
                "strain": <@value subject "http://schema.hbp.eu/dataset/search/strain"/>,
                "species": <@ref "Species" subject "http://schema.hbp.eu/dataset/search/species" "http://schema.hbp.eu/dataset/search/identifier" "http://schema.hbp.eu/dataset/search/postFix" "http://schema.hbp.eu/dataset/search/uuid"/>
            }
        }</@for>,
        "datasets":<@for el "http://schema.hbp.eu/dataset/search/datasets" ; datasetGrp>
        {
    <@meta el "http://schema.hbp.eu/dataset/search/datasets"/>
            "children": {
                "component": <@value datasetGrp "http://schema.hbp.eu/dataset/search/componentName"/>,
                "postFix": <@ref "Dataset" datasetGrp "http://schema.hbp.eu/dataset/search/instances" "http://schema.hbp.eu/dataset/search/identifier" "http://schema.hbp.eu/dataset/search/postFix" "http://schema.hbp.eu/dataset/search/uuid"/>
            }
        }
</@for>
  }<#sep>,
</#list>
]
