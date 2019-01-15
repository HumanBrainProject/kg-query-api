package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.UnauthorizedArangoQuery;
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
        q.addLine("");
        q.addLine("//*****************************");
        q.addLine("//add traversal");
        q.addLine("//*****************************");
        q.addLine("");

        UnauthorizedArangoQuery subQuery = new UnauthorizedArangoQuery();
        subQuery.addLine("LET ${alias} = ");
        subQuery.indent();
        subQuery.addLine("( FOR ${aliasDoc} ");
        subQuery.addLine("IN 1..1 ${reverse} ${previousAliasDoc} `${relation}`");
        String alias = currentAlias.getArangoName() + "_";
        aliasStack.push(new ArrayList<>());
        aliasStack.peek().add(alias);
        subQuery.setParameter("alias", alias);
        subQuery.setParameter("aliasDoc", String.format("%s_%s", alias, DOC_POSTFIX));
        subQuery.setParameter("reverse", reverse ? "INBOUND" : "OUTBOUND");
        subQuery.setParameter("previousAliasDoc", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX));
        subQuery.setParameter("relation", relationCollection.getName());
        this.addAlias(new ArangoAlias(alias));
        q.addLine(subQuery.build().getValue());
    }


    private void simpleReturn(List<String> alias) {
        q.addLine("");
        q.addLine("//*****************************");
        q.addLine("//simple return");
        q.addLine("//*****************************");
        q.addLine("");
        addOrganizationFilter();
        q.addLine(createReleaseStatusQuery().build().getValue());
        UnauthorizedArangoQuery subQuery = new UnauthorizedArangoQuery();
        subQuery.addLine(isRoot() ? "RETURN {" : "RETURN DISTINCT {").indent();
        subQuery.addLine(" \"" + JsonLdConsts.ID + "\": ${alias}.`" + JsonLdConsts.ID + "`,");
        subQuery.addLine(" \"" + SchemaOrgVocabulary.NAME + "\": ${alias}.`" + SchemaOrgVocabulary.NAME + "`,");
        subQuery.addLine(" \"" + SchemaOrgVocabulary.IDENTIFIER + "\": ${alias}.`" + SchemaOrgVocabulary.IDENTIFIER + "`,");
        subQuery.addLine(" \"status\": ${alias}_status,");
        subQuery.addLine(" \"" + JsonLdConsts.TYPE + "\": ${alias}.`" + JsonLdConsts.TYPE + "`");
        if (alias != null && !alias.isEmpty()) {
            subQuery.addLine(", \"children\": UNION_DISTINCT([],[]");
            for (String s : alias) {
                UnauthorizedArangoQuery addAlias = new UnauthorizedArangoQuery();
                addAlias.addLine(", ${alias}");
                addAlias.setParameter("alias", s);
                subQuery.addLine(addAlias.build().getValue());
            }
            subQuery.addLine(")");
        }
        subQuery.outdent().addLine("}");
        subQuery.setParameter("alias", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX));
        q.addLine(subQuery.build().getValue());
    }


    @Override
    public void leaveAdditionalTraversal(boolean reverse, ArangoCollectionReference relationCollection, int traversalDepth, boolean leaf) {
        q.addLine("");
        q.addLine("//*****************************");
        q.addLine("//leave additional traversal");
        q.addLine("//*****************************");
        q.addLine("");
        if(leaf) {
            simpleReturn(null);
        }
        else{
            simpleReturn(aliasStack.pop());
        }
        q.addLine(")");
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
        UnauthorizedArangoQuery subQuery = new UnauthorizedArangoQuery();
        q.addLine("");
        q.addLine("//*****************************");
        q.addLine("//enter traversal");
        q.addLine("//*****************************");
        q.addLine("");
        subQuery.addLine("LET ${alias} = ");
        subQuery.indent();
        subQuery.addLine("( FOR ${aliasDoc} ");
        subQuery.addLine("IN 1..1 ${reverse} ${previousAliasDoc} `${relation}`");
        subQuery.setParameter("alias", currentAlias.getArangoName());
        subQuery.setParameter("aliasDoc", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX));
        subQuery.setParameter("reverse", reverse ? "INBOUND" : "OUTBOUND");
        subQuery.setParameter("previousAliasDoc", String.format("%s_%s", previousAlias.peek().getArangoName(), DOC_POSTFIX));
        subQuery.setParameter("relation", relationCollection.getName());
        q.addLine(subQuery.build().getValue());

    }

    @Override
    public void nullFilter() {
    }

    @Override
    protected void doStartReturnStructure(boolean simple) {
        hasReturned = true;
        firstReturnEntry = true;
        q.addLine("");
        q.addLine("//*****************************");
        q.addLine("//start return structure");
        q.addLine("//*****************************");
        q.addLine("");

        q.addLine(createReleaseStatusQuery().build().getValue());
        q.addLine(isRoot() ? "RETURN {" : "RETURN DISTINCT {");
        UnauthorizedArangoQuery subQuery = new UnauthorizedArangoQuery();
        subQuery.addLine(" \"" + JsonLdConsts.ID + "\": ${alias}.`" + JsonLdConsts.ID + "`,");
        subQuery.addLine(" \"" + SchemaOrgVocabulary.NAME + "\": ${alias}.`" + SchemaOrgVocabulary.NAME + "`,");
        subQuery.addLine(" \"" + SchemaOrgVocabulary.IDENTIFIER + "\": ${alias}.`" + SchemaOrgVocabulary.IDENTIFIER + "`,");
        subQuery.addLine(" \"" + JsonLdConsts.TYPE + "\": ${alias}.`" + JsonLdConsts.TYPE + "`,");
        subQuery.addLine(" \"status\": ${alias}_status,");
        subQuery.addLine(" \"children\": UNION_DISTINCT([],[]");
        subQuery.setParameter("alias", String.format("%s_%s", isRoot() ? ROOT_ALIAS.getArangoName() : currentAlias.getArangoName(), DOC_POSTFIX));
        q.addLine(subQuery.build().getValue());
        q.indent();
    }

    private UnauthorizedArangoQuery createReleaseStatusQuery() {
        UnauthorizedArangoQuery releaseStatusQuery = new UnauthorizedArangoQuery();
        releaseStatusQuery.addLine("LET ${name}_release = (FOR ${name}_status_doc IN 1..1 INBOUND ${name}_doc `${releaseInstanceRelation}`");
        releaseStatusQuery.addLine("LET ${name}_release_instance = SUBSTITUTE(CONCAT(${name}_status_doc.`${releaseInstanceProperty}`.`" + JsonLdConsts.ID + "`, \"?rev=\", ${name}_status_doc.`${releaseRevisionProperty}`), \"${nexusBaseForInstances}/\", \"\")");
        releaseStatusQuery.addLine("RETURN ${name}_release_instance==${name}_doc.${originalId} ? \"${releasedValue}\" : \"${changedValue}\"");
        releaseStatusQuery.addLine(")");
        releaseStatusQuery.addLine("LET ${name}_doc_status = LENGTH(${name}_release)>0 ? ${name}_release[0] : \"${notReleasedValue}\"");
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
        q.addLine("");
        q.addLine("//*****************************");
        q.addLine("//end return structure");
        q.addLine("//*****************************");
        q.addLine("");
        q.addLine(")}");
        firstReturnEntry = true;
    }

    @Override
    protected void doLeaveTraversal() {
        q.addLine("");
        q.addLine("//*****************************");
        q.addLine("//leave traversal");
        q.addLine("//*****************************");
        if(!hasReturned){
            simpleReturn(aliasStack.empty() ? null : aliasStack.pop());
        }
        hasReturned = false;
        aliasStack.clear();
        q.addLine(")");
    }

    @Override
    public void buildGrouping(String groupedInstancesLabel, List<ArangoAlias> groupingFields, List<ArangoAlias> nonGroupingFields) {
    }


    @Override
    public ArangoReflectionQueryBuilder addRoot(ArangoCollectionReference rootCollection) {
        q.addLine("");
        q.addLine("//*****************************");
        q.addLine("//add root");
        q.addLine("//*****************************");
        q.addLine("");
        q.addLine(new UnauthorizedArangoQuery().addLine("FOR ${alias} IN `${collection}`").setParameter("alias", String.format("%s_%s", ROOT_ALIAS.getArangoName(), DOC_POSTFIX)).setParameter("collection", rootCollection.getName()).build().getValue());
        addOrganizationFilter();
        return this;
    }

    @Override
    public void addLimit() {
    }

    @Override
    public void addTraversalResultField(String targetName, ArangoAlias alias) {
        q.addLine("");
        q.addLine("//*****************************");
        q.addLine("//add traversal result field");
        q.addLine("//*****************************");
        q.addLine("");
        q.addLine(new UnauthorizedArangoQuery().addLine(", ${alias}").setParameter("alias", alias.getArangoName()).build().getValue());
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

        q.addLine("");
        q.addLine("//*****************************");
        q.addLine("//add merge");
        q.addLine("//*****************************");
        q.addLine("");
        UnauthorizedArangoQuery subQuery = new UnauthorizedArangoQuery();
        subQuery.addLine("LET ${targetName} = UNION_DISTINCT([]");
        for (ArangoAlias mergedField : mergedFields) {
            subQuery.addLine(new UnauthorizedArangoQuery().addLine(", ${merged}").setParameter("merged", mergedField.getArangoName()).build().getValue());
        }
        subQuery.addLine(")");
        subQuery.setParameter("targetName", leafField.getArangoName());
        q.addLine(subQuery.build().getValue());
    }

    @Override
    public void addInstanceIdFilter() {
        if (this.documentReferences != null && !this.documentReferences.isEmpty()) {
            if (this.documentReferences.size() == 1) {
                q.addLine(
                        new UnauthorizedArangoQuery().addLine("FILTER ${field}._id == \"${id}\"").
                                setParameter("field", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX)).
                                setParameter("id", this.documentReferences.iterator().next().getId()).build().getValue());
            }
            else{
                q.addLine(
                        new UnauthorizedArangoQuery().addLine("FILTER ${field}._id IN [${id}]").
                                setParameter("field", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX)).
                                setTrustedParameter("id", q.listValues(this.documentReferences.stream().map(ArangoDocumentReference::getId).collect(Collectors.toSet()))).build().getValue());

            }
        }
    }

    @Override
    public void addSearchQuery() {
    }

}
