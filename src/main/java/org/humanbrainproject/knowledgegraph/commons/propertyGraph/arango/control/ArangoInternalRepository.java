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

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.UnauthorizedAccess;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query.ArangoQueryFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.exceptions.StoredQueryNotFoundException;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ToBeTested(systemTestRequired = true)
@UnauthorizedAccess("The internal documents are open to everyone (although exposed through internal APIs only")
public class ArangoInternalRepository {

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    ArangoQueryFactory queryFactory;

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    JsonTransformer jsonTransformer;

    protected Logger logger = LoggerFactory.getLogger(ArangoRepository.class);

    private ArangoDatabase getDB(){
        return databaseFactory.getInternalDB().getOrCreateDB();
    }

    public void insertOrUpdateDocument(ArangoDocumentReference document, String documentPayload) throws IllegalAccessException{
        if (document != null && documentPayload != null) {
            String userId = authorizationContext.getUserId();
            if(userId==null){
                throw new IllegalAccessException("You have to be authenticated if you want to execute this operation");
            }
            ArangoDatabase db = getDB();
            ArangoCollection collection = db.collection(document.getCollection().getName());
            if (!collection.exists()) {
                db.createCollection(document.getCollection().getName());
                logger.info("Created collection {} in database {}", document.getCollection().getName(), db.name());
                collection = db.collection(document.getCollection().getName());
            }
            if (collection.documentExists(document.getKey())) {
                try {
                    Map payload = collection.getDocument(document.getKey(), Map.class);
                    Object createdByUser = payload.get(ArangoVocabulary.CREATED_BY_USER);
                    if(createdByUser==null || createdByUser.equals(userId)) {
                        Map map = jsonTransformer.parseToMap(documentPayload);
                        map.put(ArangoVocabulary.CREATED_BY_USER, userId);
                        collection.updateDocument(document.getKey(), jsonTransformer.getMapAsJson(map));
                        logger.info("Updated document: {} in database {}", document.getId(), db.name());
                        logger.debug("Payload of document {} in database {}: {}", document.getId(), db.name(), documentPayload);
                    }
                    else {
                        throw new IllegalAccessException("You've tried to update an already existing specification which was not created by yourself");
                    }
                } catch (ArangoDBException dbexception) {
                    logger.error(String.format("Was not able to update document: %s in database %s", document.getId(), db.name()), dbexception);
                    throw dbexception;
                }
                collection.updateDocument(document.getKey(), documentPayload);
            } else {
                try {
                    Map map = jsonTransformer.parseToMap(documentPayload);
                    map.put(ArangoVocabulary.CREATED_BY_USER,userId);
                    collection.insertDocument(jsonTransformer.getMapAsJson(map));
                    logger.info("Inserted document: {} in database {}", document.getId(), db.name());
                    logger.debug("Payload of document {} in database {}: {}", document.getId(), db.name(), documentPayload);
                } catch (ArangoDBException dbexception) {
                    logger.error(String.format("Was not able to insert document: %s in database %s", document.getId(), db.name()), dbexception);
                    throw dbexception;
                }
            }
        }
    }

    public <T> List<T> getAll(ArangoCollectionReference collection, Class<T> clazz) {
        String query = queryFactory.getAll(collection);
        try {
            return getDB().query(query, null, new AqlQueryOptions(), clazz).asListRemaining();
        } catch (ArangoDBException e) {
            logger.error("Arango query exception - {}", query);
            throw e;
        }
    }

    public Set<NexusSchemaReference> getSchemasWithSpecification(String queryId){
        String query = queryFactory.queryRootSchemasForQueryId(queryId);
        try {
            return getDB().query(query, null, new AqlQueryOptions(), String.class).asListRemaining().stream().map(url -> NexusSchemaReference.createFromUrl(url)).collect(Collectors.toSet());
        } catch (ArangoDBException e) {
            logger.error("Arango query exception - {}", query);
            throw e;
        }
    }


    public List<Map> getInternalDocuments(ArangoCollectionReference collection) {
        String query = queryFactory.getAllInternalDocumentsOfACollection(collection);
        ArangoCursor<Map> q = getDB().query(query, null, new AqlQueryOptions(), Map.class);
        return q.asListRemaining();
    }

    public <T> List<T> getInternalDocumentsWithKeyPrefix(ArangoCollectionReference collection, String keyPrefix, Class<T> returnType) {
        String query = queryFactory.getInternalDocumentsOfCollectionWithKeyPrefix(collection, keyPrefix);
        ArangoCursor<T> q = getDB().query(query, null, new AqlQueryOptions(), returnType);
        return q.asListRemaining();
    }

    public <T> T getInternalDocumentByKey(ArangoDocumentReference document, Class<T> clazz) {
        return getDB().collection(document.getCollection().getName()).getDocument(document.getKey(), clazz);
    }


    public boolean doesDocumentExist(ArangoCollectionReference collectionReference, String key) {
        ArangoDocumentReference documentReference = new ArangoDocumentReference(collectionReference, key);
        ArangoCollection collection = databaseFactory.getInternalDB().getOrCreateDB().collection(documentReference.getCollection().getName());
        if (collection.exists()) {
            return collection.documentExists(documentReference.getKey());
        }
        return false;
    }

    public void removeInternalDocument(ArangoDocumentReference document) throws IllegalAccessException, StoredQueryNotFoundException {
        String userId = authorizationContext.getUserId();
        if(userId==null){
            throw new IllegalAccessException("You have to be authenticated if you want to execute this operation");
        }
        ArangoDatabase db = getDB();
        ArangoCollection collection = db.collection(document.getCollection().getName());
        if ( collection.exists() && collection.documentExists(document.getKey())) {
            collection.deleteDocument(document.getKey());
        } else {
            throw new StoredQueryNotFoundException("Query not found");
        }
    }
}
