package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.TrustedAqlValue;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.UnauthorizedArangoQuery;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.query.entity.Filter;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.SpecField;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.Op;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.Exp;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ToBeTested
public class ArangoQueryBuilder extends AbstractArangoQueryBuilder {


    public ArangoQueryBuilder(Specification specification, Pagination pagination, Filter filter, ArangoAlias permissionGroupFieldName, Set<String> whitelistOrganizations, Set<ArangoDocumentReference> documentReferences, Set<ArangoCollectionReference> existingArangoCollections) {
        super(specification, pagination, filter, permissionGroupFieldName, whitelistOrganizations, documentReferences, existingArangoCollections);
    }

    @Override
    public void leaveAdditionalTraversal(boolean reverse, ArangoCollectionReference relationCollection, int traversalDepth, boolean leaf) {

    }

    @Override
    public void prepareLeafField(SpecField leafField) {

    }

    @Override
    public void addTraversal(boolean reverse, ArangoCollectionReference relationCollection, int traversalDepth) {
        UnauthorizedArangoQuery subQuery = new UnauthorizedArangoQuery();
        subQuery.setParameter("reverse", reverse ? "INBOUND" : "OUTBOUND");
        subQuery.setParameter("collection", relationCollection.getName());
        subQuery.addLine(", ${reverse} `${collection}`");
        q.addLine(subQuery.build().getValue());
    }

    @Override
    public void addComplexFieldRequiredFilter(ArangoAlias leafField) {
        UnauthorizedArangoQuery subQuery = new UnauthorizedArangoQuery();
        subQuery.addLine("AND ${alias}.`${field}` != null");
        subQuery.addLine("AND ${alias}.`${field}` != \"\"");
        subQuery.addLine("AND ${alias}.`${field}` != []");
        subQuery.setParameter("alias", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX));
        subQuery.setParameter("field", leafField.getOriginalName());
        q.addLine(subQuery.build().getValue());
    }

    @Override
    public void addTraversalFieldRequiredFilter(ArangoAlias alias) {
        UnauthorizedArangoQuery subQuery = new UnauthorizedArangoQuery();
        subQuery.addLine("AND ${alias} != null");
        subQuery.addLine("AND ${alias} != \"\"");
        subQuery.addLine("AND ${alias} != []");
        subQuery.setParameter("alias", alias.getArangoName());
        q.addLine(subQuery.build().getValue());
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
        if (isRoot()) {
            if (simple) {
                q.addLine("RETURN ");
            } else {
                q.addLine("RETURN {");
                q.indent();
            }
        } else {
            if (simple) {
                q.addLine("RETURN DISTINCT ");
            } else {
                q.addLine("RETURN DISTINCT {");
                q.indent();
            }
        }
    }

    @Override
    public void endReturnStructure() {
        if (!simpleReturn) {
            q.addLine("}");
            q.outdent();
        }
        simpleReturn = true;
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
    public ArangoQueryBuilder addRoot(ArangoCollectionReference rootCollection) {
        q.addLine(new UnauthorizedArangoQuery().addLine("FOR ${alias} IN `${collection}`").setParameter("alias", String.format("%s_%s", ROOT_ALIAS.getArangoName(), DOC_POSTFIX)).setParameter("collection", rootCollection.getName()).build().getValue());
        addOrganizationFilter();
        return this;
    }

    @Override
    public void addLimit() {
        if (pagination!=null && pagination.getSize() != null) {
            q.addLine(new UnauthorizedArangoQuery().addLine("LIMIT ${start}, ${size}").setParameter("start", String.valueOf(pagination.getStart())).setParameter("size", pagination.getSize().toString()).build().getValue());
        }
    }

    @Override
    public void addTraversalResultField(String targetName, ArangoAlias alias) {
        if (!firstReturnEntry) {
            q.addLine(",");
        }
        q.addLine(new UnauthorizedArangoQuery().addLine("\"${targetName}\": ${alias}").setParameter("targetName", targetName).setParameter("alias", alias.getArangoName()).build().getValue());
        firstReturnEntry = false;
    }

    @Override
    public void addSortByLeafField(Set<ArangoAlias> fields) {
        q.addLine("SORT ");
        List<ArangoAlias> fieldList = new ArrayList<>(fields);
        for (ArangoAlias field : fieldList) {
            q.addLine(new UnauthorizedArangoQuery().addLine(
                    "${alias}.`${originalName}`").
                    setParameter("alias", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX)).
                    setParameter("originalName", field.getOriginalName()).
                    build().getValue());
            if (field != fieldList.get(fieldList.size() - 1)) {
                q.addLine(",");
            }
        }
        q.addLine("ASC");
    }

    @Override
    public void ensureOrder() {
        q.addLine("SORT e._orderNumber ASC");
    }

    @Override
    public void addComplexLeafResultField(String targetName, ArangoAlias leafField) {
        if (!firstReturnEntry) {
            q.addLine(",");
        }
        q.addLine(new UnauthorizedArangoQuery().addLine("\"${targetName}\": ${alias}.`${originalName}`").setParameter("targetName", targetName).setParameter("alias", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX)).setParameter("originalName", leafField.getOriginalName()).build().getValue());
        firstReturnEntry = false;
    }

    @Override
    public void addSimpleLeafResultField(ArangoAlias leafField) {
        if (!firstReturnEntry) {
            q.addLine(",");
            addOrganizationFilter();
        }
        q.addLine(new UnauthorizedArangoQuery().addLine("RETURN DISTINCT ${alias}.`${originalName}`").setParameter("alias", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX)).setParameter("originalName", leafField.getOriginalName()).build().getValue());
        firstReturnEntry = true;
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
        if (filter != null && filter.getQueryString() != null) {
            UnauthorizedArangoQuery query = new UnauthorizedArangoQuery();
            TrustedAqlValue f = query.preventAqlInjectionForSearchQuery(filter.getQueryString());
            f = query.generateSearchTermQuery(f);
            q.addLine(
                    new UnauthorizedArangoQuery().
                            addLine("FILTER LIKE (LOWER(${root}.`" + SchemaOrgVocabulary.NAME + "`), \"${filter}\")").
                            setParameter("root", String.format("%s_%s", ROOT_ALIAS.getArangoName(), DOC_POSTFIX)).
                            setTrustedParameter("filter", f).build().getValue());
        }
    }

    @Override
    public void addFieldFilter(ArangoAlias alias){
        UnauthorizedArangoQuery subQuery = new UnauthorizedArangoQuery();
        Op op = currentField.fieldFilter.getOp();
        String operator;
        Value value = (Value) currentField.fieldFilter.getExp();
        if(op == Op.EQUALS){
            operator = "FILTER ${alias}.`${field}` == \"%s\" ";
            subQuery.addLine(String.format(operator, value.getValue()));
        }else{
            operator = "FILTER ${alias}.`${field}` LIKE \"%s\" ";
            TrustedAqlValue f = subQuery.preventAqlInjectionForSearchQuery(value.getValue());
            f = subQuery.generateSearchTermQuery(f);
            subQuery.addLine(String.format(operator, f.getValue()));
        }

        subQuery.setParameter("alias",  String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX));
        subQuery.setParameter("field", alias.getOriginalName());
        q.addLine(subQuery.build().getValue());
    }

}
