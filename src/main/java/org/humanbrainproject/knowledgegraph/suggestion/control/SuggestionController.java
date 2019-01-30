package org.humanbrainproject.knowledgegraph.suggestion.control;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoInferredRepository;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SuggestionController {

    @Autowired
    ArangoInferredRepository repository;

    public QueryResult<List<Map>> simpleSuggestByField(NexusSchemaReference originalSchema, String field, String search, Pagination pagination){
        return repository.getSuggestionsByField(originalSchema, field, search, pagination);

    }

    public Map getUserSuggestionOfSpecificInstance(NexusInstanceReference instanceReference, NexusInstanceReference userRef){
        return repository.getUserSuggestionOfSpecificInstance(instanceReference, userRef);
    }

    public Map findInstanceBySchemaAndFilter(NexusSchemaReference schema, String filterKey, String filterValue){
        return repository.findInstanceBySchemaAndFilter(schema, filterKey, filterValue);
    }

    public List<Map> getUserSuggestions(NexusInstanceReference ref){
        return repository.getSuggestionsByUser(ref);
    }
}
