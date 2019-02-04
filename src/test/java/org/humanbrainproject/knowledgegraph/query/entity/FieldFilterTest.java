package org.humanbrainproject.knowledgegraph.query.entity;

import org.apache.commons.math3.analysis.function.Exp;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.FieldFilter;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.Op;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.Value;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class FieldFilterTest {
    @Test
    public void parseMapToFilter() {
        Map<String, Object> document = new HashMap<>();
        document.put("op", "equals");
        document.put("value", "foo");
        FieldFilter f = FieldFilter.fromMap(document, null);
        Assert.assertEquals(f.getOp(), Op.EQUALS);
        Assert.assertEquals(f.getExp(), new Value("foo"));
    }

    @Test
    public void parseNestedMapToFilter() {
        Map<String, Object> document = new HashMap<>();
        Map<String, Object> nested = new HashMap<>();
        nested.put(GraphQueryKeys.GRAPH_QUERY_FILTER_OP.getFieldName(), "equals");
        nested.put(GraphQueryKeys.GRAPH_QUERY_FILTER_VALUE.getFieldName(), "bar");
        document.put(GraphQueryKeys.GRAPH_QUERY_FILTER_OP.getFieldName(), "equals");
        document.put(GraphQueryKeys.GRAPH_QUERY_FILTER_VALUE.getFieldName(), nested);
        FieldFilter f = FieldFilter.fromMap(document, null);
        FieldFilter e = (FieldFilter) f.getExp();
        Assert.assertEquals(f.getOp(), Op.EQUALS);
        Assert.assertEquals(e.getOp(), Op.EQUALS);
        Assert.assertEquals(e.getExp(), new Value("bar"));
    }

    @Test
    public void parsedMapToFilterWithParams() {
        Map<String, Object> document = new HashMap<>();
        Map<String, String> params = new HashMap<>();
        params.put("myvalue", "bar");
        document.put(GraphQueryKeys.GRAPH_QUERY_FILTER_OP.getFieldName(), "equals");
        document.put(GraphQueryKeys.GRAPH_QUERY_FILTER_VALUE.getFieldName(), "${myValue}");
        FieldFilter f = FieldFilter.fromMap(document, params);
        Assert.assertEquals(f.getOp(), Op.EQUALS);
        Assert.assertEquals(f.getExp(), new Value("bar"));
    }
}
