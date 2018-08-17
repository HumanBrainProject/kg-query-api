package org.humanbrainproject.knowledgegraph.control.arango.query;

import org.humanbrainproject.knowledgegraph.entity.specification.SpecField;
import org.humanbrainproject.knowledgegraph.entity.specification.Specification;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.util.List;
import java.util.Set;
import java.util.Stack;

public abstract class AbstractQueryBuilder {
    protected static String DOC_POSTFIX = "doc";
    protected static String ROOT_ALIAS = "root";
    protected Integer size;
    protected Integer start;
    protected Specification specification;
    protected String permissionGroupFieldName;
    protected StringBuilder sb = new StringBuilder();
    protected Stack<String> previousAlias = new Stack<>();
    protected String currentAlias = ROOT_ALIAS;
    protected boolean simpleReturn = true;
    protected boolean firstReturnEntry = true;
    protected Set<String> whitelistOrganizations;
    protected SpecField currentField;

    public void setCurrentField(SpecField currentField) {
        this.currentField = currentField;
    }

    private void addWhitelistOrganizations(){
        JSONArray array = new JSONArray();
        if(whitelistOrganizations!=null) {
            for (String whiteListOrganization : whitelistOrganizations) {
                array.put(whiteListOrganization);
            }
            sb.append(String.format("LET whitelist_organizations=%s\n", array.toString()));
        }
    }

    public AbstractQueryBuilder(Specification specification, Integer size, Integer start, String permissionGroupFieldName, Set<String> whitelistOrganizations) {
        this.size = size;
        this.start = start;
        this.specification = specification;
        this.permissionGroupFieldName = permissionGroupFieldName;
        this.whitelistOrganizations = whitelistOrganizations;
        addWhitelistOrganizations();
    }

    protected boolean isRoot(){
        return previousAlias.empty();
    }

    public String build() {
        return sb.toString();
    }

    public final void addAlias(String targetName){
        previousAlias.push(currentAlias);
        currentAlias = targetName;
    }

    public final void enterTraversal(String targetName, int numberOfTraversals, boolean reverse, String relationCollection, boolean hasGroup, boolean ensureOrder){
        doEnterTraversal(targetName, numberOfTraversals, reverse, relationCollection, hasGroup, ensureOrder);
    }

    protected abstract void doEnterTraversal(String targetName, int numberOfTraversals, boolean reverse, String relationCollection, boolean hasGroup, boolean ensureOrder);

    protected String getIndentation() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < previousAlias.size(); i++) {
            sb.append("  ");
        }
        return sb.toString();
    }

    public abstract void addTraversal(boolean reverse, String relationCollection);

    public abstract void addComplexFieldRequiredFilter(String leaf_field);

    public abstract void addTraversalFieldRequiredFilter(String alias);

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

    public abstract void buildGrouping(String groupedInstancesLabel, List<String> groupingFields, List<String> nonGroupingFields);

    public abstract AbstractQueryBuilder addRoot(String rootCollection) throws JSONException;

    public void addOrganizationFilter() {
        sb.append(String.format(" FILTER %s_%s.`%s` IN whitelist_organizations ", currentAlias, DOC_POSTFIX, permissionGroupFieldName));
    }

    public abstract void addLimit();

    public abstract void addTraversalResultField(String targetName, String alias);

    public abstract void addSortByLeafField(Set<String> fields);

    public abstract void ensureOrder();

    public abstract void addComplexLeafResultField(String targetName, String leaf_field);

    public abstract void addSimpleLeafResultField(String leaf_field);

    public abstract void addMerge(String leaf_field, Set<String> merged_fields, boolean sorted);

    public Specification getSpecification() {
        return specification;
    }
}
