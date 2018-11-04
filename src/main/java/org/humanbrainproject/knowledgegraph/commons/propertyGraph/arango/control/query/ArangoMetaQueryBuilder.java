package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query;

import com.github.jsonldjava.core.JsonLdConsts;
import deprecated.control.GraphQueryKeys;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArangoMetaQueryBuilder extends AbstractArangoQueryBuilder {


    public ArangoMetaQueryBuilder(Specification specification) {
        super(specification, null, null, null,null, null, null);
    }

    @Override
    protected void doEnterTraversal(ArangoAlias targetField, int numberOfTraversals, boolean reverse, ArangoCollectionReference relationCollection, boolean hasGroup, boolean ensureOrder) {
        createCol(currentAlias, targetField, numberOfTraversals, reverse, relationCollection, hasGroup, ensureOrder);
    }

    protected void createCol(ArangoAlias field, ArangoAlias targetField, int numberOfTraversals, boolean reverse, ArangoCollectionReference relationCollection, boolean hasGroup, boolean ensureOrder) {
        sb.append(String.format("      LET %s_col = ( FOR %s_%s IN %s_%s.`%s`\n", targetField.getName(), targetField.getName(), DOC_POSTFIX, previousAlias.size()>0 ? previousAlias.peek().getName() : ROOT_ALIAS.getName(), DOC_POSTFIX, GraphQueryKeys.GRAPH_QUERY_FIELDS.getFieldName()));
        sb.append(String.format("          FILTER %s_%s.`%s`.`@id`== \"%s\"\n", targetField.getName(), DOC_POSTFIX, GraphQueryKeys.GRAPH_QUERY_FIELDNAME.getFieldName(), currentField.fieldName));
        sb.append(String.format("          LET %s_att = MERGE(\n", targetField.getName()));
        sb.append(String.format("               FOR attr IN ATTRIBUTES(%s_%s)\n", targetField.getName(), DOC_POSTFIX));
        sb.append("               FILTER attr NOT IN internal_fields\n");
        sb.append(String.format("               RETURN {[attr]: %s_%s[attr]}\n", targetField.getName(), DOC_POSTFIX));
        sb.append("               )\n");
    }

    private String createInternalFieldFilter(){
        return String.join(", ", Arrays.stream(GraphQueryKeys.values()).map(k -> String.format("\"%s\"", k.getFieldName())).collect(Collectors.toList()));
    }

    @Override
    public void addTraversal(boolean reverse, ArangoCollectionReference relationCollection) {

    }

    @Override
    public void addComplexFieldRequiredFilter(ArangoAlias leafField) {
    }

    @Override
    public void addTraversalFieldRequiredFilter(ArangoAlias field) {

    }

    @Override
    protected void doStartReturnStructure(boolean simple) {
        if (!isRoot()) {
            sb.append(String.format("LET %s_result = FLATTEN([%s_att\n", currentAlias.getName(), currentAlias.getName()));
        } else {
            sb.append(String.format("LET %s_result = FLATTEN([%s_col\n", ROOT_ALIAS.getName(), currentAlias.getName()));
        }
    }

    @Override
    public void endReturnStructure() {
        if (!isRoot()) {
            sb.append(String.format("])\n          RETURN { \"%s\": MERGE(%s_result)}\n", currentField.fieldName, currentAlias.getName()));
        } else {
            sb.append(String.format("])\nRETURN MERGE(%s_result)", currentAlias.getName()));
        }
    }

    @Override
    protected void doLeaveTraversal() {
        sb.append(")\n\n");
    }

    @Override
    public void buildGrouping(String groupedInstancesLabel, List<ArangoAlias> groupingFields, List<ArangoAlias> nonGroupingFields) {
        sb.append(String.format("LET %s_grp = { \"%s\": MERGE(FLATTEN([(FOR  grp IN %s_col\n", currentAlias.getName(), currentField.fieldName, currentAlias.getName()));
        sb.append("COLLECT ");
        List<String> groupings = groupingFields.stream().map(f -> String.format("`%s` = grp.`%s`.`%s`", f, currentField.fieldName, f)).collect(Collectors.toList());
        sb.append(String.join(", ", groupings));
        sb.append(" INTO group\n");
        sb.append( "LET instances = ( FOR el IN group RETURN {\n");
        List<String> nonGrouping = nonGroupingFields.stream().map(s -> String.format("\"%s\": el.grp.`%s`.`%s`", s, currentField.fieldName, s)).collect(Collectors.toList());
        sb.append(String.join(",\n", nonGrouping));
        sb.append("\n} )\n");
        sb.append("RETURN {\n");

        List<String> returnGrouped = groupingFields.stream().map(f -> String.format("\"%s\": `%s`", f, f)).collect(Collectors.toList());
        sb.append(String.join(",\n", returnGrouped));
        sb.append(String.format(",\n \"%s\": instances\n", groupedInstancesLabel));
        sb.append("} ),\n");

        sb.append(String.format(" (FOR el IN %s_col\n", currentAlias.getName()));
        sb.append(String.format(" LET filtered = MERGE(FOR att IN ATTRIBUTES(el.`%s`)\n", currentField.fieldName));
        List<String> allgrouped = Stream.concat(groupingFields.stream(), nonGroupingFields.stream()).map(s-> String.format("\"%s\"", s)).collect(Collectors.toList());
        sb.append("        FILTER att NOT IN [");
        sb.append(String.join(", ", allgrouped));
        sb.append("]\n")  ;
        sb.append(String.format("RETURN {[att]: el.`%s`[att]}\n", currentField.fieldName));
        sb.append(")\n RETURN filtered\n ) \n");
        sb.append("]))}\n\n");
    }

    @Override
    public ArangoMetaQueryBuilder addRoot(ArangoCollectionReference rootCollection) throws JSONException {
        if (specification.getSpecificationId() == null) {
            sb.append(String.format("LET %s_%s = %s\n", ROOT_ALIAS.getName(), DOC_POSTFIX, specification.originalDocument));
        } else {
            sb.append(String.format("LET %s_%s = DOCUMENT(\"%s/%s\")\n", ROOT_ALIAS.getName(), DOC_POSTFIX, ArangoQuery.SPECIFICATION_QUERIES, specification.getSpecificationId()));
        }
        addOrganizationFilter();

        sb.append(String.format("\n\nLET internal_fields = [%s]\n\n", createInternalFieldFilter()));


        sb.append(String.format("\n\nLET %s_col = {\"%s\": MERGE(FOR attr IN ATTRIBUTES(%s_%s, true) ", ROOT_ALIAS.getName(), GraphQueryKeys.GRAPH_QUERY_SPECIFICATION.getFieldName(), ROOT_ALIAS.getName(), DOC_POSTFIX));
        sb.append(String.format("  FILTER attr NOT IN [\"%s\"] && attr NOT IN internal_fields \n", JsonLdConsts.CONTEXT));
        sb.append(String.format("RETURN {[attr]: %s_%s[attr]} )}\n\n", ROOT_ALIAS.getName(), DOC_POSTFIX));
        return this;
    }

    @Override
    public void nullFilter() {

    }

    @Override
    public void addLimit() {

    }

    @Override
    public void addTraversalResultField(String targetName, ArangoAlias alias) {
        sb.append(String.format(",\n %s_%s", alias.getName(), currentField.hasNestedGrouping() ? "grp" : "col"));
    }

    @Override
    public void addSortByLeafField(Set<ArangoAlias> fields) {

    }

    @Override
    public void ensureOrder() {

    }

    @Override
    public void addComplexLeafResultField(String targetName, ArangoAlias leafField) {
        sb.append(String.format(",\n[{\"%s\": MERGE( FOR `%s_%s` IN %s_%s.`%s`\n", currentField.fieldName, currentField.fieldName, DOC_POSTFIX, currentAlias.getName(), DOC_POSTFIX, GraphQueryKeys.GRAPH_QUERY_FIELDS.getFieldName()));
        sb.append(String.format("          FILTER `%s_%s`.`%s`.`@id`== \"%s\"\n", currentField.fieldName, DOC_POSTFIX, GraphQueryKeys.GRAPH_QUERY_FIELDNAME.getFieldName(), currentField.fieldName));
        sb.append("          RETURN MERGE(\n");
        sb.append(String.format("               FOR attr IN ATTRIBUTES(`%s_%s`)\n", currentField.fieldName, DOC_POSTFIX));
        sb.append("               FILTER attr NOT IN internal_fields\n");
        sb.append(String.format("               RETURN {[attr]: `%s_%s`[attr]}\n", currentField.fieldName, DOC_POSTFIX));
        sb.append("               ))}]\n");


        //sb.append(String.format(", [{\"%s\": %s_result}]\n", currentField.fieldName, currentAlias));
    }

    @Override
    public void addSimpleLeafResultField(ArangoAlias leafField) {
        doAddSimpleLeafResultField(ArangoAlias.fromSpecField(currentField), currentAlias);
    }

    private void doAddSimpleLeafResultField(ArangoAlias leafField, ArangoAlias alias) {
        sb.append(String.format("RETURN {\"%s\": %s_att}\n", leafField.getName(), alias.getName()));
    }

    @Override
    public void addMerge(ArangoAlias leafField, Set<ArangoAlias> mergeFields, boolean sorted) {
        createCol(ArangoAlias.fromSpecField(currentField), leafField, 1, false, null, false, false);
        doAddSimpleLeafResultField(ArangoAlias.fromSpecField(currentField), leafField);
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
}
