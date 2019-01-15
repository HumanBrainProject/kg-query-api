package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AuthorizedArangoQuery;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.UnauthorizedArangoQuery;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.query.entity.SpecField;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

@ToBeTested
public class ArangoMetaReflectionQueryBuilder extends AbstractArangoQueryBuilder {

    private Stack<List<String>> aliasStack = new Stack<>();

    private final String nexusInstanceBase;

    private boolean hasReturned;

    private final Set<String> fields = new HashSet<>();


    @Override
    public void prepareLeafField(SpecField leafField) {
        q.addLine("//Prepare " + leafField.fieldName);

        ArangoAlias leaf = ArangoAlias.fromOriginalFieldName(leafField.getLeafPath().pathName);
        UnauthorizedArangoQuery subQuery = new UnauthorizedArangoQuery();
        if (isLink(leaf)) {
            subQuery.addLine("LET ${alias} = FLATTEN(");
            subQuery.addLine("FOR ${alias}_doc IN 1..1 ${reverse} ${previousAliasDoc} `${relation}`");
            subQuery.addLine("LET ${alias}_url = ${alias}_doc.`" + HBPVocabulary.RELATIVE_URL_OF_INTERNAL_LINK + "`");
            subQuery.addDocumentFilter(subQuery.preventAqlInjection(String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX)));
            subQuery.addLine("RETURN DISTINCT {");
            subQuery.addLine("\"field\": \"${fieldName}\",");
            subQuery.addLine("\"type\": LEFT(${alias}_url, FIND_LAST(${alias}_url, '/')),");
            subQuery.addLine("\"isLink\": true");
            subQuery.addLine("})");
            subQuery.setParameter("reverse", leafField.getLeafPath().reverse ? "INBOUND" : "OUTBOUND");
            subQuery.setParameter("relation", ArangoCollectionReference.fromFieldName(leaf.getOriginalName()).getName());

        } else {
            subQuery.addLine("LET ${alias}_val = ${previousAliasDoc}.`${leafFieldName}`");
            subQuery.addLine("LET ${alias} = ${alias}_val!=NULL && ${alias}_val!=[] && ${alias}_val!=\"\" ? [{");
            subQuery.addLine("\"field\": \"${fieldName}\",");
            subQuery.addLine("\"type\": TYPENAME(${alias}_val),");
            subQuery.addLine("\"isLink\": false");
            subQuery.addLine("}] : []");
            subQuery.setParameter("leafFieldName", leaf.getOriginalName());
        }
        subQuery.setParameter("alias", currentAlias.getArangoName());
        subQuery.setParameter("fieldName", currentAlias.getOriginalName());
        subQuery.setParameter("previousAliasDoc", String.format("%s_%s", previousAlias.isEmpty() ? ROOT_ALIAS : previousAlias.peek().getArangoName(), DOC_POSTFIX));
        fields.add(currentAlias.getArangoName());

        q.addLine(subQuery.build().getValue());
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
            q.addLine("FOR result IN UNION(${fields})");
            q.setTrustedParameter("fields", q.listFields(fields));
            q.addLine("COLLECT field = result.field, type=result.type, isLink=result.isLink WITH COUNT INTO c");
            q.addLine("RETURN {");
            q.addLine("\"field\": field,");
            q.addLine("\"type\": type,");
            q.addLine("\"isLink\": isLink,");
            q.addLine("\"count\": c");
            q.addLine("})");

            q.addLine("LET merged = (FOR field IN r");
            q.addLine("COLLECT f = field.field, link = field.isLink INTO types");
            q.addLine("LET type = (FOR t IN types");
            q.addLine("RETURN {");
            q.addLine("\"schema\": t.field.type,");
            q.addLine("\"count\": t.field.count");
            q.addLine("})");
            q.addLine("RETURN { [f]: {");
            q.addLine("\"isLink\": link,");
            q.addLine("\"types\": type");
            q.addLine("}})");
            q.addLine("RETURN MERGE(merged)");

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
        q.addLine("");
        q.addLine("//*****************************");
        q.addLine("//add root");
        q.addLine("//*****************************");
        q.addLine("");
        q.addLine(new AuthorizedArangoQuery(whitelistOrganizations, true)
                .addLine("LET r = (FOR ${alias} IN `${collection}`").
                        addDocumentFilter(q.preventAqlInjection(String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX))).setParameter("alias", String.format("%s_%s", ROOT_ALIAS.getArangoName(), DOC_POSTFIX)).setParameter("collection", rootCollection.getName()).build().getValue());

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

}
