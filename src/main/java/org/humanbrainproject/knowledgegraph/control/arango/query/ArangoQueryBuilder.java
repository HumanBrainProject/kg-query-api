package org.humanbrainproject.knowledgegraph.control.arango.query;

import java.util.Stack;

public class ArangoQueryBuilder {
    private static String DOC_POSTFIX = "doc";
    private static String ROOT_ALIAS="root";
    private Stack<String> previousAlias = new Stack<>();
    private String currentAlias = ROOT_ALIAS;
    StringBuilder sb = new StringBuilder();
    private boolean simpleReturn = true;
    private boolean firstReturnEntry = true;

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

    public ArangoQueryBuilder addRoot(String rootCollection){
        sb.append(String.format("FOR %s_%s IN `%s`", ROOT_ALIAS, DOC_POSTFIX, rootCollection));
        return this;
    }


    public void addTraversalResultField(String targetName, String alias){
        if(!firstReturnEntry){
            sb.append(",\n");
        }
        sb.append(String.format("%s    \"%s\": %s", getIndentation(), targetName, alias));
        firstReturnEntry = false;
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
        }
        sb.append(String.format("\n%s  RETURN %s_%s.`%s`\n", getIndentation(), currentAlias, DOC_POSTFIX, leaf_field));
        firstReturnEntry = true;
    }

}
