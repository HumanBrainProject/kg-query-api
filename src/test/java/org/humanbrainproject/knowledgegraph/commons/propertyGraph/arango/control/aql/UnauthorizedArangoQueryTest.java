/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.aql;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class UnauthorizedArangoQueryTest {

    AQL q;

    @Before
    public void setup(){
        q = new AQL();
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