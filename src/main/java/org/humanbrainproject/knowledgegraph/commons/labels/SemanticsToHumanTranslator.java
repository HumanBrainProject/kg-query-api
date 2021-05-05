/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

package org.humanbrainproject.knowledgegraph.commons.labels;

import org.apache.commons.lang.StringUtils;
import org.humanbrainproject.knowledgegraph.annotations.Tested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Objects;

@Component
@Tested
public class SemanticsToHumanTranslator {

    public String translateNexusSchemaReference(NexusSchemaReference nexusSchemaReference){
        return normalize(nexusSchemaReference.getSchema());
    }

    public String translateSemanticValueToHumanReadableLabel(String semantic) {
        return normalize(extractSimpleAttributeName(semantic));
    }

    public String extractSimpleAttributeName(String semantic){
        if (semantic != null) {
            if(semantic.contains("#")){
                return semantic.substring(semantic.lastIndexOf("#")+1);
            }
            UriComponents components = UriComponentsBuilder.fromUriString(semantic).build();
            String value = components.getFragment();
            if (value == null && components.getPathSegments().size() > 0) {
                return components.getPathSegments().get(components.getPathSegments().size() - 1);
            }
        }
        return null;
    }

    String normalize(String value) {
        if(value!=null) {
            if(value.startsWith("@")){
                value = value.substring(1);
            }
            value = value.replaceAll("_", " ");
            String[] array = StringUtils.splitByCharacterTypeCamelCase(value);
            array = Arrays.stream(array).filter(Objects::nonNull).filter(s -> !s.trim().isEmpty()).map(s -> s.trim().toLowerCase()).toArray(String[]::new);
            if (array.length > 0) {
                array[0] = StringUtils.capitalize(array[0]);
            }
            return StringUtils.join(array, ' ');
        }
        return null;
    }

    public String translateArangoCollectionName(ArangoCollectionReference reference){
        String[] split = reference.getName().split("-");
        return normalize(split[split.length-1]);
    }

    /**
     * This is a very (very) simple plural to singular transformation which doesn't take into account several exceptions. But it's a start :)
     * @param plural
     * @return
     */
    public String simplePluralToSingular(String plural){
        if(plural!=null){
            if(plural.endsWith("s")){
                return plural.substring(0, plural.length()-1);
            }
        }
        return plural;


    }



}
