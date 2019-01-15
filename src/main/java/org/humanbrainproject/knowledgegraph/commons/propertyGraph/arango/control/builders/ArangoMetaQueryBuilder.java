package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.TrustedAqlValue;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.UnauthorizedArangoQuery;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.query.boundary.ArangoQuery;
import org.humanbrainproject.knowledgegraph.query.entity.GraphQueryKeys;
import org.humanbrainproject.knowledgegraph.query.entity.SpecField;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ToBeTested
public class ArangoMetaQueryBuilder extends AbstractArangoQueryBuilder {


    public ArangoMetaQueryBuilder(Specification specification) {
        super(specification, null, null, null,null, null, null);
    }

    @Override
    protected void doEnterTraversal(ArangoAlias targetField, int numberOfTraversals, boolean reverse, ArangoCollectionReference relationCollection, boolean hasGroup, boolean ensureOrder) {
        q.addLine("// **********************************************************");
        q.addLine("// Start do enter traversal");
        q.addLine("// **********************************************************");
        createCol(currentAlias, targetField, numberOfTraversals, reverse, relationCollection, hasGroup, ensureOrder);

        q.addLine("// **********************************************************");
        q.addLine("// End do enter traversal");
        q.addLine("// **********************************************************");
    }

    protected void createCol(ArangoAlias field, ArangoAlias targetField, int numberOfTraversals, boolean reverse, ArangoCollectionReference relationCollection, boolean hasGroup, boolean ensureOrder) {
        q.addLine(new UnauthorizedArangoQuery().
                addLine("LET ${targetField}_col = (").
                indent().addLine("FOR ${targetField}_${docPostfix} IN ${previousAlias}_${docPostfix}.`${graphQueryField}`").
                indent().addLine("FILTER ${targetField}_${docPostfix}.`${graphQueryFieldName}`.`"+JsonLdConsts.ID+"` == \"${currentField}\"").
                addLine("LET ${targetField}_att = MERGE(").
                indent().addLine("FOR attr IN ATTRIBUTES(${targetField}_${docPostfix})").
                addLine("FILTER attr NOT IN internal_fields").
                addLine("RETURN {[attr]: ${targetField}_${docPostfix}[attr]}").
                outdent().addLine(")").
                outdent().outdent().
                setParameter("targetField", targetField.getArangoName()).
                setParameter("docPostfix", DOC_POSTFIX).
                setParameter("previousAlias", previousAlias.size()>0 ? previousAlias.peek().getArangoName() : ROOT_ALIAS.getArangoName()).
                setParameter("graphQueryField", GraphQueryKeys.GRAPH_QUERY_FIELDS.getFieldName()).
                setParameter("graphQueryFieldName", GraphQueryKeys.GRAPH_QUERY_FIELDNAME.getFieldName()).
                setParameter("currentField", currentField.fieldName).
                build().getValue());
    }

    @Override
    public void addTraversal(boolean reverse, ArangoCollectionReference relationCollection, int traversalDepth) {

    }

    @Override
    public void addComplexFieldRequiredFilter(ArangoAlias leafField) {
    }

    @Override
    public void addTraversalFieldRequiredFilter(ArangoAlias field) {

    }

    @Override
    public void leaveAdditionalTraversal(boolean reverse, ArangoCollectionReference relationCollection, int traversalDepth, boolean leaf) {

    }

    @Override
    protected void doStartReturnStructure(boolean simple) {

        q.addLine("// **********************************************************");
        q.addLine("// Start start return structure");
        q.addLine("// **********************************************************");
        UnauthorizedArangoQuery subQuery = new UnauthorizedArangoQuery();
        if (!isRoot()) {
            subQuery.addLine("LET ${alias}_result = FLATTEN([${alias}_att");
        } else {
            subQuery.addLine("LET ${rootAlias}_result = FLATTEN([${alias}_col");
        }
        subQuery.setParameter("alias", currentAlias.getArangoName());
        subQuery.setParameter("rootAlias", ROOT_ALIAS.getArangoName());
        q.addLine(subQuery.build().getValue());

        q.addLine("// **********************************************************");
        q.addLine("// End start return structure");
        q.addLine("// **********************************************************");
    }

    @Override
    public void endReturnStructure() {

        q.addLine("// **********************************************************");
        q.addLine("// Start end return structure");
        q.addLine("// **********************************************************");

        UnauthorizedArangoQuery subQuery = new UnauthorizedArangoQuery();
        if (!isRoot()) {
            subQuery.addLine("])\n          RETURN { \"${originalName}\": MERGE(${alias}_result)}");
        } else {
            subQuery.addLine("])\n RETURN MERGE(${alias}_result)");
        }
        subQuery.setParameter("originalName", currentAlias.getOriginalName());
        subQuery.setParameter("alias", currentAlias.getArangoName());
        q.addLine(subQuery.build().getValue());
        q.addLine("// **********************************************************");
        q.addLine("// Start end return structure");
        q.addLine("// **********************************************************");
    }

    @Override
    protected void doLeaveTraversal() {
        q.addLine(")");
    }

    @Override
    public void buildGrouping(String groupedInstancesLabel, List<ArangoAlias> groupingFields, List<ArangoAlias> nonGroupingFields) {

        q.addLine("// **********************************************************");
        q.addLine("// Start build grouping");
        q.addLine("// **********************************************************");

        UnauthorizedArangoQuery subQuery = new UnauthorizedArangoQuery();

        subQuery.addLine("LET ${alias}_grp = { \"${originalName}\": MERGE(FLATTEN([(FOR  grp IN ${alias}_col");
        subQuery.addLine("COLLECT");

        for (ArangoAlias groupingField : groupingFields) {
            UnauthorizedArangoQuery group = new UnauthorizedArangoQuery();
            group.addLine("`${alias}` = grp.`${currentField}`.`${originalName}`");
            group.setParameter("alias", groupingField.getArangoName());
            group.setParameter("currentField", currentField.fieldName);
            group.setParameter("originalName", groupingField.getOriginalName());
            subQuery.addLine(group.build().getValue());
            if(groupingField != groupingFields.get(groupingFields.size()-1)){
                subQuery.addLine(",");
            }
        }
        subQuery.addLine("INTO group");
        subQuery.addLine("LET instances = ( FOR el IN group RETURN {\n");
        for (ArangoAlias nonGroupingField : nonGroupingFields) {
            UnauthorizedArangoQuery ungroup = new UnauthorizedArangoQuery();
            ungroup.addLine("\"${originalName}\": el.grp.`${currentField}`.`${originalName}`");
            ungroup.setParameter("originalName", nonGroupingField.getOriginalName());
            ungroup.setParameter("currentField", currentField.fieldName);
            subQuery.addLine(ungroup.build().getValue());
            if(nonGroupingField != nonGroupingFields.get(nonGroupingFields.size()-1)){
                subQuery.addLine(",");
            }
        }
        subQuery.addLine("} )");
        subQuery.addLine("RETURN {");

        for (ArangoAlias groupingField : groupingFields) {
            UnauthorizedArangoQuery returnGroup = new UnauthorizedArangoQuery();
            returnGroup.addLine("\"${originalName}\": `${alias}`");
            returnGroup.setParameter("originalName", groupingField.getOriginalName());
            returnGroup.setParameter("alias", groupingField.getArangoName());
            subQuery.addLine(returnGroup.build().getValue());
            if(groupingField != groupingFields.get(groupingFields.size()-1)){
                subQuery.addLine(",");
            }
        }
        subQuery.addLine(", \"${groupInstancesLabel}\": instances");
        subQuery.addLine("} ),");

        subQuery.addLine("(FOR el IN ${alias}_col");
        subQuery.addLine("LET filtered = MERGE(FOR att IN ATTRIBUTES(el.`${currentField}`)");
        subQuery.addLine("FILTER att NOT IN [ ${groupedFields} ]");

        subQuery.addLine("RETURN {[att]: el.`${currentField}`[att]}");
        subQuery.addLine(") RETURN filtered ) ");
        subQuery.addLine("]))}");

        subQuery.setParameter("alias", currentAlias.getArangoName());
        subQuery.setParameter("originalName", currentAlias.getOriginalName());
        subQuery.setParameter("groupInstancesLabel", groupedInstancesLabel);
        subQuery.setParameter("currentField", currentField.fieldName);
        Set<String> allGroupedFieldNames = Stream.concat(groupingFields.stream(), nonGroupingFields.stream()).map(ArangoAlias::getArangoName).collect(Collectors.toSet());
        subQuery.setTrustedParameter("groupedFields", subQuery.listValues(allGroupedFieldNames));

        q.addLine(subQuery.build().getValue());
        q.addLine("// **********************************************************");
        q.addLine("// End start return structure");
        q.addLine("// **********************************************************");
    }

    @Override
    public ArangoMetaQueryBuilder addRoot(ArangoCollectionReference rootCollection) throws JSONException {

        q.addLine("// **********************************************************\n");
        q.addLine("// Start add root");
        q.addLine("// **********************************************************\n");
        UnauthorizedArangoQuery subQuery = new UnauthorizedArangoQuery();

        if (specification.getSpecificationId() == null) {
            subQuery.addLine("LET ${rootAlias}_${docPostfix} = ${originalDocument}");
        } else {
            subQuery.addLine("LET ${rootAlias}_${docPostfix} = DOCUMENT(\"${specificationQueries}/${specificationId}\")");
        }
        subQuery.setParameter("rootAlias", ROOT_ALIAS.getArangoName());
        subQuery.setParameter("docPostfix", DOC_POSTFIX);
        //TODO is it safe to transform the json object into a trusted value directly or do we need further injection checks?
        subQuery.setTrustedParameter("originalDocument", new TrustedAqlValue(new JsonTransformer().getMapAsJson(specification.originalDocument)));
        subQuery.setParameter("specificationQueries", ArangoQuery.SPECIFICATION_QUERIES.getName());
        subQuery.setParameter("specificationId", specification.getSpecificationId());
        q.addLine(subQuery.build().getValue());
        addOrganizationFilter();

        UnauthorizedArangoQuery subQuery2 = new UnauthorizedArangoQuery();

        subQuery2.addLine("LET internal_fields = [${internalFields}]");
        subQuery2.addLine("LET ${rootAlias}_col = {\"${querySpecification}\": MERGE(FOR attr IN ATTRIBUTES(${rootAlias}_${docPostfix}, true)");
        subQuery2.addLine("FILTER attr NOT IN [\""+JsonLdConsts.CONTEXT+"\"] && attr NOT IN internal_fields");
        subQuery2.addLine("RETURN {[attr]: ${rootAlias}_${docPostfix}[attr]})}");
        subQuery2.setTrustedParameter("internalFields", subQuery2.listValues(Arrays.stream(GraphQueryKeys.values()).map(GraphQueryKeys::getFieldName).collect(Collectors.toSet())));
        subQuery2.setParameter("rootAlias", ROOT_ALIAS.getArangoName());
        subQuery2.setParameter("docPostfix", DOC_POSTFIX);
        subQuery2.setParameter("querySpecification", GraphQueryKeys.GRAPH_QUERY_SPECIFICATION.getFieldName());
        q.addLine(subQuery2.build().getValue());

        q.addLine("// **********************************************************\n");
        q.addLine("// End add root");
        q.addLine("// **********************************************************\n");
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
        q.addLine("// **********************************************************");
        q.addLine("// Start addTraversalResultField");
        q.addLine("// **********************************************************\n");
        q.addLine(new UnauthorizedArangoQuery().
                addLine(", ${alias}_${postfix}").
                setParameter("alias", alias.getArangoName()).
                setParameter("postfix", currentField.hasNestedGrouping() ? "grp" : "col").
                build().getValue());
        q.addLine("// **********************************************************\n");
        q.addLine("// End addTraversalResultField\n");
        q.addLine("// **********************************************************\n");
    }

    @Override
    public void addSortByLeafField(Set<ArangoAlias> fields) {

    }


    @Override
    public void ensureOrder() {

    }

    @Override
    public void addComplexLeafResultField(String targetName, ArangoAlias leafField) {
        q.addLine("// **********************************************************");
        q.addLine("// Start complex result field");
        q.addLine("// **********************************************************");
        UnauthorizedArangoQuery subQuery = new UnauthorizedArangoQuery();
        subQuery.addLine(",");
        subQuery.addLine("[{\"${targetName}\": MERGE( FOR `${currentField}_${docPostfix}` IN ${currentAlias}_${docPostfix}.`${graphQueryField}`");
        subQuery.addLine("FILTER `${currentField}_${docPostfix}`.`${graphQueryFieldName}`.`"+ JsonLdConsts.ID+"` == \"${currentField}\"");
        subQuery.addLine("RETURN MERGE (");
        subQuery.addLine("FOR attr IN ATTRIBUTES(`${currentField}_${docPostfix}`)");
        subQuery.addLine("FILTER attr NOT IN internal_fields");
        subQuery.addLine("RETURN {[attr]: `${currentField}_${docPostfix}`[attr]}");
        subQuery.addLine("))}]");
        subQuery.setParameter("targetName", targetName);
        subQuery.setParameter("currentField", currentField.fieldName);
        subQuery.setParameter("docPostfix", DOC_POSTFIX);
        subQuery.setParameter("currentAlias", currentAlias.getArangoName());
        subQuery.setParameter("graphQueryField", GraphQueryKeys.GRAPH_QUERY_FIELDS.getFieldName());
        subQuery.setParameter("graphQueryFieldName", GraphQueryKeys.GRAPH_QUERY_FIELDNAME.getFieldName());
        q.addLine(subQuery.build().getValue());
        q.addLine("// **********************************************************");
        q.addLine("// End complex result field");
        q.addLine("// **********************************************************");

        //sb.append(String.format(", [{\"%s\": %s_result}]\n", currentField.fieldName, currentAlias));
    }

    @Override
    public void addSimpleLeafResultField(ArangoAlias leafField) {
        doAddSimpleLeafResultField(ArangoAlias.fromSpecField(currentField), currentAlias);
    }

    private void doAddSimpleLeafResultField(ArangoAlias leafField, ArangoAlias alias) {
        q.addLine("// **********************************************************");
        q.addLine("// Start addSimpleLeafResultField\n");
        q.addLine("// **********************************************************");
        q.addLine(new UnauthorizedArangoQuery().addLine("RETURN {\"${originalName}\": ${alias}_att}").
                setParameter("originalName", leafField.getOriginalName()).
                setParameter("alias", alias.getArangoName()).build().getValue());
        q.addLine("// **********************************************************");
        q.addLine("// End addSimpleLeafResultField");
        q.addLine("// **********************************************************");
    }

    @Override
    public void addMerge(ArangoAlias leafField, Set<ArangoAlias> mergeFields, boolean sorted) {
        q.addLine("// **********************************************************");
        q.addLine("// Start addMerge\n");
        q.addLine("// **********************************************************");
        createCol(ArangoAlias.fromSpecField(currentField), leafField, 1, false, null, false, false);
        doAddSimpleLeafResultField(ArangoAlias.fromSpecField(currentField), leafField);
        doLeaveTraversal();
        q.addLine("// **********************************************************");
        q.addLine("// End addMerge");
        q.addLine("// **********************************************************");
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
    public void prepareLeafField(SpecField leafField) {

    }
}
