package org.humanbrainproject.knowledgegraph.suggestion.boundary;

import org.humanbrainproject.knowledgegraph.commons.suggestion.SuggestionStatus;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.humanbrainproject.knowledgegraph.suggestion.control.SuggestionController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.Map;

@Component
public class Suggest {

    @Autowired
    SuggestionController suggestionController;

    public QueryResult<List<Map>> suggestByField(NexusSchemaReference schemaReference, String field, String search, Pagination pagination){
        return suggestionController.simpleSuggestByField(schemaReference, field, search, pagination);
    }

    public NexusInstanceReference createSuggestionInstanceForUser(NexusInstanceReference ref, String userId, String clientIdExtension) throws NotFoundException{
        return suggestionController.createSuggestionInstanceForUser(ref, userId, clientIdExtension);
    }

    public Map getUserSuggestionOfSpecificInstance(NexusInstanceReference ref, String userId) throws NotFoundException {
        return suggestionController.getUserSuggestionOfSpecificInstance(ref, userId);
    }

    public List<String> getUserSuggestions(String userId, SuggestionStatus status) throws NotFoundException{
        return suggestionController.getUserSuggestions(userId, status);
    }

    public JsonDocument changeSuggestionStatus(NexusInstanceReference ref, SuggestionStatus status, String clientIdExtension) throws NotFoundException{
        return suggestionController.changeSuggestionStatus(ref, status, clientIdExtension);
    }

    public boolean deleteSuggestion(NexusInstanceReference ref) throws NotFoundException{
        return suggestionController.deleteSuggestion(ref);
    }
    public List<String> getUserReviewRequested(String userId) {
        return suggestionController.getUserReviewRequested(userId);
    }


}
