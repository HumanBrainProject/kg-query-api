package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;

import java.util.Set;

@ToBeTested(easy = true)
public class AuthorizedArangoQuery extends AQL {


    public AuthorizedArangoQuery(Set<String> permissionGroupsWithReadAccess){
        this(permissionGroupsWithReadAccess, false);
    }

    public AuthorizedArangoQuery(Set<String> permissionGroupsWithReadAccess, boolean subQuery) {
        if(!subQuery){
            addLine(trust("LET "+WHITELIST_ALIAS+"=[${"+WHITELIST_ALIAS+"}]"));
            setTrustedParameter(WHITELIST_ALIAS, listValues(permissionGroupsWithReadAccess));
        }
    }

    @Override
    public AuthorizedArangoQuery addDocumentFilter(TrustedAqlValue documentAlias) {
        super.addDocumentFilter(documentAlias);
        addLine(trust("FILTER "+documentAlias.getValue()+"."+ ArangoVocabulary.PERMISSION_GROUP+" IN "+WHITELIST_ALIAS));
        return this;
    }
}
