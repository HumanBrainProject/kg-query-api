package org.humanbrainproject.knowledgegraph.control.template;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.humanbrainproject.knowledgegraph.entity.query.QueryResult;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.*;

import static org.junit.Assert.*;

public class FreemarkerTemplatingTest {

    FreemarkerTemplating templating = new FreemarkerTemplating();

    private static final String EXTRACT_PATH = "[<#list path as first_level>[" +
            "<#list first_level as second_level>" +
            "${second_level}" +
            "<#sep>,</#list>" +
            "]<#sep>,</#list>]";

    @Test
    public void normalizeLookupPathSingleString() {
        String template = "<#include \"searchuinew\">\n<#assign path = normalizeLookupPath(\"hello:world\")/>"+EXTRACT_PATH;
        QueryResult<List<Map>> queryResult = new QueryResult<>();
        queryResult.setResults(Collections.singletonList(Collections.EMPTY_MAP));
        String s = templating.applyTemplate(template, queryResult, Collections.emptyList());
        assertEquals("[[hello_world]]", s);
    }

    @Test
    public void normalizeLookupPathArray() {
        String template = "<#include \"searchuinew\">\n<#assign path = normalizeLookupPath([\"hello:world\", \"foo:bar\"])/>"+EXTRACT_PATH;
        QueryResult<List<Map>> queryResult = new QueryResult<>();
        queryResult.setResults(Collections.singletonList(Collections.EMPTY_MAP));
        String s = templating.applyTemplate(template, queryResult, Collections.emptyList());
        assertEquals("[[hello_world],[foo_bar]]", s);
    }

    @Test
    public void normalizeLookupPathArray2() {
        String template = "<#include \"searchuinew\">\n<#assign path = normalizeLookupPath([[\"hello:world\", \"foo:bar\"]])/>"+EXTRACT_PATH;
        QueryResult<List<Map>> queryResult = new QueryResult<>();
        queryResult.setResults(Collections.singletonList(Collections.EMPTY_MAP));
        String s = templating.applyTemplate(template, queryResult, Collections.emptyList());
        assertEquals("[[hello_world,foo_bar]]", s);
    }

    @Test
    public void normalizeLookupPathFullDepth() {
        String template = "<#include \"searchuinew\">\n<#assign path = normalizeLookupPath([[\"hello:world\", \"bar:foo\"], [\"foo:bar\"]])/>"+EXTRACT_PATH;
        QueryResult<List<Map>> queryResult = new QueryResult<>();
        queryResult.setResults(Collections.singletonList(Collections.EMPTY_MAP));
        String s = templating.applyTemplate(template, queryResult, Collections.emptyList());
        assertEquals("[[hello_world,bar_foo],[foo_bar]]", s);
    }

    @Test
    public void normalizeLookupPathMixedDepth() {
        String template = "<#include \"searchuinew\">\n<#assign path = normalizeLookupPath([[\"hello:world\", \"bar:foo\"], \"foo:bar\"])/>"+EXTRACT_PATH;
        QueryResult<List<Map>> queryResult = new QueryResult<>();
        queryResult.setResults(Collections.singletonList(Collections.EMPTY_MAP));
        String s = templating.applyTemplate(template, queryResult, Collections.emptyList());
        assertEquals("[[hello_world,bar_foo],[foo_bar]]", s);
    }

    @Test
    public void flattenDistinct() {
        String template = "<#include \"searchuinew\">\n" +
                "<#list results as el>" +
                "<#assign path = [[\"hello:world\", \"bar:foo\"]]/>"+
                "<#assign flat = flatten(el path \"foo:bar\")/>"+
                "<#list flat as f>" +
                "${f} "+
                "</#list>" +
                "</#list>";
        List<Map> results = new ArrayList<>();
        QueryResult<List<Map>> queryResult = new QueryResult<>();

        Map<String, Object> mapA = new LinkedHashMap<>();
        Map<String, Object> helloWorldMapA = new LinkedHashMap<>();
        Map<String, Object> barFooMapA = new LinkedHashMap<>();
        mapA.put("hello_world", helloWorldMapA);
        helloWorldMapA.put("bar_foo", barFooMapA);
        barFooMapA.put("foo_bar", "foo");
        Map<String, Object> mapB = new LinkedHashMap<>();
        Map<String, Object> helloWorldMapB = new LinkedHashMap<>();
        Map<String, Object> barFooMapB = new LinkedHashMap<>();
        mapB.put("hello_world", helloWorldMapB);
        helloWorldMapB.put("bar_foo", barFooMapB);
        barFooMapB.put("foo_bar", "bar");
        Map<String, Object> mapC = new LinkedHashMap<>();
        Map<String, Object> helloWorldMapC = new LinkedHashMap<>();
        Map<String, Object> barFooMapC = new LinkedHashMap<>();
        mapC.put("hello_world", helloWorldMapC);
        helloWorldMapC.put("bar_foo", barFooMapC);
        barFooMapC.put("foo_bar", barFooMapA.get("foo_bar"));
        results.add(mapA);
        results.add(mapB);
        results.add(mapC);

        queryResult.setResults(results);
        String s = templating.applyTemplate(template, queryResult, Collections.emptyList());
        assertEquals("foo bar", s);
    }



}