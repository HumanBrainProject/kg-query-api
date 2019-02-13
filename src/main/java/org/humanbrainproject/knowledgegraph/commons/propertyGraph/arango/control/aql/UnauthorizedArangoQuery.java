package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql;

import org.apache.commons.text.StringSubstitutor;
import org.humanbrainproject.knowledgegraph.annotations.NoTests;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;

import java.util.*;
import java.util.stream.Collectors;

@ToBeTested(easy = true)
public class UnauthorizedArangoQuery {

    final Map<String, String> parameters = new LinkedHashMap<>();
    private StringBuilder query = new StringBuilder();
    private int indent = 0;

    @NoTests(NoTests.TRIVIAL)
    public UnauthorizedArangoQuery indent() {
        this.indent++;
        return this;
    }

    @NoTests(NoTests.TRIVIAL)
    public UnauthorizedArangoQuery outdent() {
        this.indent = Math.max(this.indent - 1, 0);
        return this;
    }

    public UnauthorizedArangoQuery setParameter(String key, String value) {
        setTrustedParameter(key, preventAqlInjection(value));
        return this;
    }

    /**
     * Use with caution! The passed parameter will not be further checked for AQL injection but rather immediately added to the query!
     */
    public UnauthorizedArangoQuery setTrustedParameter(String key, TrustedAqlValue trustedAqlValue) {
        parameters.put(key, trustedAqlValue!=null ? trustedAqlValue.getValue() : null);
        return this;
    }

    @NoTests(NoTests.TRIVIAL)
    public UnauthorizedArangoQuery addLine(String queryLine) {
        query.append(createIndent()).append(queryLine).append('\n');
        return this;
    }

    public UnauthorizedArangoQuery addDocumentFilter(TrustedAqlValue documentAlias){
        addLine("FILTER "+documentAlias.getValue()+" != NULL");
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

    public List<String> generateSearchTermParameter(String value){
        return Arrays.asList(value.split("\\s+")).stream().map(s -> "%" + s.toLowerCase() + "%").collect(Collectors.toList());
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


}
