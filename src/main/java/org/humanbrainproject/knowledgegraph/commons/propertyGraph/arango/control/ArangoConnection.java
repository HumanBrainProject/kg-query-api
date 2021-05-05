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

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionType;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.control.DatabaseConnection;
import org.springframework.beans.factory.annotation.Value;

import java.util.Set;
import java.util.stream.Collectors;

@ToBeTested(systemTestRequired = true)
public class ArangoConnection implements DatabaseConnection<ArangoDatabase>{

    @Value("${org.humanbrainproject.knowledgegraph.arango.host}")
    String host;
    @Value("${org.humanbrainproject.knowledgegraph.arango.port}")
    Integer port;
    @Value("${org.humanbrainproject.knowledgegraph.arango.user}")
    String user;
    @Value("${org.humanbrainproject.knowledgegraph.arango.pwd}")
    String pwd;

    private final String databaseName;
    private final boolean translateToMainSpace;

    ArangoDB arangoDB;

    public ArangoConnection(String databaseName, boolean translateToMainSpace) {
        this.databaseName = databaseName;
        this.translateToMainSpace = translateToMainSpace;
    }

    private ArangoDB getArangoDB(){
        if(arangoDB==null){
            arangoDB = new ArangoDB.Builder().host(host, port).user(user).password(pwd).build();
        }
        return arangoDB;
    }

    public Set<ArangoCollectionReference> getEdgesCollectionNames(){
        return getOrCreateDB().getCollections().stream().filter(c -> !c.getIsSystem() && c.getType() == CollectionType.EDGES).map(c -> new ArangoCollectionReference(c.getName())).collect(Collectors.toSet());
    }

    public ArangoDatabase getOrCreateDB() {
        return getOrCreateDB(true);
    }

    private ArangoDatabase getOrCreateDB(boolean retryOnFail){
        try {
            ArangoDatabase kg = getArangoDB().db(databaseName);
            if (!kg.exists()) {
                kg.create();
            }
            return kg;
        }
        catch(ArangoDBException exception){
            if(retryOnFail) {
                arangoDB = null;
                return getOrCreateDB(false);
            }
            else{
                throw  exception;
            }
        }
    }

    public Set<ArangoCollectionReference> getCollections(){
        return getOrCreateDB().getCollections().stream().map(r -> new ArangoCollectionReference(r.getName())).collect(Collectors.toSet());
    }


    public Set<ArangoCollectionReference> filterExistingCollectionLabels(Set<ArangoCollectionReference> collections){
        Set<ArangoCollectionReference> existingCollectionLabels = getCollections();
        return collections.stream().filter(existingCollectionLabels::contains).collect(Collectors.toSet());
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public boolean isTranslateToMainSpace() {
        return translateToMainSpace;
    }

    @Override
    public void clearData() {
        ArangoDatabase db = getOrCreateDB();
        for (CollectionEntity collectionEntity : db.getCollections()) {
            if (!collectionEntity.getName().startsWith("_")) {
                db.collection(collectionEntity.getName()).drop();
            }
        }
    }
}
