package org.humanbrainproject.knowledgegraph.control.arango.query;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.boundary.query.ArangoQuery;
import org.humanbrainproject.knowledgegraph.control.GraphQueryKeys;
import org.humanbrainproject.knowledgegraph.control.arango.ArangoNamingConvention;
import org.humanbrainproject.knowledgegraph.entity.specification.Specification;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArangoMetaQueryBuilder extends AbstractQueryBuilder {

    //TODO inject
    ArangoNamingConvention namingConvention = new ArangoNamingConvention();

    public ArangoMetaQueryBuilder(Specification specification) {
        super(specification, null, null, null,null, null, null);
    }

    @Override
    protected void doEnterTraversal(String targetName, int numberOfTraversals, boolean reverse, String relationCollection, boolean hasGroup, boolean ensureOrder) {
        createCol(currentAlias, targetName, numberOfTraversals, reverse, relationCollection, hasGroup, ensureOrder);
    }

    protected void createCol(String fieldName, String targetName, int numberOfTraversals, boolean reverse, String relationCollection, boolean hasGroup, boolean ensureOrder) {
        sb.append(String.format("      LET %s_col = ( FOR %s_%s IN %s_%s.`%s`\n", targetName, targetName, DOC_POSTFIX, previousAlias.size()>0 ? previousAlias.peek() : ROOT_ALIAS, DOC_POSTFIX, GraphQueryKeys.GRAPH_QUERY_FIELDS.getFieldName()));
        sb.append(String.format("          FILTER %s_%s.`%s`.`@id`== \"%s\"\n", targetName, DOC_POSTFIX, GraphQueryKeys.GRAPH_QUERY_FIELDNAME.getFieldName(), currentField.fieldName));
        sb.append(String.format("          LET %s_att = MERGE(\n", targetName));
        sb.append(String.format("               FOR attr IN ATTRIBUTES(%s_%s)\n", targetName, DOC_POSTFIX));
        sb.append("               FILTER attr NOT IN internal_fields\n");
        sb.append(String.format("               RETURN {[attr]: %s_%s[attr]}\n", targetName, DOC_POSTFIX));
        sb.append("               )\n");
    }

    private String createInternalFieldFilter(){
        return String.join(", ", Arrays.stream(GraphQueryKeys.values()).map(k -> {
            String fieldName = this.bindVariables.put("internalFieldFilter", k.getFieldName(), null);
            return String.format("@%s", fieldName);
        }).collect(Collectors.toList()));
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
        if (!isRoot()) {
            sb.append(String.format("LET %s_result = FLATTEN([%s_att\n", currentAlias, currentAlias));
        } else {
            sb.append(String.format("LET %s_result = FLATTEN([%s_col\n", ROOT_ALIAS, currentAlias));
        }
    }

    @Override
    public void endReturnStructure() {
        if (!isRoot()) {
            String fieldName = this.bindVariables.put("fieldName", currentField.fieldName, null);
            sb.append(String.format("])\n          RETURN { @%s: MERGE(%s_result)}\n", fieldName, currentAlias));
        } else {
            sb.append(String.format("])\nRETURN MERGE(%s_result)", currentAlias));
        }
    }

    @Override
    protected void doLeaveTraversal() {
        sb.append(")\n\n");
    }

    @Override
    public void buildGrouping(String groupedInstancesLabel, List<String> groupingFields, List<String> nonGroupingFields) {
        String fieldName = this.bindVariables.put("fieldName", currentField.fieldName, null);
        sb.append(String.format("LET %s_grp = { @%s: MERGE(FLATTEN([(FOR  grp IN %s_col\n", currentAlias, fieldName, currentAlias));
        sb.append("COLLECT ");
        List<String> groupings = groupingFields.stream().map(f -> {
            String groupName = this.bindVariables.put("groupingField", f, null);
            String generatedName = this.generateDocName();
            return String.format("`%s` = grp.@%s.@%s", generatedName , fieldName, groupName);
        }).collect(Collectors.toList());
        sb.append(String.join(", ", groupings));
        sb.append(" INTO group\n");
        sb.append( "LET instances = ( FOR el IN group RETURN {\n");
        List<String> nonGrouping = nonGroupingFields.stream().map(s -> {
            String nonGroupName = this.bindVariables.put("nonGroupingField", s, null);
            return String.format("@%s: el.grp.@%s.@%s", nonGroupName,fieldName, nonGroupName);
        }).collect(Collectors.toList());
        sb.append(String.join(",\n", nonGrouping));
        sb.append("\n} )\n");
        sb.append("RETURN {\n");

        List<String> returnGrouped = groupingFields.stream().map(f -> {
            String groupName = this.bindVariables.put("groupingField", f, null);
            return String.format("@%s: @%s", groupName, groupName);
        }).collect(Collectors.toList());
        sb.append(String.join(",\n", returnGrouped));
        String instancesLabel = this.bindVariables.put("instancesLabel", groupedInstancesLabel, null);
        sb.append(String.format(",\n @%s: instances\n", instancesLabel));
        sb.append("} ),\n");

        sb.append(String.format(" (FOR el IN %s_col\n", currentAlias));
        sb.append(String.format(" LET filtered = MERGE(FOR att IN ATTRIBUTES(el.@%s)\n", fieldName));
        List<String> allgrouped = Stream.concat(groupingFields.stream(), nonGroupingFields.stream()).map(s-> {
            String allgroupName = this.bindVariables.put("allGrouped", s, null);
            return String.format("@%s", allgroupName);
        }).collect(Collectors.toList());
        sb.append("        FILTER att NOT IN [");
        sb.append(String.join(", ", allgrouped));
        sb.append("]\n")  ;
        sb.append(String.format("RETURN {[att]: el.@%s[att]}\n", fieldName));
        sb.append(")\n RETURN filtered\n ) \n");
        sb.append("]))}\n\n");
    }

    @Override
    public ArangoMetaQueryBuilder addRoot(String rootCollection) throws JSONException {
        if (specification.getSpecificationId() == null) {
            sb.append(String.format("LET %s_%s = %s\n", ROOT_ALIAS, DOC_POSTFIX, specification.originalDocument));
        } else {
            sb.append(String.format("LET %s_%s = DOCUMENT(\"%s/%s\"))\n", ROOT_ALIAS, DOC_POSTFIX, ArangoQuery.SPECIFICATION_QUERIES, specification.getSpecificationId()));
        }
        addOrganizationFilter();

        sb.append(String.format("\n\nLET internal_fields = [%s]\n\n", createInternalFieldFilter()));


        sb.append(String.format("\n\nLET %s_col = {\"%s\": MERGE(FOR attr IN ATTRIBUTES(%s_%s, true) ", ROOT_ALIAS, GraphQueryKeys.GRAPH_QUERY_SPECIFICATION.getFieldName(), ROOT_ALIAS, DOC_POSTFIX));
        sb.append(String.format("  FILTER attr NOT IN [\"%s\"] && attr NOT IN internal_fields \n", JsonLdConsts.CONTEXT));
        sb.append(String.format("RETURN {[attr]: %s_%s[attr]} )}\n\n", ROOT_ALIAS, DOC_POSTFIX));
        return this;
    }

    @Override
    public void nullFilter() {

    }

    @Override
    public void addLimit() {

    }

    @Override
    public void addTraversalResultField(String targetName, String alias) {
        sb.append(String.format(",\n %s_%s", alias, currentField.hasNestedGrouping() ? "grp" : "col"));
    }

    @Override
    public void addSortByLeafField(Set<String> fields) {

    }

    @Override
    public void ensureOrder() {

    }

    @Override
    public void addComplexLeafResultField(String targetName, String leaf_field) {
        String currentFieldName = this.bindVariables.put("currentField",  currentField.fieldName, null);
        String docName = generateDocName();
        sb.append(String.format(",\n[{@%s: MERGE( FOR `%s_%s` IN %s_%s.`%s`\n",currentFieldName, docName, DOC_POSTFIX, currentAlias, DOC_POSTFIX, GraphQueryKeys.GRAPH_QUERY_FIELDS.getFieldName()));
        sb.append(String.format("          FILTER `%s_%s`.`%s`.`@id`== @%s\n", docName, DOC_POSTFIX, GraphQueryKeys.GRAPH_QUERY_FIELDNAME.getFieldName(), currentFieldName));
        sb.append("          RETURN MERGE(\n");
        sb.append(String.format("               FOR attr IN ATTRIBUTES(`%s_%s`)\n",docName, DOC_POSTFIX));
        sb.append("               FILTER attr NOT IN internal_fields\n");
        sb.append(String.format("               RETURN {[attr]: `%s_%s`[attr]}\n",docName, DOC_POSTFIX));
        sb.append("               ))}]\n");


        //sb.append(String.format(", [{\"%s\": %s_result}]\n", currentField.fieldName, currentAlias));
    }

    @Override
    public void addSimpleLeafResultField(String leaf_field) {
        doAddSimpleLeafResultField(currentField.fieldName, currentAlias);
    }

    private void doAddSimpleLeafResultField(String leaf_field, String alias) {
        String leafField = this.bindVariables.put("leafField", leaf_field, null);
        sb.append(String.format("RETURN {@%s: %s_att}\n",leafField, alias));
    }

    @Override
    public void addMerge(String leaf_field, Set<String> merged_fields, boolean sorted) {
        createCol(currentField.fieldName, leaf_field, 1, false, null, false, false);
        doAddSimpleLeafResultField(currentField.fieldName, leaf_field);
        doLeaveTraversal();
    }

    @Override
    public void addOrganizationFilter() {

    }

    @Override
    public void addInstanceIdFilter() {

    }

    @Override
    public void addSearchQuery(){

    }

    @Override
    protected String escapeVariableName(String s) {
        return super.escapeVariableName(s);
    }

    @Override
    protected String generateDocName() {
        return super.generateDocName();
    }
}
