package org.humanbrainproject.knowledgegraph.indexing.control;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.nexus.control.NexusConfiguration;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Edge;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.JsonPath;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Vertex;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.Step;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.Stack;

@Component
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
        map.put(HBPVocabulary.PROVENANCE_INDEXED_IN_ARANGO_AT, ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        map.put(HBPVocabulary.PROVENANCE_LAST_MODIFICATION_USER_ID, message.getUserId());
        map.put(HBPVocabulary.PROVENANCE_MODIFIED_AT, message.getTimestamp());
        return new QualifiedIndexingMessage(message, map);
    }

    /**
     * Takes a jsonLdPayload (fully qualified) and transforms it into a vertex including subvertices (for embedded instances) and their outgoing edges and prepared properties.
     */
    public Vertex createVertexStructure(QualifiedIndexingMessage qualifiedNexusIndexingMessage){
        Vertex targetVertex = new Vertex(qualifiedNexusIndexingMessage);
        findEdges(targetVertex, new Stack<>(), qualifiedNexusIndexingMessage.getQualifiedMap(), null);
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

    private void findEdges(Vertex vertex, Stack<Step> path, Object map, Integer orderNumber) {
        if (map instanceof Map) {
            for (Object key : ((Map) map).keySet()) {
                Stack<Step> currentPath = new Stack<>();
                currentPath.addAll(path);
                currentPath.push(new Step((String)key, orderNumber != null ? orderNumber : 0));
                Object value = ((Map) map).get(key);
                NexusInstanceReference internalReference = getInternalReference(value);
                if (internalReference != null) {
                    vertex.getEdges().add(new Edge(vertex, new JsonPath(currentPath), internalReference));
                } else {
                    findEdges(vertex, currentPath, ((Map) map).get(key), null);
                }
            }
        } else if (map instanceof Collection) {
            int counter = 0;
            for (Object o : ((Collection) map)) {
                findEdges(vertex, path, o, counter);
            }
        }
    }


}
