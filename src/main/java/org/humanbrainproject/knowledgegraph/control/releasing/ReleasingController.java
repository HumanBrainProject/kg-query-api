package org.humanbrainproject.knowledgegraph.control.releasing;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDefaultDatabaseDriver;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoReleasedDatabaseDriver;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdProperty;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import javax.json.JsonObject;
import java.util.*;

@Component
public class ReleasingController {

    private static final String RELEASE_TYPE="http://hbp.eu/minds#Release";
    private static final String RELEASE_INSTANCE_PROPERTYNAME = "http://hbp.eu/minds#releaseinstance";

    @Autowired
    ArangoDefaultDatabaseDriver arango;

    @Autowired
    ArangoReleasedDatabaseDriver arangoReleased;

    @Autowired
    ArangoRepository repository;

    public boolean isRelevantForReleasing(List<JsonLdVertex> vertices){
        for (JsonLdVertex vertex : vertices) {
            JsonLdProperty typeProperty = vertex.getPropertyByName(JsonLdConsts.TYPE);
            if(typeProperty!=null && RELEASE_TYPE.equals(typeProperty.getValue())){
                return true;
            }
        }
        return false;
    }


    public List<JsonLdVertex> getVerticesToBeReleased(List<JsonLdVertex> originalVertices) throws JSONException {
        for (JsonLdVertex originalVertex : originalVertices) {
            JsonLdProperty releaseInstances = originalVertex.getPropertyByName(RELEASE_INSTANCE_PROPERTYNAME);
            if(releaseInstances!=null) {
                if (releaseInstances.getValue() instanceof List) {
                    for (Object o : ((List) releaseInstances.getValue())) {
                        if (o instanceof JSONObject) {
                            releaseInstance((JSONObject) o);
                        } else {
                            throw new RuntimeException(String.format("Was not able to release instance! Release structure passed non-interpretable type %s", o.getClass()));
                        }
                    }
                } else if (releaseInstances.getValue() instanceof JSONObject) {
                    releaseInstance((JSONObject) releaseInstances.getValue());
                } else {
                    throw new RuntimeException(String.format("Was not able to release instance! Release structure passed non-interpretable type %s", releaseInstances.getValue().getClass()));
                }
            }
        }
        return originalVertices;
    }


    private void releaseInstance(JSONObject object) throws JSONException {
        if(object.has(JsonLdConsts.ID)){
            Set<String> edgesCollectionNames = arango.getEdgesCollectionNames();
            Set<String> embeddedInstances = repository.getEmbeddedInstances(Collections.singletonList(object.getString(JsonLdConsts.ID)), arango, edgesCollectionNames, new LinkedHashSet<>());
            repository.stageElementsToReleased(embeddedInstances, arango, arangoReleased);
        }
        else{
            throw new RuntimeException("Release object did not contain a valid reference");
        }
    }

}
