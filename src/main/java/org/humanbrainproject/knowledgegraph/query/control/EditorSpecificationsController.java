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

package org.humanbrainproject.knowledgegraph.query.control;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.google.gson.Gson;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoConnection;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoNamingHelper;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class EditorSpecificationsController {
    private Gson gson = new Gson();

    protected Logger logger = LoggerFactory.getLogger(FreemarkerTemplating.class);

    private final static ArangoCollectionReference TEMPLATES = new ArangoCollectionReference("editor_specifications");

    public void saveSpecification(String specification, String specificationId, ArangoConnection driver) {
        ArangoDatabase db = driver.getOrCreateDB();
        ArangoCollection collection = db.collection(TEMPLATES.getName());
        if(!collection.exists()){
            db.createCollection(TEMPLATES.getName());
        }
        if(collection.documentExists(specificationId)) {
            Map document = collection.getDocument(specificationId, Map.class);
            document.replace("uiSpec", gson.fromJson(specification, Map.class));
            collection.replaceDocument(specificationId, document);
        } else {
            Map document = new HashMap<>();
            String key = ArangoNamingHelper.createCompatibleId(specificationId);
            document.put(ArangoVocabulary.KEY, key);
            document.put("uiSpec", gson.fromJson(specification, Map.class));
            collection.insertDocument(document);
        }
    }
}
