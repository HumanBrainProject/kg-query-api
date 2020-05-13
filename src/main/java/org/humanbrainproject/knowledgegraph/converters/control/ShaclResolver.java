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

package org.humanbrainproject.knowledgegraph.converters.control;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusClient;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ShaclResolver {

    @Autowired
    JsonLdStandardization jsonLdStandardization;

    @Autowired
    NexusConfiguration nexusConfiguration;

    @Autowired
    NexusClient nexusClient;

    @Autowired
    AuthorizationContext authorizationContext;

    private static String OWL_NAMESPACE ="http://www.w3.org/2002/07/owl#";

    public List<JsonDocument> resolve(NexusSchemaReference schemaReference){
        ArrayList<JsonDocument> collector = new ArrayList<>();
        resolve(schemaReference, new HashSet<>(), collector);
        return collector;
    }


    void resolve(NexusSchemaReference schemaReference, Set<NexusSchemaReference> alreadyVisited, List<JsonDocument> importCollector){
        Map map = nexusClient.get(schemaReference.getRelativeUrl(), authorizationContext.getCredential());
        JsonDocument qualified = new JsonDocument(jsonLdStandardization.fullyQualify(map));
        importCollector.add(qualified);
        resolveImport(qualified, alreadyVisited, importCollector);
    }

    void resolveImport(JsonDocument qualifiedDoc, Set<NexusSchemaReference> alreadyVisited, List<JsonDocument> importCollector){
        Object imports = qualifiedDoc.get(OWL_NAMESPACE+"imports");
        if(imports!=null){
            List<Map> importList;
            if(imports instanceof List){
                importList = (List)imports;
            }
            else{
                importList = Collections.singletonList((Map)imports);
            }
            for (Map map : importList) {
                Object id = map.get(JsonLdConsts.ID);
                if(id!=null){
                    if(id.toString().startsWith(nexusConfiguration.getNexusBase())){
                        NexusSchemaReference fromUrl = NexusSchemaReference.createFromUrl(id.toString());
                        if(!alreadyVisited.contains(fromUrl)) {
                            alreadyVisited.add(fromUrl);
                            resolve(fromUrl, alreadyVisited, importCollector);
                        }
                    }
                }
            }
        }
    }
}
