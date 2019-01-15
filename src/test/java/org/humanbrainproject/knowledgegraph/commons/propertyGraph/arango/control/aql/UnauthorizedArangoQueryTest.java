package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class UnauthorizedArangoQueryTest {

    UnauthorizedArangoQuery q;

    @Before
    public void setup(){
        q = new UnauthorizedArangoQuery();
    }

    @Test
    public void preventAqlInjectionRemoveTicks() {
        String original = "`foobar`";
        String result = q.preventAqlInjection(original).getValue();
        assertEquals("foobar", result);
    }

    @Test
    public void preventAqlInjectionRemoveQuotes() {
        String original = "\"foobar\"";
        String result = q.preventAqlInjection(original).getValue();
        assertEquals("foobar", result);
    }

    @Test
    public void preventAqlInjectionRemoveSingleQuotes() {
        String original = "\'foobar\'";
        String result = q.preventAqlInjection(original).getValue();
        assertEquals("foobar", result);
    }

    @Test
    public void preventAqlInjectionRemovePercentage() {
        String original = "foobar%";
        String result = q.preventAqlInjection(original).getValue();
        assertEquals("foobar", result);
    }


    @Test
    public void preventAqlInjectionRemoveOperators() {
        String original = "foobar!~=>[]+\\";
        String result = q.preventAqlInjection(original).getValue();
        assertEquals("foobar", result);
    }

    @Test
    public void preventAqlInjectionAllowSemantics() {
        String original = "http://foo.bar/foobar#bar";
        String result = q.preventAqlInjection(original).getValue();
        assertEquals("http://foo.bar/foobar#bar", result);
    }

    @Test
    public void setParameter(){
        q.setParameter("foo", "foobar!~=>[]+\\");
        String foo = q.parameters.get("foo");
        assertEquals("foobar", foo);
    }

    @Test
    public void setTrustedParameter(){
        q.setTrustedParameter("foo", new TrustedAqlValue("foobar!~=>[]+\\"));
        String foo = q.parameters.get("foo");
        assertEquals("foobar!~=>[]+\\", foo);
    }


}