package org.humanbrainproject.knowledgegraph.control.arango.query;

import org.humanbrainproject.knowledgegraph.entity.specification.Specification;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.util.List;
import java.util.Set;

public class ArangoReflectQueryBuilder extends AbstractQueryBuilder{

    public ArangoReflectQueryBuilder(Specification specification, Integer size, Integer start, String permissionGroupFieldName, Set<String> whitelistOrganizations) {
        super(specification, size, start, permissionGroupFieldName, whitelistOrganizations);
    }

    @Override
    protected void doEnterTraversal(String targetName, int numberOfTraversals, boolean reverse, String relationCollection, boolean hasGroup, boolean ensureOrder) {
        sb.append(String.format("\n   LET %s = ( FOR %s_%s IN %d..%d %s %s_%s `%s`", currentAlias, currentAlias, DOC_POSTFIX, numberOfTraversals, numberOfTraversals, reverse? "INBOUND" : "OUTBOUND", previousAlias.peek(), DOC_POSTFIX, relationCollection));
        addOrganizationFilter();
        sb.append(String.format("\n      LET %s_rel = ( FOR coll IN COLLECTIONS()\n", currentAlias));
        sb.append(String.format("         FILTER IS_SAME_COLLECTION(coll.name, %s_%s)\n", reverse ? currentAlias : previousAlias.peek(), DOC_POSTFIX));
        sb.append("         RETURN { \"schema\": coll.name } )\n\n");
    }

    @Override
    public void addTraversal(boolean reverse, String relationCollection) {

    }

    @Override
    public void addComplexFieldRequiredFilter(String leaf_field) {

    }

    @Override
    public void addTraversalFieldRequiredFilter(String alias) {
    }

    @Override
    protected void doStartReturnStructure(boolean simple) {
        sb.append(String.format("      LET %s_col = ( FOR coll IN COLLECTIONS()\n", currentAlias));
        sb.append(String.format("         FILTER IS_SAME_COLLECTION(coll.name, %s_%s)\n", currentAlias, DOC_POSTFIX));
        sb.append("         RETURN { \"schema\": coll.name,  \n");
        sb.append("                  \"properties\": [  \n");
    }

    @Override
    public void endReturnStructure() {
        if(isRoot()){
            sb.append(String.format("null ] } )\n   RETURN %s_col )\n", currentAlias));
            sb.append("RETURN (FOR e IN FLATTEN(docs)\n");
            sb.append(" COLLECT schema = e.schema INTO grp\n");
            sb.append(" RETURN {\n");
            sb.append("        \"schema\": schema,\n");
            sb.append("        \"properties\": (FOR p IN FLATTEN(grp[*].e.properties[*])\n");
            sb.append("        RETURN DISTINCT p\n");
            sb.append("       )\n");
            sb.append("    })\n");
        }
        else {
            sb.append(String.format("null ] } )\n   RETURN APPEND(%s_rel, %s_col)\n", currentAlias, currentAlias));
        }
    }

    @Override
    protected void doLeaveTraversal() {
        sb.append(")\n");
    }

    @Override
    public void buildGrouping(String groupedInstancesLabel, List<String> groupingFields, List<String> nonGroupingFields) {

    }

    @Override
    public ArangoReflectQueryBuilder addRoot(String rootCollection) throws JSONException {
        sb.append(String.format("LET docs= (FOR %s_%s IN `%s`\n", ROOT_ALIAS, DOC_POSTFIX, rootCollection));
        addOrganizationFilter();
        return this;
    }


    @Override
    public void addLimit() {

    }

    @Override
    public void addTraversalResultField(String targetName, String alias) {
        sb.append("FLATTEN(").append(alias).append("),\n");
    }

    @Override
    public void addSortByLeafField(Set<String> fields) {

    }

    @Override
    public void ensureOrder() {

    }

    @Override
    public void addComplexLeafResultField(String targetName, String leaf_field) {
        sb.append('\"').append(leaf_field).append("\",\n");
    }

    @Override
    public void addSimpleLeafResultField(String leaf_field) {
        sb.append(String.format("      LET %s_col = ( FOR coll IN COLLECTIONS()\n", currentAlias));
        sb.append(String.format("         FILTER IS_SAME_COLLECTION(coll.name, %s_%s)\n", currentAlias, DOC_POSTFIX));
        sb.append("         RETURN { \"schema\": coll.name,  \n");
        sb.append("                  \"properties\": [\"").append(leaf_field).append("\"\n");
        sb.append(String.format("] } )\n   RETURN APPEND(%s_rel, %s_col)\n", currentAlias, currentAlias));
    }

    @Override
    public void addMerge(String leaf_field, Set<String> merged_fields, boolean sorted) {

    }
}
