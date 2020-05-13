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

package org.humanbrainproject.knowledgegraph.query.entity;

import org.humanbrainproject.knowledgegraph.query.control.SpecificationInterpreter;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.FieldFilter;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.Op;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.Parameter;
import org.humanbrainproject.knowledgegraph.query.entity.fieldFilter.Value;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FieldFilterTest {
    @Test
    public void parseMapToFilter() throws JSONException {
        Map<String, Object> document = new HashMap<>();
        document.put(GraphQueryKeys.GRAPH_QUERY_FILTER_OP.getFieldName(), "equals");
        document.put(GraphQueryKeys.GRAPH_QUERY_FILTER_VALUE.getFieldName(), "foo");
        FieldFilter fieldFilter = SpecificationInterpreter.createFieldFilter(new JSONObject(document));
        Assert.assertEquals(fieldFilter.getOp(), Op.EQUALS);
        Assert.assertEquals(fieldFilter.getValue(), new Value("foo"));
    }

//    @Test
//    public void parseNestedMapToFilter() {
//        Map<String, Object> document = new HashMap<>();
//        Map<String, Object> nested = new HashMap<>();
//        nested.put(GraphQueryKeys.GRAPH_QUERY_FILTER_OP.getFieldName(), "equals");
//        nested.put(GraphQueryKeys.GRAPH_QUERY_FILTER_VALUE.getFieldName(), "bar");
//        document.put(GraphQueryKeys.GRAPH_QUERY_FILTER_OP.getFieldName(), "equals");
//        document.put(GraphQueryKeys.GRAPH_QUERY_FILTER_VALUE.getFieldName(), nested);
//        FieldFilterLeaf f = FieldFilterLeaf.fromMap(document, null);
//        FieldFilterLeaf e = (FieldFilterLeaf) f.getExp();
//        Assert.assertEquals(f.getOp(), Op.EQUALS);
//        Assert.assertEquals(e.getOp(), Op.EQUALS);
//        Assert.assertEquals(e.getExp(), new Value("bar"));
//    }

    @Test
    public void parsedMapToFilterWithParams() throws JSONException {
        Map<String, Object> document = new HashMap<>();
        Map<String, String> params = new HashMap<>();
        params.put("myValue", "bar");
        document.put(GraphQueryKeys.GRAPH_QUERY_FILTER_OP.getFieldName(), "equals");
        document.put(GraphQueryKeys.GRAPH_QUERY_FILTER_PARAM.getFieldName(), "myValue");
        FieldFilter fieldFilter = SpecificationInterpreter.createFieldFilter(new JSONObject(document));

        Assert.assertEquals(fieldFilter.getOp(), Op.EQUALS);
        Assert.assertEquals(fieldFilter.getParameter(), new Parameter("myValue"));
    }
}
