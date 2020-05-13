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

package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.structure.boundary.StructureFromNexus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@ToBeTested
public class ArangoToNexusLookupMap {

    @Autowired
    StructureFromNexus structureFromNexus;

    protected Logger logger = LoggerFactory.getLogger(ArangoToNexusLookupMap.class);

    private static final Map<String, NexusSchemaReference> schemaReferenceMap = Collections.synchronizedMap(new HashMap<>());
    private static boolean initialFetch = false;


    public static void addToSchemaReferenceMap(ArangoCollectionReference arangoName, NexusSchemaReference schemaReference){
        if(!schemaReferenceMap.containsKey(arangoName.getName())) {
            schemaReferenceMap.put(arangoName.getName(), schemaReference);
        }
    }

    public synchronized NexusSchemaReference getNexusSchema(ArangoCollectionReference arangoCollectionReference){
        NexusSchemaReference schemaReference = schemaReferenceMap.get(arangoCollectionReference.getName());
        if(schemaReference==null){
            //Refetch schemas from nexus and translate them
            refetch();
            schemaReference = schemaReferenceMap.get(arangoCollectionReference.getName());
        }
        return schemaReference;
    }

    private void refetch(){
        initialFetch = true;
        logger.info("Start fetching schemas - cache population in process");
        structureFromNexus.getAllSchemasInMainSpace().forEach(ArangoCollectionReference::fromNexusSchemaReference);
        logger.info("Done fetching schemas - the cache is populated");
    }


    public synchronized Map<String, NexusSchemaReference> getLookupTable(boolean refetch){
        if(!initialFetch || schemaReferenceMap.isEmpty() || refetch){
            refetch();
        }
        return schemaReferenceMap;
    }

}
