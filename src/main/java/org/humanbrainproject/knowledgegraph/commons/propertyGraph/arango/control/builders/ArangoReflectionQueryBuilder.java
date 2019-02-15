package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.query.entity.SpecField;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;


import static org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL.*;

@ToBeTested
public class ArangoReflectionQueryBuilder extends AbstractArangoQueryBuilder {

    private final String nexusInstanceBase;

    private Stack<List<String>> aliasStack = new Stack<>();

    private boolean hasReturned;

    public ArangoReflectionQueryBuilder(Specification specification, ArangoAlias permissionGroupFieldName, Set<String> whitelistOrganizations, Set<ArangoDocumentReference> documentReferences, Set<ArangoCollectionReference> existingArangoCollections, String nexusInstanceBase) {
        super(specification, null, null, permissionGroupFieldName, whitelistOrganizations, documentReferences, existingArangoCollections);
        this.nexusInstanceBase = nexusInstanceBase;
    }

    @Override
    public void prepareLeafField(SpecField leafField) {

    }

    @Override
    public void addTraversal(boolean reverse, ArangoCollectionReference relationCollection, int traversalDepth) {
        q.addLine(trust(""));
        q.addLine(trust("//*****************************"));
        q.addLine(trust("//add traversal"));
        q.addLine(trust("//*****************************"));
        q.addLine(trust(""));

        AQL subQuery = new AQL();
        subQuery.addLine(trust("LET ${alias} = "));
        subQuery.indent();
        subQuery.addLine(trust("( FOR ${aliasDoc} "));
        subQuery.addLine(trust("IN 1..1 ${reverse} ${previousAliasDoc} `${relation}`"));
        String alias = currentAlias.getArangoName() + "_";
        aliasStack.push(new ArrayList<>());
        aliasStack.peek().add(alias);
        subQuery.setParameter("alias", alias);
        subQuery.setParameter("aliasDoc", String.format("%s_%s", alias, DOC_POSTFIX));
        subQuery.setParameter("reverse", reverse ? "INBOUND" : "OUTBOUND");
        subQuery.setParameter("previousAliasDoc", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX));
        subQuery.setParameter("relation", relationCollection.getName());
        this.addAlias(new ArangoAlias(alias));
        q.addLine(subQuery.build());
    }


    private void simpleReturn(List<String> alias) {
        q.addLine(trust(""));
        q.addLine(trust("//*****************************"));
        q.addLine(trust("//simple return"));
        q.addLine(trust("//*****************************"));
        q.addLine(trust(""));
        addOrganizationFilter();
        q.addLine(trust(createReleaseStatusQuery().build().getValue()));
        AQL subQuery = new AQL();
        subQuery.addLine(trust(isRoot() ? "RETURN {" : "RETURN DISTINCT {")).indent();
        subQuery.addLine(trust(" \"" + JsonLdConsts.ID + "\": ${alias}.`" + JsonLdConsts.ID + "`,"));
        subQuery.addLine(trust(" \"" + SchemaOrgVocabulary.NAME + "\": ${alias}.`" + SchemaOrgVocabulary.NAME + "`,"));
        subQuery.addLine(trust(" \"" + SchemaOrgVocabulary.IDENTIFIER + "\": ${alias}.`" + SchemaOrgVocabulary.IDENTIFIER + "`,"));
        subQuery.addLine(trust(" \"status\": ${alias}_status,"));
        subQuery.addLine(trust(" \"" + JsonLdConsts.TYPE + "\": ${alias}.`" + JsonLdConsts.TYPE + "`"));
        if (alias != null && !alias.isEmpty()) {
            subQuery.addLine(trust(", \"children\": UNION_DISTINCT([],[]"));
            for (String s : alias) {
                AQL addAlias = new AQL();
                addAlias.addLine(trust(", ${alias}"));
                addAlias.setParameter("alias", s);
                subQuery.addLine(trust(addAlias.build().getValue()));
            }
            subQuery.addLine(trust(")"));
        }
        subQuery.outdent().addLine(trust("}"));
        subQuery.setParameter("alias", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX));
        q.addLine(subQuery.build());
    }


    @Override
    public void leaveAdditionalTraversal(boolean reverse, ArangoCollectionReference relationCollection, int traversalDepth, boolean leaf) {
        q.addLine(trust(""));
        q.addLine(trust("//*****************************"));
        q.addLine(trust("//leave additional traversal"));
        q.addLine(trust("//*****************************"));
        q.addLine(trust(""));
        if(leaf) {
            simpleReturn(null);
        }
        else{
            simpleReturn(aliasStack.pop());
        }
        q.addLine(trust(")"));
        this.dropAlias();
    }

    @Override
    public void addComplexFieldRequiredFilter(ArangoAlias leafField) {
    }

    @Override
    public void addTraversalFieldRequiredFilter(ArangoAlias alias) {
    }

    @Override
    protected void doEnterTraversal(ArangoAlias targetName, int numberOfTraversals, boolean reverse, ArangoCollectionReference relationCollection, boolean hasGroup, boolean ensureOrder) {
        hasReturned = false;
        AQL subQuery = new AQL();
        q.addLine(trust(""));
        q.addLine(trust("//*****************************"));
        q.addLine(trust("//enter traversal"));
        q.addLine(trust("//*****************************"));
        q.addLine(trust(""));
        subQuery.addLine(trust("LET ${alias} = "));
        subQuery.indent();
        subQuery.addLine(trust("( FOR ${aliasDoc} "));
        subQuery.addLine(trust("IN 1..1 ${reverse} ${previousAliasDoc} `${relation}`"));
        subQuery.setParameter("alias", currentAlias.getArangoName());
        subQuery.setParameter("aliasDoc", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX));
        subQuery.setParameter("reverse", reverse ? "INBOUND" : "OUTBOUND");
        subQuery.setParameter("previousAliasDoc", String.format("%s_%s", previousAlias.peek().getArangoName(), DOC_POSTFIX));
        subQuery.setParameter("relation", relationCollection.getName());
        q.addLine(trust(subQuery.build().getValue()));

    }

    @Override
    public void nullFilter() {
    }

    @Override
    protected void doStartReturnStructure(boolean simple) {
        hasReturned = true;
        firstReturnEntry = true;
        q.addLine(trust(""));
        q.addLine(trust("//*****************************"));
        q.addLine(trust("//start return structure"));
        q.addLine(trust("//*****************************"));
        q.addLine(trust(""));

        q.addLine(trust(createReleaseStatusQuery().build().getValue()));
        q.addLine(trust(isRoot() ? "RETURN {" : "RETURN DISTINCT {"));
        AQL subQuery = new AQL();
        subQuery.addLine(trust(" \"" + JsonLdConsts.ID + "\": ${alias}.`" + JsonLdConsts.ID + "`,"));
        subQuery.addLine(trust(" \"" + SchemaOrgVocabulary.NAME + "\": ${alias}.`" + SchemaOrgVocabulary.NAME + "`,"));
        subQuery.addLine(trust(" \"" + SchemaOrgVocabulary.IDENTIFIER + "\": ${alias}.`" + SchemaOrgVocabulary.IDENTIFIER + "`,"));
        subQuery.addLine(trust(" \"" + JsonLdConsts.TYPE + "\": ${alias}.`" + JsonLdConsts.TYPE + "`,"));
        subQuery.addLine(trust(" \"status\": ${alias}_status,"));
        subQuery.addLine(trust(" \"children\": UNION_DISTINCT([],[]"));
        subQuery.setParameter("alias", String.format("%s_%s", isRoot() ? ROOT_ALIAS.getArangoName() : currentAlias.getArangoName(), DOC_POSTFIX));
        q.addLine(subQuery.build());
        q.indent();
    }

    private AQL createReleaseStatusQuery() {
        AQL releaseStatusQuery = new AQL();
        releaseStatusQuery.addLine(trust("LET ${name}_release = (FOR ${name}_status_doc IN 1..1 INBOUND ${name}_doc `${releaseInstanceRelation}`"));
        releaseStatusQuery.addLine(trust("LET ${name}_release_instance = SUBSTITUTE(CONCAT(${name}_status_doc.`${releaseInstanceProperty}`.`" + JsonLdConsts.ID + "`, \"?rev=\", ${name}_status_doc.`${releaseRevisionProperty}`), \"${nexusBaseForInstances}/\", \"\")"));
        releaseStatusQuery.addLine(trust("RETURN ${name}_release_instance==${name}_doc.${originalId} ? \"${releasedValue}\" : \"${changedValue}\""));
        releaseStatusQuery.addLine(trust(")"));
        releaseStatusQuery.addLine(trust("LET ${name}_doc_status = LENGTH(${name}_release)>0 ? ${name}_release[0] : \"${notReleasedValue}\""));
        releaseStatusQuery.setParameter("name", currentAlias.getArangoName());
        releaseStatusQuery.setParameter("releaseInstanceRelation", ArangoCollectionReference.fromFieldName(HBPVocabulary.RELEASE_INSTANCE).getName());
        releaseStatusQuery.setParameter("releaseInstanceProperty", HBPVocabulary.RELEASE_INSTANCE);
        releaseStatusQuery.setParameter("releaseRevisionProperty", HBPVocabulary.RELEASE_REVISION);
        releaseStatusQuery.setParameter("nexusBaseForInstances", nexusInstanceBase);
        releaseStatusQuery.setParameter("originalId", ArangoVocabulary.NEXUS_RELATIVE_URL_WITH_REV);
        releaseStatusQuery.setParameter("releasedValue", ReleaseStatus.RELEASED.name());
        releaseStatusQuery.setParameter("changedValue", ReleaseStatus.HAS_CHANGED.name());
        releaseStatusQuery.setParameter("notReleasedValue", ReleaseStatus.NOT_RELEASED.name());
        return releaseStatusQuery;
    }

    @Override
    public void endReturnStructure() {
        q.addLine(trust(""));
        q.addLine(trust("//*****************************"));
        q.addLine(trust("//end return structure"));
        q.addLine(trust("//*****************************"));
        q.addLine(trust(""));
        q.addLine(trust(")}"));
        firstReturnEntry = true;
    }

    @Override
    protected void doLeaveTraversal() {
        q.addLine(trust(""));
        q.addLine(trust("//*****************************"));
        q.addLine(trust("//leave traversal"));
        q.addLine(trust("//*****************************"));
        if(!hasReturned){
            simpleReturn(aliasStack.empty() ? null : aliasStack.pop());
        }
        hasReturned = false;
        aliasStack.clear();
        q.addLine(trust(")"));
    }

    @Override
    public void buildGrouping(String groupedInstancesLabel, List<ArangoAlias> groupingFields, List<ArangoAlias> nonGroupingFields) {
    }


    @Override
    public ArangoReflectionQueryBuilder addRoot(ArangoCollectionReference rootCollection) {
        q.addLine(trust(""));
        q.addLine(trust("//*****************************"));
        q.addLine(trust("//add root"));
        q.addLine(trust("//*****************************"));
        q.addLine(trust(""));
        q.addLine(new AQL().addLine(trust("FOR ${alias} IN `${collection}`")).setParameter("alias", String.format("%s_%s", ROOT_ALIAS.getArangoName(), DOC_POSTFIX)).setParameter("collection", rootCollection.getName()).build());
        addOrganizationFilter();
        return this;
    }

    @Override
    public void addLimit() {
    }

    @Override
    public void addTraversalResultField(String targetName, ArangoAlias alias) {
        q.addLine(trust(""));
        q.addLine(trust("//*****************************"));
        q.addLine(trust("//add traversal result field"));
        q.addLine(trust("//*****************************"));
        q.addLine(trust(""));
        q.addLine(new AQL().addLine(trust(", ${alias}")).setParameter("alias", alias.getArangoName()).build());
        firstReturnEntry = false;
    }

    @Override
    public void addSortByLeafField(Set<ArangoAlias> fields) {
    }

    @Override
    public void ensureOrder() {
    }

    @Override
    public void addComplexLeafResultField(String targetName, ArangoAlias leafField) {

    }

    @Override
    public void addSimpleLeafResultField(ArangoAlias leafField) {

    }

    @Override
    public void addMerge(ArangoAlias leafField, Set<ArangoAlias> mergedFields, boolean sorted) {

        q.addLine(trust(""));
        q.addLine(trust("//*****************************"));
        q.addLine(trust("//add merge"));
        q.addLine(trust("//*****************************"));
        q.addLine(trust(""));
        AQL subQuery = new AQL();
        subQuery.addLine(trust("LET ${targetName} = UNION_DISTINCT([]"));
        for (ArangoAlias mergedField : mergedFields) {
            subQuery.addLine(new AQL().addLine(trust(", ${merged}")).setParameter("merged", mergedField.getArangoName()).build());
        }
        subQuery.addLine(trust(")"));
        subQuery.setParameter("targetName", leafField.getArangoName());
        q.addLine(subQuery.build());
    }

    @Override
    public void addInstanceIdFilter() {
        if (this.documentReferences != null && !this.documentReferences.isEmpty()) {
            if (this.documentReferences.size() == 1) {
                q.addLine(
                        new AQL().addLine(trust("FILTER ${field}._id == \"${id}\"")).
                                setParameter("field", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX)).
                                setParameter("id", this.documentReferences.iterator().next().getId()).build());
            }
            else{
                q.addLine(
                        new AQL().addLine(trust("FILTER ${field}._id IN [${id}]")).
                                setParameter("field", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX)).
                                setTrustedParameter("id", q.listValues(this.documentReferences.stream().map(ArangoDocumentReference::getId).collect(Collectors.toSet()))).build());

            }
        }
    }

    @Override
    public void addSearchQuery() {
    }

    @Override
    public void addFieldFilter(ArangoAlias alias) {
    }

}
