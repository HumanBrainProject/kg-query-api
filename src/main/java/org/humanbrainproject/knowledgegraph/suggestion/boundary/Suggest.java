package org.humanbrainproject.knowledgegraph.suggestion.boundary;

import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.humanbrainproject.knowledgegraph.suggestion.control.SuggestionController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class Suggest {

    @Autowired
    SuggestionController suggestionController;

    public QueryResult<List<Map>> suggestByField(NexusSchemaReference schemaReference, String field, String search, Pagination pagination){
        return suggestionController.simpleSuggestByField(schemaReference, field, search, pagination);
    }


}
