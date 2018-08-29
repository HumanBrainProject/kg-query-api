package org.humanbrainproject.knowledgegraph.control.releasing;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoDriver;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoNamingConvention;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoRepository;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdEdge;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdProperty;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.humanbrainproject.knowledgegraph.exceptions.InvalidPayloadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ReleasingController {

    private static final String RELEASE_TYPE = "http://hbp.eu/minds#Release";
    private static final String RELEASE_INSTANCE_PROPERTYNAME = "http://hbp.eu/minds#releaseinstance";

    @Autowired
    ArangoRepository repository;

    @Autowired
    ArangoNamingConvention namingConvention;

    public List<List<String>> findDocumentsToBeUnreleased(List<JsonLdVertex> jsonLdVertices, ArangoDriver defaultDB) {
        return jsonLdVertices.stream().map(v -> {
            List<JsonLdEdge> edgesToBeRemoved = repository.getEdgesToBeRemoved(v, defaultDB);
            return edgesToBeRemoved.stream().map(e -> repository.getTargetVertexId(e, defaultDB)).collect(Collectors.toList());
        }).collect(Collectors.toList());
    }

    public void unreleaseDocuments(List<List<String>> verticesToBeUnreleased, ArangoDriver releasedDB) {
        if (verticesToBeUnreleased != null) {
            verticesToBeUnreleased.forEach(vertices -> vertices.forEach(v -> unreleaseInstance(v, releasedDB)));
        }
    }

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
                        if (o instanceof JsonLdProperty) {
                            releaseInstance((JsonLdProperty)o, defaultDb, releaseDb);
                        } else {
                            throw new RuntimeException(String.format("Was not able to release instance! Release structure passed non-interpretable type %s", o.getClass()));
                        }
                    }
                } else if (releaseInstances.getValue() instanceof JsonLdProperty) {
                    releaseInstance((JsonLdProperty) releaseInstances.getValue(), defaultDb, releaseDb);
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
                    if (o instanceof Map && ((Map) o).containsKey(JsonLdConsts.ID)) {
                        unreleaseInstance(((Map) o).get(JsonLdConsts.ID).toString(), releaseDb);
                    }
                }
            } else if (releaseInstanceUrls instanceof Map && ((Map) releaseInstanceUrls).containsKey(JsonLdConsts.ID)) {
                unreleaseInstance(((Map) releaseInstanceUrls).get(JsonLdConsts.ID).toString(), releaseDb);
            } else {
                throw new RuntimeException(String.format("Was not able to unrelease instance! %s", releaseInstanceUrls));
            }
        }
    }

    void releaseInstance(JsonLdProperty jsonLdProperty, ArangoDriver defaultDb, ArangoDriver releaseDb) throws JSONException {
        if (jsonLdProperty.getValue() instanceof JsonLdProperty) {
            releaseInstance((JsonLdProperty) jsonLdProperty.getValue(), defaultDb, releaseDb);
        }
        else {
            if (jsonLdProperty.getName().equals(JsonLdConsts.ID) && jsonLdProperty.getValue() != null && jsonLdProperty.getValue().toString().startsWith("http")) {
                Set<String> edgesCollectionNames = defaultDb.getEdgesCollectionNames();
                Set<String> embeddedInstances = repository.getEmbeddedInstances(Collections.singletonList(jsonLdProperty.getValue().toString()), defaultDb, edgesCollectionNames, new LinkedHashSet<>());
                repository.stageElementsToReleased(embeddedInstances, defaultDb, releaseDb);
                return;
            }
        }
        throw new InvalidPayloadException("Release object did not contain a valid reference");

    }

    void unreleaseInstance(String url, ArangoDriver releaseDb) {
        //The url needs to be absolute - everything else is not resolvable.
        if(url.startsWith("http")) {
            Set<String> edgesCollectionNames = releaseDb.getEdgesCollectionNames();
            Set<String> embeddedInstances = repository.getEmbeddedInstances(Collections.singletonList(url), releaseDb, edgesCollectionNames, new LinkedHashSet<>());
            for (String embeddedInstance : embeddedInstances) {
                repository.deleteVertex(embeddedInstance, releaseDb);
            }
        }
    }

}
