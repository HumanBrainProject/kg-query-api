package org.humanbrainproject.knowledgegraph.query.boundary;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.authorization.entity.Credential;
import org.humanbrainproject.knowledgegraph.commons.labels.SemanticsToHumanTranslator;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ArangoGraph {

    @Autowired
    ArangoDatabaseFactory databaseFactory;

    @Autowired
    ArangoRepository arangoRepository;

    @Autowired
    SemanticsToHumanTranslator translator;

    public Map getGraph(NexusInstanceReference instance, Integer step, Credential credential) {
        List<Map> maps = arangoRepository.inDepthGraph(ArangoDocumentReference.fromNexusInstance(instance), step, databaseFactory.getInferredDB(), credential);
        JsonDocument result = new JsonDocument();
        List<Map<String, Object>> nodesList = new ArrayList<>();
        List<Map<String, Object>> linksList = new ArrayList<>();
        result.put("nodes", nodesList);
        result.put("links", linksList);
        Map<String, String> arangoToNexusId = new HashMap<>();
        Set<String> documentIds = new HashSet<>();
        for (Map map : maps) {
            List<Map> vertices = (List<Map>) map.get("vertices");
            for (Map vertex : vertices) {
                JsonDocument node = new JsonDocument();
                String id = (String) vertex.get(HBPVocabulary.RELATIVE_URL_OF_INTERNAL_LINK);
                if (!documentIds.contains(id)) {
                    arangoToNexusId.put((String) vertex.get(ArangoVocabulary.ID), id);
                    node.put("id", id);
                    Object type = vertex.get(JsonLdConsts.TYPE);
                    if (type instanceof List) {
                        type = ((List) type).isEmpty() ? null : ((List) type).get(0);
                    }
                    node.put("name", translator.translateSemanticValueToHumanReadableLabel((String) type));
                    node.put("dataType", type);
                    node.put("title", vertex.get(SchemaOrgVocabulary.NAME));
                    nodesList.add(node);
                    documentIds.add(id);
                }
            }
        }
        for (Map map : maps) {
            List<Map> edges = (List<Map>) map.get("edges");
            for (Map edge : edges) {
                JsonDocument link = new JsonDocument();
                String id = (String) edge.get(ArangoVocabulary.ID);
                if (!documentIds.contains(id)) {
                    link.put("id", id);
                    link.put("title", translator.translateSemanticValueToHumanReadableLabel((String) edge.get(ArangoVocabulary.NAME)));
                    link.put("source", arangoToNexusId.get(edge.get(ArangoVocabulary.FROM)));
                    link.put("target", arangoToNexusId.get(edge.get(ArangoVocabulary.TO)));
                    linksList.add(link);
                    documentIds.add(id);
                }
            }
        }
        return result;
    }

    public List<Map> getInternalDocuments(ArangoCollectionReference collection) {
        //FIXME: This method gives access to all documents inside the internal database and exposes it through the @link{org.humanbrainproject.knowledgegraph.query.api.GraphInternalAPI} this could become a vulnerability in the long term (depending on what is stored inside the internal database)
        return arangoRepository.getInternalDocuments(collection);
    }

    public Map getInstanceList(NexusSchemaReference schemaReference, Integer from, Integer size, String searchTerm, Credential credential) {
        return arangoRepository.getInstanceList(ArangoCollectionReference.fromNexusSchemaReference(schemaReference), from, size, searchTerm, databaseFactory.getInferredDB(), credential);
    }

    public Map getBookmarks(NexusInstanceReference instanceRef, Integer from, Integer size, String searchTerm, Credential credential) {
        return arangoRepository.getBookmarks(instanceRef, from, size, searchTerm, databaseFactory.getInferredDB(), credential);
    }
}
