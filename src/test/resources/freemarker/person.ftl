<#include "searchuinew">
[
<#list results as el>
  {
    "title": <@value el "search:title"/>,
    "description": <@value el "search:description"/>,
    "phone": <@value el "search:phone"/>,
    "publications": <@for el "search:publications" ; pub>
        <#assign url_string=get_value(pub "search:citation")+"\n[DOI: "+get_value(pub "search:doi")+"]\n[DOI: "+get_value(pub "search:doi")+"]: https://doi.org/"+get_value(pub "search:doi")?url />
        <@direct_value url_string/>
    </@for>,
    "address": <@value el "search:address"/>,
    "contributions": <@ref "Dataset" el "search:contributions" "search:identifier" "search:name" "search:uuid"/>,
    "email": <@value el "search:email"/>
}<#sep>,
</#list>
]