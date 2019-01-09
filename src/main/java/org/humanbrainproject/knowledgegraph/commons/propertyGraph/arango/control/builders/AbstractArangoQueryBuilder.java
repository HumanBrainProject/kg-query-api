package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.builders;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql.AuthorizedArangoQuery;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoCollectionReference;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoDocumentReference;
import org.humanbrainproject.knowledgegraph.query.entity.Filter;
import org.humanbrainproject.knowledgegraph.query.entity.Pagination;
import org.humanbrainproject.knowledgegraph.query.entity.SpecField;
import org.humanbrainproject.knowledgegraph.query.entity.Specification;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Stack;

@ToBeTested
public abstract class AbstractArangoQueryBuilder {
    protected static String DOC_POSTFIX = "doc";
    protected static ArangoAlias ROOT_ALIAS = new ArangoAlias("root");
    protected Pagination pagination;
    protected Filter filter;
    protected Specification specification;
    protected ArangoAlias permissionGroupFieldName;
    protected AuthorizedArangoQuery q;
    protected Stack<ArangoAlias> previousAlias = new Stack<>();
    protected ArangoAlias currentAlias = ROOT_ALIAS;
    protected boolean simpleReturn = true;
    protected boolean firstReturnEntry = true;
    protected Set<String> whitelistOrganizations;
    protected SpecField currentField;
    protected Set<ArangoDocumentReference> documentReferences;
    protected final Set<ArangoCollectionReference> existingArangoCollections;


    public void setCurrentField(SpecField currentField) {
        this.currentField = currentField;
    }

    public AbstractArangoQueryBuilder(Specification specification, Pagination pagination, Filter filter, ArangoAlias permissionGroupFieldName, Set<String> whitelistOrganizations, Set<ArangoDocumentReference> documentReferences, Set<ArangoCollectionReference> existingArangoCollections) {
        this.pagination = pagination;
        this.filter = filter;
        this.specification = specification;
        this.permissionGroupFieldName = permissionGroupFieldName;
        this.q = new AuthorizedArangoQuery(whitelistOrganizations);
        this.whitelistOrganizations = whitelistOrganizations;
        this.documentReferences = documentReferences;
        this.existingArangoCollections = existingArangoCollections!=null ? Collections.unmodifiableSet(existingArangoCollections) : null;
    }

    public Set<ArangoCollectionReference> getExistingArangoCollections() {
        return existingArangoCollections;
    }

    protected boolean isRoot(){
        return previousAlias.empty();
    }

    public String build() {
        return q.build().getValue();
    }

    public final void addAlias(ArangoAlias targetName){
        previousAlias.push(currentAlias);
        currentAlias = targetName;
    }

    public final void enterTraversal(ArangoAlias targetField, int numberOfTraversals, boolean reverse, ArangoCollectionReference relationCollection, boolean hasGroup, boolean ensureOrder){
        doEnterTraversal(targetField, numberOfTraversals, reverse, relationCollection, hasGroup, ensureOrder);
    }

    protected abstract void doEnterTraversal(ArangoAlias targetField, int numberOfTraversals, boolean reverse, ArangoCollectionReference relationCollection, boolean hasGroup, boolean ensureOrder);

    protected String getIndentation() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < previousAlias.size(); i++) {
            sb.append("  ");
        }
        return sb.toString();
    }

    public abstract void addTraversal(boolean reverse, ArangoCollectionReference relationCollection, int traversalDepth);
    public abstract void leaveAdditionalTraversal(boolean reverse, ArangoCollectionReference relationCollection, int traversalDepth, boolean leaf);


    public abstract void addComplexFieldRequiredFilter(ArangoAlias leafField);

    public abstract void addTraversalFieldRequiredFilter(ArangoAlias field);

    protected abstract void doStartReturnStructure(boolean simple);

    public final void startReturnStructure(boolean simple){
        doStartReturnStructure(simple);
        simpleReturn = simple;
    }

    public abstract void endReturnStructure();

    public final void leaveTraversal(){
        doLeaveTraversal();
    }

    public final void dropAlias(){
        currentAlias = previousAlias.pop();
    }

    protected abstract void doLeaveTraversal();

    public abstract void buildGrouping(String groupedInstancesLabel, List<ArangoAlias> groupingFields, List<ArangoAlias> nonGroupingFields);

    public abstract AbstractArangoQueryBuilder addRoot(ArangoCollectionReference rootCollection) throws JSONException;

    public void addOrganizationFilter() {
        q.addDocumentFilter(q.preventAqlInjection(String.format("%s_%s", currentAlias.getArangoName(), DOC_POSTFIX)));
    }

    public abstract void addLimit();

    public abstract void addTraversalResultField(String targetName, ArangoAlias alias);

    public abstract void addSortByLeafField(Set<ArangoAlias> field);

    public abstract void ensureOrder();

    public abstract void nullFilter();

    public abstract void addComplexLeafResultField(String targetName, ArangoAlias leafField);

    public abstract void addSimpleLeafResultField(ArangoAlias leafField);

    public abstract void addMerge(ArangoAlias leafField, Set<ArangoAlias> mergedField, boolean sorted);

    public Specification getSpecification() {
        return specification;
    }

    public abstract void addInstanceIdFilter();

    public abstract void addSearchQuery();

    public abstract void prepareLeafField(SpecField leafField);

    public SpecField getCurrentField() {
        return currentField;
    }

    public Pagination getPagination() {
        return pagination;
    }
}
