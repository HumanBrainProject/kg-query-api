/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;

import java.util.Set;

@ToBeTested(easy = true)
public class AuthorizedArangoQuery extends AQL {


    public AuthorizedArangoQuery(Set<String> permissionGroupsWithReadAccess) {
        this(permissionGroupsWithReadAccess, null, false);
    }

    public AuthorizedArangoQuery(Set<String> permissionGroupsWithReadAccess, Set<String> invitations) {
        this(permissionGroupsWithReadAccess, invitations, false);
    }

    public AuthorizedArangoQuery(Set<String> permissionGroupsWithReadAccess, boolean subQuery) {
        this(permissionGroupsWithReadAccess, null, subQuery);
    }

    public AuthorizedArangoQuery(Set<String> permissionGroupsWithReadAccess, Set<String> invitations, boolean subQuery) {
        if (!subQuery) {
            addLine(trust("LET " + WHITELIST_ALIAS + "=[${" + WHITELIST_ALIAS + "}]"));
            addLine(trust("LET " + INVITATION_ALIAS + "=[${" + INVITATION_ALIAS + "}]"));
            setTrustedParameter(WHITELIST_ALIAS, listValues(permissionGroupsWithReadAccess));
            setTrustedParameter(INVITATION_ALIAS, listValues(invitations));
        }
    }

    @Override
    public AuthorizedArangoQuery addDocumentFilter(TrustedAqlValue documentAlias) {
        super.addDocumentFilter(documentAlias);
        addLine(trust("FILTER " + documentAlias.getValue() + "." + ArangoVocabulary.PERMISSION_GROUP + " IN " + WHITELIST_ALIAS + " OR " + documentAlias.getValue() + ".`" + JsonLdConsts.ID + "` IN " + INVITATION_ALIAS));
        return this;
    }
}
