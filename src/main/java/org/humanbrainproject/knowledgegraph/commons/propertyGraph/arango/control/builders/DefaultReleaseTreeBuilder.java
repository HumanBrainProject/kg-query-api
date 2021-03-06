/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AuthorizedArangoQuery;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;

import java.util.Set;

import static org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL.*;

public class DefaultReleaseTreeBuilder {

    private final ArangoDocumentReference instanceId;
    private final AuthorizedArangoQuery q;
    private final String nexusInstanceBase;
    private TreeScope scope;


    private final ArangoAlias rootAlias = new ArangoAlias("root");

    public DefaultReleaseTreeBuilder(Set<String> permissionGroupsWithReadAccess, ArangoDocumentReference instanceId, String nexusInstanceBase) {
        this.q = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
        this.instanceId = instanceId;
        this.nexusInstanceBase = nexusInstanceBase;
        if(scope == null){
            scope = TreeScope.ALL;
        }
        this.scope = scope;
    }

    public String build() {
        q.setParameter("rootFieldName", rootAlias.getArangoName());
        q.setParameter("rootDoc", rootAlias.getArangoDocName());
        q.setParameter("collection", instanceId.getCollection().getName());
        q.setParameter("instanceId", instanceId.getKey());
        q.addLine(trust(""));
        q.addLine(trust("//*****************************"));
        q.addLine(trust("//add root"));
        q.addLine(trust("//*****************************"));
        q.addLine(trust(""));
        q.addLine(trust("FOR ${rootDoc} IN `${collection}`")).indent();
        q.addDocumentFilter(rootAlias);
        q.addLine(trust("FILTER ${rootDoc}._id == \"${id}\""));
        q.addLine(ReleaseStatusQuery.createReleaseStatusQuery(rootAlias, nexusInstanceBase).build());
        q.addLine(trust("RETURN {"));
        q.setParameter("id", instanceId.getId());
        q.addLine(trust(" \"" + JsonLdConsts.ID + "\": ${rootDoc}.`" + JsonLdConsts.ID + "`,"));
        q.addLine(trust(" \"" + SchemaOrgVocabulary.NAME + "\": ${rootDoc}.`" + SchemaOrgVocabulary.NAME + "`,"));
        q.addLine(trust(" \"" + SchemaOrgVocabulary.IDENTIFIER + "\": ${rootDoc}.`" + SchemaOrgVocabulary.IDENTIFIER + "`,"));
        q.addLine(trust(" \"status\": ${rootDoc}_status,"));
        q.addLine(trust(" \"" + JsonLdConsts.TYPE + "\": ${rootDoc}.`" + JsonLdConsts.TYPE + "`"));
        q.addLine(trust("}"));
        return q.build().getValue();
    }

}
