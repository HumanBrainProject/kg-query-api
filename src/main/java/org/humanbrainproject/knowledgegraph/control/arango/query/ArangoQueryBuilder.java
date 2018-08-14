package org.humanbrainproject.knowledgegraph.control.arango.query;

import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

public class ArangoQueryBuilder {
    private static String DOC_POSTFIX = "doc";
    private static String ROOT_ALIAS="root";
    private Stack<String> previousAlias = new Stack<>();
    private String currentAlias = ROOT_ALIAS;
    StringBuilder sb = new StringBuilder();
    private boolean simpleReturn = true;
    private boolean firstReturnEntry = true;
    private Integer size;
    private Integer start;
    private String permissionGroupFieldName;

    public ArangoQueryBuilder(Integer size, Integer start, String permissionGroupFieldName) {
        this.size = size;
        this.start = start;
        this.permissionGroupFieldName = permissionGroupFieldName;
    }

    public String build(){
        return sb.toString();
    }

    public void enterTraversal(String targetName, int numberOfTraversals, boolean reverse, String relationCollection){
        previousAlias.push(currentAlias);
        currentAlias = targetName;
        sb.append(String.format("\n%sLET %s = ( FOR %s_%s IN %d..%d %s %s_%s `%s`", getIndentation(), currentAlias, currentAlias, DOC_POSTFIX, numberOfTraversals, numberOfTraversals, reverse? "INBOUND" : "OUTBOUND", previousAlias.peek(), DOC_POSTFIX, relationCollection));
    }

    private String getIndentation(){
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<previousAlias.size(); i++){
            sb.append("  ");
        }
        return sb.toString();
    }

    public void addTraversal(boolean reverse, String relationCollection){
        sb.append(String.format(", %s `%s`", reverse ? "INBOUND" : "OUTBOUND", relationCollection));
    }

    public void addComplexFieldRequiredFilter(String leaf_field){
        sb.append(String.format("\n%s AND %s_%s.`%s` != null ", getIndentation(), currentAlias, DOC_POSTFIX, leaf_field));
    }

    public void addTraversalFieldRequiredFilter(String alias){
        sb.append(String.format("\n%s AND %s != []", getIndentation(), alias));
    }



    public void startReturnStructure(boolean simple){
        sb.append(String.format("\n%s  RETURN %s", getIndentation(), simple ? "": "{\n"));
        simpleReturn = simple;
    }

    public void endReturnStructure(){
        if(!simpleReturn){
            sb.append(String.format("\n%s  }", getIndentation()));
        }
        simpleReturn = true;
        firstReturnEntry = true;
    }

    public void leaveTraversal(){
        sb.append(")\n");
        currentAlias = previousAlias.pop();
    }

    public ArangoQueryBuilder addRoot(String rootCollection, Set<String> whiteListOrganizations) throws JSONException {
        JSONArray array = new JSONArray();
        for (String whiteListOrganization : whiteListOrganizations) {
            array.put(whiteListOrganization);
        }
        sb.append(String.format("LET whitelist_organizations=%s\n", array.toString()));
        sb.append(String.format("FOR %s_%s IN `%s`\n", ROOT_ALIAS, DOC_POSTFIX, rootCollection));
        addFilter();
        return this;
    }

    public void addFilter() {
        sb.append(String.format(" FILTER %s_%s.`%s` IN whitelist_organizations ", currentAlias, DOC_POSTFIX, permissionGroupFieldName));
    }

    public void addLimit(){
        if(size!=null){
            if(start!=null){
                sb.append(String.format("\nLIMIT %d, %d\n", start, size));
            }
            else{
                sb.append(String.format("\nLIMIT %d\n", size));
            }
        }
    }

    public void addTraversalResultField(String targetName, String alias){
        if(!firstReturnEntry){
            sb.append(",\n");
        }
        sb.append(String.format("%s    \"%s\": %s", getIndentation(), targetName, alias));
        firstReturnEntry = false;
    }

    public void addSortByLeafField(Set<String> fields){
        List<String> fullSortFields = fields.stream().map(s -> String.format("%s_%s.`%s`", currentAlias, DOC_POSTFIX, s)).collect(Collectors.toList());
        String concat = String.join(", ", fullSortFields);
        sb.append(String.format("%s   SORT %s ASC\n", getIndentation(), concat));
    }

    public void addComplexLeafResultField(String targetName, String leaf_field){
        if(!firstReturnEntry){
            sb.append(",\n");
        }
        sb.append(String.format("%s    \"%s\": %s_%s.`%s`", getIndentation(), targetName, currentAlias, DOC_POSTFIX, leaf_field));
        firstReturnEntry = false;
    }

    public void addSimpleLeafResultField(String leaf_field){
        if(!firstReturnEntry){
            sb.append(",\n");
            addFilter();
        }
        sb.append(String.format("\n%s  RETURN DISTINCT %s_%s.`%s`\n", getIndentation(), currentAlias, DOC_POSTFIX, leaf_field));
        firstReturnEntry = true;
    }

}
