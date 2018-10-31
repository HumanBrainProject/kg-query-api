package org.humanbrainproject.knowledgegraph.indexing.control;

import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdStandardization;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonLdToVerticesAndEdges;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.MainVertex;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.ResolvedVertexStructure;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.IndexingMessage;
import org.humanbrainproject.knowledgegraph.indexing.entity.QualifiedIndexingMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
        map.put(HBPVocabulary.INDEXED_IN_ARANGO_AT, ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        map.put(HBPVocabulary.LAST_MODIFICATION_USER_ID, message.getUserId());
        map.put(HBPVocabulary.MODIFIED_AT, message.getTimestamp());
        return new QualifiedIndexingMessage(message, map);
    }

    public ResolvedVertexStructure createVertexStructure(QualifiedIndexingMessage qualifiedNexusIndexingMessage){
        MainVertex vertex = jsonLdToVerticesAndEdges.transformFullyQualifiedJsonLdToVerticesAndEdges(qualifiedNexusIndexingMessage);
        return new ResolvedVertexStructure(qualifiedNexusIndexingMessage, vertex);
    }
}
