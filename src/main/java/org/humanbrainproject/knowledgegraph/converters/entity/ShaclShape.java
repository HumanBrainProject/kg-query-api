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

package org.humanbrainproject.knowledgegraph.converters.entity;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShaclShape {
    private static final String NEXUS_VOCAB = "https://bbp-nexus.epfl.ch/vocabs/nexus/core/terms/v0.1.0/";
    private static final String RDF_VOCAB = "http://www.w3.org/2000/01/rdf-schema#";
    private static final String SHACL_VOCAB = "http://www.w3.org/ns/shacl#";

    private final JsonDocument shape;

    public ShaclShape(JsonDocument shape){
        this.shape = shape;
    }

    public String getLabel(){
        return (String) ((Map)shape.get(RDF_VOCAB+"label")).get(JsonLdConsts.VALUE);
    }

    public List<ShaclProperty> getProperties(){
        List<Map> properties = lookupProperties();
        return properties.stream().map(p -> new ShaclProperty(new JsonDocument(p))).collect(Collectors.toList());
    }

    private List<Map> lookupProperties(){
        String propertyKey = SHACL_VOCAB + "property";
        List<Map> result;
        if(this.shape.containsKey(propertyKey)){
            Object o = this.shape.get(propertyKey);
            if(o instanceof List){
                result = ((List<Map>)o);
            }
            else{
                result = Collections.singletonList((Map)o);
            }
        }
        else{
            result = new ArrayList<>();
            for (Object value : this.shape.values()) {
                if(value instanceof Map && ((Map)value).containsKey(JsonLdConsts.LIST)){
                    List l = (List)((Map)value).get(JsonLdConsts.LIST);
                    for (Object v : l) {
                        if(v instanceof Map && ((Map)v).containsKey(propertyKey)) {
                            Object propertyValue = ((Map) v).get(propertyKey);
                            if(propertyValue instanceof List){
                                result.addAll((List)propertyValue);
                            }
                            else if(propertyValue instanceof Map){
                                result.add((Map)propertyValue);
                            }
                        }
                    }
                }
            }
        }
        return result.stream().map(e -> {e.put("shapeDeclaration", this.shape.get(JsonLdConsts.ID)); return e;}).collect(Collectors.toList());

    }





}
