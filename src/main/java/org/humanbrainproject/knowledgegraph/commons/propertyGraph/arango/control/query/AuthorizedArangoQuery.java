package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query;

import java.util.Set;

public class AuthorizedArangoQuery extends UnauthorizedArangoQuery {

    public final String WHITELIST_ALIAS = "whitelist";

    public AuthorizedArangoQuery(Set<String> permissionGroupsWithReadAccess){
        this(permissionGroupsWithReadAccess, false);
    }

    public AuthorizedArangoQuery(Set<String> permissionGroupsWithReadAccess, boolean subQuery) {
        if(!subQuery){
            addLine("LET "+WHITELIST_ALIAS+"=[${"+WHITELIST_ALIAS+"}]");
            setTrustedParameter(WHITELIST_ALIAS, listValues(',', permissionGroupsWithReadAccess));
        }
    }

    @Override
    public AuthorizedArangoQuery addDocumentFilter(String documentAlias) {
        super.addDocumentFilter(documentAlias);
        addLine("FILTER v._permissionGroup IN "+WHITELIST_ALIAS);
        return this;
    }
}
