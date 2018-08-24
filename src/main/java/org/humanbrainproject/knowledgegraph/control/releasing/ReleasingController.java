package org.humanbrainproject.knowledgegraph.control.releasing;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoNamingConvention;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdProperty;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.humanbrainproject.knowledgegraph.exceptions.InvalidPayloadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ReleasingController {

    private static final String RELEASE_TYPE = "http://hbp.eu/minds#Release";
    private static final String RELEASE_INSTANCE_PROPERTYNAME = "http://hbp.eu/minds#releaseinstance";

    @Autowired
    ArangoRepository repository;

    @Autowired
    ArangoNamingConvention namingConvention;

    public boolean isRelevantForReleasing(List<JsonLdVertex> vertices) {
        for (JsonLdVertex vertex : vertices) {
            JsonLdProperty typeProperty = vertex.getPropertyByName(JsonLdConsts.TYPE);
            if (typeProperty != null && RELEASE_TYPE.equals(typeProperty.getValue())) {
                return true;
            }
        }
        return false;
    }

    public boolean isRelevantForReleasing(Map object) {
        return object.containsKey(JsonLdConsts.TYPE) && RELEASE_TYPE.equals(object.get(JsonLdConsts.TYPE));
    }

    public void releaseVertices(List<JsonLdVertex> originalVertices, ArangoDriver defaultDb, ArangoDriver releaseDb) throws JSONException {
        for (JsonLdVertex originalVertex : originalVertices) {
            JsonLdProperty releaseInstances = originalVertex.getPropertyByName(RELEASE_INSTANCE_PROPERTYNAME);
            if (releaseInstances != null) {
                if (releaseInstances.getValue() instanceof List) {
                    for (Object o : ((List) releaseInstances.getValue())) {
                        if (o instanceof JSONObject) {
                            releaseInstance((JSONObject) o, defaultDb, releaseDb);
                        } else {
                            throw new RuntimeException(String.format("Was not able to release instance! Release structure passed non-interpretable type %s", o.getClass()));
                        }
                    }
                } else if (releaseInstances.getValue() instanceof JSONObject) {
                    releaseInstance((JSONObject) releaseInstances.getValue(), defaultDb, releaseDb);
                } else {
                    throw new RuntimeException(String.format("Was not able to release instance! Release structure passed non-interpretable type %s", releaseInstances.getValue().getClass()));
                }
            }
        }
    }


    public void unreleaseVertices(Map instance, ArangoDriver releaseDb) {
        Object releaseInstanceUrls = instance.get(RELEASE_INSTANCE_PROPERTYNAME);
        if (releaseInstanceUrls != null) {
            if (releaseInstanceUrls instanceof List) {
                for (Object o : ((List) releaseInstanceUrls)) {
                    if(o instanceof Map && ((Map)o).containsKey(JsonLdConsts.ID)){
                        unreleaseInstance(((Map) o).get(JsonLdConsts.ID).toString(), releaseDb);
                    }
                }
            } else if (releaseInstanceUrls instanceof Map && ((Map)releaseInstanceUrls).containsKey(JsonLdConsts.ID)) {
                unreleaseInstance(((Map) releaseInstanceUrls).get(JsonLdConsts.ID).toString(), releaseDb);
            } else {
                throw new RuntimeException(String.format("Was not able to unrelease instance! %s", releaseInstanceUrls));
            }
        }
    }

    private void releaseInstance(JSONObject object, ArangoDriver defaultDb, ArangoDriver releaseDb) throws JSONException {
        if (object.has(JsonLdConsts.ID) && object.getString(JsonLdConsts.ID).startsWith("http")) {
            Set<String> edgesCollectionNames = defaultDb.getEdgesCollectionNames();
            Set<String> embeddedInstances = repository.getEmbeddedInstances(Collections.singletonList(object.getString(JsonLdConsts.ID)), defaultDb, edgesCollectionNames, new LinkedHashSet<>());
            repository.stageElementsToReleased(embeddedInstances, defaultDb, releaseDb);
        } else {
            throw new InvalidPayloadException("Release object did not contain a valid reference");
        }
    }

    public void unreleaseInstance(String url, ArangoDriver releaseDb) {
        Set<String> edgesCollectionNames = releaseDb.getEdgesCollectionNames();
        Set<String> embeddedInstances = repository.getEmbeddedInstances(Collections.singletonList(url), releaseDb, edgesCollectionNames, new LinkedHashSet<>());
        for (String embeddedInstance : embeddedInstances) {
            repository.deleteVertex(embeddedInstance, releaseDb);
        }
    }

}
