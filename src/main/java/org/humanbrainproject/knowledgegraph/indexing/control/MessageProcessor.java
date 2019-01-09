package org.humanbrainproject.knowledgegraph.indexing.control;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Edge;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.JsonPath;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Step;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Stack;

/**
 * The message processor takes care of the applied manipulations to the original message payload. It e.g. fully qualifies the Json-LD payload,
 * enriches it with computed meta-data and analyzes the payload in terms of structures (detection of vertices and edges).
 */
@Component
@ToBeTested(integrationTestRequired = true)
public class MessageProcessor {

    @Autowired
    JsonLdStandardization jsonLdStandardization;

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    NexusConfiguration configuration;


    public QualifiedIndexingMessage qualify(IndexingMessage message) {
        Map map = jsonTransformer.parseToMap(message.getPayload());
        jsonLdStandardization.ensureContext(map, message.getInstanceReference().createUniqueNamespace());
        map = jsonLdStandardization.fullyQualify(map);
        map = jsonLdStandardization.filterKeysByVocabBlacklists(map);
        map = jsonLdStandardization.flattenLists(map, null, null);
        map.put(HBPVocabulary.PROVENANCE_LAST_MODIFICATION_USER_ID, message.getUserId());
        map.put(HBPVocabulary.PROVENANCE_MODIFIED_AT, message.getTimestamp());
        return new QualifiedIndexingMessage(message, map);
    }

    /**
     * Takes a jsonLdPayload (fully qualified) and transforms it into a vertex including subvertices (for embedded instances) and their outgoing edges and prepared properties.
     */
    public Vertex createVertexStructure(QualifiedIndexingMessage qualifiedNexusIndexingMessage) {
        Vertex targetVertex = new Vertex(qualifiedNexusIndexingMessage);
        findEdges(targetVertex, new Stack<>(), qualifiedNexusIndexingMessage.getQualifiedMap(), 0);
        return targetVertex;
    }


    private NexusInstanceReference getInternalReference(Object value) {
        if (value instanceof Map && ((Map) value).containsKey(JsonLdConsts.ID)) {
            Object id = ((Map) value).get(JsonLdConsts.ID);
            if (id instanceof String && ((String) id).startsWith(configuration.getNexusBase())) {
                return NexusInstanceReference.createFromUrl((String) id);
            }
        }
        return null;
    }

    void findEdges(Vertex vertex, Stack<Step> path, Object map, int globalEdgesCounter) {
        if (map instanceof Map) {
            for (Object key : ((Map) map).keySet()) {
                if(!HBPVocabulary.INFERENCE_ALTERNATIVES.equals(key)) {
                    Object value = ((Map) map).get(key);
                    if (value != null) {
                        if (!(value instanceof Collection)) {
                            value = Arrays.asList(value);
                        }
                        for (Object o : ((Collection) value)) {
                            NexusInstanceReference internalReference = getInternalReference(o);
                            Stack<Step> currentPath = new Stack<>();
                            currentPath.addAll(path);
                            if (internalReference != null) {
                                currentPath.push(new Step((String) key, globalEdgesCounter++));
                                vertex.getEdges().add(new Edge(vertex, new JsonPath(currentPath), internalReference));
                            } else {
                                findEdges(vertex, currentPath, ((Map) map).get(key), globalEdgesCounter);
                            }
                        }
                    }
                }
            }
        } else if (map instanceof Collection) {
            for (Object o : ((Collection) map)) {
                findEdges(vertex, path, o, globalEdgesCounter);
            }
        }
    }


}
