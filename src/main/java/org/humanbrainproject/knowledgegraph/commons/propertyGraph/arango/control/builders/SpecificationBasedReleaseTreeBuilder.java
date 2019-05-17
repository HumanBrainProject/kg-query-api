package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AuthorizedArangoQuery;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.TrustedAqlValue;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.query.entity.SpecField;
import org.humanbrainproject.knowledgegraph.query.entity.SpecTraverse;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import static org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL.*;

public class SpecificationBasedReleaseTreeBuilder extends SpecificationBasedScopeBuilder {

    public SpecificationBasedReleaseTreeBuilder(Specification specification, Set<String> permissionGroupsWithReadAccess, ArangoDocumentReference instanceId, Set<ArangoCollectionReference> existingCollections, String nexusInstanceBase, TreeScope scope) {
        super(specification, permissionGroupsWithReadAccess, instanceId, existingCollections, nexusInstanceBase, scope);
    }

    @Override
    protected void queryExtraFieldsForAlias(AQL query, ArangoAlias alias){
        query.addLine(ReleaseStatusQuery.createReleaseStatusQuery(alias, nexusInstanceBase).build());
    }

    @Override
    protected void handleReturnOfExtraFields(AQL query, TrustedAqlValue alias, boolean linkingInstance){
        query.addLine(trust(" \"" + JsonLdConsts.TYPE + "\": ${"+alias.getValue()+"}.`" + JsonLdConsts.TYPE + "`,"));
        query.addLine(trust(" \"status\": ${"+alias.getValue()+"}_status,"));
        if(linkingInstance){
            query.addLine(trust(" \"" + SchemaOrgVocabulary.NAME + "\": \"Linking instance\","));
        }
        else {
            query.addLine(trust(" \"" + SchemaOrgVocabulary.NAME + "\": ${"+alias.getValue()+"}.`" + SchemaOrgVocabulary.NAME + "`,"));
        }
        query.addLine(trust(" \"" + SchemaOrgVocabulary.IDENTIFIER + "\": ${"+alias.getValue()+"}.`" + SchemaOrgVocabulary.IDENTIFIER + "`,"));
    }

}
