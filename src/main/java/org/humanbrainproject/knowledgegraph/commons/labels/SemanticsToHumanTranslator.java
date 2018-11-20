package org.humanbrainproject.knowledgegraph.commons.labels;

import org.apache.commons.lang.StringUtils;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Objects;

@Component
public class SemanticsToHumanTranslator {

    public String translateNexusSchemaReference(NexusSchemaReference nexusSchemaReference){
        return normalize(nexusSchemaReference.getSchema());
    }

    public String translateSemanticValueToHumanReadableLabel(String semantic) {
        if (semantic == null) {
            return null;
        }
        UriComponents components = UriComponentsBuilder.fromUriString(semantic).build();
        String value = components.getFragment();
        if(value ==null){
            value = components.getPathSegments().get(components.getPathSegments().size()-1);
        }
        return normalize(value);
    }

    private String normalize(String value) {
        value = value.replaceAll("_", " ");
        String[] array = StringUtils.splitByCharacterTypeCamelCase(value);
        array = Arrays.stream(array).filter(Objects::nonNull).filter(s -> !s.trim().isEmpty()).map(s -> s.trim().toLowerCase()).toArray(String[]::new);
        if(array.length>0){
            array[0] = StringUtils.capitalize(array[0]);
        }
        return StringUtils.join(array, ' ');
    }

    public String translateArangoCollectionName(ArangoCollectionReference reference){
        String[] split = reference.getName().split("-");
        return normalize(split[split.length-1]);
    }



}
