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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShaclSchema {
    private static final String NEXUS_VOCAB = "https://bbp-nexus.epfl.ch/vocabs/nexus/core/terms/v0.1.0/";
    private static final String RDF_VOCAB = "http://www.w3.org/2000/01/rdf-schema#";

    private final JsonDocument shaclDocument;

    public ShaclSchema(JsonDocument shaclDocument) {
        this.shaclDocument = shaclDocument;
    }

    public List<ShaclShape> getShaclShapes() {
        Object shapes = null;
        if (this.shaclDocument.containsKey(NEXUS_VOCAB + "shapes")) {
            shapes = this.shaclDocument.get(NEXUS_VOCAB + "shapes");
        } else if (this.shaclDocument.containsKey(JsonLdConsts.REVERSE)) {
            Object reverse = this.shaclDocument.get(JsonLdConsts.REVERSE);
            if (reverse instanceof Map && ((Map) reverse).containsKey(RDF_VOCAB + "isDefinedBy")) {
                shapes = ((Map) reverse).get(RDF_VOCAB + "isDefinedBy");
            }
        }

        List shapeList;
        if (shapes == null) {
            shapeList = Collections.emptyList();
        } else if (!(shapes instanceof List)) {
            shapeList = Collections.singletonList(shapes);
        } else {
            shapeList = (List) shapes;
        }
        return (List<ShaclShape>) (shapeList.stream().filter(s -> s instanceof Map).map(s -> new ShaclShape(new JsonDocument((Map) s))).collect(Collectors.toList()));

    }
}
