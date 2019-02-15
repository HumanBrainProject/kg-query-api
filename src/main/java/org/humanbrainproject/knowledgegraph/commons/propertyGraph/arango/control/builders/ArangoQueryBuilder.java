package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.TrustedAqlValue;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.SchemaOrgVocabulary;
import org.humanbrainproject.knowledgegraph.query.entity.Filter;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.SpecField;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.Op;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL.*;


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
        AQL subQuery = new AQL();
        subQuery.setParameter("reverse", reverse ? "INBOUND" : "OUTBOUND");
        subQuery.setParameter("collection", relationCollection.getName());
        subQuery.addLine(trust(", ${reverse} `${collection}`"));
        q.addLine(AQL.trust(subQuery.build().getValue()));
    }

    @Override
    public void addComplexFieldRequiredFilter(ArangoAlias leafField) {
        AQL subQuery = new AQL();
        subQuery.addLine(trust("AND ${alias}.`${field}` != null"));
        subQuery.addLine(trust("AND ${alias}.`${field}` != \"\""));
        subQuery.addLine(trust("AND ${alias}.`${field}` != []"));
        subQuery.setParameter("alias", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX));
        subQuery.setParameter("field", leafField.getOriginalName());
        q.addLine(subQuery.build());
    }

    @Override
    public void addTraversalFieldRequiredFilter(ArangoAlias alias) {
        AQL subQuery = new AQL();
        subQuery.addLine(trust("AND ${alias} != null"));
        subQuery.addLine(trust("AND ${alias} != \"\""));
        subQuery.addLine(trust("AND ${alias} != []"));
        subQuery.setParameter("alias", alias.getArangoName());
        q.addLine(subQuery.build());
    }

    @Override
    protected void doEnterTraversal(ArangoAlias targetName, int numberOfTraversals, boolean reverse, ArangoCollectionReference relationCollection, boolean hasGroup, boolean ensureOrder) {
        AQL subQuery = new AQL();
        subQuery.addLine(trust("LET ${alias} = "));
        if (hasGroup) {
            subQuery.indent();
            subQuery.addLine(trust("( FOR grp IN "));
        }
        subQuery.indent();
        subQuery.addLine(trust("( FOR ${aliasDoc} "));
        if (ensureOrder) {
            subQuery.indent();
            subQuery.addLine(trust(", e "));
            subQuery.outdent();
        }
        subQuery.addLine(trust("IN ${numberOfTraversals}..${numberOfTraversals} ${reverse} ${previousAliasDoc} `${relation}`"));
        subQuery.setParameter("alias", currentAlias.getArangoName());
        subQuery.setParameter("aliasDoc", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX));
        subQuery.setParameter("numberOfTraversals", String.valueOf(numberOfTraversals));
        subQuery.setParameter("reverse", reverse ? "INBOUND" : "OUTBOUND");
        subQuery.setParameter("previousAliasDoc", String.format("%s_%s", previousAlias.peek().getArangoName(), DOC_POSTFIX));
        subQuery.setParameter("relation", relationCollection.getName());
        q.addLine(subQuery.build());
    }

    @Override
    public void nullFilter() {
        AQL subQuery = new AQL();
        subQuery.addLine(trust("FILTER ${alias} != null"));
        subQuery.setParameter("alias", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX));
        q.addLine(subQuery.build());
    }

    @Override
    protected void doStartReturnStructure(boolean simple) {
        if (isRoot()) {
            if (simple) {
                q.addLine(trust("RETURN "));
            } else {
                q.addLine(trust("RETURN {"));
                q.indent();
            }
        } else {
            if (simple) {
                q.addLine(trust("RETURN DISTINCT "));
            } else {
                q.addLine(trust("RETURN DISTINCT {"));
                q.indent();
            }
        }
    }

    @Override
    public void endReturnStructure() {
        if (!simpleReturn) {
            q.addLine(trust("}"));
            q.outdent();
        }
        simpleReturn = true;
        firstReturnEntry = true;
    }

    @Override
    protected void doLeaveTraversal() {
        q.addLine(trust(")"));
    }

    @Override
    public void buildGrouping(String groupedInstancesLabel, List<ArangoAlias> groupingFields, List<ArangoAlias> nonGroupingFields) {
        AQL subQuery = new AQL();
        subQuery.addLine(trust("COLLECT "));
        for (ArangoAlias field : groupingFields) {
            subQuery.addLine(new AQL().addLine(trust("`${originalName}` = grp.`${originalName}`"))
                    .setParameter("originalName", field.getOriginalName()).build());
            if (field != groupingFields.get(groupingFields.size() - 1)) {
                subQuery.addComma();
            }
        }
        subQuery.addLine(trust("INTO group"));
        subQuery.addLine(trust("LET instances = (FOR el IN group RETURN {"));

        for (ArangoAlias field : nonGroupingFields) {
            subQuery.addLine(new AQL().addLine(trust("\"${originalName}\":  el.grp.`${originalName}`"))
                    .setParameter("originalName", field.getOriginalName()).build());
            if (field != nonGroupingFields.get(nonGroupingFields.size() - 1)) {
                subQuery.addComma();
            }
        }
        subQuery.addLine(trust("} )"));
        subQuery.addLine(trust("RETURN { "));

        for (ArangoAlias field : groupingFields) {
            subQuery.addLine(new AQL().addLine(trust("\"${originalName}\": `${originalName}`"))
                    .setParameter("originalName", field.getOriginalName()).build());
            if (field != groupingFields.get(groupingFields.size() - 1)) {
                subQuery.addComma();
            }
        }
        subQuery.addLine(trust(", \"${groupedInstanceLabel}\": instances"));
        subQuery.addLine(trust("} )"));
        subQuery.setParameter("groupedInstanceLabel", groupedInstancesLabel);
        q.addLine(subQuery.build());
    }


    @Override
    public ArangoQueryBuilder addRoot(ArangoCollectionReference rootCollection) {
        q.addLine(new AQL().addLine(trust("FOR ${alias} IN `${collection}`")).setParameter("alias", String.format("%s_%s", ROOT_ALIAS.getArangoName(), DOC_POSTFIX)).setParameter("collection", rootCollection.getName()).build());
        addOrganizationFilter();
        return this;
    }

    @Override
    public void addLimit() {
        if (pagination!=null && pagination.getSize() != null) {
            q.addLine(new AQL().addLine(trust("LIMIT ${start}, ${size}")).setParameter("start", String.valueOf(pagination.getStart())).setParameter("size", pagination.getSize().toString()).build());
        }
    }

    @Override
    public void addTraversalResultField(String targetName, ArangoAlias alias) {
        if (!firstReturnEntry) {
            q.addComma();
        }
        q.addLine(new AQL().addLine(trust("\"${targetName}\": ${alias}")).setParameter("targetName", targetName).setParameter("alias", alias.getArangoName()).build());
        firstReturnEntry = false;
    }

    @Override
    public void addSortByLeafField(Set<ArangoAlias> fields) {
        q.addLine(trust("SORT "));
        List<ArangoAlias> fieldList = new ArrayList<>(fields);
        for (ArangoAlias field : fieldList) {
            q.addLine(new AQL().addLine(
                    trust("${alias}.`${originalName}`")).
                    setParameter("alias", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX)).
                    setParameter("originalName", field.getOriginalName()).
                    build());
            if (field != fieldList.get(fieldList.size() - 1)) {
                q.addComma();
            }
        }
        q.addLine(trust("ASC"));
    }

    @Override
    public void ensureOrder() {
        q.addLine(trust("SORT e._orderNumber ASC"));
    }

    @Override
    public void addComplexLeafResultField(String targetName, ArangoAlias leafField) {
        if (!firstReturnEntry) {
            q.addComma();
        }
        q.addLine(new AQL().addLine(trust("\"${targetName}\": ${alias}.`${originalName}`")).setParameter("targetName", targetName).setParameter("alias", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX)).setParameter("originalName", leafField.getOriginalName()).build());
        firstReturnEntry = false;
    }

    @Override
    public void addSimpleLeafResultField(ArangoAlias leafField) {
        if (!firstReturnEntry) {
            q.addComma();
            addOrganizationFilter();
        }
        q.addLine(new AQL().addLine(trust("RETURN DISTINCT ${alias}.`${originalName}`")).setParameter("alias", String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX)).setParameter("originalName", leafField.getOriginalName()).build());
        firstReturnEntry = true;
    }

    @Override
    public void addMerge(ArangoAlias leafField, Set<ArangoAlias> mergedFields, boolean sorted) {
        AQL subQuery = new AQL();
        subQuery.addLine(trust("LET ${targetName} = "));
        if (sorted) {
            subQuery.addLine(trust("( FOR el IN"));
        }
        subQuery.addLine(trust("APPEND (${collections}, true) "));
        if (sorted) {
            subQuery.addLine(trust("SORT el ASC RETURN el)"));
        }
        subQuery.setParameter("targetName", leafField.getArangoName());
        subQuery.setTrustedParameter("collections", subQuery.listFields(mergedFields.stream().map(ArangoAlias::getArangoName).collect(Collectors.toSet())));
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
        if (filter != null && filter.getQueryString() != null) {
            AQL query = new AQL();
            TrustedAqlValue f = query.preventAqlInjectionForSearchQuery(filter.getQueryString());
            f = query.generateSearchTermQuery(f);
            q.addLine(
                    new AQL().
                            addLine(trust("FILTER LIKE (LOWER(${root}.`" + SchemaOrgVocabulary.NAME + "`), \"${filter}\")")).
                            setParameter("root", String.format("%s_%s", ROOT_ALIAS.getArangoName(), DOC_POSTFIX)).
                            setTrustedParameter("filter", f).build());
        }
    }

    @Override
    public void addFieldFilter(ArangoAlias alias){
        AQL subQuery = new AQL();
        Op op = currentField.fieldFilter.getOp();
        String operator;
        Value value = (Value) currentField.fieldFilter.getExp();
        if(op == Op.EQUALS){
            subQuery.addLine(new AQL().addLine(trust("FILTER ${alias}.`${field}` == \"${value}\" ")).setParameter("value", value.getValue()).build());
        }else{
            TrustedAqlValue searchTermQuery = subQuery.generateSearchTermQuery(subQuery.preventAqlInjectionForSearchQuery(value.getValue()));
            subQuery.addLine(new AQL().addLine(trust("FILTER ${alias}.`${field}`  LIKE \"${value}\" ")).setTrustedParameter("value", searchTermQuery).build());
        }

        subQuery.setParameter("alias",  String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX));
        subQuery.setParameter("field", alias.getOriginalName());
        q.addLine(subQuery.build());
    }

}
