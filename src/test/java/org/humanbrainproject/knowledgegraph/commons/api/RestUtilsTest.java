package org.humanbrainproject.knowledgegraph.commons.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.humanbrainproject.knowledgegraph.query.entity.QueryResult;
import org.junit.Assert;
import org.junit.Test;

public class RestUtilsTest {

    @Test
    public void splitCommaSeparatedValues() {
        String[] strings = RestUtils.splitCommaSeparatedValues("foo, bar");
        Assert.assertEquals(2, strings.length);
        Assert.assertEquals("foo", strings[0]);
        Assert.assertEquals("bar", strings[1]);
    }

    @Test
    public void splitCommaSeparatedValuesNull() {
        String[] strings = RestUtils.splitCommaSeparatedValues(null);
        Assert.assertNull(strings);
    }

    @Test
    public void toJsonResultIfPossible() {
        QueryResult<String> stringResult = new QueryResult<>();
        stringResult.setResults("{\"foo\":\"bar\"}");

        QueryResult queryResult = RestUtils.toJsonResultIfPossible(stringResult);
        Assert.assertTrue(queryResult.getResults() instanceof ObjectNode);
        ObjectNode node = (ObjectNode)queryResult.getResults();
        Assert.assertEquals("bar", node.get("foo").textValue());

    }

    @Test
    public void toJsonResultIfPossibleNoString() {
        QueryResult<Integer> integerResult = new QueryResult<>();
        integerResult.setResults(10);

        QueryResult queryResult = RestUtils.toJsonResultIfPossible(integerResult);
        Assert.assertEquals(10, queryResult.getResults());
    }

    @Test
    public void toJsonResultIfPossibleNull() {
        QueryResult queryResult = RestUtils.toJsonResultIfPossible(null);
        Assert.assertNull(queryResult);

    }
}