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
