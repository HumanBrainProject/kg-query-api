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

package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.UnauthorizedAccess;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query.ArangoQueryFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@ToBeTested(systemTestRequired = true)
@UnauthorizedAccess("Querying the data structure is public knowledge - there is no data exposed")
public class ArangoStructureRepository {

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    ArangoQueryFactory queryFactory;

    @Autowired
    AuthorizationContext authorizationContext;


    public List<Map> getAttributesWithCount(ArangoCollectionReference reference) {
        ArangoDatabase db = databaseFactory.getInferredDB(true).getOrCreateDB();
        if (db.collection(reference.getName()).exists()) {
            String q = queryFactory.getAttributesWithCount(reference);
            ArangoCursor<Map> result = db.query(q, null, new AqlQueryOptions(), Map.class);
            return result.asListRemaining();
        } else {
            return Collections.emptyList();
        }
    }

    public List<Map> getInboundRelationsForDocument(ArangoDocumentReference documentReference) {
        ArangoConnection inferredDB = databaseFactory.getInferredDB(false);
        Set<ArangoCollectionReference> edgesCollectionNames = inferredDB.getEdgesCollectionNames();
        String q = queryFactory.queryInboundRelationsForDocument(documentReference, edgesCollectionNames, authorizationContext.getReadableOrganizations(), false);
        ArangoCursor<Map> result = inferredDB.getOrCreateDB().query(q, null, new AqlQueryOptions(), Map.class);
        return result.asListRemaining();
    }

    public List<Map> getDirectRelationsWithType(ArangoCollectionReference collectionReference, boolean outbound){
        ArangoConnection inferredDB = databaseFactory.getInferredDB(true);
        if(inferredDB.getOrCreateDB().collection(collectionReference.getName()).exists()) {
            Set<ArangoCollectionReference> edgesCollectionNames = inferredDB.getEdgesCollectionNames();
            String q = queryFactory.queryDirectRelationsWithType(collectionReference, edgesCollectionNames, outbound);
            ArangoCursor<Map> result = inferredDB.getOrCreateDB().query(q, null, new AqlQueryOptions(), Map.class);
            return result.asListRemaining();
        }
        return Collections.emptyList();
    }
}
