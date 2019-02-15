package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AuthorizedArangoQuery;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.query.entity.SpecField;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;


import static org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AQL.*;
@ToBeTested
public class ArangoMetaReflectionQueryBuilder extends AbstractArangoQueryBuilder {

    private Stack<List<String>> aliasStack = new Stack<>();

    private final String nexusInstanceBase;

    private boolean hasReturned;

    private final Set<String> fields = new HashSet<>();


    @Override
    public void prepareLeafField(SpecField leafField) {
        q.addLine(trust("//Prepare " + leafField.fieldName));

        ArangoAlias leaf = ArangoAlias.fromOriginalFieldName(leafField.getLeafPath().pathName);
        AQL subQuery = new AQL();
        if (isLink(leaf)) {
            subQuery.addLine(trust("LET ${alias} = FLATTEN("));
            subQuery.addLine(trust("FOR ${alias}_doc IN 1..1 ${reverse} ${previousAliasDoc} `${relation}`"));
            subQuery.addLine(trust("LET ${alias}_url = ${alias}_doc.`" + HBPVocabulary.RELATIVE_URL_OF_INTERNAL_LINK + "`"));
            subQuery.addDocumentFilter(subQuery.preventAqlInjection(String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX)));
            subQuery.addLine(trust("RETURN DISTINCT {"));
            subQuery.addLine(trust("\"field\": \"${fieldName}\","));
            subQuery.addLine(trust("\"type\": LEFT(${alias}_url, FIND_LAST(${alias}_url, '/')),"));
            subQuery.addLine(trust("\"isLink\": true"));
            subQuery.addLine(trust("})"));
            subQuery.setParameter("reverse", leafField.getLeafPath().reverse ? "INBOUND" : "OUTBOUND");
            subQuery.setParameter("relation", ArangoCollectionReference.fromFieldName(leaf.getOriginalName()).getName());

        } else {
            subQuery.addLine(trust("LET ${alias}_val = ${previousAliasDoc}.`${leafFieldName}`"));
            subQuery.addLine(trust("LET ${alias} = ${alias}_val!=NULL && ${alias}_val!=[] && ${alias}_val!=\"\" ? [{"));
            subQuery.addLine(trust("\"field\": \"${fieldName}\","));
            subQuery.addLine(trust("\"type\": TYPENAME(${alias}_val),"));
            subQuery.addLine(trust("\"isLink\": false"));
            subQuery.addLine(trust("}] : []"));
            subQuery.setParameter("leafFieldName", leaf.getOriginalName());
        }
        subQuery.setParameter("alias", currentAlias.getArangoName());
        subQuery.setParameter("fieldName", currentAlias.getOriginalName());
        subQuery.setParameter("previousAliasDoc", String.format("%s_%s", previousAlias.isEmpty() ? ROOT_ALIAS : previousAlias.peek().getArangoName(), DOC_POSTFIX));
        fields.add(currentAlias.getArangoName());

        q.addLine(subQuery.build());
    }

    public ArangoMetaReflectionQueryBuilder(Specification specification, ArangoAlias permissionGroupFieldName, Set<String> whitelistOrganizations, Set<ArangoCollectionReference> existingArangoCollections, String nexusInstanceBase) {
        super(specification, null, null, permissionGroupFieldName, whitelistOrganizations, null, existingArangoCollections);
        this.nexusInstanceBase = nexusInstanceBase;
    }

    @Override
    public void addTraversal(boolean reverse, ArangoCollectionReference relationCollection, int traversalDepth) {

    }


    private void simpleReturn(List<String> alias) {
    }


    @Override
    public void leaveAdditionalTraversal(boolean reverse, ArangoCollectionReference relationCollection, int traversalDepth, boolean leaf) {

    }

    @Override
    public void addComplexFieldRequiredFilter(ArangoAlias leafField) {
    }

    @Override
    public void addTraversalFieldRequiredFilter(ArangoAlias alias) {
    }

    @Override
    protected void doEnterTraversal(ArangoAlias targetName, int numberOfTraversals, boolean reverse, ArangoCollectionReference relationCollection, boolean hasGroup, boolean ensureOrder) {

    }

    @Override
    public void nullFilter() {
    }

    @Override
    protected void doStartReturnStructure(boolean simple) {
        if (isRoot()) {
            q.addLine(trust("FOR result IN UNION(${fields})"));
            q.setTrustedParameter("fields", q.listFields(fields));
            q.addLine(trust("COLLECT field = result.field, type=result.type, isLink=result.isLink WITH COUNT INTO c"));
            q.addLine(trust("RETURN {"));
            q.addLine(trust("\"field\": field,"));
            q.addLine(trust("\"type\": type,"));
            q.addLine(trust("\"isLink\": isLink,"));
            q.addLine(trust("\"count\": c"));
            q.addLine(trust("})"));

            q.addLine(trust("LET merged = (FOR field IN r"));
            q.addLine(trust("COLLECT f = field.field, link = field.isLink INTO types"));
            q.addLine(trust("LET type = (FOR t IN types"));
            q.addLine(trust("RETURN {"));
            q.addLine(trust("\"schema\": t.field.type,"));
            q.addLine(trust("\"count\": t.field.count"));
            q.addLine(trust("})"));
            q.addLine(trust("RETURN { [f]: {"));
            q.addLine(trust("\"isLink\": link,"));
            q.addLine(trust("\"types\": type"));
            q.addLine(trust("}})"));
            q.addLine(trust("RETURN MERGE(merged)"));

        }


    }

    @Override
    public void endReturnStructure() {

    }

    @Override
    protected void doLeaveTraversal() {
    }

    @Override
    public void buildGrouping(String groupedInstancesLabel, List<ArangoAlias> groupingFields, List<ArangoAlias> nonGroupingFields) {
    }


    @Override
    public ArangoMetaReflectionQueryBuilder addRoot(ArangoCollectionReference rootCollection) {
        q.addLine(trust(""));
        q.addLine(trust("//*****************************"));
        q.addLine(trust("//add root"));
        q.addLine(trust("//*****************************"));
        q.addLine(trust(""));
        q.addLine(new AuthorizedArangoQuery(whitelistOrganizations, true)
                .addLine(trust("LET r = (FOR ${alias} IN `${collection}`")).
                        addDocumentFilter(q.preventAqlInjection(String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX))).setParameter("alias", String.format("%s_%s", ROOT_ALIAS.getArangoName(), DOC_POSTFIX)).setParameter("collection", rootCollection.getName()).build());

        return this;
    }

    @Override
    public void addLimit() {
    }

    @Override
    public void addTraversalResultField(String targetName, ArangoAlias alias) {
    }

    @Override
    public void addSortByLeafField(Set<ArangoAlias> fields) {
    }

    @Override
    public void ensureOrder() {
    }

    private boolean isLink(ArangoAlias leafField) {
        return existingArangoCollections.contains(ArangoCollectionReference.fromFieldName(leafField.getOriginalName()));
    }

    @Override
    public void addComplexLeafResultField(String targetName, ArangoAlias leafField) {

    }

    @Override
    public void addSimpleLeafResultField(ArangoAlias leafField) {

    }

    @Override
    public void addMerge(ArangoAlias leafField, Set<ArangoAlias> mergedFields, boolean sorted) {

    }

    @Override
    public void addInstanceIdFilter() {
    }

    @Override
    public void addSearchQuery() {
    }

    @Override
    public void addFieldFilter(ArangoAlias alias) {
    }

}
