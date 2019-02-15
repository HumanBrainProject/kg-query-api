package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.jsonld.control.JsonTransformer;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.TrustedAqlValue;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL;
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


import static org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL.*;

@ToBeTested
public class ArangoMetaQueryBuilder extends AbstractArangoQueryBuilder {


    public ArangoMetaQueryBuilder(Specification specification, Set<ArangoCollectionReference> existingArangoCollections) {
        super(specification, null, null, null,null, null, existingArangoCollections);
    }

    @Override
    protected void doEnterTraversal(ArangoAlias targetField, int numberOfTraversals, boolean reverse, ArangoCollectionReference relationCollection, boolean hasGroup, boolean ensureOrder) {
        q.addLine(trust("// **********************************************************"));
        q.addLine(trust("// Start do enter traversal"));
        q.addLine(trust("// **********************************************************"));
        createCol(currentAlias, targetField, numberOfTraversals, reverse, relationCollection, hasGroup, ensureOrder);

        q.addLine(trust("// **********************************************************"));
        q.addLine(trust("// End do enter traversal"));
        q.addLine(trust("// **********************************************************"));
    }

    protected void createCol(ArangoAlias field, ArangoAlias targetField, int numberOfTraversals, boolean reverse, ArangoCollectionReference relationCollection, boolean hasGroup, boolean ensureOrder) {
        q.addLine(new AQL().
                addLine(trust("LET ${targetField}_col = (")).
                indent().addLine(trust("FOR ${targetField}_${docPostfix} IN ${previousAlias}_${docPostfix}.`${graphQueryField}`")).
                indent().addLine(trust("FILTER ${targetField}_${docPostfix}.`${graphQueryFieldName}`.`"+JsonLdConsts.ID+"` == \"${currentField}\"")).
                addLine(trust("LET ${targetField}_att = MERGE(")).
                indent().addLine(trust("FOR attr IN ATTRIBUTES(${targetField}_${docPostfix})")).
                addLine(trust("FILTER attr NOT IN internal_fields")).
                addLine(trust("RETURN {[attr]: ${targetField}_${docPostfix}[attr]}")).
                outdent().addLine(trust(")")).
                outdent().outdent().
                setParameter("targetField", targetField.getArangoName()).
                setParameter("docPostfix", DOC_POSTFIX).
                setParameter("previousAlias", previousAlias.size()>0 ? previousAlias.peek().getArangoName() : ROOT_ALIAS.getArangoName()).
                setParameter("graphQueryField", GraphQueryKeys.GRAPH_QUERY_FIELDS.getFieldName()).
                setParameter("graphQueryFieldName", GraphQueryKeys.GRAPH_QUERY_FIELDNAME.getFieldName()).
                setParameter("currentField", currentField.fieldName).
                build());
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

        q.addLine(trust("// **********************************************************"));
        q.addLine(trust("// Start start return structure"));
        q.addLine(trust("// **********************************************************"));
        AQL subQuery = new AQL();
        if (!isRoot()) {
            subQuery.addLine(trust("LET ${alias}_result = FLATTEN([${alias}_att"));
        } else {
            subQuery.addLine(trust("LET ${rootAlias}_result = FLATTEN([${alias}_col"));
        }
        subQuery.setParameter("alias", currentAlias.getArangoName());
        subQuery.setParameter("rootAlias", ROOT_ALIAS.getArangoName());
        q.addLine(subQuery.build());

        q.addLine(trust("// **********************************************************"));
        q.addLine(trust("// End start return structure"));
        q.addLine(trust("// **********************************************************"));
    }

    @Override
    public void endReturnStructure() {

        q.addLine(trust("// **********************************************************"));
        q.addLine(trust("// Start end return structure"));
        q.addLine(trust("// **********************************************************"));

        AQL subQuery = new AQL();
        if (!isRoot()) {
            subQuery.addLine(trust("])\n          RETURN { \"${originalName}\": MERGE(${alias}_result)}"));
        } else {
            subQuery.addLine(trust("])\n RETURN MERGE(${alias}_result)"));
        }
        subQuery.setParameter("originalName", currentAlias.getOriginalName());
        subQuery.setParameter("alias", currentAlias.getArangoName());
        q.addLine(trust(subQuery.build().getValue()));
        q.addLine(trust("// **********************************************************"));
        q.addLine(trust("// Start end return structure"));
        q.addLine(trust("// **********************************************************"));
    }

    @Override
    protected void doLeaveTraversal() {
        q.addLine(trust(")"));
    }

    @Override
    public void buildGrouping(String groupedInstancesLabel, List<ArangoAlias> groupingFields, List<ArangoAlias> nonGroupingFields) {

        q.addLine(trust("// **********************************************************"));
        q.addLine(trust("// Start build grouping"));
        q.addLine(trust("// **********************************************************"));

        AQL subQuery = new AQL();

        subQuery.addLine(trust("LET ${alias}_grp = { \"${originalName}\": MERGE(FLATTEN([(FOR  grp IN ${alias}_col"));
        subQuery.addLine(trust("COLLECT"));

        for (ArangoAlias groupingField : groupingFields) {
            AQL group = new AQL();
            group.addLine(trust("`${alias}` = grp.`${currentField}`.`${originalName}`"));
            group.setParameter("alias", groupingField.getArangoName());
            group.setParameter("currentField", currentField.fieldName);
            group.setParameter("originalName", groupingField.getOriginalName());
            subQuery.addLine(group.build());
            if(groupingField != groupingFields.get(groupingFields.size()-1)){
                subQuery.addComma();
            }
        }
        subQuery.addLine(trust("INTO group"));
        subQuery.addLine(trust("LET instances = ( FOR el IN group RETURN {\n"));
        for (ArangoAlias nonGroupingField : nonGroupingFields) {
            AQL ungroup = new AQL();
            ungroup.addLine(trust("\"${originalName}\": el.grp.`${currentField}`.`${originalName}`"));
            ungroup.setParameter("originalName", nonGroupingField.getOriginalName());
            ungroup.setParameter("currentField", currentField.fieldName);
            subQuery.addLine(trust(ungroup.build().getValue()));
            if(nonGroupingField != nonGroupingFields.get(nonGroupingFields.size()-1)){
                subQuery.addComma();
            }
        }
        subQuery.addLine(trust("} )"));
        subQuery.addLine(trust("RETURN {"));

        for (ArangoAlias groupingField : groupingFields) {
            AQL returnGroup = new AQL();
            returnGroup.addLine(trust("\"${originalName}\": `${alias}`"));
            returnGroup.setParameter("originalName", groupingField.getOriginalName());
            returnGroup.setParameter("alias", groupingField.getArangoName());
            subQuery.addLine(trust(returnGroup.build().getValue()));
            if(groupingField != groupingFields.get(groupingFields.size()-1)){
                subQuery.addComma();
            }
        }
        subQuery.addLine(trust(", \"${groupInstancesLabel}\": instances"));
        subQuery.addLine(trust("} ),"));

        subQuery.addLine(trust("(FOR el IN ${alias}_col"));
        subQuery.addLine(trust("LET filtered = MERGE(FOR att IN ATTRIBUTES(el.`${currentField}`)"));
        subQuery.addLine(trust("FILTER att NOT IN [ ${groupedFields} ]"));

        subQuery.addLine(trust("RETURN {[att]: el.`${currentField}`[att]}"));
        subQuery.addLine(trust(") RETURN filtered ) "));
        subQuery.addLine(trust("]))}"));

        subQuery.setParameter("alias", currentAlias.getArangoName());
        subQuery.setParameter("originalName", currentAlias.getOriginalName());
        subQuery.setParameter("groupInstancesLabel", groupedInstancesLabel);
        subQuery.setParameter("currentField", currentField.fieldName);
        Set<String> allGroupedFieldNames = Stream.concat(groupingFields.stream(), nonGroupingFields.stream()).map(ArangoAlias::getArangoName).collect(Collectors.toSet());
        subQuery.setTrustedParameter("groupedFields", subQuery.listValues(allGroupedFieldNames));

        q.addLine(trust(subQuery.build().getValue()));
        q.addLine(trust("// **********************************************************"));
        q.addLine(trust("// End start return structure"));
        q.addLine(trust("// **********************************************************"));
    }

    @Override
    public ArangoMetaQueryBuilder addRoot(ArangoCollectionReference rootCollection) throws JSONException {

        q.addLine(trust("// **********************************************************\n"));
        q.addLine(trust("// Start add root"));
        q.addLine(trust("// **********************************************************\n"));
        AQL subQuery = new AQL();

        if (specification.getSpecificationId() == null) {
            subQuery.addLine(trust("LET ${rootAlias}_${docPostfix} = ${originalDocument}"));
        } else {
            subQuery.addLine(trust("LET ${rootAlias}_${docPostfix} = DOCUMENT(\"${specificationQueries}/${specificationId}\")"));
        }
        subQuery.setParameter("rootAlias", ROOT_ALIAS.getArangoName());
        subQuery.setParameter("docPostfix", DOC_POSTFIX);
        //TODO is it safe to transform the json object into a trusted value directly or do we need further injection checks?
        subQuery.setTrustedParameter("originalDocument", new TrustedAqlValue(new JsonTransformer().getMapAsJson(specification.originalDocument)));
        subQuery.setParameter("specificationQueries", ArangoQuery.SPECIFICATION_QUERIES.getName());
        subQuery.setParameter("specificationId", specification.getSpecificationId());
        q.addLine(subQuery.build());
        addOrganizationFilter();

        AQL subQuery2 = new AQL();

        subQuery2.addLine(trust("LET internal_fields = [${internalFields}]"));
        subQuery2.addLine(trust("LET ${rootAlias}_col = {\"${querySpecification}\": MERGE(FOR attr IN ATTRIBUTES(${rootAlias}_${docPostfix}, true)"));
        subQuery2.addLine(trust("FILTER attr NOT IN [\""+JsonLdConsts.CONTEXT+"\"] && attr NOT IN internal_fields"));
        subQuery2.addLine(trust("RETURN {[attr]: ${rootAlias}_${docPostfix}[attr]})}"));
        subQuery2.setTrustedParameter("internalFields", subQuery2.listValues(Arrays.stream(GraphQueryKeys.values()).map(GraphQueryKeys::getFieldName).collect(Collectors.toSet())));
        subQuery2.setParameter("rootAlias", ROOT_ALIAS.getArangoName());
        subQuery2.setParameter("docPostfix", DOC_POSTFIX);
        subQuery2.setParameter("querySpecification", GraphQueryKeys.GRAPH_QUERY_SPECIFICATION.getFieldName());
        q.addLine(subQuery2.build());

        q.addLine(trust("// **********************************************************\n"));
        q.addLine(trust("// End add root"));
        q.addLine(trust("// **********************************************************\n"));
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
        q.addLine(trust("// **********************************************************"));
        q.addLine(trust("// Start addTraversalResultField"));
        q.addLine(trust("// **********************************************************"));
        q.addLine(new AQL().
                addLine(trust(", ${alias}_${postfix}")).
                setParameter("alias", alias.getArangoName()).
                setParameter("postfix", currentField.hasNestedGrouping() ? "grp" : "col").
                build());
        q.addLine(trust("// **********************************************************"));
        q.addLine(trust("// End addTraversalResultField"));
        q.addLine(trust("// **********************************************************"));
    }

    @Override
    public void addSortByLeafField(Set<ArangoAlias> fields) {

    }


    @Override
    public void ensureOrder() {

    }

    @Override
    public void addComplexLeafResultField(String targetName, ArangoAlias leafField) {
        q.addLine(trust("// **********************************************************"));
        q.addLine(trust("// Start complex result field"));
        q.addLine(trust("// **********************************************************"));
        AQL subQuery = new AQL();
        subQuery.addLine(trust(","));
        subQuery.addLine(trust("[{\"${targetName}\": MERGE( FOR `${currentField}_${docPostfix}` IN ${currentAlias}_${docPostfix}.`${graphQueryField}`"));
        subQuery.addLine(trust("FILTER `${currentField}_${docPostfix}`.`${graphQueryFieldName}`.`"+ JsonLdConsts.ID+"` == \"${currentField}\""));
        subQuery.addLine(trust("RETURN MERGE ("));
        subQuery.addLine(trust("FOR attr IN ATTRIBUTES(`${currentField}_${docPostfix}`)"));
        subQuery.addLine(trust("FILTER attr NOT IN internal_fields"));
        subQuery.addLine(trust("RETURN {[attr]: `${currentField}_${docPostfix}`[attr]}"));
        subQuery.addLine(trust("))}]"));
        subQuery.setParameter("targetName", targetName);
        subQuery.setParameter("currentField", currentField.fieldName);
        subQuery.setParameter("docPostfix", DOC_POSTFIX);
        subQuery.setParameter("currentAlias", currentAlias.getArangoName());
        subQuery.setParameter("graphQueryField", GraphQueryKeys.GRAPH_QUERY_FIELDS.getFieldName());
        subQuery.setParameter("graphQueryFieldName", GraphQueryKeys.GRAPH_QUERY_FIELDNAME.getFieldName());
        q.addLine(subQuery.build());
        q.addLine(trust("// **********************************************************"));
        q.addLine(trust("// End complex result field"));
        q.addLine(trust("// **********************************************************"));

        //sb.append(String.format(", [{\"%s\": %s_result}]\n", currentField.fieldName, currentAlias));
    }

    @Override
    public void addSimpleLeafResultField(ArangoAlias leafField) {
        doAddSimpleLeafResultField(ArangoAlias.fromSpecField(currentField), currentAlias);
    }

    private void doAddSimpleLeafResultField(ArangoAlias leafField, ArangoAlias alias) {
        q.addLine(trust("// **********************************************************"));
        q.addLine(trust("// Start addSimpleLeafResultField\n"));
        q.addLine(trust("// **********************************************************"));
        q.addLine(new AQL().addLine(trust("RETURN {\"${originalName}\": ${alias}_att}")).
                setParameter("originalName", leafField.getOriginalName()).
                setParameter("alias", alias.getArangoName()).build());
        q.addLine(trust("// **********************************************************"));
        q.addLine(trust("// End addSimpleLeafResultField"));
        q.addLine(trust("// **********************************************************"));
    }

    @Override
    public void addMerge(ArangoAlias leafField, Set<ArangoAlias> mergeFields, boolean sorted) {
        q.addLine(trust("// **********************************************************"));
        q.addLine(trust("// Start addMerge\n"));
        q.addLine(trust("// **********************************************************"));
        createCol(ArangoAlias.fromSpecField(currentField), leafField, 1, false, null, false, false);
        doAddSimpleLeafResultField(ArangoAlias.fromSpecField(currentField), leafField);
        doLeaveTraversal();
        q.addLine(trust("// **********************************************************"));
        q.addLine(trust("// End addMerge"));
        q.addLine(trust("// **********************************************************"));
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
    public void addFieldFilter(ArangoAlias alias){

    }

    @Override
    public void prepareLeafField(SpecField leafField) {

    }
}
