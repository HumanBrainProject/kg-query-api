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

import java.util.Map;

public class ShaclProperty {
    private static final String NEXUS_VOCAB = "https://bbp-nexus.epfl.ch/vocabs/nexus/core/terms/v0.1.0/";
    private static final String RDF_VOCAB = "http://www.w3.org/2000/01/rdf-schema#";

    private static final String SHACL_VOCAB = "http://www.w3.org/ns/shacl#";

    private final JsonDocument property;

    public ShaclProperty(JsonDocument property){
        this.property = property;
    }


    public String getKey(){
        Object path = property.get(SHACL_VOCAB + "path");
        return path != null ? (String)((Map)path).get(JsonLdConsts.ID) : null;
    }

    public String getName(){
        return (String)property.get(SHACL_VOCAB+"name");
    }

    public String getShapeDeclaration(){
        return (String)property.get("shapeDeclaration");
    }

    public boolean isLinkToInstance(){
        return property.containsKey(SHACL_VOCAB+"node");
    }






}
