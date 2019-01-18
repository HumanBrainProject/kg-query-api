package org.humanbrainproject.knowledgegraph.suggestion.control;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoInferredRepository;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.suggestion.entity.Suggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SuggestionController {

    @Autowired
    ArangoInferredRepository repository;


    public List<Suggestion> simpleSuggestByField(NexusSchemaReference originalSchema, String field, String search, Pagination pagination){
        List<Map> suggestions = repository.getSuggestionsByField(originalSchema, field, search, pagination);
        return suggestions.stream().map(s -> {
            Suggestion suggestion = new Suggestion();
            suggestion.setId((String) s.get("id"));
            suggestion.setLabel((String) s.get("name"));
            return suggestion;
        }).collect(Collectors.toList());

    }

    public List<Suggestion> simpleSuggestByType(NexusSchemaReference originalSchema, String type, String search, Pagination pagination){
        List<Map> suggestions = repository.getSuggestionsByField(originalSchema, type, search, pagination);
        return suggestions.stream().map(s -> {
            Suggestion suggestion = new Suggestion();
            suggestion.setId((String) s.get("id"));
            suggestion.setLabel((String) s.get("name"));
            return suggestion;
        }).collect(Collectors.toList());

    }

}
