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

public class SpecificationBasedScopeBuilder {

    private final Specification specification;
    private final ArangoDocumentReference instanceId;
    private final AuthorizedArangoQuery q;
    protected final String nexusInstanceBase;
    private TreeScope scope;

    private final Set<ArangoCollectionReference> existingCollections;

    private int internalFieldCounter = 0;
    private final ArangoAlias rootAlias = new ArangoAlias("root");

    public SpecificationBasedScopeBuilder(Specification specification, Set<String> permissionGroupsWithReadAccess, ArangoDocumentReference instanceId, Set<ArangoCollectionReference> existingCollections, String nexusInstanceBase, TreeScope scope) {
        this.q = new AuthorizedArangoQuery(permissionGroupsWithReadAccess);
        this.specification = specification;
        this.instanceId = instanceId;
        this.existingCollections = existingCollections;
        this.nexusInstanceBase = nexusInstanceBase;
        if(scope == null){
            scope = TreeScope.ALL;
        }
        this.scope = scope;
    }

    public String build() {
        q.setParameter("rootFieldName", rootAlias.getArangoName());
        q.setParameter("rootDoc", rootAlias.getArangoDocName());
        q.setParameter("collection", getRootCollection());
        q.setParameter("instanceId", instanceId.getKey());
        q.addLine(trust(""));
        q.addLine(trust("//*****************************"));
        q.addLine(trust("//add root"));
        q.addLine(trust("//*****************************"));
        q.addLine(trust(""));
        q.addLine(trust("FOR ${rootDoc} IN `${collection}`")).indent();
        q.addDocumentFilter(rootAlias);
        q.addLine(trust("FILTER ${rootDoc}._id == \"${id}\""));
        queryExtraFieldsForAlias(q, rootAlias);
        List<ArangoAlias> fieldAliases = null;
        if(!scope.name().equals(TreeScope.TOP_INSTANCE_ONLY.name())){
            fieldAliases = processFields(rootAlias, specification.getFields());
        }
        q.addLine(trust("RETURN {"));
        createReturnObject(fieldAliases);
        q.addLine(trust("}"));
        return q.build().getValue();
    }

    private void createReturnObject(List<ArangoAlias> fieldAliases){
        q.setParameter("id", instanceId.getId());
        if(scope.name().equals(TreeScope.CHILDREN_ONLY.name())){
            handleReturnStructureOfSubfields(q, fieldAliases, true);
        }else{
            if(scope.name().equals(TreeScope.ALL.name())){
                handleReturnStructureOfSubfields(q, fieldAliases, false);
            }
            handleReturnOfExtraFields(q, trust("rootDoc"), false);
            q.addLine(trust(" \"" + JsonLdConsts.ID + "\": ${rootDoc}.`" + JsonLdConsts.ID + "`"));
        }
    }


    private List<ArangoAlias> processFields(ArangoAlias originalAlias, List<SpecField> fields) {
        List<ArangoAlias> result = new ArrayList<>();
        for (SpecField field : fields) {
            if(field.isMerge()){
                for (SpecField specField : field.fields) {
                    doProcessField(originalAlias, result, specField);
                }
            }
            else {
                doProcessField(originalAlias, result, field);
            }
        }
        return result;
    }

    private void doProcessField(ArangoAlias originalAlias, List<ArangoAlias> result, SpecField field) {
        if (field.needsTraversal()) {
            Stack<ArangoAlias> aliasStack = new Stack<>();
            Stack<SpecTraverse> traverseStack = new Stack<>();
            for (SpecTraverse traverse : field.traversePath) {
                ArangoCollectionReference collection = ArangoCollectionReference.fromSpecTraversal(traverse);
                if(existingCollections.contains(collection)) {
                    ArangoAlias alias = new ArangoAlias(ArangoAlias.fromLeafPath(traverse).getArangoName() + "_" + internalFieldCounter);
                    AQL subQuery = new AQL();
                    subQuery.addLine(trust("//Adding ${alias}"));
                    subQuery.addLine(trust("LET ${alias} = ("));
                    ArangoAlias linkingInstanceAlias = new ArangoAlias(alias.getArangoName() + "_lnk");
                    if (traverse.isLinkingInstance()) {
                        subQuery.addLine(trust("FOR ${aliasDoc}, ${aliasLnkDoc} IN 1..1 ${inOutBound} ${previousDoc} `${relation}`"));
                        queryExtraFieldsForAlias(subQuery, linkingInstanceAlias);
                        subQuery.setParameter("aliasLnkDoc", linkingInstanceAlias.getArangoDocName());
                    } else {
                        subQuery.addLine(trust("FOR ${aliasDoc} IN 1..1 ${inOutBound} ${previousDoc} `${relation}`"));
                    }
                    queryExtraFieldsForAlias(subQuery, alias);
                    subQuery.setParameter("alias", alias.getArangoName());
                    subQuery.setParameter("aliasDoc", alias.getArangoDocName());
                    subQuery.setParameter("inOutBound", traverse.reverse ? "INBOUND" : "OUTBOUND");
                    subQuery.setParameter("previousDoc", aliasStack.empty() ? originalAlias.getArangoDocName() : aliasStack.peek().getArangoDocName());
                    subQuery.setParameter("relation", ArangoCollectionReference.fromSpecTraversal(traverse).getName());
                    subQuery.addDocumentFilter(alias);
                    if (traverse.isLinkingInstance()) {
                        aliasStack.push(linkingInstanceAlias);
                    }
                    aliasStack.push(alias);
                    traverseStack.push(traverse);
                    q.addLine(subQuery.build());
                }
                else{
                    break;
                }
            }
            ArangoAlias previousAlias = null;
            while(!traverseStack.empty()){
                SpecTraverse traverse = traverseStack.pop();
                ArangoAlias alias = aliasStack.pop();
                AQL subQuery = new AQL();
                subQuery.addLine(trust("RETURN DISTINCT {"));
                ArangoAlias lnkAlias;
                if (traverse.isLinkingInstance()) {
                    lnkAlias = aliasStack.pop();
                    handleReturnOfExtraFields(subQuery, trust("aliasLnkDoc"), true);
                    subQuery.addLine(trust(" \"" + JsonLdConsts.ID + "\": ${aliasLnkDoc}.`" + JsonLdConsts.ID + "`,"));
                    subQuery.addLine(trust(" \"children\": [{"));
                    subQuery.setParameter("aliasLnkDoc", lnkAlias.getArangoDocName());
                }
                if(previousAlias!=null){
                    subQuery.addLine(trust(" \"children\":${previousAlias},"));
                    subQuery.setParameter("previousAlias", previousAlias.getArangoName());
                }
                else if(field.hasSubFields()){
                    List<ArangoAlias> fieldAliases = processFields(alias, field.fields);
                    handleReturnStructureOfSubfields(subQuery, fieldAliases, false);
                }
                handleReturnOfExtraFields(subQuery, trust("aliasDoc"), false);
                subQuery.addLine(trust(" \"" + JsonLdConsts.ID + "\": ${aliasDoc}.`" + JsonLdConsts.ID + "`"));
                subQuery.setParameter("aliasDoc", alias.getArangoDocName());
                if (traverse.isLinkingInstance()) {
                    subQuery.addLine(trust("}]"));
                }
                subQuery.addLine(trust("})"));
                q.addLine(subQuery.build());
                previousAlias = alias;
            }
            internalFieldCounter++;
            if(previousAlias!=null) {
                result.add(previousAlias);
            }
        }
    }

    protected void queryExtraFieldsForAlias(AQL query, ArangoAlias alias){
    }

    protected void handleReturnOfExtraFields(AQL query, TrustedAqlValue alias, boolean isLinkingInstance){
    }


    private void handleReturnStructureOfSubfields(AQL query, List<ArangoAlias> fieldAliases, boolean isOnlyElement) {
        if(fieldAliases!=null && !fieldAliases.isEmpty()){
            query.addLine(trust(" \"children\": UNION_DISTINCT("));
            for (ArangoAlias fieldAlias : fieldAliases) {
                query.addLine(new AQL().add(trust("${a},")).setParameter("a", fieldAlias.getArangoName()).build());
            }
            query.addLine(trust("[])"));
            if(!isOnlyElement){
                query.addLine(trust(","));
            }
        }
    }


    private String getRootCollection() {
        return ArangoCollectionReference.fromNexusSchemaReference(NexusSchemaReference.createFromUrl(specification.getRootSchema())).getName();
    }


}
