/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.humanbrainproject.knowledgegraph.converters.boundary;

import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.converters.control.Shacl2Editor;
import org.humanbrainproject.knowledgegraph.converters.control.ShaclResolver;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Converter {

    @Autowired
    ShaclResolver resolver;

    @Autowired
    Shacl2Editor shacl2Editor;

    @Autowired
    NexusClient nexusClient;

    @Autowired
    JsonLdStandardization jsonLdStandardization;

    @Autowired
    AuthorizationContext authorizationContext;

    public JsonDocument convertShaclToEditor(NexusSchemaReference schemaReference){
        List<JsonDocument> resolvedAndQualified = resolver.resolve(schemaReference);
        return shacl2Editor.convert(schemaReference, resolvedAndQualified);
    }


}
