package org.humanbrainproject.knowledgegraph.query.boundary;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoDatabaseFactory;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.control.SuggestionControl;
import org.humanbrainproject.knowledgegraph.query.entity.SuggestionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@ToBeTested(integrationTestRequired = true, systemTestRequired = true)
public class Suggestions {

    @Autowired
    SuggestionControl suggestionControl;

    @Autowired
    ArangoDatabaseFactory databaseFactory;


    public List<SuggestionResult> suggestionForInstanceByField(NexusSchemaReference schemaReference, String field){
        return suggestionControl.suggestionForInstanceByField(schemaReference, field);
    }

    public List<SuggestionResult> suggestionForInstanceById(List<String> ids){
        SuggestionResult r = new SuggestionResult();
        r.setId("foo");
        r.setLabel("bar");
        r.setType("foobar");
        return Arrays.asList(r);
    }
}
