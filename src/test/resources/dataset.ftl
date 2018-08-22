<#include "searchuifast">
[
<#list results as el>
  {

    "title": <@value el "search:title"/>,
    "doi": <@for el "search:doi" ; pub>
        <#assign url_string=get_value(pub "search:citation")+"\n[DOI: "+get_value(pub "search:doi")+"]\n[DOI: "+get_value(pub "search:doi")+"]: https://doi.org/"+get_value(pub "search:doi")?url />
        <@direct_value url_string/>
    </@for>,
    "component": <@ref "Project" el "search:component" "search:identifier" "search:name" "search:uuid" />,
    "description": <@value el "search:description"/>,
    "image": <@value el "search:image"/>,
    "contributors": <@ref "Person" el "search:contributors" "search:identifier" "search:name" "search:uuid"/>,
    "owners": <@ref "Person" el "search:owners" "search:identifier" "search:name" "search:uuid"/>,
    "publications":  <@for el "search:publications" ; pub>
        <#assign url_string=get_value(pub "search:citation")+"\n[DOI: "+get_value(pub "search:doi")+"]\n[DOI: "+get_value(pub "search:doi")+"]: https://doi.org/"+get_value(pub "search:doi")?url />
        <@direct_value url_string/>
    </@for>,
    "atlas": <@value el "search:parcellationAtlas"/>,
    "region": <@value el "search:parcellationRegion"/>,
    "preparation": <@value el "search:preparation"/>,
    "methods": <@value el "search:methods"/>,
    "protocol": <@value el "search:protocols"/>,
    "license_info": <@link el "search:license" "search:url" "search:name"/>,
    "external_datalink": <@value el "search:external_datalink"/>,
    "files": <@fileDownload el "search:files" "search:absolute_path" "search:name"/>,
    "viewer": <@link el "search:neuroglancer" "search:url" {"text": "Show in brain atlas viewer"}/>,
    "subjects": <@for el "search:subjects" ; subject>
        {
            "children": {
                "subject_name": <@direct_ref "Subject" subject "search:identifier" "search:name" "search:uuid"/>,
                "species": <@ref "Species" subject "search:species" "search:identifier" "search:name" "search:uuid"/>,
                "sex": <@value subject "search:sex"/>,
                "strain": <@value subject "search:strain"/>,
                "genotype": <@value subject "search:genotype"/>,
                "samples": <@ref "Sample" subject "search:samples" "search:identifier" "search:name" "search:uuid"/>
            }
        }</@for>,
    "embargo": <@value el "search:embargo"/>,
    "releasedate": <@value el "search:releasedate"/>,
    "viewer_mapping": <@value el "search:brainAtlasViewer"/>
}<#sep>,
</#list>
]