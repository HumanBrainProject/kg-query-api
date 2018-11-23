package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query;

import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.query.entity.Filter;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ArangoQueryBuilder extends AbstractArangoQueryBuilder {


    public ArangoQueryBuilder(Specification specification, Pagination pagination, Filter filter, ArangoAlias permissionGroupFieldName, Set<String> whitelistOrganizations, ArangoDocumentReference documentReference, Set<ArangoCollectionReference> existingArangoCollections) {
        super(specification, pagination, filter, permissionGroupFieldName, whitelistOrganizations, documentReference, existingArangoCollections);
    }

    @Override
    public void addTraversal(boolean reverse, ArangoCollectionReference relationCollection) {
        UnauthorizedArangoQuery subQ = new UnauthorizedArangoQuery();
        subQ.setParameter("reverse", reverse ? "INBOUND" : "OUTBOUND");
        subQ.setParameter("relation", relationCollection.getName());
        subQ.addLine(", ${reverse} `${collection}`");
        q.addLine(subQ.build().getValue());
    }

    @Override
    public void addComplexFieldRequiredFilter(ArangoAlias leafField) {
        sb.append(String.format("\n%s AND %s_%s.`%s` != null ", getIndentation(), currentAlias.getArangoName(), DOC_POSTFIX, leafField.getOriginalName()));
        sb.append(String.format("\n%s AND %s_%s.`%s` != \"\" ", getIndentation(), currentAlias.getArangoName(), DOC_POSTFIX, leafField.getOriginalName()));
        sb.append(String.format("\n%s AND %s_%s.`%s` != [] ", getIndentation(), currentAlias.getArangoName(), DOC_POSTFIX, leafField.getOriginalName()));
    }

    @Override
    public void addTraversalFieldRequiredFilter(ArangoAlias alias) {
        sb.append(String.format("\n%s AND %s != null", getIndentation(), alias.getArangoName()));
        sb.append(String.format("\n%s AND %s != \"\"", getIndentation(), alias.getArangoName()));
        sb.append(String.format("\n%s AND %s != []", getIndentation(), alias.getArangoName()));
    }

    @Override
    protected void doEnterTraversal(ArangoAlias targetName, int numberOfTraversals, boolean reverse, ArangoCollectionReference relationCollection, boolean hasGroup, boolean ensureOrder) {
        sb.append(String.format("\n%sLET %s = %s ( FOR %s_%s %s IN %d..%d %s %s_%s `%s` ", getIndentation(), currentAlias.getArangoName(), hasGroup ? " (FOR grp IN " : "", currentAlias.getArangoName(), DOC_POSTFIX, ensureOrder ? ", e" : "", numberOfTraversals, numberOfTraversals, reverse ? "INBOUND" : "OUTBOUND", previousAlias.peek().getArangoName(), DOC_POSTFIX, relationCollection.getName()));
    }

    @Override
    public void nullFilter() {
        sb.append(String.format(" FILTER %s_%s != null ", currentAlias.getArangoName(), DOC_POSTFIX));
    }

    @Override
    protected void doStartReturnStructure(boolean simple) {
        sb.append(String.format("\n%s  RETURN DISTINCT %s", getIndentation(), simple ? "" : "{\n"));
    }

    @Override
    public void endReturnStructure() {
        if (!simpleReturn) {
            sb.append(String.format("\n%s  }", getIndentation()));
        }
        simpleReturn = true;
        firstReturnEntry = true;
    }

    @Override
    protected void doLeaveTraversal() {
        sb.append(")\n");
    }

    @Override
    public void buildGrouping(String groupedInstancesLabel, List<ArangoAlias> groupingFields, List<ArangoAlias> nonGroupingFields) {
        sb.append("COLLECT ");
        List<String> groupings = groupingFields.stream().map(f -> String.format("`%s` = grp.`%s`", f.getOriginalName(), f.getOriginalName())).collect(Collectors.toList());
        sb.append(String.join(", ", groupings));
        sb.append(" INTO group\n");
        sb.append("LET instances = ( FOR el IN group RETURN {\n");

        List<String> nonGrouping = nonGroupingFields.stream().map(s -> String.format("\"%s\": el.grp.`%s`", s.getOriginalName(), s.getOriginalName())).collect(Collectors.toList());
        sb.append(String.join(",\n", nonGrouping));
        sb.append("\n} )\n");
        sb.append("RETURN {\n");

        List<String> returnGrouped = groupingFields.stream().map(f -> String.format("\"%s\": `%s`", f.getOriginalName(), f.getOriginalName())).collect(Collectors.toList());
        sb.append(String.join(",\n", returnGrouped));
        sb.append(String.format(",\n \"%s\": instances\n", groupedInstancesLabel));
        sb.append("} )");
    }



    @Override
    public ArangoQueryBuilder addRoot(ArangoCollectionReference rootCollection) {
        sb.append(String.format("FOR %s_%s IN `%s`\n", ROOT_ALIAS.getArangoName(), DOC_POSTFIX, rootCollection.getName()));
        addOrganizationFilter();
        return this;
    }

    @Override
    public void addLimit() {
        if (pagination!=null && pagination.getSize() != null) {
            if (pagination.getStart() != null) {
                sb.append(String.format("\nLIMIT %d, %d\n", pagination.getStart(), pagination.getSize()));
            } else {
                sb.append(String.format("\nLIMIT %d\n", pagination.getSize()));
            }
        }
    }

    @Override
    public void addTraversalResultField(String targetName, ArangoAlias alias) {
        if (!firstReturnEntry) {
            sb.append(",\n");
        }
        sb.append(String.format("%s    \"%s\": %s", getIndentation(), targetName, alias.getArangoName()));
        firstReturnEntry = false;
    }

    @Override
    public void addSortByLeafField(Set<ArangoAlias> fields) {
        List<String> fullSortFields = fields.stream().map(s -> String.format("%s_%s.`%s`", currentAlias.getArangoName(), DOC_POSTFIX, s.getOriginalName())).collect(Collectors.toList());
        String concat = String.join(", ", fullSortFields);
        sb.append(String.format("%s   SORT %s ASC\n", getIndentation(), concat));
    }

    @Override
    public void ensureOrder() {
        sb.append(String.format("\n%s SORT e._orderNumber ASC\n", getIndentation()));
    }

    @Override
    public void addComplexLeafResultField(String targetName, ArangoAlias leafField) {
        if (!firstReturnEntry) {
            sb.append(",\n");
        }
        sb.append(String.format("%s    \"%s\": %s_%s.`%s`", getIndentation(), targetName, currentAlias.getArangoName(), DOC_POSTFIX, leafField.getOriginalName()));
        firstReturnEntry = false;
    }

    @Override
    public void addSimpleLeafResultField(ArangoAlias leafField) {
        if (!firstReturnEntry) {
            sb.append(",\n");
            addOrganizationFilter();
        }
        sb.append(String.format("\n%s  RETURN DISTINCT %s_%s.`%s`\n", getIndentation(), currentAlias.getArangoName(), DOC_POSTFIX, leafField.getOriginalName()));
        firstReturnEntry = true;
    }

    @Override
    public void addMerge(ArangoAlias leafField, Set<ArangoAlias> mergedFields, boolean sorted) {
        sb.append(String.format("\n%s LET %s = %s APPEND(%s, true) %s\n", getIndentation(), leafField.getArangoName(), sorted ? "( FOR el IN" : "", String.join(", ", mergedFields.stream().map(ArangoAlias::getArangoName).collect(Collectors.toSet())), sorted ? " SORT el ASC RETURN el)" : ""));
    }

    @Override
    public void addInstanceIdFilter() {
        sb.append(String.format("\nFILTER %s_%s._id == \"%s\"\n", currentAlias.getArangoName(), DOC_POSTFIX, this.documentReference.getId()));
    }

    @Override
    public void addSearchQuery() {
        if (filter != null && filter.getQueryString()!=null) {
            sb.append(String.format("\n FILTER LIKE(%s_%s.`http://schema.org/name`, \"%%%s%%\")", ROOT_ALIAS.getArangoName(), DOC_POSTFIX, filter.getQueryString()));
        }
    }

}
