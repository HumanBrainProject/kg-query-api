/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

package org.humanbrainproject.knowledgegraph.commons.jsonld.control;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class JsonTransformerTest {

    JsonTransformer transformer;

    @Before
    public void setUp() throws Exception {
        this.transformer = new JsonTransformer();
    }

    @Test
    public void getMapAsJson() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("foo", "bar");
        String mapAsJson = transformer.getMapAsJson(map);
        assertEquals("{\"foo\":\"bar\"}", mapAsJson);
    }

    @Test
    public void parseToMap() {
        String s = "{\"foo\":\"bar\"}";
        Map map = transformer.parseToMap(s);
        assertEquals("bar", map.get("foo"));
    }

    @Test
    public void parseToMapFromList() {
        String s = "[{\"foo\":\"bar\"}]";
        Map map = transformer.parseToMap(s);
        assertEquals("bar", map.get("foo"));
    }

    @Test
    public void parseToMapFromListWithMultipleEntries() {
        String s = "[{\"foo\":\"bar\"}, {\"bar\":\"foo\"} ]";
        Map map = transformer.parseToMap(s);
        assertEquals("bar", map.get("foo"));
    }


    @Test
    public void parseToMapFromNull() {
        Map map = transformer.parseToMap(null);
        assertNull(map);
    }


    @Test
    public void parseToMapFromListString() {
        Map map = transformer.parseToMap("\"foo\"");
        assertNull(map);
    }

    @Test
    public void parseToListOfStrings() {
        List<String> strings = transformer.parseToListOfStrings("[\"foo\", \"bar\"]");
        assertEquals(2, strings.size());
        assertEquals("foo", strings.get(0));
        assertEquals("bar", strings.get(1));
    }

    @Test
    public void parseToListOfStringsInvalidStructure() {
        List<String> strings = transformer.parseToListOfStrings("\"foo\"");
        assertNull(strings);
    }

    @Test
    public void parseToListOfMaps() {
        List<Map> maps = transformer.parseToListOfMaps("[{\"foo\": \"bar\"}, {\"bar\": \"foo\"}]");
        assertEquals(2, maps.size());
    }

    @Test
    public void normalize() {
        String uglyJson = "{'foo':\"bar\"\n,'bar': \"foo\"  \t,'foobar':null}";
        String normalized = transformer.normalize(uglyJson);
        assertEquals("{\"foo\":\"bar\",\"bar\":\"foo\"}", normalized);
    }
}