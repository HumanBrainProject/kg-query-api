package org.humanbrainproject.knowledgegraph.suggestion.boundary;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoNativeRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.control.InstanceLookupController;
import org.humanbrainproject.knowledgegraph.instances.control.InstanceManipulationController;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.humanbrainproject.knowledgegraph.suggestion.control.SuggestionController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Suggest {

    @Autowired
    SuggestionController suggestionController;

    @Autowired
    InstanceManipulationController instanceManipulationController;

    @Autowired
    ArangoNativeRepository arangoNativeRepository;


    public QueryResult<List<Map>> suggestByField(NexusSchemaReference schemaReference, String field, String search, Pagination pagination){
        return suggestionController.simpleSuggestByField(schemaReference, field, search, pagination);
    }

    public NexusInstanceReference createSuggestionInstanceForUser(NexusInstanceReference ref, String userId, String clientIdExtension){
        NexusInstanceReference originalId = arangoNativeRepository.findOriginalId(ref);

        Map<String, Object> payload = new HashMap<>();
        payload.put(HBPVocabulary.SUGGESTION_OF, ref.getRelativeUrl().getUrl());
        payload.put(HBPVocabulary.SUGGESTION_OF_ORIGINAL, originalId.getRelativeUrl().getUrl());
        payload.put(HBPVocabulary.SUGGESTION_USER, userId);
        NexusSchemaReference schemaRef = ref.getNexusSchema();
        return instanceManipulationController.createNewInstance(schemaRef.toSubSpace(SubSpace.SUGGESTION), payload, clientIdExtension);
    }

    public Map getUserSuggestion(NexusInstanceReference ref, String userId){
        return suggestionController.getUserSuggestion(ref, userId);
    }


}
