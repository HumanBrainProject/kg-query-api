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
        String name = this.bindVariables.put("@traversalCollection", relationCollection, null);
        sb.append(String.format(", %s @%s", reverse ? "INBOUND" : "OUTBOUND", name));
    }

    @Override
    public void addComplexFieldRequiredFilter(String leaf_field) {
        String name = this.bindVariables.put("complexFieldsRequired", leaf_field, null);
        sb.append(String.format("\n%s AND %s_%s.@%s != null ", getIndentation(), currentAlias, DOC_POSTFIX, name));
        sb.append(String.format("\n%s AND %s_%s.@%s != \"\" ", getIndentation(), currentAlias, DOC_POSTFIX, name));
        sb.append(String.format("\n%s AND %s_%s.@%s != [] ", getIndentation(), currentAlias, DOC_POSTFIX, name));
    }

    @Override
    public void addTraversalFieldRequiredFilter(String alias) {
        String name = this.bindVariables.put("traversalFieldRequired", alias, null);
        sb.append(String.format("\n%s AND @%s != null", getIndentation(), name));
        sb.append(String.format("\n%s AND @%s != \"\"", getIndentation(), name));
        sb.append(String.format("\n%s AND @%s != []", getIndentation(), name));
    }

    @Override
    protected void doEnterTraversal(String targetName, int numberOfTraversals, boolean reverse, String relationCollection, boolean hasGroup, boolean ensureOrder) {
        String relColName = this.bindVariables.put("@relationCollection", relationCollection, null);
        String numOfTraversalName = this.bindVariables.put("numberOfTraversals",numberOfTraversals, null);
        sb.append(String.format("\n%sLET %s = %s ( FOR %s_%s %s IN @%s..@%s %s %s_%s @%s ", getIndentation(), currentAlias,
                hasGroup ? " (FOR grp IN " : "", currentAlias, DOC_POSTFIX,
                ensureOrder ? ", e" : "", numOfTraversalName, numOfTraversalName,
                reverse ? "INBOUND" : "OUTBOUND", previousAlias.peek(), DOC_POSTFIX, relColName));
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
        List<String> groupings = groupingFields.stream().map(f -> {
            String name = this.bindVariables.put("groupTarget", f, null);
            String escaped = this.escapeVariableName(f);
            return String.format("`%s` = grp.@%s", escaped, name);
        }).collect(Collectors.toList());
        sb.append(String.join(", ", groupings));
        sb.append(" INTO group\n");
        sb.append("LET instances = ( FOR el IN group RETURN {\n");

        List<String> nonGrouping = nonGroupingFields.stream().map(s -> {
            String name = this.bindVariables.put("groupTarget", s, null);
            return String.format("@%s: el.grp.@%s", name, name);
        }).collect(Collectors.toList());
        sb.append(String.join(",\n", nonGrouping));
        sb.append("\n} )\n");
        sb.append("RETURN {\n");

        List<String> returnGrouped = groupingFields.stream().map(f -> {
            String name = this.bindVariables.put("groupTarget", f, null);
            return String.format("@%s: @%s", name, name);
        }).collect(Collectors.toList());
        sb.append(String.join(",\n", returnGrouped));
        String name = this.bindVariables.put("groupInstance", groupedInstancesLabel, null);
        sb.append(String.format(",\n @%s: instances\n", name));
        sb.append("} )");
    }



    @Override
    public ArangoQueryBuilder addRoot(String rootCollection) {
        String name = this.bindVariables.put("@rootCollection", rootCollection, null);
        sb.append(String.format("FOR %s_%s IN @%s\n", ROOT_ALIAS, DOC_POSTFIX, name));
        addOrganizationFilter();
        return this;
    }

    @Override
    public void addLimit() {
        if (size != null) {
            this.bindVariables.put("size", size.toString(), null);
            if (start != null) {
                this.bindVariables.put("start", start.toString(), null);
                sb.append("\nLIMIT @start, @size\n");
            } else {
                sb.append("\nLIMIT @size\n");
            }
        }
    }

    @Override
    public void addTraversalResultField(String targetName, String alias) {
        if (!firstReturnEntry) {
            sb.append(",\n");
        }
        String name = this.bindVariables.put("traversalResultField", targetName, null);
        sb.append(String.format("%s    @%s: %s", getIndentation(), name, alias));
        firstReturnEntry = false;
    }

    @Override
    public void addSortByLeafField(Set<String> fields) {
        List<String> fullSortFields = fields.stream().map(s -> {
                    String name = this.bindVariables.put("sortByLeafField", s, null);
                    return String.format("%s_%s.@%s", currentAlias, DOC_POSTFIX, name);
                }
        )
        .collect(Collectors.toList());
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
        String genTargetName = this.bindVariables.put("complexLeafFieldTarget", targetName, null);
        String name = this.bindVariables.put("complexLeafField", leaf_field, null);
        sb.append(String.format("%s    @%s: %s_%s.@%s", getIndentation(), genTargetName, currentAlias, DOC_POSTFIX, name));
        firstReturnEntry = false;
    }

    @Override
    public void addSimpleLeafResultField(String leaf_field) {
        if (!firstReturnEntry) {
            sb.append(",\n");
            addOrganizationFilter();
        }
        String name = this.bindVariables.put("simpleLeafField", leaf_field, null);
        sb.append(String.format("\n%s  RETURN DISTINCT %s_%s.@%s\n", getIndentation(), currentAlias, DOC_POSTFIX, name));
        firstReturnEntry = true;
    }

    @Override
    public void addMerge(String leaf_field, Set<String> merged_fields, boolean sorted) {
        String fields =  String.join(", ", merged_fields);
        sb.append(String.format("\n%s LET %s = %s APPEND(%s, true) %s\n", getIndentation(), leaf_field, sorted ? "( FOR el IN" : "", fields, sorted ? " SORT el ASC RETURN el)" : ""));
    }

    @Override
    public void addInstanceIdFilter() {
        String name = this.bindVariables.put("instanceFilter", this.instanceId, null);
        sb.append(String.format("\nFILTER %s_%s._id == @%s\n", currentAlias, DOC_POSTFIX, name));
    }

    @Override
    public void addSearchQuery() {
        if (searchTerm != null) {
            String name = this.bindVariables.put("search", searchTerm, null);
            sb.append(String.format("\n FILTER LIKE(%s_%s.`http://schema.org/name`, CONCAT(\"%%\", @%s, \"%%\"))", ROOT_ALIAS, DOC_POSTFIX, name));
        }
    }

    private String escapeVariableName(String s){
        return s
                .replaceAll("`", "\\`")
                .replaceAll("\"", "\\\"");
    }

}
