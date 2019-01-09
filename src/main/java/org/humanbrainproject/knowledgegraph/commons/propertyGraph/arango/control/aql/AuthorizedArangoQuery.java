package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;

import java.util.Set;

@ToBeTested(easy = true)
public class AuthorizedArangoQuery extends UnauthorizedArangoQuery {

    public final String WHITELIST_ALIAS = "whitelist";

    public AuthorizedArangoQuery(Set<String> permissionGroupsWithReadAccess){
        this(permissionGroupsWithReadAccess, false);
    }

    public AuthorizedArangoQuery(Set<String> permissionGroupsWithReadAccess, boolean subQuery) {
        if(!subQuery){
            addLine("LET "+WHITELIST_ALIAS+"=[${"+WHITELIST_ALIAS+"}]");
            setTrustedParameter(WHITELIST_ALIAS, listValues(permissionGroupsWithReadAccess));
        }
    }

    @Override
    public AuthorizedArangoQuery addDocumentFilter(TrustedAqlValue documentAlias) {
        super.addDocumentFilter(documentAlias);
        addLine("FILTER "+documentAlias.getValue()+"._permissionGroup IN "+WHITELIST_ALIAS);
        return this;
    }
}
