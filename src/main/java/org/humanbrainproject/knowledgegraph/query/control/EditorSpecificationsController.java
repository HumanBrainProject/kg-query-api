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
