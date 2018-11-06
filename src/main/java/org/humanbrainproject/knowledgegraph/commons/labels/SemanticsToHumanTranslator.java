package org.humanbrainproject.knowledgegraph.commons.labels;

import org.apache.commons.lang.StringUtils;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SemanticsToHumanTranslator {

    public String translateSemanticValueToHumanReadableLabel(String semantic) {
        if (semantic == null) {
            return null;
        }
        UriComponents components = UriComponentsBuilder.fromUriString(semantic).build();
        String value = components.getFragment();
        if(value ==null){
            value = components.getPathSegments().get(components.getPathSegments().size()-1);
        }
        return StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(value), ' ');
    }

    public String translateArangoCollectionName(ArangoCollectionReference reference){
        String[] split = reference.getName().split("-");
        return split[split.length-1];
    }



}
