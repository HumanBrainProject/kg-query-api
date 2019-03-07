package org.humanbrainproject.knowledgegraph.suggestion.boundary;

import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.suggestion.SuggestionStatus;
import org.humanbrainproject.knowledgegraph.context.QueryContext;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.humanbrainproject.knowledgegraph.suggestion.control.SuggestionController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.json.Json;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.Map;

@Component
public class Suggest {

    @Autowired
    SuggestionController suggestionController;

    @Autowired
    QueryContext queryContext;

    @Autowired
    ArangoRepository arangoRepository;

    @Autowired
    JsonTransformer jsonTransformer;

    public QueryResult<List<Map>> suggestByField(NexusSchemaReference schemaReference, String field, String search, Pagination pagination){
        return suggestionController.simpleSuggestByField(schemaReference, field, search, pagination);
    }

    public NexusInstanceReference createSuggestionInstanceForUser(NexusInstanceReference ref, String userId, String clientIdExtension) throws ResponseStatusException{
        return suggestionController.createSuggestionInstanceForUser(ref, userId, clientIdExtension);
    }

    public Map getUserSuggestionOfSpecificInstance(NexusInstanceReference ref, String userId) throws ResponseStatusException {
        return suggestionController.getUserSuggestionOfSpecificInstance(ref, userId);
    }

    public List<String> getUserSuggestions(String userId, SuggestionStatus status) throws ResponseStatusException{
        return suggestionController.getUserSuggestions(userId, status);
    }

    public NexusInstanceReference changeSuggestionStatus(NexusInstanceReference ref, SuggestionStatus status, String clientIdExtension) throws ResponseStatusException {
        return suggestionController.changeSuggestionStatus(ref, status, clientIdExtension);
    }

    public boolean deleteSuggestion(NexusInstanceReference ref) throws ResponseStatusException{
        return suggestionController.deleteSuggestion(ref);
    }
    public List<String> getUserReviewRequested(String userId) {
        return suggestionController.getUserReviewRequested(userId);
    }

    public NexusInstanceReference updateInstance(NexusInstanceReference ref, String content, String clientIdExtension) throws ResponseStatusException{
        return suggestionController.updateInstance(ref, jsonTransformer.parseToMap(content), clientIdExtension);
    }

    public Map getInstance(NexusInstanceReference ref) {
       return arangoRepository.getInstance(ArangoDocumentReference.fromNexusInstance(ref), queryContext.getDatabaseConnection());
    }


}
