package org.humanbrainproject.knowledgegraph.query.control;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.authorization.control.AuthorizationContext;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.context.QueryContext;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.SuggestionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ToBeTested
public class SuggestionControl {

    @Autowired
    ArangoRepository repository;

    @Autowired
    AuthorizationContext authorizationContext;

    @Autowired
    QueryContext queryContext;


    public List<SuggestionResult> suggestionForInstanceByField(NexusSchemaReference schemaReference, String field){
        ArangoCollectionReference collection = ArangoCollectionReference.fromNexusSchemaReference(schemaReference);
        String arangoName = ArangoCollectionReference.fromFieldName(field).getName();
        if(queryContext.getDatabase().collection(arangoName).exists()){
            //It's a relation
        }
        else{
            //It's a leaf field
        }
        if(collection==null){
            return null;
        }
        return null;
    }


}
