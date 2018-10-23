package org.humanbrainproject.knowledgegraph.indexing.control;

import org.humanbrainproject.knowledgegraph.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.jsonld.control.JsonLdToVerticesAndEdges;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.MainVertex;
import org.humanbrainproject.knowledgegraph.propertyGraph.entity.ResolvedVertexStructure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MessageProcessor {

    @Autowired
    JsonLdStandardization jsonLdStandardization;

    @Autowired
    JsonLdToVerticesAndEdges jsonLdToVerticesAndEdges;

    @Autowired
    JsonTransformer jsonTransformer;


    public QualifiedIndexingMessage qualify(IndexingMessage message) {
        Map map = jsonTransformer.parseToMap(message.getPayload());
        jsonLdStandardization.ensureContext(map, message.getInstanceReference().createUniqueNamespace());
        map = jsonLdStandardization.fullyQualify(map);
        map = jsonLdStandardization.filterKeysByVocabBlacklists(map);
        return new QualifiedIndexingMessage(message, map);
    }

    public ResolvedVertexStructure createVertexStructure(QualifiedIndexingMessage qualifiedNexusIndexingMessage){
        return createVertexStructureInAlternativeSpace(qualifiedNexusIndexingMessage, null);
    }

    public ResolvedVertexStructure createVertexStructureInAlternativeSpace(QualifiedIndexingMessage qualifiedMessage, SubSpace targetSubSpace){
        MainVertex vertex = jsonLdToVerticesAndEdges.transformFullyQualifiedJsonLdToVerticesAndEdges(qualifiedMessage, targetSubSpace);
        return new ResolvedVertexStructure(qualifiedMessage, vertex, targetSubSpace);
    }
}
