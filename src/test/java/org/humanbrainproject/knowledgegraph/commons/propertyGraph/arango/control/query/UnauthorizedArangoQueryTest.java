package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.query;

import org.junit.Assert;
import org.junit.Test;

public class UnauthorizedArangoQueryTest {

    UnauthorizedArangoQuery q = new UnauthorizedArangoQuery();

    @Test
    public void preventAqlInjectionRemoveTicks() {
        String original = "`foobar`";
        String result = q.preventAqlInjection(original).getValue();
        Assert.assertEquals("foobar", result);
    }

    @Test
    public void preventAqlInjectionRemoveQuotes() {
        String original = "\"foobar\"";
        String result = q.preventAqlInjection(original).getValue();
        Assert.assertEquals("foobar", result);
    }

    @Test
    public void preventAqlInjectionRemoveSingleQuotes() {
        String original = "\'foobar\'";
        String result = q.preventAqlInjection(original).getValue();
        Assert.assertEquals("foobar", result);
    }

    @Test
    public void preventAqlInjectionRemovePercentage() {
        String original = "foobar%";
        String result = q.preventAqlInjection(original).getValue();
        Assert.assertEquals("foobar", result);
    }


    @Test
    public void preventAqlInjectionRemoveOperators() {
        String original = "foobar!~=>[]+\\";
        String result = q.preventAqlInjection(original).getValue();
        Assert.assertEquals("foobar", result);
    }

    @Test
    public void preventAqlInjectionAllowSemantics() {
        String original = "http://foo.bar/foobar#bar";
        String result = q.preventAqlInjection(original).getValue();
        Assert.assertEquals("http://foo.bar/foobar#bar", result);

    }
}