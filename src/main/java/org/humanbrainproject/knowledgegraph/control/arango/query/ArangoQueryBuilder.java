package org.humanbrainproject.knowledgegraph.control.arango.query;

import org.humanbrainproject.knowledgegraph.entity.specification.Specification;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ArangoQueryBuilder extends AbstractQueryBuilder {


    public ArangoQueryBuilder(Specification specification, Integer size, Integer start, String searchTerm, String permissionGroupFieldName, Set<String> whitelistOrganizations, String instanceId) {
        super(specification, size, start, searchTerm, permissionGroupFieldName, whitelistOrganizations, instanceId);
    }

    @Override
    public void addTraversal(boolean reverse, String relationCollection) {
        sb.append(String.format(", %s `%s`", reverse ? "INBOUND" : "OUTBOUND", relationCollection));
    }

    @Override
    public void addComplexFieldRequiredFilter(String leaf_field) {
        sb.append(String.format("\n%s AND %s_%s.`%s` != null ", getIndentation(), currentAlias, DOC_POSTFIX, leaf_field));
        sb.append(String.format("\n%s AND %s_%s.`%s` != \"\" ", getIndentation(), currentAlias, DOC_POSTFIX, leaf_field));
        sb.append(String.format("\n%s AND %s_%s.`%s` != [] ", getIndentation(), currentAlias, DOC_POSTFIX, leaf_field));
    }

    @Override
    public void addTraversalFieldRequiredFilter(String alias) {
        sb.append(String.format("\n%s AND %s != null", getIndentation(), alias));
        sb.append(String.format("\n%s AND %s != \"\"", getIndentation(), alias));
        sb.append(String.format("\n%s AND %s != []", getIndentation(), alias));
    }

    @Override
    protected void doEnterTraversal(String targetName, int numberOfTraversals, boolean reverse, String relationCollection, boolean hasGroup, boolean ensureOrder) {
        sb.append(String.format("\n%sLET %s = %s ( FOR %s_%s %s IN %d..%d %s %s_%s `%s` ", getIndentation(), currentAlias, hasGroup ? " (FOR grp IN " : "", currentAlias, DOC_POSTFIX, ensureOrder ? ", e" : "", numberOfTraversals, numberOfTraversals, reverse ? "INBOUND" : "OUTBOUND", previousAlias.peek(), DOC_POSTFIX, relationCollection));
    }

    @Override
    public void nullFilter() {
        sb.append(String.format(" FILTER %s_%s != null ", currentAlias, DOC_POSTFIX));
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
    public void buildGrouping(String groupedInstancesLabel, List<String> groupingFields, List<String> nonGroupingFields) {
        sb.append("COLLECT ");
        List<String> groupings = groupingFields.stream().map(f -> String.format("`%s` = grp.`%s`", f, f)).collect(Collectors.toList());
        sb.append(String.join(", ", groupings));
        sb.append(" INTO group\n");
        sb.append("LET instances = ( FOR el IN group RETURN {\n");

        List<String> nonGrouping = nonGroupingFields.stream().map(s -> String.format("\"%s\": el.grp.`%s`", s, s)).collect(Collectors.toList());
        sb.append(String.join(",\n", nonGrouping));
        sb.append("\n} )\n");
        sb.append("RETURN {\n");

        List<String> returnGrouped = groupingFields.stream().map(f -> String.format("\"%s\": `%s`", f, f)).collect(Collectors.toList());
        sb.append(String.join(",\n", returnGrouped));
        sb.append(String.format(",\n \"%s\": instances\n", groupedInstancesLabel));
        sb.append("} )");
    }



    @Override
    public ArangoQueryBuilder addRoot(String rootCollection) {
        sb.append(String.format("FOR %s_%s IN `%s`\n", ROOT_ALIAS, DOC_POSTFIX, rootCollection));
        addOrganizationFilter();
        return this;
    }

    @Override
    public void addLimit() {
        if (size != null) {
            if (start != null) {
                sb.append(String.format("\nLIMIT %d, %d\n", start, size));
            } else {
                sb.append(String.format("\nLIMIT %d\n", size));
            }
        }
    }

    @Override
    public void addTraversalResultField(String targetName, String alias) {
        if (!firstReturnEntry) {
            sb.append(",\n");
        }
        sb.append(String.format("%s    \"%s\": %s", getIndentation(), targetName, alias));
        firstReturnEntry = false;
    }

    @Override
    public void addSortByLeafField(Set<String> fields) {
        List<String> fullSortFields = fields.stream().map(s -> String.format("%s_%s.`%s`", currentAlias, DOC_POSTFIX, s)).collect(Collectors.toList());
        String concat = String.join(", ", fullSortFields);
        sb.append(String.format("%s   SORT %s ASC\n", getIndentation(), concat));
    }

    @Override
    public void ensureOrder() {
        sb.append(String.format("\n%s SORT e.orderNumber ASC\n", getIndentation()));
    }

    @Override
    public void addComplexLeafResultField(String targetName, String leaf_field) {
        if (!firstReturnEntry) {
            sb.append(",\n");
        }
        sb.append(String.format("%s    \"%s\": %s_%s.`%s`", getIndentation(), targetName, currentAlias, DOC_POSTFIX, leaf_field));
        firstReturnEntry = false;
    }

    @Override
    public void addSimpleLeafResultField(String leaf_field) {
        if (!firstReturnEntry) {
            sb.append(",\n");
            addOrganizationFilter();
        }
        sb.append(String.format("\n%s  RETURN DISTINCT %s_%s.`%s`\n", getIndentation(), currentAlias, DOC_POSTFIX, leaf_field));
        firstReturnEntry = true;
    }

    @Override
    public void addMerge(String leaf_field, Set<String> merged_fields, boolean sorted) {
        sb.append(String.format("\n%s LET %s = %s APPEND(%s, true) %s\n", getIndentation(), leaf_field, sorted ? "( FOR el IN" : "", String.join(", ", merged_fields), sorted ? " SORT el ASC RETURN el)" : ""));
    }

    @Override
    public void addInstanceIdFilter() {
        sb.append(String.format("\nFILTER %s_%s._id == \"%s\"\n", currentAlias, DOC_POSTFIX, this.instanceId));
    }

    @Override
    public void addSearchQuery() {
        if (searchTerm != null) {
            sb.append(String.format("\n FILTER LIKE(%s_%s.`http://schema.org/name`, \"%%%s%%\")", ROOT_ALIAS, DOC_POSTFIX, searchTerm));
        }
    }

}
