package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql;

import org.apache.commons.text.StringSubstitutor;
import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity.ArangoAlias;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.ArangoVocabulary;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ToBeTested(easy = true)
public class AQL {

    public final String WHITELIST_ALIAS = "whitelist";

    final Map<String, String> parameters = new LinkedHashMap<>();
    private StringBuilder query = new StringBuilder();
    private int indent = 0;

    public final static TrustedAqlValue trust(String trustedString){
        return new TrustedAqlValue(trustedString);
    }


    @NoTests(NoTests.TRIVIAL)
    public AQL indent() {
        this.indent++;
        return this;
    }

    @NoTests(NoTests.TRIVIAL)
    public AQL outdent() {
        this.indent = Math.max(this.indent - 1, 0);
        return this;
    }

    public AQL setParameter(String key, String value) {
        setTrustedParameter(key, preventAqlInjection(value));
        return this;
    }

    /**
     * Use with caution! The passed parameter will not be further checked for AQL injection but rather immediately added to the query!
     */
    public AQL setTrustedParameter(String key, TrustedAqlValue trustedAqlValue) {
        parameters.put(key, trustedAqlValue!=null ? trustedAqlValue.getValue() : null);
        return this;
    }

    @NoTests(NoTests.TRIVIAL)
    public AQL addLine(TrustedAqlValue queryLine) {
        if(queryLine!=null) {
            query.append(createIndent());
            add(queryLine);
            query.append('\n');
        }
        return this;
    }

    public AQL add(TrustedAqlValue trustedAqlValue){
        if(trustedAqlValue!=null) {
            query.append(trustedAqlValue.getValue());
        }
        return this;
    }

    public AQL addComma(){
        query.append(", ");
        return this;
    }


    public TrustedAqlValue listCollections(Set<String> values) {
        return listValuesWithQuote('`', values);
    }

    public TrustedAqlValue listValues(Set<String> values) {
        return listValuesWithQuote('"', values);
    }

    public TrustedAqlValue listFields(Set<String> values){
        return listValuesWithQuote(null, values);
    }


    private TrustedAqlValue listValuesWithQuote(Character quote, Set<String> values) {
        if(values!=null && values.size()>0){
            String q = quote!=null ? String.valueOf(quote) : "";
            return new TrustedAqlValue(q+String.join(q + "," + q, values.stream().map(v -> preventAqlInjection(v).getValue()).collect(Collectors.toSet()))+q);
        }
        else{
            return new TrustedAqlValue("");
        }
    }

    public TrustedAqlValue preventAqlInjection(String value){
        return value!=null ? new TrustedAqlValue(value.replaceAll("[^A-Za-z0-9\\-_:.#/@]", "")) : null;
    }

    public TrustedAqlValue preventAqlInjectionForSearchQuery(String value){
        String f = value.replaceAll("[^\\sA-Za-z0-9\\-_:.#/@]", "");
        return new TrustedAqlValue(f);
    }

    public TrustedAqlValue generateSearchTermQuery(TrustedAqlValue value){
        String f = String.join(" ", Arrays.stream(value.getValue().split(" ")).map(el -> String.format("%%%s%%", el.trim().toLowerCase())).collect(Collectors.toList()));
        if(f.isEmpty()){
            f = "%";
        }
        return new TrustedAqlValue(f);
    }


    public TrustedAqlValue build() {
        return new TrustedAqlValue(StringSubstitutor.replace(query.toString(), parameters));
    }

    private String createIndent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.indent; i++) {
            sb.append("   ");
        }
        return sb.toString();
    }

    public AQL addDocumentFilter(TrustedAqlValue documentAlias) {
        addLine(new TrustedAqlValue("FILTER "+documentAlias.getValue()+" != NULL"));
        return this;
    }

    public AQL addDocumentFilter(ArangoAlias alias){
        return addDocumentFilter(preventAqlInjection(alias.getArangoDocName()));
    }
    public AQL addDocumentFilterWithWhitelistFilter(ArangoAlias alias) {
        return addDocumentFilterWithWhitelistFilter(preventAqlInjection(alias.getArangoDocName()));
    }


    public AQL addDocumentFilterWithWhitelistFilter(TrustedAqlValue documentAlias) {
        addDocumentFilter(documentAlias);
        addLine(trust("FILTER "+documentAlias.getValue()+"."+ ArangoVocabulary.PERMISSION_GROUP+" IN "+WHITELIST_ALIAS));
        return this;
    }
}
