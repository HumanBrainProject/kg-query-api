package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.humanbrainproject.knowledgegraph.releasing.entity.ReleaseStatus;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ArangoReflectionQueryBuilder extends AbstractArangoQueryBuilder {

    private final String nexusInstanceBase;

    public ArangoReflectionQueryBuilder(Specification specification, ArangoAlias permissionGroupFieldName, Set<String> whitelistOrganizations, ArangoDocumentReference documentReference, Set<ArangoCollectionReference> existingArangoCollections, String nexusInstanceBase) {
        super(specification, null, null, permissionGroupFieldName, whitelistOrganizations, documentReference, existingArangoCollections);
        this.nexusInstanceBase = nexusInstanceBase;
    }

    @Override
    public void addTraversal(boolean reverse, ArangoCollectionReference relationCollection) {
        UnauthorizedArangoQuery subQuery = new UnauthorizedArangoQuery();
        subQuery.setParameter("reverse", reverse ? "INBOUND" : "OUTBOUND");
        subQuery.setParameter("collection", relationCollection.getName());
        subQuery.addLine(", ${reverse} `${collection}`");
        q.addLine(subQuery.build().getValue());
    }

    @Override
    public void addComplexFieldRequiredFilter(ArangoAlias leafField) {
    }

    @Override
    public void addTraversalFieldRequiredFilter(ArangoAlias alias) {
    }

    @Override
    protected void doEnterTraversal(ArangoAlias targetName, int numberOfTraversals, boolean reverse, ArangoCollectionReference relationCollection, boolean hasGroup, boolean ensureOrder) {
        UnauthorizedArangoQuery subQuery = new UnauthorizedArangoQuery();
        subQuery.addLine("LET ${alias} = ");
        if (hasGroup) {
            subQuery.indent();
            subQuery.addLine("( FOR grp IN ");
        }
        subQuery.indent();
        subQuery.addLine("( FOR ${aliasDoc} ");
        if (ensureOrder) {
            subQuery.indent();
            subQuery.addLine(", e ");
            subQuery.outdent();
        }
        subQuery.addLine("IN ${numberOfTraversals}..${numberOfTraversals} ${reverse} ${previousAliasDoc} `${relation}`");
        subQuery.setParameter("alias", currentAlias.getArangoName());
        subQuery.setParameter("aliasDoc", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX));
        subQuery.setParameter("numberOfTraversals", String.valueOf(numberOfTraversals));
        subQuery.setParameter("reverse", reverse ? "INBOUND" : "OUTBOUND");
        subQuery.setParameter("previousAliasDoc", String.format("%s_%s", previousAlias.peek().getArangoName(), DOC_POSTFIX));
        subQuery.setParameter("relation", relationCollection.getName());
        q.addLine(subQuery.build().getValue());
    }

    @Override
    public void nullFilter() {
        UnauthorizedArangoQuery subQuery = new UnauthorizedArangoQuery();
        subQuery.addLine("FILTER ${alias} != null");
        subQuery.setParameter("alias", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX));
        q.addLine(subQuery.build().getValue());
    }

    @Override
    protected void doStartReturnStructure(boolean simple) {
        createReleaseStatusQuery();

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
        firstReturnEntry = false;
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
        q.addLine(")}");
        q.outdent();
        firstReturnEntry = true;
    }

    @Override
    protected void doLeaveTraversal() {
        q.addLine(")");
    }

    @Override
    public void buildGrouping(String groupedInstancesLabel, List<ArangoAlias> groupingFields, List<ArangoAlias> nonGroupingFields) {
        UnauthorizedArangoQuery subQuery = new UnauthorizedArangoQuery();
        subQuery.addLine("COLLECT ");
        for (ArangoAlias field : groupingFields) {
            subQuery.addLine(new UnauthorizedArangoQuery().addLine("`${originalName}` = grp.`${originalName}`")
                    .setParameter("originalName", field.getOriginalName()).build().getValue());
            if (field != groupingFields.get(groupingFields.size() - 1)) {
                subQuery.addLine(",");
            }
        }
        subQuery.addLine("INTO group");
        subQuery.addLine("LET instances = (FOR el IN group RETURN {");

        for (ArangoAlias field : nonGroupingFields) {
            subQuery.addLine(new UnauthorizedArangoQuery().addLine("\"${originalName}\":  el.grp.`${originalName}`")
                    .setParameter("originalName", field.getOriginalName()).build().getValue());
            if (field != nonGroupingFields.get(nonGroupingFields.size() - 1)) {
                subQuery.addLine(",");
            }
        }
        subQuery.addLine("} )");
        subQuery.addLine("RETURN { ");

        for (ArangoAlias field : groupingFields) {
            subQuery.addLine(new UnauthorizedArangoQuery().addLine("\"${originalName}\": `${originalName}`")
                    .setParameter("originalName", field.getOriginalName()).build().getValue());
            if (field != groupingFields.get(groupingFields.size() - 1)) {
                subQuery.addLine(",");
            }
        }
        subQuery.addLine(", \"${groupedInstanceLabel}\": instances");
        subQuery.addLine("} )");
        subQuery.setParameter("groupedInstanceLabel", groupedInstancesLabel);
        q.addLine(subQuery.build().getValue());
    }


    @Override
    public ArangoReflectionQueryBuilder addRoot(ArangoCollectionReference rootCollection) {
        q.addLine(new UnauthorizedArangoQuery().addLine("FOR ${alias} IN `${collection}`").setParameter("alias", String.format("%s_%s", ROOT_ALIAS.getArangoName(), DOC_POSTFIX)).setParameter("collection", rootCollection.getName()).build().getValue());
        addOrganizationFilter();
        return this;
    }

    @Override
    public void addLimit() {
        if (pagination != null && pagination.getSize() != null) {
            if (pagination.getStart() != null) {
                q.addLine(new UnauthorizedArangoQuery().addLine("LIMIT ${start}, ${size}").setParameter("start", pagination.getStart().toString()).setParameter("size", pagination.getSize().toString()).build().getValue());
            } else {
                q.addLine(new UnauthorizedArangoQuery().addLine("LIMIT ${size}").setParameter("size", pagination.getSize().toString()).build().getValue());
            }
        }
    }

    @Override
    public void addTraversalResultField(String targetName, ArangoAlias alias) {
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
        addSimpleLeafResultField(leafField);
    }

    @Override
    public void addSimpleLeafResultField(ArangoAlias leafField) {
        if (firstReturnEntry) {
            addOrganizationFilter();
            q.addLine(createReleaseStatusQuery().build().getValue());
            UnauthorizedArangoQuery subQuery = new UnauthorizedArangoQuery();
            subQuery.addLine("RETURN {").indent();
            subQuery.addLine(" \"" +    JsonLdConsts.ID + "\": ${alias}.`" + JsonLdConsts.ID + "`,");
            subQuery.addLine(" \"" + SchemaOrgVocabulary.NAME + "\": ${alias}.`" + SchemaOrgVocabulary.NAME + "`,");
            subQuery.addLine(" \"" + SchemaOrgVocabulary.IDENTIFIER + "\": ${alias}.`" + SchemaOrgVocabulary.IDENTIFIER + "`,");
            subQuery.addLine(" \"status\": ${alias}_status,");
            subQuery.addLine(" \"" + JsonLdConsts.TYPE + "\": ${alias}.`" + JsonLdConsts.TYPE + "`");
            subQuery.outdent().addLine("}");
            subQuery.setParameter("alias", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX));
            q.addLine(subQuery.build().getValue());
        }
        firstReturnEntry = false;
    }

    @Override
    public void addMerge(ArangoAlias leafField, Set<ArangoAlias> mergedFields, boolean sorted) {
        UnauthorizedArangoQuery subQuery = new UnauthorizedArangoQuery();
        subQuery.addLine("LET ${targetName} = ");
        if (sorted) {
            subQuery.addLine("( FOR el IN");
        }
        subQuery.addLine("APPEND (${collections}, true) ");
        if (sorted) {
            subQuery.addLine("SORT el ASC RETURN el)");
        }
        subQuery.setParameter("targetName", leafField.getArangoName());
        subQuery.setTrustedParameter("collections", subQuery.listFields(mergedFields.stream().map(ArangoAlias::getArangoName).collect(Collectors.toSet())));
        q.addLine(subQuery.build().getValue());
    }

    @Override
    public void addInstanceIdFilter() {
        q.addLine(
                new UnauthorizedArangoQuery().addLine("FILTER ${field}._id == \"${id}\"").
                        setParameter("field", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX)).
                        setParameter("id", this.documentReference.getId()).build().getValue());
    }

    @Override
    public void addSearchQuery() {
    }

}
